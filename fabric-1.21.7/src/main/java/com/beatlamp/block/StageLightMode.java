package com.beatlamp.block;

public enum StageLightMode {
	SWEEP,
	BEAT_STEP,
	STATIC,
	STROBE,
	CHASE;

	public StageLightMode next() {
		StageLightMode[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}

	public static StageLightMode byName(String name) {
		for (StageLightMode mode : values()) {
			if (mode.name().equalsIgnoreCase(name)) {
				return mode;
			}
		}
		return SWEEP;
	}
}
