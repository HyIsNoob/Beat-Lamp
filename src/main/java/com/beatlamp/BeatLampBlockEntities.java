package com.beatlamp;

import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.block.StageJukeboxBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;

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

	public static final BlockEntityType<BeatEmitterBlockEntity> BEAT_EMITTER = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "beat_emitter"),
		BlockEntityType.Builder.of(BeatEmitterBlockEntity::new, BeatLampBlocks.BEAT_EMITTER).build(null)
	);

	public static final BlockEntityType<StageLightBlockEntity> STAGE_LIGHT = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "stage_light"),
		BlockEntityType.Builder.of(StageLightBlockEntity::new, BeatLampBlocks.STAGE_LIGHT).build(null)
	);

	public static final BlockEntityType<FountainBlockEntity> FOUNTAIN = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "fountain"),
		BlockEntityType.Builder.of(FountainBlockEntity::new, BeatLampBlocks.FOUNTAIN).build(null)
	);

	public static final BlockEntityType<LaserProjectorBlockEntity> LASER_PROJECTOR = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "laser_projector"),
		BlockEntityType.Builder.of(LaserProjectorBlockEntity::new, BeatLampBlocks.LASER_PROJECTOR).build(null)
	);

	public static final BlockEntityType<FogGeneratorBlockEntity> FOG_GENERATOR = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "fog_generator"),
		BlockEntityType.Builder.of(FogGeneratorBlockEntity::new, BeatLampBlocks.FOG_GENERATOR).build(null)
	);

	public static final BlockEntityType<StageJukeboxBlockEntity> STAGE_JUKEBOX = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "stage_jukebox"),
		BlockEntityType.Builder.of(StageJukeboxBlockEntity::new, BeatLampBlocks.STAGE_JUKEBOX).build(null)
	);

	public static void register() {
	}
}
