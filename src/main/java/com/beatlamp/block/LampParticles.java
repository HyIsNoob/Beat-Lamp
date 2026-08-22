package com.beatlamp.block;

import net.minecraft.util.StringRepresentable;

public enum LampParticles implements StringRepresentable {
	OFF("off"),
	NOTE("note"),
	END_ROD("end_rod"),
	FIREWORK("firework"),
	GLOW("glow"),
	MIXED("mixed");

	private final String name;

	LampParticles(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	public LampParticles next() {
		return values()[(this.ordinal() + 1) % values().length];
	}

	public static LampParticles byName(String name) {
		for (LampParticles value : values()) {
			if (value.name.equals(name)) {
				return value;
			}
		}

		return NOTE;
	}
}
