package com.beatlamp.client.config;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.util.Mth;

public final class BeatLampClientConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = Path.of("config", "beatlamp-client.json");

	public enum AudioQualityProfile {
		LITE("lite"),
		STUDIO("studio");

		private final String id;

		AudioQualityProfile(String id) {
			this.id = id;
		}

		public String getId() {
			return this.id;
		}

		public AudioQualityProfile next() {
			return this == LITE ? STUDIO : LITE;
		}

		public static AudioQualityProfile byId(String id) {
			for (AudioQualityProfile profile : values()) {
				if (profile.id.equalsIgnoreCase(id)) {
					return profile;
				}
			}
			return LITE;
		}
	}

	private static AudioQualityProfile qualityProfile = AudioQualityProfile.STUDIO;
	public static boolean studioQuality = true;
	public static int beamRenderDistance = 64;
	public static float laserRenderIntensity = 1.0F;
	public static boolean enableFogParticles = true;
	public static float particleDensityMultiplier = 1.0F;
	public static boolean enableLaserBeams = true;

	public static AudioQualityProfile getQualityProfile() {
		return qualityProfile;
	}

	public static void setQualityProfile(AudioQualityProfile newProfile) {
		if (newProfile != null && qualityProfile != newProfile) {
			qualityProfile = newProfile;
			save();
		}
	}

	public static boolean isStudioQuality() {
		return qualityProfile == AudioQualityProfile.STUDIO;
	}

	public static void load() {
		if (!Files.exists(CONFIG_PATH)) {
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			if (json.has("audioQuality")) {
				qualityProfile = AudioQualityProfile.byId(json.get("audioQuality").getAsString());
			}
			if (json.has("beamRenderDistance")) {
				beamRenderDistance = Mth.clamp(json.get("beamRenderDistance").getAsInt(), 16, 256);
			}
			if (json.has("laserRenderIntensity")) {
				laserRenderIntensity = Mth.clamp(json.get("laserRenderIntensity").getAsFloat(), 0.0F, 2.0F);
			}
			if (json.has("enableFogParticles")) {
				enableFogParticles = json.get("enableFogParticles").getAsBoolean();
			}
			if (json.has("particleDensityMultiplier")) {
				particleDensityMultiplier = Mth.clamp(json.get("particleDensityMultiplier").getAsFloat(), 0.0F, 2.0F);
			}
			if (json.has("enableLaserBeams")) {
				enableLaserBeams = json.get("enableLaserBeams").getAsBoolean();
			}
		} catch (Exception exception) {
			qualityProfile = AudioQualityProfile.STUDIO;
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			JsonObject json = new JsonObject();
			json.addProperty("audioQuality", qualityProfile.getId());
			json.addProperty("beamRenderDistance", beamRenderDistance);
			json.addProperty("laserRenderIntensity", laserRenderIntensity);
			json.addProperty("enableFogParticles", enableFogParticles);
			json.addProperty("particleDensityMultiplier", particleDensityMultiplier);
			json.addProperty("enableLaserBeams", enableLaserBeams);

			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(json, writer);
			}
		} catch (Exception ignored) {
		}
	}
}
