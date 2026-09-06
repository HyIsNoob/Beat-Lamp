package com.beatlamp;

import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.DmxConsoleBlockEntity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.block.StageJukeboxBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BeatLampBlockEntities {
	public static BlockEntityType<BeatLampBlockEntity> BEAT_LAMP;
	public static BlockEntityType<BeatEmitterBlockEntity> BEAT_EMITTER;
	public static BlockEntityType<StageLightBlockEntity> STAGE_LIGHT;
	public static BlockEntityType<FountainBlockEntity> FOUNTAIN;
	public static BlockEntityType<LaserProjectorBlockEntity> LASER_PROJECTOR;
	public static BlockEntityType<FogGeneratorBlockEntity> FOG_GENERATOR;
	public static BlockEntityType<StageJukeboxBlockEntity> STAGE_JUKEBOX;
	public static BlockEntityType<DmxConsoleBlockEntity> DMX_CONSOLE;

	public static BlockEntityType<BeatLampBlockEntity> createBeatLamp() {
		return FabricBlockEntityTypeBuilder.create(BeatLampBlockEntity::new, BeatLampBlocks.BEAT_LAMP).build();
	}
	public static BlockEntityType<BeatEmitterBlockEntity> createBeatEmitter() {
		return FabricBlockEntityTypeBuilder.create(BeatEmitterBlockEntity::new, BeatLampBlocks.BEAT_EMITTER).build();
	}
	public static BlockEntityType<StageLightBlockEntity> createStageLight() {
		return FabricBlockEntityTypeBuilder.create(StageLightBlockEntity::new, BeatLampBlocks.STAGE_LIGHT).build();
	}
	public static BlockEntityType<FountainBlockEntity> createFountain() {
		return FabricBlockEntityTypeBuilder.create(FountainBlockEntity::new, BeatLampBlocks.FOUNTAIN).build();
	}
	public static BlockEntityType<LaserProjectorBlockEntity> createLaserProjector() {
		return FabricBlockEntityTypeBuilder.create(LaserProjectorBlockEntity::new, BeatLampBlocks.LASER_PROJECTOR).build();
	}
	public static BlockEntityType<FogGeneratorBlockEntity> createFogGenerator() {
		return FabricBlockEntityTypeBuilder.create(FogGeneratorBlockEntity::new, BeatLampBlocks.FOG_GENERATOR).build();
	}
	public static BlockEntityType<StageJukeboxBlockEntity> createStageJukebox() {
		return FabricBlockEntityTypeBuilder.create(StageJukeboxBlockEntity::new, BeatLampBlocks.STAGE_JUKEBOX).build();
	}
	public static BlockEntityType<DmxConsoleBlockEntity> createDmxConsole() {
		return FabricBlockEntityTypeBuilder.create(DmxConsoleBlockEntity::new, BeatLampBlocks.DMX_CONSOLE).build();
	}
}
