package com.beatlamp.block;

import net.minecraft.network.chat.Component;

public enum LaserMode {
	FAN_SWEEP("fan_sweep"),
	CONE_SPIN("cone_spin"),
	STATIC_FAN("static_fan"),
	BEAT_BURST("beat_burst");

	private final String name;

	LaserMode(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public Component getDisplayName() {
		return Component.translatable("screen.beatlamp.laser.mode." + this.name);
	}

	public LaserMode next() {
		LaserMode[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}

	public static LaserMode byName(String name) {
		for (LaserMode mode : values()) {
			if (mode.name.equalsIgnoreCase(name)) {
				return mode;
			}
		}
		return FAN_SWEEP;
	}
}
