package com.beatlamp.block;

import net.minecraft.util.StringRepresentable;

public enum LampMode implements StringRepresentable {
	PULSE("pulse"),
	RGB("rgb"),
	SPECTRUM("spectrum"),
	RIPPLE("ripple"),
	WAVE("wave"),
	SCAN("scan");

	private final String name;

	LampMode(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	public LampMode next() {
		return values()[(this.ordinal() + 1) % values().length];
	}

	public static LampMode byName(String name) {
		for (LampMode lampMode : values()) {
			if (lampMode.name.equals(name)) {
				return lampMode;
			}
		}

		return PULSE;
	}
}
