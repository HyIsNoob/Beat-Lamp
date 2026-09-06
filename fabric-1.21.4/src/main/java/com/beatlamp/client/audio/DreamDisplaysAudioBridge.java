package com.beatlamp.client.audio;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.beatlamp.BeatLamp;
import com.beatlamp.client.config.BeatLampClientConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class DreamDisplaysAudioBridge {
	private static final double AUDIBLE_RADIUS = 64.0;
	private static final Map<UUID, ActiveDisplay> ACTIVE_DISPLAYS = new ConcurrentHashMap<>();
	private static final ThreadLocal<float[]> MONO_BUFFER = ThreadLocal.withInitial(() -> new float[4096]);
	private static float effectTime;

	private static boolean initialized = false;
	private static boolean available = false;
	private static Method screensMethod;
	private static Object registryInstance;
	private static Field screensField;
	private static Method getDistanceMethod;
	private static Method getPosMethod;
	private static Method isInScreenMethod;
	private static Method isPausedMethod;
	private static Method getDimensionMethod;
	private static volatile boolean firstChunkLogged = false;

	private DreamDisplaysAudioBridge() {
	}

	public static final class ActiveDisplay {
		public final UUID uuid;
		public final AudioAnalyzer analyzer;
		public volatile long lastChunkTimeMs;
		public volatile boolean playing;
		public float beatPulse;
		public float kickPulse;
		public float snarePulse;
		public float hihatPulse;
		public float impactPulse;

		public ActiveDisplay(UUID uuid) {
			this.uuid = uuid;
			this.analyzer = new AudioAnalyzer(44100, BeatLampClientConfig.isStudioQuality());
			this.lastChunkTimeMs = System.currentTimeMillis();
			this.playing = true;
		}
	}

	private static synchronized void ensureInitialized() {
		if (initialized) {
			return;
		}
		initialized = true;

		try {
			Class<?> registryClass = Class.forName("com.dreamdisplays.platform.client.displays.DisplayRegistry");
			try {
				screensMethod = registryClass.getMethod("getScreens");
				Field instanceField = registryClass.getField("INSTANCE");
				registryInstance = instanceField.get(null);
			} catch (Throwable t) {
				try {
					screensField = registryClass.getDeclaredField("screens");
					screensField.setAccessible(true);
				} catch (Throwable t2) {
					BeatLamp.LOGGER.warn("Could not reflect DisplayRegistry.screens: {}", t2.getMessage());
				}
			}

			Class<?> screenClass = Class.forName("com.dreamdisplays.platform.client.displays.DisplayScreen");
			try {
				getDistanceMethod = screenClass.getMethod("getDistanceToScreen", BlockPos.class);
			} catch (Throwable t) {
				BeatLamp.LOGGER.warn("Could not find getDistanceToScreen: {}", t.getMessage());
			}

			try {
				isInScreenMethod = screenClass.getMethod("isInScreen", BlockPos.class);
			} catch (Throwable ignored) {
			}

			try {
				getPosMethod = screenClass.getMethod("getPos");
			} catch (NoSuchMethodException e) {
				try {
					getPosMethod = screenClass.getMethod("pos");
				} catch (NoSuchMethodException ignored) {
				}
			}

			try {
				isPausedMethod = screenClass.getMethod("isPaused");
			} catch (NoSuchMethodException e) {
				try {
					isPausedMethod = screenClass.getMethod("getPaused");
				} catch (NoSuchMethodException ignored) {
				}
			}

			try {
				getDimensionMethod = screenClass.getMethod("getDimensionKey");
			} catch (NoSuchMethodException ignored) {
			}

			available = true;
			BeatLamp.LOGGER.info("Beat Lamp successfully hooked DreamDisplays client integration!");
		} catch (Throwable t) {
			available = false;
			BeatLamp.LOGGER.warn("DreamDisplays client integration reflection failed: {}", t.getMessage());
		}
	}

	public static boolean isAvailable() {
		if (!initialized) {
			ensureInitialized();
		}
		return available;
	}

	private static Map<UUID, ?> getScreensMap() {
		if (!isAvailable()) {
			return null;
		}
		if (screensMethod != null && registryInstance != null) {
			try {
				return (Map<UUID, ?>) screensMethod.invoke(registryInstance);
			} catch (Throwable ignored) {
			}
		}
		if (screensField != null) {
			try {
				return (Map<UUID, ?>) screensField.get(null);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	private static Object getScreen(UUID uuid) {
		Map<UUID, ?> map = getScreensMap();
		return map != null ? map.get(uuid) : null;
	}

	private static double getDistanceToScreen(Object screen, BlockPos pos) {
		if (screen == null || getDistanceMethod == null) {
			return Double.MAX_VALUE;
		}
		try {
			return (Double) getDistanceMethod.invoke(screen, pos);
		} catch (Throwable t) {
			return Double.MAX_VALUE;
		}
	}

	private static boolean isInScreen(Object screen, BlockPos pos) {
		if (screen == null || isInScreenMethod == null) {
			return false;
		}
		try {
			return (Boolean) isInScreenMethod.invoke(screen, pos);
		} catch (Throwable t) {
			return false;
		}
	}

	private static BlockPos getScreenPos(Object screen) {
		if (screen == null || getPosMethod == null) {
			return null;
		}
		try {
			return (BlockPos) getPosMethod.invoke(screen);
		} catch (Throwable t) {
			return null;
		}
	}

	private static boolean isScreenPaused(Object screen) {
		if (screen == null || isPausedMethod == null) {
			return false;
		}
		try {
			return (Boolean) isPausedMethod.invoke(screen);
		} catch (Throwable t) {
			return false;
		}
	}

	private static String getScreenDimension(Object screen) {
		if (screen == null || getDimensionMethod == null) {
			return null;
		}
		try {
			return (String) getDimensionMethod.invoke(screen);
		} catch (Throwable t) {
			return null;
		}
	}

	public static void onAudioChunk(String debugLabel, byte[] chunk, int len) {
		if (debugLabel == null || chunk == null || len <= 0) {
			return;
		}

		int slash = debugLabel.indexOf('/');
		String uuidStr = slash > 0 ? debugLabel.substring(0, slash) : debugLabel;
		UUID uuid;
		try {
			uuid = UUID.fromString(uuidStr);
		} catch (IllegalArgumentException e) {
			return;
		}

		int frames = len / 4;
		if (frames <= 0) {
			return;
		}

		float[] mono = MONO_BUFFER.get();
		if (mono.length < frames) {
			mono = new float[frames];
			MONO_BUFFER.set(mono);
		}

		for (int i = 0; i < frames; i++) {
			int idx = i * 4;
			short left = (short) ((chunk[idx] & 0xFF) | (chunk[idx + 1] << 8));
			short right = (short) ((chunk[idx + 2] & 0xFF) | (chunk[idx + 3] << 8));
			mono[i] = ((left / 32768.0F) + (right / 32768.0F)) * 0.5F;
		}

		ActiveDisplay display = ACTIVE_DISPLAYS.computeIfAbsent(uuid, ActiveDisplay::new);
		display.lastChunkTimeMs = System.currentTimeMillis();
		display.playing = true;
		display.analyzer.push(mono, frames);

		if (!firstChunkLogged) {
			firstChunkLogged = true;
			BeatLamp.LOGGER.info("Beat Lamp received first DreamDisplays audio chunk! (uuid={}, frames={})", uuid, frames);
		}
	}

	public static void onAudioStopped(String debugLabel) {
		if (debugLabel == null) return;
		int slash = debugLabel.indexOf('/');
		String uuidStr = slash > 0 ? debugLabel.substring(0, slash) : debugLabel;
		try {
			UUID uuid = UUID.fromString(uuidStr);
			ActiveDisplay display = ACTIVE_DISPLAYS.get(uuid);
			if (display != null) {
				display.playing = false;
				display.beatPulse = 0.0F;
				display.kickPulse = 0.0F;
				display.snarePulse = 0.0F;
				display.hihatPulse = 0.0F;
				display.impactPulse = 0.0F;
			}
		} catch (IllegalArgumentException ignored) {
		}
	}

	public static void onAudioPaused(String debugLabel) {
		if (debugLabel == null) return;
		int slash = debugLabel.indexOf('/');
		String uuidStr = slash > 0 ? debugLabel.substring(0, slash) : debugLabel;
		try {
			UUID uuid = UUID.fromString(uuidStr);
			ActiveDisplay display = ACTIVE_DISPLAYS.get(uuid);
			if (display != null) {
				display.playing = false;
			}
		} catch (IllegalArgumentException ignored) {
		}
	}

	public static void onAudioResumed(String debugLabel) {
		if (debugLabel == null) return;
		int slash = debugLabel.indexOf('/');
		String uuidStr = slash > 0 ? debugLabel.substring(0, slash) : debugLabel;
		try {
			UUID uuid = UUID.fromString(uuidStr);
			ActiveDisplay display = ACTIVE_DISPLAYS.get(uuid);
			if (display != null) {
				display.playing = true;
				display.lastChunkTimeMs = System.currentTimeMillis();
			}
		} catch (IllegalArgumentException ignored) {
		}
	}

	public static void clientTick() {
		if (ACTIVE_DISPLAYS.isEmpty()) {
			return;
		}

		long now = System.currentTimeMillis();
		float maxBeat = 0.0F;

		for (Iterator<Map.Entry<UUID, ActiveDisplay>> it = ACTIVE_DISPLAYS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, ActiveDisplay> entry = it.next();
			UUID uuid = entry.getKey();
			ActiveDisplay display = entry.getValue();

			Object screen = getScreen(uuid);
			boolean screenPaused = screen != null && isScreenPaused(screen);
			boolean timedOut = (now - display.lastChunkTimeMs > 250);

			if (screenPaused || timedOut || !display.playing) {
				display.beatPulse *= 0.70F;
				display.kickPulse *= 0.70F;
				display.snarePulse *= 0.70F;
				display.hihatPulse *= 0.70F;
				display.impactPulse *= 0.70F;
				if (display.beatPulse < 0.01F) {
					display.beatPulse = 0.0F;
				}

				if (now - display.lastChunkTimeMs > 1500) {
					it.remove();
				}
				continue;
			}

			if (display.analyzer.consumeBeat()) {
				display.beatPulse = 1.0F;
			} else {
				display.beatPulse *= 0.80F;
			}

			float grid = display.analyzer.getGridPulse();
			if (display.beatPulse < grid * 0.40F) {
				display.beatPulse = grid * 0.40F;
			}

			if (display.analyzer.consumeKick()) {
				display.kickPulse = 1.0F;
			} else {
				display.kickPulse *= 0.78F;
			}

			if (display.analyzer.consumeSnare()) {
				display.snarePulse = 1.0F;
			} else {
				display.snarePulse *= 0.82F;
			}

			if (display.analyzer.consumeHihat()) {
				display.hihatPulse = 1.0F;
			} else {
				display.hihatPulse *= 0.85F;
			}

			if (display.analyzer.consumeImpact()) {
				display.impactPulse = display.analyzer.getImpactLevel();
			} else {
				display.impactPulse *= 0.70F;
			}

			if (display.beatPulse > maxBeat) {
				maxBeat = display.beatPulse;
			}
		}

		if (maxBeat > 0.0F) {
			effectTime += 1.0F + 1.5F * maxBeat;
		}
	}

	public static void clear() {
		ACTIVE_DISPLAYS.clear();
	}

	public static float getEffectTime() {
		return effectTime;
	}

	private static boolean checkDimension(Object screen) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && screen != null) {
			String currentDim = mc.level.dimension().location().toString();
			String screenDim = getScreenDimension(screen);
			if (screenDim != null && !screenDim.isEmpty() && !screenDim.equals(currentDim)) {
				return false;
			}
		}
		return true;
	}

	private static boolean matchesSource(Object screen, BlockPos source) {
		if (screen == null || source == null) {
			return false;
		}
		if (isInScreen(screen, source)) {
			return true;
		}
		BlockPos anchor = getScreenPos(screen);
		return anchor != null && anchor.equals(source);
	}

	public static boolean isAnyDisplayPlayingNear(Vec3 position) {
		if (ACTIVE_DISPLAYS.isEmpty()) return false;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);

		for (ActiveDisplay display : ACTIVE_DISPLAYS.values()) {
			if (!display.playing || System.currentTimeMillis() - display.lastChunkTimeMs > 250) continue;
			Object screen = getScreen(display.uuid);
			if (screen != null) {
				if (!checkDimension(screen)) continue;
				if (getDistanceToScreen(screen, pos) < AUDIBLE_RADIUS) {
					return true;
				}
			} else {
				return true;
			}
		}
		return false;
	}

	public static boolean isDisplayPlayingNear(Vec3 position, BlockPos source) {
		if (ACTIVE_DISPLAYS.isEmpty()) return false;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);

		for (ActiveDisplay display : ACTIVE_DISPLAYS.values()) {
			if (!display.playing || System.currentTimeMillis() - display.lastChunkTimeMs > 250) continue;
			Object screen = getScreen(display.uuid);
			if (screen != null) {
				if (!checkDimension(screen)) continue;
				if (source != null && !matchesSource(screen, source)) continue;
				if (getDistanceToScreen(screen, pos) < AUDIBLE_RADIUS) {
					return true;
				}
			} else {
				if (source == null) return true;
			}
		}
		return false;
	}

	public static float getLevelAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISPLAYS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisplay display : ACTIVE_DISPLAYS.values()) {
			if (!display.playing || System.currentTimeMillis() - display.lastChunkTimeMs > 250) continue;
			Object screen = getScreen(display.uuid);
			float falloff = 1.0F;
			if (screen != null) {
				if (!checkDimension(screen)) continue;
				if (source != null && !matchesSource(screen, source)) continue;

				double dist = getDistanceToScreen(screen, pos);
				falloff = falloff(dist);
				if (falloff <= 0.0F) continue;
			} else if (source != null) {
				continue;
			}

			float level = Math.max(display.analyzer.getLevel(), display.analyzer.getGridPulse() * 0.28F) * falloff;
			if (level > best) best = level;
		}

		return best;
	}

	public static float getRawLevelAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISPLAYS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisplay display : ACTIVE_DISPLAYS.values()) {
			if (!display.playing || System.currentTimeMillis() - display.lastChunkTimeMs > 250) continue;
			Object screen = getScreen(display.uuid);
			float falloff = 1.0F;
			if (screen != null) {
				if (!checkDimension(screen)) continue;
				if (source != null && !matchesSource(screen, source)) continue;

				double dist = getDistanceToScreen(screen, pos);
				falloff = falloff(dist);
				if (falloff <= 0.0F) continue;
			} else if (source != null) {
				continue;
			}

			float level = display.analyzer.getLevel() * falloff;
			if (level > best) best = level;
		}

		return best;
	}

	public static float getGridPulseAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISPLAYS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisplay display : ACTIVE_DISPLAYS.values()) {
			if (!display.playing || System.currentTimeMillis() - display.lastChunkTimeMs > 250) continue;
			Object screen = getScreen(display.uuid);
			float falloff = 1.0F;
			if (screen != null) {
				if (!checkDimension(screen)) continue;
				if (source != null && !matchesSource(screen, source)) continue;

				double dist = getDistanceToScreen(screen, pos);
				falloff = falloff(dist);
				if (falloff <= 0.0F) continue;
			} else if (source != null) {
				continue;
			}

			float grid = display.analyzer.getGridPulse() * falloff;
			if (grid > best) best = grid;
		}

		return best;
	}

	public static float getBeatPulseAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISPLAYS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisplay display : ACTIVE_DISPLAYS.values()) {
			if (!display.playing || System.currentTimeMillis() - display.lastChunkTimeMs > 250) continue;
			Object screen = getScreen(display.uuid);
			float falloff = 1.0F;
			if (screen != null) {
				if (!checkDimension(screen)) continue;
				if (source != null && !matchesSource(screen, source)) continue;

				double dist = getDistanceToScreen(screen, pos);
				falloff = falloff(dist);
				if (falloff <= 0.0F) continue;
			} else if (source != null) {
				continue;
			}

			float pulse = display.beatPulse * falloff;
			if (pulse > best) best = pulse;
		}

		return best;
	}

	public static float getKickPulseAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISPLAYS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisplay display : ACTIVE_DISPLAYS.values()) {
			if (!display.playing || System.currentTimeMillis() - display.lastChunkTimeMs > 250) continue;
			Object screen = getScreen(display.uuid);
			float falloff = 1.0F;
			if (screen != null) {
				if (!checkDimension(screen)) continue;
				if (source != null && !matchesSource(screen, source)) continue;

				double dist = getDistanceToScreen(screen, pos);
				falloff = falloff(dist);
				if (falloff <= 0.0F) continue;
			} else if (source != null) {
				continue;
			}

			float pulse = display.kickPulse * falloff;
			if (pulse > best) best = pulse;
		}

		return best;
	}

	public static float getSnarePulseAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISPLAYS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisplay display : ACTIVE_DISPLAYS.values()) {
			if (!display.playing || System.currentTimeMillis() - display.lastChunkTimeMs > 250) continue;
			Object screen = getScreen(display.uuid);
			float falloff = 1.0F;
			if (screen != null) {
				if (!checkDimension(screen)) continue;
				if (source != null && !matchesSource(screen, source)) continue;

				double dist = getDistanceToScreen(screen, pos);
				falloff = falloff(dist);
				if (falloff <= 0.0F) continue;
			} else if (source != null) {
				continue;
			}

			float pulse = display.snarePulse * falloff;
			if (pulse > best) best = pulse;
		}

		return best;
	}

	public static float getHihatPulseAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISPLAYS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisplay display : ACTIVE_DISPLAYS.values()) {
			if (!display.playing || System.currentTimeMillis() - display.lastChunkTimeMs > 250) continue;
			Object screen = getScreen(display.uuid);
			float falloff = 1.0F;
			if (screen != null) {
				if (!checkDimension(screen)) continue;
				if (source != null && !matchesSource(screen, source)) continue;

				double dist = getDistanceToScreen(screen, pos);
				falloff = falloff(dist);
				if (falloff <= 0.0F) continue;
			} else if (source != null) {
				continue;
			}

			float pulse = display.hihatPulse * falloff;
			if (pulse > best) best = pulse;
		}

		return best;
	}

	public static float getImpactPulseAt(Vec3 position, BlockPos source) {
		if (ACTIVE_DISPLAYS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = 0.0F;

		for (ActiveDisplay display : ACTIVE_DISPLAYS.values()) {
			if (!display.playing || System.currentTimeMillis() - display.lastChunkTimeMs > 250) continue;
			Object screen = getScreen(display.uuid);
			float falloff = 1.0F;
			if (screen != null) {
				if (!checkDimension(screen)) continue;
				if (source != null && !matchesSource(screen, source)) continue;

				double dist = getDistanceToScreen(screen, pos);
				falloff = falloff(dist);
				if (falloff <= 0.0F) continue;
			} else if (source != null) {
				continue;
			}

			float pulse = display.impactPulse * falloff;
			if (pulse > best) best = pulse;
		}

		return best;
	}

	public static float getBandAt(Vec3 position, int band, BlockPos source) {
		if (ACTIVE_DISPLAYS.isEmpty()) return 0.0F;
		BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
		float best = -1.0F;
		ActiveDisplay bestDisplay = null;
		float bestFalloff = 0.0F;

		for (ActiveDisplay display : ACTIVE_DISPLAYS.values()) {
			if (!display.playing || System.currentTimeMillis() - display.lastChunkTimeMs > 250) continue;
			Object screen = getScreen(display.uuid);
			float falloff = 1.0F;
			if (screen != null) {
				if (!checkDimension(screen)) continue;
				if (source != null && !matchesSource(screen, source)) continue;

				double dist = getDistanceToScreen(screen, pos);
				falloff = falloff(dist);
				if (falloff <= 0.0F) continue;
			} else if (source != null) {
				continue;
			}

			float level = display.analyzer.getLevel() * falloff;
			if (level > best) {
				best = level;
				bestDisplay = display;
				bestFalloff = falloff;
			}
		}

		if (bestDisplay == null) return 0.0F;
		float[] bands = bestDisplay.analyzer.getBands();
		if (band < 0 || band >= bands.length) return 0.0F;
		return source != null ? bands[band] : bands[band] * bestFalloff;
	}

	private static float falloff(double distance) {
		if (distance >= AUDIBLE_RADIUS) {
			return 0.0F;
		}
		return (float) (1.0 - distance / AUDIBLE_RADIUS);
	}
}
