package com.beatlamp.block;

public enum FountainParticles {
	FLAME,
	SOUL_FLAME,
	FIREWORK,
	GLOW,
	SPARK,
	DUST,
	MIXED;

	public FountainParticles next() {
		FountainParticles[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}

	public static FountainParticles byName(String name) {
		for (FountainParticles p : values()) {
			if (p.name().equalsIgnoreCase(name)) {
				return p;
			}
		}
		return FLAME;
	}
}
