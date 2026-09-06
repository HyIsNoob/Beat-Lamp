package com.beatlamp.config;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.beatlamp.BeatLamp;

public final class BeatLampConfig {
	private static final String FILE_NAME = "beatlamp-common.properties";

	public static int maxAudioRadius = 64;
	public static int maxGroupLinkSize = 256;
	public static boolean enablePhysicalFireworks = true;
	public static boolean enableRedstoneEmitter = true;
	public static boolean blackoutDisablesRedstone = true;

	private BeatLampConfig() {
	}

	public static void load() {
		Path path = configPath();
		if (!Files.exists(path)) {
			save();
			return;
		}

		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(path)) {
			properties.load(in);

			maxAudioRadius = parseClampedInt(properties.getProperty("maxAudioRadius"), 64, 8, 256);
			maxGroupLinkSize = parseClampedInt(properties.getProperty("maxGroupLinkSize"), 256, 2, 1024);
			enablePhysicalFireworks = parseBoolean(properties.getProperty("enablePhysicalFireworks"), true);
			enableRedstoneEmitter = parseBoolean(properties.getProperty("enableRedstoneEmitter"), true);
			blackoutDisablesRedstone = parseBoolean(properties.getProperty("blackoutDisablesRedstone"), true);
		} catch (Exception e) {
			BeatLamp.LOGGER.warn("Failed to load Beat Lamp common config, using defaults", e);
		}
	}

	public static void save() {
		Path path = configPath();
		Properties properties = new Properties();
		properties.setProperty("maxAudioRadius", String.valueOf(maxAudioRadius));
		properties.setProperty("maxGroupLinkSize", String.valueOf(maxGroupLinkSize));
		properties.setProperty("enablePhysicalFireworks", String.valueOf(enablePhysicalFireworks));
		properties.setProperty("enableRedstoneEmitter", String.valueOf(enableRedstoneEmitter));
		properties.setProperty("blackoutDisablesRedstone", String.valueOf(blackoutDisablesRedstone));

		try {
			Files.createDirectories(path.getParent());
			try (OutputStream out = Files.newOutputStream(path)) {
				properties.store(out, """
					# Beat Lamp Server & Common Configuration
					# maxAudioRadius: Distance in blocks for stage audio tracking & DMX range (8-256, default: 64)
					# maxGroupLinkSize: Maximum devices allowed in a linked group (2-1024, default: 256)
					# enablePhysicalFireworks: Allow Fountain to launch physical firework entities on drop (true/false)
					# enableRedstoneEmitter: Allow Beat Emitter to emit Redstone power (true/false)
					# blackoutDisablesRedstone: Force Redstone to 0 during DMX Blackout (true/false)
					""");
			}
		} catch (Exception e) {
			BeatLamp.LOGGER.warn("Failed to save Beat Lamp common config", e);
		}
	}

	private static int parseClampedInt(String value, int defaultValue, int min, int max) {
		if (value == null) return defaultValue;
		try {
			return Math.max(min, Math.min(max, Integer.parseInt(value.trim())));
		} catch (NumberFormatException ignored) {
			return defaultValue;
		}
	}

	private static boolean parseBoolean(String value, boolean defaultValue) {
		if (value == null) return defaultValue;
		return Boolean.parseBoolean(value.trim());
	}

	private static Path configPath() {
		return Path.of("config", FILE_NAME);
	}
}
