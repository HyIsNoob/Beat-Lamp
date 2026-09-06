package com.beatlamp.client.config;

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

public final class BeatLampClientConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("beatlamp");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final File CONFIG_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "beatlamp-client.json");

	public static int maxVisualizerLights = 64;
	public static float particleDensityMultiplier = 1.0F;
	public static boolean enableBloomGlow = true;
	public static boolean studioQuality = true;

	private BeatLampClientConfig() {
	}

	public static boolean isStudioQuality() {
		return studioQuality;
	}

	public static void load() {
		if (!CONFIG_FILE.exists()) {
			save();
			return;
		}

		try (FileReader reader = new FileReader(CONFIG_FILE)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			if (json.has("maxVisualizerLights")) {
				maxVisualizerLights = json.get("maxVisualizerLights").getAsInt();
			}
			if (json.has("particleDensityMultiplier")) {
				particleDensityMultiplier = json.get("particleDensityMultiplier").getAsFloat();
			}
			if (json.has("enableBloomGlow")) {
				enableBloomGlow = json.get("enableBloomGlow").getAsBoolean();
			}
			if (json.has("studioQuality")) {
				studioQuality = json.get("studioQuality").getAsBoolean();
			}
			LOGGER.info("Loaded client config from {}", CONFIG_FILE.getName());
		} catch (Exception e) {
			LOGGER.error("Failed to load client config", e);
		}
	}

	public static void save() {
		JsonObject json = new JsonObject();
		json.addProperty("maxVisualizerLights", maxVisualizerLights);
		json.addProperty("particleDensityMultiplier", particleDensityMultiplier);
		json.addProperty("enableBloomGlow", enableBloomGlow);
		json.addProperty("studioQuality", studioQuality);

		try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
			GSON.toJson(json, writer);
		} catch (IOException e) {
			LOGGER.error("Failed to save client config", e);
		}
	}
}
