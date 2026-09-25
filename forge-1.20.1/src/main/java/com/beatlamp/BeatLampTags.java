package com.beatlamp;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class BeatLampTags {
	private BeatLampTags() {
	}

	public static final TagKey<Block> JUKEBOX_SOURCES = TagKey.create(
		Registries.BLOCK,
		new ResourceLocation(BeatLamp.MOD_ID, "jukebox_sources")
	);

	public static final TagKey<Block> STAGE_DEVICES = TagKey.create(
		Registries.BLOCK,
		new ResourceLocation(BeatLamp.MOD_ID, "stage_devices")
	);
}
