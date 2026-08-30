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
	private static final Path CONFIG_PATH = Path.of("config", "beatlamp.json");

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

	private static AudioQualityProfile qualityProfile = AudioQualityProfile.LITE;

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
		} catch (Exception exception) {
			qualityProfile = AudioQualityProfile.LITE;
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			JsonObject json = new JsonObject();
			json.addProperty("audioQuality", qualityProfile.getId());

			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(json, writer);
			}
		} catch (Exception ignored) {
		}
	}
}
