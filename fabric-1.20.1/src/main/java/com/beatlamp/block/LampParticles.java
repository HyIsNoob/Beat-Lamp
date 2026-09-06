package com.beatlamp.block;

public enum LampParticles {
	OFF("screen.beatlamp.particles.off"),
	NOTE("screen.beatlamp.particles.note"),
	FIREWORK("screen.beatlamp.particles.firework"),
	GLOW("screen.beatlamp.particles.glow"),
	SOUL("screen.beatlamp.particles.soul");

	private final String translationKey;

	LampParticles(String translationKey) {
		this.translationKey = translationKey;
	}

	public String getTranslationKey() {
		return this.translationKey;
	}

	public LampParticles next() {
		LampParticles[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}
}
