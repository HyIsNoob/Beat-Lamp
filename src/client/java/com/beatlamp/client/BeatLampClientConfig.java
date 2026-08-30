package com.beatlamp.client;

/**
 * @deprecated Use {@link com.beatlamp.client.config.BeatLampClientConfig} instead.
 */
@Deprecated
public final class BeatLampClientConfig {
	private BeatLampClientConfig() {
	}

	public static void load() {
		com.beatlamp.client.config.BeatLampClientConfig.load();
	}

	public static void save() {
		com.beatlamp.client.config.BeatLampClientConfig.save();
	}
}
