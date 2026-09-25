package com.beatlamp.client.audio;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.beatlamp.BeatLamp;
import com.beatlamp.client.config.BeatLampClientConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class MusicDiscMakerAudioBridge {
	public static double getAudibleRadius() {
		return Math.max(64.0, (double) com.beatlamp.config.BeatLampConfig.maxAudioRadius);
	}
	private static final Map<Object, ActiveDisc> ACTIVE_DISCS = new ConcurrentHashMap<>();
	private static float effectTime;
	private static volatile boolean firstChunkLogged = false;

	private MusicDiscMakerAudioBridge() {
	}

	public static final class ActiveDisc {
		public final Object streamOrKey;
		public volatile Object discSoundInstance;
		public volatile AudioAnalyzer analyzer;
		public volatile int sampleRate;
		public volatile long lastChunkTimeMs;
		public volatile boolean playing;
		public volatile Vec3 position;
		public volatile BlockPos blockPos;
		public float beatPulse;
		public float kickPulse;
		public float snarePulse;
		public float hihatPulse;
		public float impactPulse;

		private final ConcurrentLinkedQueue<float[]> pcmQueue = new ConcurrentLinkedQueue<>();
		private volatile Thread meterThread;
		private volatile boolean threadRunning = true;
		private long totalSamplesPushed = 0L;
		private long startNanos = -1L;

		public ActiveDisc(Object streamOrKey, int sampleRate) {
			this.streamOrKey = streamOrKey;
			int rate = sampleRate > 0 ? sampleRate : 44100;
			this.sampleRate = rate;
			this.analyzer = new AudioAnalyzer(rate, BeatLampClientConfig.isStudioQuality());
			this.lastChunkTimeMs = System.currentTimeMillis();
			this.playing = true;
			startMeterThread();
		}

		public synchronized void updateSampleRate(int newRate) {
			if (newRate > 0 && newRate != this.sampleRate) {
				this.sampleRate = newRate;
				this.analyzer = new AudioAnalyzer(newRate, BeatLampClientConfig.isStudioQuality());
				this.totalSamplesPushed = 0L;
				this.startNanos = -1L;
			}
		}

		public void updatePosition(Vec3 pos) {
			if (pos != null) {
				this.position = pos;
				this.blockPos = BlockPos.containing(pos.x, pos.y, pos.z);
			}
		}

		public void enqueueSamples(float[] samples) {
			if (samples == null || samples.length == 0) return;
			pcmQueue.add(samples);
			startMeterThread();
		}

		public synchronized void startMeterThread() {
			if (meterThread != null && meterThread.isAlive()) return;
			threadRunning = true;
			meterThread = new Thread(() -> {
				float[] slice = new float[1024];
				while (threadRunning && playing) {
					float[] chunk = pcmQueue.poll();
					if (chunk == null) {
						java.util.concurrent.locks.LockSupport.parkNanos(5_000_000L);
						continue;
					}

					int offset = 0;
					while (offset < chunk.length && threadRunning && playing) {
						int take = Math.min(slice.length, chunk.length - offset);
						System.arraycopy(chunk, offset, slice, 0, take);
						offset += take;

						if (startNanos < 0L) {
							startNanos = System.nanoTime();
						}

						analyzer.push(slice, take);
						totalSamplesPushed += take;

						long elapsedNanos = System.nanoTime() - startNanos;
						double expectedSeconds = (double) totalSamplesPushed / analyzer.getSampleRate();
						double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
						double lead = expectedSeconds - elapsedSeconds;
						if (lead > 0.005) {
							long parkNanos = (long) ((lead - 0.002) * 1_000_000_000.0);
							if (parkNanos > 0) {
								java.util.concurrent.locks.LockSupport.parkNanos(parkNanos);
							}
						} else if (lead < -0.150) {
							startNanos = System.nanoTime() - (long) (expectedSeconds * 1_000_000_000.0);
						}
					}
				}
			}, "BeatLamp-MDM-Meter");
			meterThread.setDaemon(true);
			meterThread.start();
		}

		public void stopMeterThread() {
			threadRunning = false;
			if (meterThread != null) {
				meterThread.interrupt();
				meterThread = null;
			}
			pcmQueue.clear();
		}
	}

	public static volatile SoundInstance LATEST_SOUND_INSTANCE;

	public static void registerSoundInstance(SoundInstance soundInstance) {
		if (soundInstance == null) return;
		LATEST_SOUND_INSTANCE = soundInstance;
		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			if (disc.discSoundInstance == null) {
				disc.discSoundInstance = soundInstance;
				updateDiscCoords(disc);
			}
		}
	}

	public static void registerStream(Object stream, Object soundInstance) {
		if (stream == null) return;
		ActiveDisc disc = ACTIVE_DISCS.computeIfAbsent(stream, s -> new ActiveDisc(s, 44100));
		disc.discSoundInstance = soundInstance;
		disc.playing = true;
		updateDiscCoords(disc);
	}

	private static BlockPos resolveBlockPosFromAnchor(Object anchor) {
		if (anchor == null) return null;
		Object current = anchor;
		for (int depth = 0; depth < 5 && current != null; depth++) {
			// A. Check pos() or configPos() methods
			for (String methodName : new String[]{"pos", "configPos", "m_1234_"}) {
				try {
					for (Method m : current.getClass().getMethods()) {
						if (methodName.equals(m.getName()) && m.getParameterCount() == 0) {
							m.setAccessible(true);
							Object res = m.invoke(current);
							if (res instanceof BlockPos bp) {
								return bp;
							}
						}
					}
					for (Method m : current.getClass().getDeclaredMethods()) {
						if (methodName.equals(m.getName()) && m.getParameterCount() == 0) {
							m.setAccessible(true);
							Object res = m.invoke(current);
							if (res instanceof BlockPos bp) {
								return bp;
							}
						}
					}
				} catch (Throwable ignored) {
				}
			}

			// B. Check field 'pos' or 'configPos'
			for (String fieldName : new String[]{"pos", "configPos"}) {
				try {
					Field f = current.getClass().getDeclaredField(fieldName);
					f.setAccessible(true);
					Object res = f.get(current);
					if (res instanceof BlockPos bp) {
						return bp;
					}
				} catch (Throwable ignored) {
				}
			}

			// C. Check if current wraps another anchor via 'source' or 'sourceAnchor'
			Object next = null;
			for (String wrapField : new String[]{"source", "sourceAnchor"}) {
				try {
					Field f = current.getClass().getDeclaredField(wrapField);
					f.setAccessible(true);
					Object inner = f.get(current);
					if (inner != null && inner != current) {
						next = inner;
						break;
					}
				} catch (Throwable ignored) {
				}
			}
			if (next != null) {
				current = next;
			} else {
				break;
			}
		}

		// D. If not directly found via pos(), try worldPos(0.0F) on the anchor
		try {
			for (Method m : anchor.getClass().getMethods()) {
				if ("worldPos".equals(m.getName()) && m.getParameterCount() == 1) {
					m.setAccessible(true);
					Object res = m.invoke(anchor, 0.0F);
					if (res instanceof Vec3 v) {
						if (Math.abs(v.x) > 0.001 || Math.abs(v.y) > 0.001 || Math.abs(v.z) > 0.001) {
							return BlockPos.containing(v.x, v.y, v.z);
						}
					}
				}
			}
		} catch (Throwable ignored) {
		}

		return null;
	}

	private static void updateDiscCoords(ActiveDisc disc) {
		if (disc == null) return;
		Object inst = disc.discSoundInstance;
		if (inst == null && LATEST_SOUND_INSTANCE != null) {
			inst = LATEST_SOUND_INSTANCE;
			disc.discSoundInstance = LATEST_SOUND_INSTANCE;
		}
		if (inst == null) return;

		// 1. Try extracting BlockPos from anchor field in DiscSoundInstance
		try {
			Field fAnchor = inst.getClass().getDeclaredField("anchor");
			fAnchor.setAccessible(true);
			Object anchor = fAnchor.get(inst);
			if (anchor != null) {
				BlockPos resolvedPos = resolveBlockPosFromAnchor(anchor);
				if (resolvedPos != null) {
					disc.blockPos = resolvedPos.immutable();
					disc.position = Vec3.atCenterOf(resolvedPos);
					return;
				}
			}
		} catch (Throwable ignored) {
		}

		// 2. Direct SoundInstance getX/getY/getZ (only if non-zero, since flat-audible zeroes out coordinates)
		if (inst instanceof SoundInstance si) {
			double x = si.getX();
			double y = si.getY();
			double z = si.getZ();
			if (Math.abs(x) > 0.001 || Math.abs(y) > 0.001 || Math.abs(z) > 0.001) {
				disc.updatePosition(new Vec3(x, y, z));
				return;
			}
		}

		// 3. Methods getX/m_7773_/method_4775
		try {
			Method getX = null, getY = null, getZ = null;
			for (Method m : inst.getClass().getMethods()) {
				String name = m.getName();
				if (("getX".equals(name) || "m_7773_".equals(name) || "method_4775".equals(name)) && m.getParameterCount() == 0) getX = m;
				if (("getY".equals(name) || "m_7775_".equals(name) || "method_4778".equals(name)) && m.getParameterCount() == 0) getY = m;
				if (("getZ".equals(name) || "m_7774_".equals(name) || "method_4779".equals(name)) && m.getParameterCount() == 0) getZ = m;
			}
			if (getX != null && getY != null && getZ != null) {
				getX.setAccessible(true);
				getY.setAccessible(true);
				getZ.setAccessible(true);
				double x = ((Number) getX.invoke(inst)).doubleValue();
				double y = ((Number) getY.invoke(inst)).doubleValue();
				double z = ((Number) getZ.invoke(inst)).doubleValue();
				if (Math.abs(x) > 0.001 || Math.abs(y) > 0.001 || Math.abs(z) > 0.001) {
					disc.updatePosition(new Vec3(x, y, z));
					return;
				}
			}
		} catch (Throwable ignored) {
		}

		// 4. Fields x, y, z / f_119575_ / field_5471
		Class<?> c = inst.getClass();
		while (c != null && c != Object.class) {
			try {
				Field fx = null, fy = null, fz = null;
				for (Field f : c.getDeclaredFields()) {
					String n = f.getName();
					if ("x".equals(n) || "f_119575_".equals(n) || "field_5471".equals(n)) fx = f;
					if ("y".equals(n) || "f_119576_".equals(n) || "field_5472".equals(n)) fy = f;
					if ("z".equals(n) || "f_119577_".equals(n) || "field_5473".equals(n)) fz = f;
				}
				if (fx != null && fy != null && fz != null) {
					fx.setAccessible(true);
					fy.setAccessible(true);
					fz.setAccessible(true);
					double x = fx.getDouble(inst);
					double y = fy.getDouble(inst);
					double z = fz.getDouble(inst);
					if (Math.abs(x) > 0.001 || Math.abs(y) > 0.001 || Math.abs(z) > 0.001) {
						disc.updatePosition(new Vec3(x, y, z));
						return;
					}
				}
			} catch (Throwable ignored) {
			}
			c = c.getSuperclass();
		}
	}

	public static void onAudioChunk(Object stream, byte[] chunk, int len, Object audioFormat) {
		if (stream == null || chunk == null || len <= 0) {
			return;
		}

		int channels = 2;
		int sampleRate = 44100;
		if (audioFormat != null) {
			try {
				Method getChannels = audioFormat.getClass().getMethod("getChannels");
				Method getSampleRate = audioFormat.getClass().getMethod("getSampleRate");
				channels = (Integer) getChannels.invoke(audioFormat);
				sampleRate = (int) ((Float) getSampleRate.invoke(audioFormat)).floatValue();
			} catch (Throwable ignored) {
			}
		}

		int bytesPerSample = 2;
		int frameSize = channels * bytesPerSample;
		int frames = len / frameSize;
		if (frames <= 0) {
			return;
		}

		float[] mono = new float[frames];
		for (int i = 0; i < frames; i++) {
			int idx = i * frameSize;
			float sum = 0.0F;
			for (int ch = 0; ch < channels; ch++) {
				int chIdx = idx + ch * bytesPerSample;
				short s = (short) ((chunk[chIdx] & 0xFF) | (chunk[chIdx + 1] << 8));
				sum += s / 32768.0F;
			}
			mono[i] = sum / channels;
		}

		int finalSampleRate = sampleRate;
		ActiveDisc disc = ACTIVE_DISCS.computeIfAbsent(stream, s -> new ActiveDisc(s, finalSampleRate));
		disc.updateSampleRate(finalSampleRate);
		disc.lastChunkTimeMs = System.currentTimeMillis();
		disc.playing = true;
		updateDiscCoords(disc);
		disc.enqueueSamples(mono);

		if (!firstChunkLogged) {
			firstChunkLogged = true;
			BeatLamp.LOGGER.info("Beat Lamp received first Music Disc Maker audio chunk (frames={}, rate={}Hz, ch={})", frames, sampleRate, channels);
		}
	}

	public static void onDiscStopped(Object discSoundInstance) {
		if (discSoundInstance == null) return;
		if (LATEST_SOUND_INSTANCE == discSoundInstance) {
			LATEST_SOUND_INSTANCE = null;
		}
		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			if (disc.discSoundInstance == discSoundInstance) {
				disc.playing = false;
				disc.stopMeterThread();
				disc.beatPulse = 0.0F;
				disc.kickPulse = 0.0F;
				disc.snarePulse = 0.0F;
				disc.hihatPulse = 0.0F;
				disc.impactPulse = 0.0F;
			}
		}
	}

	public static void onStreamEnded(Object stream) {
		if (stream == null) return;
		ActiveDisc disc = ACTIVE_DISCS.get(stream);
		if (disc != null) {
			disc.playing = false;
			disc.stopMeterThread();
			disc.beatPulse = 0.0F;
			disc.kickPulse = 0.0F;
			disc.snarePulse = 0.0F;
			disc.hihatPulse = 0.0F;
			disc.impactPulse = 0.0F;
		}
	}

	public static void stopDiscAt(BlockPos pos) {
		if (pos == null) return;
		for (Iterator<Map.Entry<Object, ActiveDisc>> it = ACTIVE_DISCS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Object, ActiveDisc> entry = it.next();
			ActiveDisc disc = entry.getValue();
			if (pos.equals(disc.blockPos)) {
				disc.playing = false;
				disc.stopMeterThread();
				disc.beatPulse = 0.0F;
				disc.kickPulse = 0.0F;
				disc.snarePulse = 0.0F;
				disc.hihatPulse = 0.0F;
				disc.impactPulse = 0.0F;
				it.remove();
			}
		}

		try {
			Class<?> clazz = Class.forName("com.kuronami.musicdiscmaker.client.audio.ClientPlaybackManager");
			Method getMethod = clazz.getMethod("get");
			Object manager = getMethod.invoke(null);
			if (manager != null) {
				Method stopMethod = clazz.getMethod("stopPlayback", BlockPos.class);
				stopMethod.invoke(manager, pos);
			}
		} catch (Throwable ignored) {
		}

		try {
			Class<?> clazz = Class.forName("com.kuronami.musicdiscmaker.client.audio.VanillaSpeakerPlayback");
			Method stopVanilla = clazz.getMethod("stopVanilla", BlockPos.class);
			stopVanilla.invoke(null, pos);
		} catch (Throwable ignored) {
		}
	}

	public static void startCustomDiscPlayback(BlockPos pos, net.minecraft.world.item.ItemStack stack, long startOffsetMs, int range, int volume) {
		if (pos == null || stack == null || stack.isEmpty()) return;
		try {
			Class<?> controllerClass = Class.forName("com.kuronami.musicdiscmaker.event.JukeboxDiscController");
			Object track = null;
			for (Method m : controllerClass.getDeclaredMethods()) {
				if ("track".equals(m.getName()) || "getTrack".equals(m.getName())) {
					m.setAccessible(true);
					track = m.invoke(null, stack);
					break;
				}
			}
			if (track == null) {
				try {
					Class<?> modComponents = Class.forName("com.kuronami.musicdiscmaker.component.ModDataComponentTypes");
					Field fTrack = modComponents.getField("CUSTOM_TRACK_DATA");
					Object type = fTrack.get(null);
					for (Method m : stack.getClass().getMethods()) {
						if ("get".equals(m.getName()) && m.getParameterCount() == 1) {
							track = m.invoke(stack, type);
							if (track != null) break;
						}
					}
				} catch (Throwable ignored) {
				}
			}

			if (track != null) {
				Class<?> managerClass = Class.forName("com.kuronami.musicdiscmaker.client.audio.ClientPlaybackManager");
				Object manager = managerClass.getMethod("get").invoke(null);
				if (manager != null) {
					for (Method m : manager.getClass().getMethods()) {
						if ("startPlayback".equals(m.getName()) && m.getParameterCount() >= 5) {
							if (m.getParameterCount() == 6) {
								m.invoke(manager, pos, track, startOffsetMs, range, volume, true);
							} else if (m.getParameterCount() == 5) {
								m.invoke(manager, pos, track, startOffsetMs, range, volume);
							}
							break;
						}
					}
				}
			}
		} catch (Throwable ignored) {
		}
	}

	public static void updateDiscSettings(BlockPos pos, int volume, int range) {
		if (pos == null) return;
		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			if (pos.equals(disc.blockPos)) {
				Object inst = disc.discSoundInstance;
				if (inst != null) {
					try {
						Method mVol = inst.getClass().getMethod("setVolumePercent", int.class);
						mVol.invoke(inst, volume);
					} catch (Throwable ignored) {
					}
					try {
						Method mRange = inst.getClass().getMethod("setRangeBlocks", int.class);
						mRange.invoke(inst, range);
					} catch (Throwable ignored) {
					}
				}
			}
		}
	}

	public static void clientTick() {
		if (ACTIVE_DISCS.isEmpty()) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		long now = System.currentTimeMillis();
		float maxBeat = 0.0F;

		for (Iterator<Map.Entry<Object, ActiveDisc>> it = ACTIVE_DISCS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Object, ActiveDisc> entry = it.next();
			ActiveDisc disc = entry.getValue();

			if (disc.blockPos != null && mc.level != null && mc.level.hasChunkAt(disc.blockPos)) {
				net.minecraft.world.level.block.state.BlockState state = mc.level.getBlockState(disc.blockPos);
				if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HAS_RECORD)
					&& !state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HAS_RECORD)) {
					stopDiscAt(disc.blockPos);
					continue;
				}
			}

			boolean active = disc.playing;
			if (now - disc.lastChunkTimeMs > 3500) {
				if (disc.discSoundInstance instanceof SoundInstance si) {
					if (mc.getSoundManager() != null && !mc.getSoundManager().isActive(si)) {
						active = false;
					}
				} else {
					active = false;
				}
			}

			if (!active) {
				disc.playing = false;
				disc.stopMeterThread();
				disc.beatPulse *= 0.70F;
				disc.kickPulse *= 0.70F;
				disc.snarePulse *= 0.70F;
				disc.hihatPulse *= 0.70F;
				disc.impactPulse *= 0.70F;
				if (disc.beatPulse < 0.01F) {
					disc.beatPulse = 0.0F;
				}

				if (now - disc.lastChunkTimeMs > 5000) {
					it.remove();
				}
				continue;
			}

			updateDiscCoords(disc);

			if (disc.analyzer.consumeBeat()) {
				disc.beatPulse = 1.0F;
			} else {
				disc.beatPulse *= 0.80F;
			}

			float grid = disc.analyzer.getGridPulse();
			if (disc.beatPulse < grid * 0.40F) {
				disc.beatPulse = grid * 0.40F;
			}

			if (disc.analyzer.consumeKick()) {
				disc.kickPulse = 1.0F;
			} else {
				disc.kickPulse *= 0.78F;
			}

			if (disc.analyzer.consumeSnare()) {
				disc.snarePulse = 1.0F;
			} else {
				disc.snarePulse *= 0.82F;
			}

			if (disc.analyzer.consumeHihat()) {
				disc.hihatPulse = 1.0F;
			} else {
				disc.hihatPulse *= 0.85F;
			}

			if (disc.analyzer.consumeImpact()) {
				disc.impactPulse = disc.analyzer.getImpactLevel();
			} else {
				disc.impactPulse *= 0.70F;
			}

			if (disc.beatPulse > maxBeat) {
				maxBeat = disc.beatPulse;
			}
		}

		if (maxBeat > 0.0F) {
			effectTime += 1.0F + 1.5F * maxBeat;
		}
	}

	public static void clear() {
		LATEST_SOUND_INSTANCE = null;
		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			disc.stopMeterThread();
		}
		ACTIVE_DISCS.clear();
	}

	public static float getEffectTime() {
		return effectTime;
	}

	private static float falloff(double dist) {
		double maxRadius = getAudibleRadius();
		if (dist >= maxRadius) return 0.0F;
		float norm = (float) (dist / maxRadius);
		return Math.max(0.0F, 1.0F - norm * norm);
	}

	private static double getDistanceToDisc(ActiveDisc disc, BlockPos pos) {
		if (disc.position != null) {
			return disc.position.distanceTo(Vec3.atCenterOf(pos));
		}
		if (disc.blockPos != null) {
			return Math.sqrt(disc.blockPos.distSqr(pos));
		}
		return Double.MAX_VALUE;
	}

	private static boolean isDiscAtSource(ActiveDisc disc, BlockPos source) {
		if (source == null) return true;
		if (disc == null) return false;
		if (disc.blockPos != null) {
			return disc.blockPos.equals(source);
		}
		if (disc.position != null) {
			BlockPos pos = BlockPos.containing(disc.position.x, disc.position.y, disc.position.z);
			return pos.equals(source);
		}
		return false;
	}

	public static boolean isAnyDiscPlayingNear(Vec3 position) {
		return isPlayingAt(position);
	}

	public static boolean isDiscPlayingAt(BlockPos source) {
		if (source == null || ACTIVE_DISCS.isEmpty()) return false;
		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			if (disc.playing && isDiscAtSource(disc, source)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPlayingAt(Vec3 position) {
		if (JukeboxAudioTracker.getEffectiveVolumeMultiplier() <= 0.001F) return false;
		if (ACTIVE_DISCS.isEmpty()) return false;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		double maxRadius = getAudibleRadius();

		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			if (!disc.playing) continue;
			if (getDistanceToDisc(disc, pos) < maxRadius) {
				return true;
			}
		}
		return false;
	}

	public static boolean isDiscPlayingNear(Vec3 position, BlockPos source) {
		if (JukeboxAudioTracker.getEffectiveVolumeMultiplier() <= 0.001F) return false;
		if (ACTIVE_DISCS.isEmpty()) return false;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		double maxRadius = getAudibleRadius();

		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			if (!disc.playing) continue;
			if (source != null) {
				if (isDiscAtSource(disc, source)) {
					return true;
				}
			} else if (getDistanceToDisc(disc, pos) < maxRadius) {
				return true;
			}
		}
		return false;
	}

	public static float getLevelAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISCS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			if (!disc.playing) continue;
			if (source != null && !isDiscAtSource(disc, source)) continue;

			double dist = getDistanceToDisc(disc, pos);
			float falloff = (source != null && isDiscAtSource(disc, source)) ? 1.0F : falloff(dist);
			if (falloff <= 0.0F) continue;

			float level = Math.max(disc.analyzer.getLevel(), disc.analyzer.getGridPulse() * 0.28F) * falloff;
			if (level > best) best = level;
		}

		return best * JukeboxAudioTracker.getEffectiveVolumeMultiplier();
	}

	public static float getRawLevelAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISCS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			if (!disc.playing) continue;
			if (source != null && !isDiscAtSource(disc, source)) continue;

			double dist = getDistanceToDisc(disc, pos);
			float falloff = (source != null && isDiscAtSource(disc, source)) ? 1.0F : falloff(dist);
			if (falloff <= 0.0F) continue;

			float level = disc.analyzer.getLevel() * falloff;
			if (level > best) best = level;
		}

		return best * JukeboxAudioTracker.getEffectiveVolumeMultiplier();
	}

	public static float getGridPulseAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISCS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			if (!disc.playing) continue;
			if (source != null && !isDiscAtSource(disc, source)) continue;

			double dist = getDistanceToDisc(disc, pos);
			float falloff = (source != null && isDiscAtSource(disc, source)) ? 1.0F : falloff(dist);
			if (falloff <= 0.0F) continue;

			float grid = disc.analyzer.getGridPulse() * falloff;
			if (grid > best) best = grid;
		}

		return best * JukeboxAudioTracker.getEffectiveVolumeMultiplier();
	}

	public static float getBeatPulseAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISCS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			if (!disc.playing) continue;
			if (source != null && !isDiscAtSource(disc, source)) continue;

			double dist = getDistanceToDisc(disc, pos);
			float falloff = (source != null && isDiscAtSource(disc, source)) ? 1.0F : falloff(dist);
			if (falloff <= 0.0F) continue;

			float pulse = disc.beatPulse * falloff;
			if (pulse > best) best = pulse;
		}

		return best * JukeboxAudioTracker.getEffectiveVolumeMultiplier();
	}

	public static float getKickPulseAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISCS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			if (!disc.playing) continue;
			if (source != null && !isDiscAtSource(disc, source)) continue;

			double dist = getDistanceToDisc(disc, pos);
			float falloff = (source != null && isDiscAtSource(disc, source)) ? 1.0F : falloff(dist);
			if (falloff <= 0.0F) continue;

			float pulse = disc.kickPulse * falloff;
			if (pulse > best) best = pulse;
		}

		return best * JukeboxAudioTracker.getEffectiveVolumeMultiplier();
	}

	public static float getSnarePulseAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISCS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			if (!disc.playing) continue;
			if (source != null && !isDiscAtSource(disc, source)) continue;

			double dist = getDistanceToDisc(disc, pos);
			float falloff = (source != null && isDiscAtSource(disc, source)) ? 1.0F : falloff(dist);
			if (falloff <= 0.0F) continue;

			float pulse = disc.snarePulse * falloff;
			if (pulse > best) best = pulse;
		}

		return best * JukeboxAudioTracker.getEffectiveVolumeMultiplier();
	}

	public static float getHihatPulseAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISCS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			if (!disc.playing) continue;
			if (source != null && !isDiscAtSource(disc, source)) continue;

			double dist = getDistanceToDisc(disc, pos);
			float falloff = (source != null && isDiscAtSource(disc, source)) ? 1.0F : falloff(dist);
			if (falloff <= 0.0F) continue;

			float pulse = disc.hihatPulse * falloff;
			if (pulse > best) best = pulse;
		}

		return best * JukeboxAudioTracker.getEffectiveVolumeMultiplier();
	}

	public static float getImpactPulseAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISCS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			if (!disc.playing) continue;
			if (source != null && !isDiscAtSource(disc, source)) continue;

			double dist = getDistanceToDisc(disc, pos);
			float falloff = (source != null && isDiscAtSource(disc, source)) ? 1.0F : falloff(dist);
			if (falloff <= 0.0F) continue;

			float pulse = disc.impactPulse * falloff;
			if (pulse > best) best = pulse;
		}

		return best * JukeboxAudioTracker.getEffectiveVolumeMultiplier();
	}

	public static float getBandAt(Vec3 position, int bandIndex, BlockPos source) {
		if (ACTIVE_DISCS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisc disc : ACTIVE_DISCS.values()) {
			if (!disc.playing) continue;
			if (source != null && !isDiscAtSource(disc, source)) continue;

			double dist = getDistanceToDisc(disc, pos);
			float falloff = (source != null && isDiscAtSource(disc, source)) ? 1.0F : falloff(dist);
			if (falloff <= 0.0F) continue;

			float[] bands = disc.analyzer.getBands();
			if (bands != null && bandIndex >= 0 && bandIndex < bands.length) {
				float val = bands[bandIndex] * falloff;
				if (val > best) best = val;
			}
		}

		return best * JukeboxAudioTracker.getEffectiveVolumeMultiplier();
	}
}
