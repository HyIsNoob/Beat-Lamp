package com.beatlamp.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraftforge.fml.loading.FMLPaths;

public final class BeatLampConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("beatlamp");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final File CONFIG_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "beatlamp.json");

	public static int maxGroupFloodFill = 256;
	public static int maxGroupLinkSize = 256;
	public static double maxLinkerDistance = 64.0;
	public static double defaultAudibleRadius = 64.0;
	public static boolean enableDmxProtocol = true;

	private BeatLampConfig() {
	}

	public static void load() {
		if (!CONFIG_FILE.exists()) {
			save();
			return;
		}

		try (FileReader reader = new FileReader(CONFIG_FILE)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			if (json.has("maxGroupLinkSize")) {
				maxGroupLinkSize = json.get("maxGroupLinkSize").getAsInt();
			} else if (json.has("maxGroupFloodFill")) {
				maxGroupLinkSize = json.get("maxGroupFloodFill").getAsInt();
			}
			if (json.has("maxGroupFloodFill")) {
				maxGroupFloodFill = json.get("maxGroupFloodFill").getAsInt();
			}
			if (json.has("maxLinkerDistance")) {
				maxLinkerDistance = json.get("maxLinkerDistance").getAsDouble();
			}
			if (json.has("defaultAudibleRadius")) {
				defaultAudibleRadius = json.get("defaultAudibleRadius").getAsDouble();
			}
			if (json.has("enableDmxProtocol")) {
				enableDmxProtocol = json.get("enableDmxProtocol").getAsBoolean();
			}
			LOGGER.info("Loaded server config from {}", CONFIG_FILE.getName());
		} catch (Exception e) {
			LOGGER.error("Failed to load server config", e);
		}
	}

	public static void save() {
		JsonObject json = new JsonObject();
		json.addProperty("maxGroupFloodFill", maxGroupFloodFill);
		json.addProperty("maxGroupLinkSize", maxGroupLinkSize);
		json.addProperty("maxLinkerDistance", maxLinkerDistance);
		json.addProperty("defaultAudibleRadius", defaultAudibleRadius);
		json.addProperty("enableDmxProtocol", enableDmxProtocol);

		try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
			GSON.toJson(json, writer);
		} catch (IOException e) {
			LOGGER.error("Failed to save server config", e);
		}
	}
}
