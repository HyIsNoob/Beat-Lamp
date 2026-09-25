package com.beatlamp.block;

public enum RainbowLedMode {
	RAINBOW,
	BREATHING,
	STROBE,
	STATIC,
	WAVE;

	public RainbowLedMode next() {
		RainbowLedMode[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}
}
