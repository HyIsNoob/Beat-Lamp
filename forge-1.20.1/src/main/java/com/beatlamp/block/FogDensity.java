package com.beatlamp.block;

import net.minecraft.network.chat.Component;

public enum FogDensity {
	LOW("low", 1, 0.03F, 0.5F),
	MEDIUM("medium", 3, 0.06F, 1.0F),
	HIGH("high", 6, 0.10F, 2.0F);

	private final String name;
	private final int particleCount;
	private final float speed;
	private final float multiplier;

	FogDensity(String name, int particleCount, float speed, float multiplier) {
		this.name = name;
		this.particleCount = particleCount;
		this.speed = speed;
		this.multiplier = multiplier;
	}

	public String getName() {
		return this.name;
	}

	public int getParticleCount() {
		return this.particleCount;
	}

	public float getSpeed() {
		return this.speed;
	}

	public float getMultiplier() {
		return this.multiplier;
	}

	public String getTranslationKey() {
		return "screen.beatlamp.fog.density." + this.name;
	}

	public Component getDisplayName() {
		return Component.translatable(this.getTranslationKey());
	}

	public FogDensity next() {
		FogDensity[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}

	public static FogDensity byName(String name) {
		for (FogDensity density : values()) {
			if (density.name.equalsIgnoreCase(name)) {
				return density;
			}
		}
		return MEDIUM;
	}
}
