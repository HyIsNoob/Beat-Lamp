package com.beatlamp.client.audio;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sound.sampled.AudioFormat;

import com.beatlamp.BeatLamp;
import com.beatlamp.client.config.BeatLampClientConfig;

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

public final class JukeboxAudioTracker {
	private static final double AUDIBLE_RADIUS = 64.0;
	private static final Map<BlockPos, ActiveSong> ACTIVE_SONGS = new ConcurrentHashMap<>();
	private static float effectTime;

	private JukeboxAudioTracker() {
	}

	private static final class ActiveSong {
		final Vec3 position;
		final AudioAnalyzer analyzer;
		volatile boolean running = true;
		Thread thread;

		ActiveSong(Vec3 position, AudioAnalyzer analyzer, Thread thread) {
			this.position = position;
			this.analyzer = analyzer;
			this.thread = thread;
		}

		float beatPulse;
		float kickPulse;
		float snarePulse;
		float hihatPulse;
		float impactPulse;
	}

	public static void onSoundPlayed(SoundInstance soundInstance) {
		if (soundInstance.getSource() != SoundSource.RECORDS) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		SoundManager soundManager = minecraft.getSoundManager();
		WeighedSoundEvents weighedSoundEvents = soundInstance.resolve(soundManager);

		if (weighedSoundEvents == null) {
			return;
		}

		Sound sound = soundInstance.getSound();

		if (sound == null || sound == SoundManager.EMPTY_SOUND) {
			WeighedSoundEvents next = soundManager.getSoundEvent(soundInstance.getLocation());

			if (next == null) {
				return;
			}

			sound = next.getSound(RandomSource.create());
		}

		startSong(BlockPos.containing(soundInstance.getX(), soundInstance.getY(), soundInstance.getZ()), sound.getPath(), minecraft);
	}

	private static void startSong(BlockPos blockPos, ResourceLocation path, Minecraft minecraft) {
		stopSong(blockPos);

		try {
			InputStream inputStream = minecraft.getResourceManager().open(path);
			JOrbisAudioStream audioStream = new JOrbisAudioStream(inputStream);
			AudioFormat format = audioStream.getFormat();
			AudioAnalyzer analyzer = new AudioAnalyzer((int) format.getSampleRate(), BeatLampClientConfig.isStudioQuality());
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
		for (ActiveSong song : ACTIVE_SONGS.values()) {
			song.running = false;
			if (song.thread != null) {
				song.thread.interrupt();
			}
		}
		ACTIVE_SONGS.clear();
	}

	public static boolean isJukeboxPlayingAt(BlockPos pos) {
		return ACTIVE_SONGS.containsKey(pos);
	}

	public static boolean isAnyJukeboxPlayingNear(Vec3 position) {
		for (ActiveSong song : ACTIVE_SONGS.values()) {
			if (song.position.distanceTo(position) < AUDIBLE_RADIUS) {
				return true;
			}
		}
		return false;
	}

	public static boolean isAnyJukeboxPlayingNear(Vec3 position, BlockPos source) {
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			return song != null && song.position.distanceTo(position) < AUDIBLE_RADIUS;
		}
		return isAnyJukeboxPlayingNear(position);
	}

	private static void decodeLoop(
		Minecraft minecraft,
		JOrbisAudioStream audioStream,
		AudioAnalyzer analyzer,
		int channels,
		ActiveSong song
	) {
		float[] interleaved = new float[4096 * channels];
		float[] mono = new float[4096];
		long startNanos = -1L;

		try {
			while (song.running) {
				if (minecraft.isPaused()) {
					try {
						Thread.sleep(20L);
					} catch (InterruptedException interruptedException) {
						Thread.currentThread().interrupt();
						break;
					}
					continue;
				}

				ByteBuffer byteBuffer;
				try {
					byteBuffer = audioStream.read(4096);
				} catch (Exception exception) {
					break;
				}

				if (byteBuffer == null || !byteBuffer.hasRemaining()) {
					break;
				}

				int frames = decodePcmToMono(byteBuffer, channels, interleaved, mono);
				if (frames <= 0) {
					continue;
				}

				if (startNanos < 0L) {
					startNanos = System.nanoTime();
				}

				analyzer.push(mono, frames);

				long elapsedNanos = System.nanoTime() - startNanos;
				double expectedSeconds = (double) analyzer.getTotalSamples() / analyzer.getSampleRate();
				double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
				double lead = expectedSeconds - elapsedSeconds;
				if (lead > 0.035) {
					try {
						Thread.sleep((long) ((lead - 0.020) * 1000.0));
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

	private static int decodePcmToMono(ByteBuffer byteBuffer, int channels, float[] raw, float[] mono) {
		ByteBuffer buffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		int sampleCount = 0;

		while (buffer.remaining() >= 2 && sampleCount < raw.length) {
			short s = buffer.getShort();
			raw[sampleCount++] = s / 32768.0F;
		}

		int frames = sampleCount / channels;
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
		float maxBeat = 0.0F;

		for (ActiveSong song : ACTIVE_SONGS.values()) {
			if (song.analyzer.consumeBeat()) {
				song.beatPulse = 1.0F;
			} else {
				song.beatPulse *= 0.80F;
			}

			// Sustain internal rhythmic pulse (~0.35) during vocal breakdown
			float grid = song.analyzer.getGridPulse();
			if (song.beatPulse < grid * 0.40F) {
				song.beatPulse = grid * 0.40F;
			}

			if (song.analyzer.consumeKick()) {
				song.kickPulse = 1.0F;
			} else {
				song.kickPulse *= 0.78F;
			}

			if (song.analyzer.consumeSnare()) {
				song.snarePulse = 1.0F;
			} else {
				song.snarePulse *= 0.82F;
			}

			if (song.analyzer.consumeHihat()) {
				song.hihatPulse = 1.0F;
			} else {
				song.hihatPulse *= 0.85F;
			}

			float analyzerImpact = song.analyzer.getImpactLevel();
			if (analyzerImpact > song.impactPulse) {
				song.impactPulse = analyzerImpact;
			} else {
				song.impactPulse *= 0.88F;
			}

			if (song.beatPulse > maxBeat) {
				maxBeat = song.beatPulse;
			}
		}

		effectTime += 1.0F + 1.5F * maxBeat;
	}

	public static float getImpactPulseAt(Vec3 position, BlockPos source) {
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			return song == null ? 0.0F : song.impactPulse;
		}

		float best = 0.0F;

		for (ActiveSong song : ACTIVE_SONGS.values()) {
			float falloff = falloff(song.position.distanceTo(position));
			float impact = song.impactPulse * falloff;
			if (impact > best) {
				best = impact;
			}
		}

		return best;
	}

	public static float getEffectTime() {
		return effectTime;
	}

	public static float getLevelAt(Vec3 position) {
		return getLevelAt(position, null);
	}

	public static float getRawLevelAt(Vec3 position, BlockPos source) {
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			return song == null ? 0.0F : song.analyzer.getLevel();
		}

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

	public static float getGridPulseAt(Vec3 position, BlockPos source) {
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			return song == null ? 0.0F : song.analyzer.getGridPulse();
		}

		float best = 0.0F;
		for (ActiveSong song : ACTIVE_SONGS.values()) {
			float falloff = falloff(song.position.distanceTo(position));
			if (falloff <= 0.0F) {
				continue;
			}

			float grid = song.analyzer.getGridPulse() * falloff;
			if (grid > best) {
				best = grid;
			}
		}

		return best;
	}

	public static float getLevelAt(Vec3 position, BlockPos source) {
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			if (song == null) return 0.0F;
			return Math.max(song.analyzer.getLevel(), song.analyzer.getGridPulse() * 0.28F);
		}

		float best = 0.0F;
		for (ActiveSong song : ACTIVE_SONGS.values()) {
			float falloff = falloff(song.position.distanceTo(position));
			if (falloff <= 0.0F) {
				continue;
			}

			float level = Math.max(song.analyzer.getLevel(), song.analyzer.getGridPulse() * 0.28F) * falloff;
			if (level > best) {
				best = level;
			}
		}

		return best;
	}

	public static float getBeatPulseAt(Vec3 position) {
		return getBeatPulseAt(position, null);
	}

	public static float getBeatPulseAt(Vec3 position, BlockPos source) {
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			return song == null ? 0.0F : song.beatPulse;
		}

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

	public static float getKickPulseAt(Vec3 position, BlockPos source) {
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			return song == null ? 0.0F : song.kickPulse;
		}

		float best = 0.0F;
		for (ActiveSong song : ACTIVE_SONGS.values()) {
			float falloff = falloff(song.position.distanceTo(position));
			float pulse = song.kickPulse * falloff;
			if (pulse > best) best = pulse;
		}
		return best;
	}

	public static float getSnarePulseAt(Vec3 position, BlockPos source) {
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			return song == null ? 0.0F : song.snarePulse;
		}

		float best = 0.0F;
		for (ActiveSong song : ACTIVE_SONGS.values()) {
			float falloff = falloff(song.position.distanceTo(position));
			float pulse = song.snarePulse * falloff;
			if (pulse > best) best = pulse;
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

	public static float getBandAt(Vec3 position, int band, BlockPos source) {
		if (source == null) {
			return getBandAt(position, band);
		}

		ActiveSong song = ACTIVE_SONGS.get(source);
		if (song == null) {
			return 0.0F;
		}

		float[] bands = song.analyzer.getBands();
		if (band < 0 || band >= bands.length) {
			return 0.0F;
		}

		return bands[band];
	}

	private static float falloff(double distance) {
		if (distance >= AUDIBLE_RADIUS) {
			return 0.0F;
		}

		return (float) (1.0 - distance / AUDIBLE_RADIUS);
	}
}
