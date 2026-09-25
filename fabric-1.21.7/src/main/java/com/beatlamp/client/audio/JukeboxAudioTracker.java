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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class JukeboxAudioTracker {
	public static double getAudibleRadius() {
		return Math.max(64.0, (double) com.beatlamp.config.BeatLampConfig.maxAudioRadius);
	}
	private static final Map<BlockPos, ActiveSong> ACTIVE_SONGS = new ConcurrentHashMap<>();
	private static float effectTime;

	private JukeboxAudioTracker() {
	}

	private static final class ActiveSong {
		final Vec3 position;
		final AudioAnalyzer analyzer;
		final SoundInstance soundInstance;
		volatile boolean running = true;
		Thread thread;
		int ticksAlive;

		ActiveSong(Vec3 position, AudioAnalyzer analyzer, SoundInstance soundInstance, Thread thread) {
			this.position = position;
			this.analyzer = analyzer;
			this.soundInstance = soundInstance;
			this.thread = thread;
		}

		float beatPulse;
		float kickPulse;
		float snarePulse;
		float hihatPulse;
		float impactPulse;
	}

	public static void onSoundPlayed(SoundInstance soundInstance) {
		if (BeatLampClientConfig.isAudioDisabled()) {
			return;
		}
		if (soundInstance == null || soundInstance.getSource() != SoundSource.RECORDS) {
			return;
		}

		ResourceLocation location = soundInstance.getLocation();
		String className = soundInstance.getClass().getName();
		if (location != null && "music_disc_maker".equals(location.getNamespace())) {
			MusicDiscMakerAudioBridge.registerSoundInstance(soundInstance);
			return;
		}
		if (className.contains("musicdiscmaker")) {
			if (className.contains("DiscSoundInstance") || (location != null && !"minecraft".equals(location.getNamespace()))) {
				MusicDiscMakerAudioBridge.registerSoundInstance(soundInstance);
				return;
			}
			// VanillaSpeakerSoundInstance playing vanilla disc falls through to vanilla audio decoder
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

		startSong(BlockPos.containing(soundInstance.getX(), soundInstance.getY(), soundInstance.getZ()), sound.getPath(), minecraft, soundInstance);
	}

	private static void startSong(BlockPos blockPos, ResourceLocation path, Minecraft minecraft, SoundInstance soundInstance) {
		ActiveSong existing = ACTIVE_SONGS.get(blockPos);
		if (existing != null && existing.soundInstance == soundInstance && existing.running) {
			return;
		}
		stopSong(blockPos);

		try {
			InputStream inputStream = minecraft.getResourceManager().open(path);
			JOrbisAudioStream audioStream = new JOrbisAudioStream(inputStream);
			AudioFormat format = audioStream.getFormat();
			AudioAnalyzer analyzer = new AudioAnalyzer((int) format.getSampleRate(), BeatLampClientConfig.isStudioQuality());
			int channels = format.getChannels();
			BeatLamp.LOGGER.info("Beat Lamp tracking jukebox at {} -> {} ({}Hz, {}ch)", blockPos.toShortString(), path, format.getSampleRate(), channels);

			ActiveSong song = new ActiveSong(Vec3.atCenterOf(blockPos), analyzer, soundInstance, null);
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
		if (soundInstance == null || soundInstance.getSource() != SoundSource.RECORDS) {
			return;
		}

		BlockPos pos = BlockPos.containing(soundInstance.getX(), soundInstance.getY(), soundInstance.getZ());
		stopSong(pos);

		for (Map.Entry<BlockPos, ActiveSong> entry : ACTIVE_SONGS.entrySet()) {
			if (entry.getValue().soundInstance == soundInstance) {
				stopSong(entry.getKey());
			}
		}
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
		DreamDisplaysAudioBridge.clear();
		MusicDiscMakerAudioBridge.clear();
	}

	public static float getEffectiveVolumeMultiplier() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options == null) return 1.0F;
		float master = mc.options.getSoundSourceVolume(SoundSource.MASTER);
		float records = mc.options.getSoundSourceVolume(SoundSource.RECORDS);
		if (master <= 0.001F || records <= 0.001F) {
			return 0.0F;
		}
		return 1.0F;
	}

	public static boolean isJukeboxPlayingAt(BlockPos pos) {
		if (getEffectiveVolumeMultiplier() <= 0.001F) return false;
		return ACTIVE_SONGS.containsKey(pos);
	}

	public static boolean isAnyJukeboxPlayingNear(Vec3 position) {
		if (getEffectiveVolumeMultiplier() <= 0.001F) return false;
		double maxRadius = getAudibleRadius();
		for (ActiveSong song : ACTIVE_SONGS.values()) {
			if (song.position.distanceTo(position) < maxRadius) {
				return true;
			}
		}
		if (DreamDisplaysAudioBridge.isAnyDisplayPlayingNear(position)) {
			return true;
		}
		if (MusicDiscMakerAudioBridge.isAnyDiscPlayingNear(position)) {
			return true;
		}
		return false;
	}

	public static boolean isAnyJukeboxPlayingNear(Vec3 position, BlockPos source) {
		if (getEffectiveVolumeMultiplier() <= 0.001F) return false;
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			if (song != null) {
				return true;
			}
			if (DreamDisplaysAudioBridge.isDisplayPlayingNear(position, source)) {
				return true;
			}
			if (MusicDiscMakerAudioBridge.isDiscPlayingNear(position, source)) {
				return true;
			}
			return false;
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
		long pauseStartNanos = -1L;

		try {
			while (song.running) {
				if (minecraft.isPaused()) {
					if (pauseStartNanos < 0L) {
						pauseStartNanos = System.nanoTime();
					}
					try {
						Thread.sleep(20L);
					} catch (InterruptedException interruptedException) {
						Thread.currentThread().interrupt();
						break;
					}
					continue;
				} else if (pauseStartNanos > 0L) {
					if (startNanos > 0L) {
						startNanos += (System.nanoTime() - pauseStartNanos);
					}
					pauseStartNanos = -1L;
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
				if (lead > 0.005) {
					long parkNanos = (long) ((lead - 0.002) * 1_000_000_000.0);
					if (parkNanos > 0) {
						java.util.concurrent.locks.LockSupport.parkNanos(parkNanos);
					}
					if (Thread.currentThread().isInterrupted()) {
						break;
					}
				} else if (lead < -0.150) {
					// Catch-up clamp: if lag caused real time to get > 150ms ahead,
					// smoothly realign startNanos to prevent runaway fast-forward decoding burst!
					startNanos = System.nanoTime() - (long) (expectedSeconds * 1_000_000_000.0);
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
		if (BeatLampClientConfig.isAudioDisabled()) {
			if (!ACTIVE_SONGS.isEmpty()) {
				clear();
			}
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		scanNearbyJukeboxes(mc);

		for (Map.Entry<BlockPos, ActiveSong> entry : ACTIVE_SONGS.entrySet()) {
			BlockPos pos = entry.getKey();
			ActiveSong song = entry.getValue();

			song.ticksAlive++;

			if (mc.level != null && mc.level.hasChunkAt(pos)) {
				BlockState state = mc.level.getBlockState(pos);
				if (state.hasProperty(BlockStateProperties.HAS_RECORD) && !state.getValue(BlockStateProperties.HAS_RECORD)) {
					stopSong(pos);
					stopLevelRendererRecord(mc, pos);
					MusicDiscMakerAudioBridge.stopDiscAt(pos);
					RECENT_ATTEMPTS.remove(pos);
					continue;
				}
			}

			if (song.ticksAlive > 10 && song.soundInstance != null && mc.getSoundManager() != null && !mc.getSoundManager().isActive(song.soundInstance)) {
				if (mc.level != null && mc.level.hasChunkAt(pos)) {
					BlockState state = mc.level.getBlockState(pos);
					if (state.hasProperty(BlockStateProperties.HAS_RECORD) && !state.getValue(BlockStateProperties.HAS_RECORD)) {
						stopSong(pos);
						stopLevelRendererRecord(mc, pos);
						MusicDiscMakerAudioBridge.stopDiscAt(pos);
						RECENT_ATTEMPTS.remove(pos);
						continue;
					}
				} else {
					stopSong(pos);
					stopLevelRendererRecord(mc, pos);
					MusicDiscMakerAudioBridge.stopDiscAt(pos);
					RECENT_ATTEMPTS.remove(pos);
					continue;
				}
			}
		}

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

			if (song.analyzer.consumeImpact()) {
				song.impactPulse = song.analyzer.getImpactLevel();
			} else {
				song.impactPulse *= 0.70F;
			}

			if (song.beatPulse > maxBeat) {
				maxBeat = song.beatPulse;
			}
		}

		if (!ACTIVE_SONGS.isEmpty()) {
			effectTime += 1.0F + 1.5F * maxBeat;
		}

		DreamDisplaysAudioBridge.clientTick();
		MusicDiscMakerAudioBridge.clientTick();
	}

	private static final Map<BlockPos, Long> RECENT_ATTEMPTS = new ConcurrentHashMap<>();
	private static int scanCooldown = 0;

	private static void scanNearbyJukeboxes(Minecraft mc) {
		if (mc.level == null || mc.player == null) return;
		if (++scanCooldown < 10) return;
		scanCooldown = 0;

		float volumeMul = getEffectiveVolumeMultiplier();
		if (volumeMul <= 0.001F) return;

		BlockPos playerPos = mc.player.blockPosition();
		int playerChunkX = playerPos.getX() >> 4;
		int playerChunkZ = playerPos.getZ() >> 4;
		double radius = getAudibleRadius();
		long now = System.currentTimeMillis();

		if (RECENT_ATTEMPTS.size() > 50) {
			RECENT_ATTEMPTS.entrySet().removeIf(e -> now - e.getValue() > 10000L);
		}

		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				net.minecraft.world.level.chunk.LevelChunk chunk = mc.level.getChunkSource().getChunk(playerChunkX + dx, playerChunkZ + dz, false);
				if (chunk == null) continue;

				for (net.minecraft.world.level.block.entity.BlockEntity be : chunk.getBlockEntities().values()) {
					BlockPos pos = be.getBlockPos();
					if (!playerPos.closerThan(pos, radius)) continue;

					if (be instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity jukebox) {
						checkAndCatchUpVanillaJukebox(mc, jukebox, pos, now);
					} else if (be instanceof com.beatlamp.block.StageJukeboxBlockEntity stageJukebox) {
						checkAndCatchUpStageJukebox(mc, stageJukebox, pos, now);
					}
				}
			}
		}
	}

	private static void checkAndCatchUpVanillaJukebox(Minecraft mc, net.minecraft.world.level.block.entity.JukeboxBlockEntity jukebox, BlockPos pos, long now) {
		if (mc.level == null) return;
		BlockState state = mc.level.getBlockState(pos);
		if (!state.hasProperty(BlockStateProperties.HAS_RECORD) || !state.getValue(BlockStateProperties.HAS_RECORD)) {
			stopSong(pos);
			stopLevelRendererRecord(mc, pos);
			MusicDiscMakerAudioBridge.stopDiscAt(pos);
			return;
		}

		if (ACTIVE_SONGS.containsKey(pos) || MusicDiscMakerAudioBridge.isDiscPlayingAt(pos)) {
			return;
		}
		if (now - RECENT_ATTEMPTS.getOrDefault(pos, 0L) < 3000L) {
			return;
		}

		net.minecraft.world.item.ItemStack stack = jukebox.getTheItem();
		if (stack.isEmpty() || !jukebox.getSongPlayer().isPlaying()) {
			return;
		}

		java.util.Optional<net.minecraft.core.Holder<net.minecraft.world.item.JukeboxSong>> optSong =
				net.minecraft.world.item.JukeboxSong.fromStack(mc.level.registryAccess(), stack);

		if (optSong.isPresent()) {
			RECENT_ATTEMPTS.put(pos, now);
			int songId = mc.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.JUKEBOX_SONG).getId(optSong.get().value());
			mc.level.levelEvent(1010, pos, songId);
		} else {
			RECENT_ATTEMPTS.put(pos, now);
			MusicDiscMakerAudioBridge.startCustomDiscPlayback(pos, stack, 0L, 64, 100);
		}
	}

	private static void checkAndCatchUpStageJukebox(Minecraft mc, com.beatlamp.block.StageJukeboxBlockEntity stageJukebox, BlockPos pos, long now) {
		if (mc.level == null) return;
		BlockState state = mc.level.getBlockState(pos);
		if (!state.hasProperty(BlockStateProperties.HAS_RECORD) || !state.getValue(BlockStateProperties.HAS_RECORD)) {
			stopSong(pos);
			stopLevelRendererRecord(mc, pos);
			MusicDiscMakerAudioBridge.stopDiscAt(pos);
			return;
		}

		if (ACTIVE_SONGS.containsKey(pos) || MusicDiscMakerAudioBridge.isDiscPlayingAt(pos)) {
			return;
		}
		if (now - RECENT_ATTEMPTS.getOrDefault(pos, 0L) < 3000L) {
			return;
		}

		net.minecraft.world.item.ItemStack stack = stageJukebox.getRecord();
		if (stack.isEmpty() || stageJukebox.isPaused()) {
			return;
		}

		java.util.Optional<net.minecraft.core.Holder<net.minecraft.world.item.JukeboxSong>> optSong =
				net.minecraft.world.item.JukeboxSong.fromStack(mc.level.registryAccess(), stack);

		if (optSong.isPresent()) {
			if (stageJukebox.isPlaying() || stageJukebox.getElapsedTicks() < stageJukebox.getTotalDurationTicks()) {
				RECENT_ATTEMPTS.put(pos, now);
				int songId = mc.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.JUKEBOX_SONG).getId(optSong.get().value());
				mc.level.levelEvent(1010, pos, songId);
			}
		} else {
			if (stageJukebox.getElapsedTicks() < stageJukebox.getTotalDurationTicks()) {
				RECENT_ATTEMPTS.put(pos, now);
				long offsetMs = stageJukebox.getElapsedTicks() * 50L;
				MusicDiscMakerAudioBridge.startCustomDiscPlayback(pos, stack, offsetMs, stageJukebox.getRange(), stageJukebox.getVolume());
			}
		}
	}

	private static void stopLevelRendererRecord(Minecraft mc, BlockPos pos) {
		if (mc.level != null) {
			mc.level.levelEvent(1011, pos, 0);
		}
	}

	public static float getImpactPulseAt(Vec3 position, BlockPos source) {
		float best = 0.0F;
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			if (song != null) {
				best = song.impactPulse;
			}
		} else {
			for (ActiveSong song : ACTIVE_SONGS.values()) {
				float falloff = falloff(song.position.distanceTo(position));
				float impact = song.impactPulse * falloff;
				if (impact > best) {
					best = impact;
				}
			}
		}

		float ddImpact = DreamDisplaysAudioBridge.getImpactPulseAt(position, source);
		float mdmImpact = MusicDiscMakerAudioBridge.getImpactPulseAt(position, source);
		return Math.max(best, Math.max(ddImpact, mdmImpact)) * getEffectiveVolumeMultiplier();
	}

	public static float getEffectTime() {
		return effectTime + DreamDisplaysAudioBridge.getEffectTime() + MusicDiscMakerAudioBridge.getEffectTime();
	}

	public static float getLevelAt(Vec3 position) {
		return getLevelAt(position, null);
	}

	public static float getRawLevelAt(Vec3 position, BlockPos source) {
		float best = 0.0F;
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			if (song != null) {
				best = song.analyzer.getLevel();
			}
		} else {
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
		}

		float ddRaw = DreamDisplaysAudioBridge.getRawLevelAt(position, source);
		float mdmRaw = MusicDiscMakerAudioBridge.getRawLevelAt(position, source);
		return Math.max(best, Math.max(ddRaw, mdmRaw)) * getEffectiveVolumeMultiplier();
	}

	public static float getGridPulseAt(Vec3 position, BlockPos source) {
		float best = 0.0F;
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			if (song != null) {
				best = song.analyzer.getGridPulse();
			}
		} else {
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
		}

		float ddGrid = DreamDisplaysAudioBridge.getGridPulseAt(position, source);
		float mdmGrid = MusicDiscMakerAudioBridge.getGridPulseAt(position, source);
		return Math.max(best, Math.max(ddGrid, mdmGrid)) * getEffectiveVolumeMultiplier();
	}

	public static float getLevelAt(Vec3 position, BlockPos source) {
		float best = 0.0F;
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			if (song != null) {
				best = Math.max(song.analyzer.getLevel(), song.analyzer.getGridPulse() * 0.28F);
			}
		} else {
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
		}

		float ddLevel = DreamDisplaysAudioBridge.getLevelAt(position, source);
		float mdmLevel = MusicDiscMakerAudioBridge.getLevelAt(position, source);
		return Math.max(best, Math.max(ddLevel, mdmLevel)) * getEffectiveVolumeMultiplier();
	}

	public static float getBeatPulseAt(Vec3 position) {
		return getBeatPulseAt(position, null);
	}

	public static float getBeatPulseAt(Vec3 position, BlockPos source) {
		float best = 0.0F;
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			if (song != null) {
				best = song.beatPulse;
			}
		} else {
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
		}

		float ddBeat = DreamDisplaysAudioBridge.getBeatPulseAt(position, source);
		float mdmBeat = MusicDiscMakerAudioBridge.getBeatPulseAt(position, source);
		return Math.max(best, Math.max(ddBeat, mdmBeat)) * getEffectiveVolumeMultiplier();
	}

	public static float getKickPulseAt(Vec3 position, BlockPos source) {
		float best = 0.0F;
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			if (song != null) {
				best = song.kickPulse;
			}
		} else {
			for (ActiveSong song : ACTIVE_SONGS.values()) {
				float falloff = falloff(song.position.distanceTo(position));
				float pulse = song.kickPulse * falloff;
				if (pulse > best) best = pulse;
			}
		}

		float ddKick = DreamDisplaysAudioBridge.getKickPulseAt(position, source);
		float mdmKick = MusicDiscMakerAudioBridge.getKickPulseAt(position, source);
		return Math.max(best, Math.max(ddKick, mdmKick)) * getEffectiveVolumeMultiplier();
	}

	public static float getSnarePulseAt(Vec3 position, BlockPos source) {
		float best = 0.0F;
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			if (song != null) {
				best = song.snarePulse;
			}
		} else {
			for (ActiveSong song : ACTIVE_SONGS.values()) {
				float falloff = falloff(song.position.distanceTo(position));
				float pulse = song.snarePulse * falloff;
				if (pulse > best) best = pulse;
			}
		}

		float ddSnare = DreamDisplaysAudioBridge.getSnarePulseAt(position, source);
		float mdmSnare = MusicDiscMakerAudioBridge.getSnarePulseAt(position, source);
		return Math.max(best, Math.max(ddSnare, mdmSnare)) * getEffectiveVolumeMultiplier();
	}

	public static float getHihatPulseAt(Vec3 position, BlockPos source) {
		float best = 0.0F;
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			if (song != null) {
				best = song.hihatPulse;
			}
		} else {
			for (ActiveSong song : ACTIVE_SONGS.values()) {
				float falloff = falloff(song.position.distanceTo(position));
				float pulse = song.hihatPulse * falloff;
				if (pulse > best) best = pulse;
			}
		}

		float ddHihat = DreamDisplaysAudioBridge.getHihatPulseAt(position, source);
		float mdmHihat = MusicDiscMakerAudioBridge.getHihatPulseAt(position, source);
		return Math.max(best, Math.max(ddHihat, mdmHihat)) * getEffectiveVolumeMultiplier();
	}

	public static float getBandAt(Vec3 position, int band) {
		return getBandAt(position, band, null);
	}

	public static float getBandAt(Vec3 position, int band, BlockPos source) {
		float best = 0.0F;
		if (source != null) {
			ActiveSong song = ACTIVE_SONGS.get(source);
			if (song != null) {
				float[] bands = song.analyzer.getBands();
				if (band >= 0 && band < bands.length) {
					best = bands[band];
				}
			}
		} else {
			ActiveSong bestSong = null;
			float bestFalloff = 0.0F;
			float bestLevel = -1.0F;

			for (ActiveSong song : ACTIVE_SONGS.values()) {
				float falloff = falloff(song.position.distanceTo(position));
				if (falloff <= 0.0F) {
					continue;
				}

				float level = song.analyzer.getLevel() * falloff;
				if (level > bestLevel) {
					bestLevel = level;
					bestSong = song;
					bestFalloff = falloff;
				}
			}

			if (bestSong != null) {
				float[] bands = bestSong.analyzer.getBands();
				if (band >= 0 && band < bands.length) {
					best = bands[band] * bestFalloff;
				}
			}
		}

		float ddBand = DreamDisplaysAudioBridge.getBandAt(position, band, source);
		float mdmBand = MusicDiscMakerAudioBridge.getBandAt(position, band, source);
		return Math.max(best, Math.max(ddBand, mdmBand)) * getEffectiveVolumeMultiplier();
	}

	private static float falloff(double distance) {
		double maxRadius = getAudibleRadius();
		if (distance >= maxRadius) {
			return 0.0F;
		}

		return (float) (1.0 - distance / maxRadius);
	}
}
