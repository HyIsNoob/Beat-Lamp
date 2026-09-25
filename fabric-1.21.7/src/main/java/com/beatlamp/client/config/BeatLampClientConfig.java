package com.beatlamp.client.config;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class BeatLampClientConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = Path.of("config", "beatlamp-client.json");

	public enum AudioQualityProfile {
		LITE("lite"),
		STUDIO("studio"),
		OFF("off");

		private final String id;

		AudioQualityProfile(String id) {
			this.id = id;
		}

		public String getId() {
			return this.id;
		}

		public AudioQualityProfile next() {
			return switch (this) {
				case STUDIO -> LITE;
				case LITE -> OFF;
				case OFF -> STUDIO;
			};
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

	public enum BeamQuality {
		HIGH("high"),
		MEDIUM("medium"),
		OFF("off");

		private final String id;

		BeamQuality(String id) {
			this.id = id;
		}

		public String getId() {
			return this.id;
		}

		public BeamQuality next() {
			return switch (this) {
				case HIGH -> MEDIUM;
				case MEDIUM -> OFF;
				case OFF -> HIGH;
			};
		}

		public static BeamQuality byId(String id) {
			for (BeamQuality quality : values()) {
				if (quality.id.equalsIgnoreCase(id)) {
					return quality;
				}
			}
			return HIGH;
		}
	}

	private static AudioQualityProfile qualityProfile = AudioQualityProfile.STUDIO;
	public static boolean enableStageEffects = true;
	public static boolean antiStrobe = false;
	public static BeamQuality beamQuality = BeamQuality.HIGH;
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

	public static boolean isAudioDisabled() {
		return qualityProfile == AudioQualityProfile.OFF;
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
			if (json.has("enableStageEffects")) {
				enableStageEffects = json.get("enableStageEffects").getAsBoolean();
			}
			if (json.has("antiStrobe")) {
				antiStrobe = json.get("antiStrobe").getAsBoolean();
			}
			if (json.has("beamQuality")) {
				beamQuality = BeamQuality.byId(json.get("beamQuality").getAsString());
			}
			if (json.has("beamRenderDistance")) {
				beamRenderDistance = Math.clamp(json.get("beamRenderDistance").getAsInt(), 16, 256);
			}
			if (json.has("laserRenderIntensity")) {
				laserRenderIntensity = Math.clamp(json.get("laserRenderIntensity").getAsFloat(), 0.0F, 2.0F);
			}
			if (json.has("enableFogParticles")) {
				enableFogParticles = json.get("enableFogParticles").getAsBoolean();
			}
			if (json.has("particleDensityMultiplier")) {
				particleDensityMultiplier = Math.clamp(json.get("particleDensityMultiplier").getAsFloat(), 0.0F, 2.0F);
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
			json.addProperty("enableStageEffects", enableStageEffects);
			json.addProperty("antiStrobe", antiStrobe);
			json.addProperty("beamQuality", beamQuality.getId());
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
