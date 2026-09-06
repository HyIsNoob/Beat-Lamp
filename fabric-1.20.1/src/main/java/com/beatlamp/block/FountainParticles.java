package com.beatlamp.block;

public enum FountainParticles {
	FLAME("screen.beatlamp.fountain.particle.flame"),
	SOUL_FLAME("screen.beatlamp.fountain.particle.soul_flame"),
	FIREWORK("screen.beatlamp.fountain.particle.firework"),
	GLOW("screen.beatlamp.fountain.particle.glow"),
	SPARK("screen.beatlamp.fountain.particle.spark"),
	DUST("screen.beatlamp.fountain.particle.dust"),
	MIXED("screen.beatlamp.fountain.particle.mixed");

	private final String translationKey;

	FountainParticles(String translationKey) {
		this.translationKey = translationKey;
	}

	public String getTranslationKey() {
		return this.translationKey;
	}

	public FountainParticles next() {
		FountainParticles[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}
}
