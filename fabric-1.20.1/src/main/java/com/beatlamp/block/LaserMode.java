package com.beatlamp.block;

public enum LaserMode {
	FAN_SWEEP("screen.beatlamp.laser.mode.fan_sweep"),
	CONE_SPIN("screen.beatlamp.laser.mode.cone_spin"),
	STATIC_FAN("screen.beatlamp.laser.mode.static_fan"),
	BEAT_BURST("screen.beatlamp.laser.mode.beat_burst");

	private final String translationKey;

	LaserMode(String translationKey) {
		this.translationKey = translationKey;
	}

	public String getTranslationKey() {
		return this.translationKey;
	}

	public LaserMode next() {
		LaserMode[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}
}
