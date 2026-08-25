package com.beatlamp.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public final class BeatLampClientConfig {
	public static boolean highQualityBeat = true;
	private static final String FILE_NAME = "beatlamp-client.properties";

	private BeatLampClientConfig() {
	}

	public static void load() {
		Path path = configPath();

		if (!Files.exists(path)) {
			return;
		}

		Properties properties = new Properties();

		try (InputStream inputStream = Files.newInputStream(path)) {
			properties.load(inputStream);
			highQualityBeat = !"low".equalsIgnoreCase(properties.getProperty("audioQuality", "high").trim());
		} catch (Exception ignored) {
		}
	}

	public static void save() {
		Path path = configPath();
		Properties properties = new Properties();
		properties.setProperty("audioQuality", highQualityBeat ? "high" : "low");

		try {
			Files.createDirectories(path.getParent());

			try (OutputStream outputStream = Files.newOutputStream(path)) {
				properties.store(outputStream, "Beat Lamp client config");
			}
		} catch (Exception ignored) {
		}
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}
}
