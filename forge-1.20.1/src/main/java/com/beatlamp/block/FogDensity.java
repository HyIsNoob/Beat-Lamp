package com.beatlamp.block;

public enum FogDensity {
	LOW("screen.beatlamp.fog.density.low", 0.5F),
	MEDIUM("screen.beatlamp.fog.density.medium", 1.0F),
	HIGH("screen.beatlamp.fog.density.high", 2.0F);

	private final String translationKey;
	private final float multiplier;

	FogDensity(String translationKey, float multiplier) {
		this.translationKey = translationKey;
		this.multiplier = multiplier;
	}

	public String getTranslationKey() {
		return this.translationKey;
	}

	public float getMultiplier() {
		return this.multiplier;
	}

	public FogDensity next() {
		FogDensity[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}
}
