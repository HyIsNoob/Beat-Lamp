package com.beatlamp.block;

import net.minecraft.network.chat.Component;

public enum EmitterMode {
	PULSE("Pulse (Beat Clock)"),
	DROP("Drop Pulse (TNT / Pyros)"),
	ENERGY("Continuous (0-15 Energy)"),
	KICK("Kick Drum (Bass)"),
	SNARE("Snare / Clap"),
	HIHAT("Hi-Hat (Treble)");

	private final String displayName;

	EmitterMode(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public Component getTranslation() {
		return Component.translatable("screen.beatlamp.emitter.mode." + this.name().toLowerCase());
	}

	public EmitterMode next() {
		EmitterMode[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}
}
