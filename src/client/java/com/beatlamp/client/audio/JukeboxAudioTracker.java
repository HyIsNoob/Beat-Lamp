package com.beatlamp.client.audio;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.beatlamp.BeatLamp;
import com.beatlamp.client.BeatLampClientConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import javax.sound.sampled.AudioFormat;

public final class JukeboxAudioTracker {
	private static final double AUDIBLE_RADIUS = 64.0;
	private static final int MAX_CONSECUTIVE_ERRORS = 64;
	private static final Map<BlockPos, ActiveSong> ACTIVE_SONGS = new ConcurrentHashMap<>();

	private JukeboxAudioTracker() {
	}

	private static final class ActiveSong {
		final Vec3 position;
		final AudioAnalyzer analyzer;
		Thread thread;
		volatile boolean running = true;
		volatile float beatPulse;

		ActiveSong(Vec3 position, AudioAnalyzer analyzer, Thread thread) {
			this.position = position;
			this.analyzer = analyzer;
			this.thread = thread;
		}
	}

	public static void onSoundPlayed(SoundInstance soundInstance) {
		if (soundInstance.getSource() != SoundSource.RECORDS) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		SoundManager soundManager = minecraft.getSoundManager();
		WeighedSoundEvents weighedSoundEvents = soundManager.getSoundEvent(soundInstance.getLocation());
		if (weighedSoundEvents == null) {
			return;
		}

		Sound sound = weighedSoundEvents.getSound(RandomSource.create());
		int guard = 0;
		while (sound != null && sound.getType() == Sound.Type.SOUND_EVENT && guard++ < 8) {
			WeighedSoundEvents next = soundManager.getSoundEvent(sound.getLocation());
			if (next == null) {
				break;
			}

			sound = next.getSound(RandomSource.create());
		}

		if (sound == null || sound == SoundManager.EMPTY_SOUND) {
			return;
		}

		startSong(BlockPos.containing(soundInstance.getX(), soundInstance.getY(), soundInstance.getZ()), sound.getPath(), minecraft);
	}

	private static void startSong(BlockPos blockPos, ResourceLocation path, Minecraft minecraft) {
		stopSong(blockPos);

		try {
			InputStream inputStream = minecraft.getResourceManager().open(path);
			JOrbisAudioStream audioStream = new JOrbisAudioStream(inputStream);
			AudioFormat format = audioStream.getFormat();
			AudioAnalyzer analyzer = new AudioAnalyzer((int) format.getSampleRate(), BeatLampClientConfig.highQualityBeat);
			int channels = format.getChannels();
			BeatLamp.LOGGER.info("Beat Lamp tracking jukebox at {} -> {} ({}Hz, {}ch)", blockPos.toShortString(), path, format.getSampleRate(), channels);

			ActiveSong song = new ActiveSong(Vec3.atCenterOf(blockPos), analyzer, null);
			Thread thread = new Thread(() -> {
				decodeLoop(minecraft, audioStream, analyzer, channels, song);
				if (song.running) {
					ACTIVE_SONGS.remove(blockPos, song);
					BeatLamp.LOGGER.info("Beat Lamp finished tracking jukebox at {}", blockPos.toShortString());
				}
			}, "BeatLamp-Decode-" + blockPos.toShortString());
			song.thread = thread;
			thread.setDaemon(true);
			ACTIVE_SONGS.put(blockPos, song);
			thread.start();
		} catch (Exception exception) {
			BeatLamp.LOGGER.warn("Beat Lamp failed to decode jukebox audio {}", path, exception);
		}
	}

	public static void onSoundStopped(SoundInstance soundInstance) {
		if (soundInstance.getSource() != SoundSource.RECORDS) {
			return;
		}

		stopSong(BlockPos.containing(soundInstance.getX(), soundInstance.getY(), soundInstance.getZ()));
	}

	private static void stopSong(BlockPos blockPos) {
		ActiveSong song = ACTIVE_SONGS.remove(blockPos);
		if (song != null) {
			song.running = false;
			if (song.thread != null) {
				song.thread.interrupt();
			}
		}
	}

	public static void clear() {
		ACTIVE_SONGS.values().forEach(song -> {
			song.running = false;
			if (song.thread != null) {
				song.thread.interrupt();
			}
		});
		ACTIVE_SONGS.clear();
	}

	private static void decodeLoop(Minecraft minecraft, JOrbisAudioStream audioStream, AudioAnalyzer analyzer, int channels, ActiveSong song) {
		float[] raw = new float[16384];
		float[] mono = new float[16384];
		long startNanos = System.nanoTime();
		long pauseStartNanos = 0L;
		boolean wasPaused = false;
		int consecutiveErrors = 0;

		try {
			while (song.running && !Thread.currentThread().isInterrupted()) {
				if (minecraft.isPaused()) {
					if (!wasPaused) {
						pauseStartNanos = System.nanoTime();
						wasPaused = true;
					}

					try {
						Thread.sleep(50L);
					} catch (InterruptedException interruptedException) {
						Thread.currentThread().interrupt();
						break;
					}

					continue;
				}

				if (wasPaused) {
					startNanos += System.nanoTime() - pauseStartNanos;
					wasPaused = false;
				}

				int[] count = {0};
				boolean more;

				try {
					more = audioStream.readChunk(sample -> {
						if (count[0] < raw.length) {
							raw[count[0]++] = sample;
						}
					});
					consecutiveErrors = 0;
				} catch (Exception exception) {
					if (++consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
						BeatLamp.LOGGER.warn("Beat Lamp decode giving up after {} consecutive errors", consecutiveErrors, exception);
						break;
					}

					try {
						Thread.sleep(10L);
					} catch (InterruptedException interruptedException) {
						Thread.currentThread().interrupt();
						break;
					}

					continue;
				}

				int monoCount = toMono(raw, count[0], channels, mono);

				for (int offset = 0; offset < monoCount; offset += 1024) {
					analyzer.push(mono, offset, Math.min(1024, monoCount - offset));
				}

				if (!more) {
					break;
				}

				long elapsedNanos = System.nanoTime() - startNanos;
				double expectedSeconds = (double) analyzer.getTotalSamples() / analyzer.getSampleRate();
				double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
				double aheadSeconds = expectedSeconds - elapsedSeconds;
				if (aheadSeconds > 0.08) {
					try {
						Thread.sleep((long) ((aheadSeconds - 0.05) * 1000.0));
					} catch (InterruptedException interruptedException) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		} finally {
			try {
				audioStream.close();
			} catch (Exception ignored) {
			}
		}
	}

	private static int toMono(float[] raw, int count, int channels, float[] mono) {
		if (channels <= 1) {
			System.arraycopy(raw, 0, mono, 0, count);
			return count;
		}

		int frames = count / channels;
		for (int frame = 0; frame < frames; frame++) {
			float sum = 0.0F;
			for (int channel = 0; channel < channels; channel++) {
				sum += raw[frame * channels + channel];
			}

			mono[frame] = sum / channels;
		}

		return frames;
	}

	public static void clientTick() {
		ACTIVE_SONGS.values().forEach(song -> {
			if (song.analyzer.consumeBeat()) {
				song.beatPulse = 1.0F;
			} else {
				song.beatPulse *= 0.85F;
			}
		});
	}

	public static float getLevelAt(Vec3 position) {
		float best = 0.0F;

		for (ActiveSong song : ACTIVE_SONGS.values()) {
			float falloff = falloff(song.position.distanceTo(position));
			if (falloff <= 0.0F) {
				continue;
			}

			float level = song.analyzer.getLevel() * falloff;
			if (level > best) {
				best = level;
			}
		}

		return best;
	}

	public static float getBeatPulseAt(Vec3 position) {
		float best = 0.0F;

		for (ActiveSong song : ACTIVE_SONGS.values()) {
			float falloff = falloff(song.position.distanceTo(position));
			if (falloff <= 0.0F) {
				continue;
			}

			float pulse = song.beatPulse * falloff;
			if (pulse > best) {
				best = pulse;
			}
		}

		return best;
	}

	public static float getBandAt(Vec3 position, int band) {
		float best = -1.0F;
		ActiveSong bestSong = null;
		float bestFalloff = 0.0F;

		for (ActiveSong song : ACTIVE_SONGS.values()) {
			float falloff = falloff(song.position.distanceTo(position));
			if (falloff <= 0.0F) {
				continue;
			}

			float level = song.analyzer.getLevel() * falloff;
			if (level > best) {
				best = level;
				bestSong = song;
				bestFalloff = falloff;
			}
		}

		if (bestSong == null) {
			return 0.0F;
		}

		float[] bands = bestSong.analyzer.getBands();
		if (band < 0 || band >= bands.length) {
			return 0.0F;
		}

		return bands[band] * bestFalloff;
	}

	public static float getLevelAt(Vec3 position, BlockPos source) {
		if (source == null) {
			return getLevelAt(position);
		}

		ActiveSong song = ACTIVE_SONGS.get(source);
		if (song == null) {
			return 0.0F;
		}

		float falloff = falloff(song.position.distanceTo(position));
		return falloff <= 0.0F ? 0.0F : song.analyzer.getLevel() * falloff;
	}

	public static float getBeatPulseAt(Vec3 position, BlockPos source) {
		if (source == null) {
			return getBeatPulseAt(position);
		}

		ActiveSong song = ACTIVE_SONGS.get(source);
		if (song == null) {
			return 0.0F;
		}

		float falloff = falloff(song.position.distanceTo(position));
		return falloff <= 0.0F ? 0.0F : song.beatPulse * falloff;
	}

	public static float getBandAt(Vec3 position, int band, BlockPos source) {
		if (source == null) {
			return getBandAt(position, band);
		}

		ActiveSong song = ACTIVE_SONGS.get(source);
		if (song == null) {
			return 0.0F;
		}

		float falloff = falloff(song.position.distanceTo(position));
		if (falloff <= 0.0F) {
			return 0.0F;
		}

		float[] bands = song.analyzer.getBands();
		if (band < 0 || band >= bands.length) {
			return 0.0F;
		}

		return bands[band] * falloff;
	}

	private static float falloff(double distance) {
		if (distance >= AUDIBLE_RADIUS) {
			return 0.0F;
		}

		return (float) (1.0 - distance / AUDIBLE_RADIUS);
	}
}
