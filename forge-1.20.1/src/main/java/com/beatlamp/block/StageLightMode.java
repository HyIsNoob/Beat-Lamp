package com.beatlamp.block;

public enum StageLightMode {
	SWEEP("screen.beatlamp.stagelight.mode.sweep"),
	BEAT_STEP("screen.beatlamp.stagelight.mode.beat_step"),
	STATIC("screen.beatlamp.stagelight.mode.static"),
	STROBE("screen.beatlamp.stagelight.mode.strobe"),
	CHASE("screen.beatlamp.stagelight.mode.chase");

	private final String translationKey;

	StageLightMode(String translationKey) {
		this.translationKey = translationKey;
	}

	public String getTranslationKey() {
		return this.translationKey;
	}

	public StageLightMode next() {
		StageLightMode[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}
}
