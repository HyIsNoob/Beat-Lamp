package com.beatlamp.block;

public enum EmitterMode {
	PULSE("screen.beatlamp.emitter.mode.pulse"),
	DROP("screen.beatlamp.emitter.mode.drop"),
	ENERGY("screen.beatlamp.emitter.mode.energy"),
	KICK("screen.beatlamp.emitter.mode.kick"),
	SNARE("screen.beatlamp.emitter.mode.snare"),
	HIHAT("screen.beatlamp.emitter.mode.hihat");

	private final String translationKey;

	EmitterMode(String translationKey) {
		this.translationKey = translationKey;
	}

	public String getTranslationKey() {
		return this.translationKey;
	}

	public EmitterMode next() {
		EmitterMode[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}
}
