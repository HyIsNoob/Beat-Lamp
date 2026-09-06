package com.beatlamp.block;

public enum LampMode {
	PULSE("screen.beatlamp.mode.pulse"),
	RGB("screen.beatlamp.mode.rgb"),
	SPECTRUM("screen.beatlamp.mode.spectrum"),
	VU_METER("screen.beatlamp.mode.vu_meter"),
	OSCILLOSCOPE("screen.beatlamp.mode.oscilloscope"),
	MATRIX_RAIN("screen.beatlamp.mode.matrix_rain"),
	RIPPLE("screen.beatlamp.mode.ripple"),
	WAVE("screen.beatlamp.mode.wave"),
	SCAN("screen.beatlamp.mode.scan");

	private final String translationKey;

	LampMode(String translationKey) {
		this.translationKey = translationKey;
	}

	public String getTranslationKey() {
		return this.translationKey;
	}

	public LampMode next() {
		LampMode[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}
}
