package com.beatlamp.block;

import net.minecraft.network.chat.Component;

public enum FogDensity {
	LOW("low", 1, 0.03F),
	MEDIUM("medium", 3, 0.06F),
	HIGH("high", 6, 0.10F);

	private final String name;
	private final int particleCount;
	private final float speed;

	FogDensity(String name, int particleCount, float speed) {
		this.name = name;
		this.particleCount = particleCount;
		this.speed = speed;
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

	public Component getDisplayName() {
		return Component.translatable("screen.beatlamp.fog.density." + this.name);
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
