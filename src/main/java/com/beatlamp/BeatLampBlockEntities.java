package com.beatlamp;

import com.beatlamp.block.BeatLampBlockEntity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BeatLampBlockEntities {
	public static final BlockEntityType<BeatLampBlockEntity> BEAT_LAMP = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "beat_lamp"),
		BlockEntityType.Builder.of(BeatLampBlockEntity::new, BeatLampBlocks.BEAT_LAMP).build(null)
	);

	public static void register() {
	}
}
