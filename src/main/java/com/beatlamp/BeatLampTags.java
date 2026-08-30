package com.beatlamp;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class BeatLampTags {
	private BeatLampTags() {
	}

	/**
	 * Tag containing blocks that can act as audio sources for Beat Lamp devices (e.g. Jukeboxes, custom mod radios).
	 */
	public static final TagKey<Block> JUKEBOX_SOURCES = TagKey.create(
		Registries.BLOCK,
		ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "jukebox_sources")
	);

	/**
	 * Tag containing all Beat Lamp concert and stage equipment.
	 */
	public static final TagKey<Block> STAGE_DEVICES = TagKey.create(
		Registries.BLOCK,
		ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "stage_devices")
	);
}
