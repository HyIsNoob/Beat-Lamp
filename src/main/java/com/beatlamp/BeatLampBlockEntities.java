package com.beatlamp;

import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.DmxConsoleBlockEntity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.block.StageJukeboxBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;

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
		return BlockEntityType.Builder.of(BeatLampBlockEntity::new, BeatLampBlocks.BEAT_LAMP).build(null);
	}
	public static BlockEntityType<BeatEmitterBlockEntity> createBeatEmitter() {
		return BlockEntityType.Builder.of(BeatEmitterBlockEntity::new, BeatLampBlocks.BEAT_EMITTER).build(null);
	}
	public static BlockEntityType<StageLightBlockEntity> createStageLight() {
		return BlockEntityType.Builder.of(StageLightBlockEntity::new, BeatLampBlocks.STAGE_LIGHT).build(null);
	}
	public static BlockEntityType<FountainBlockEntity> createFountain() {
		return BlockEntityType.Builder.of(FountainBlockEntity::new, BeatLampBlocks.FOUNTAIN).build(null);
	}
	public static BlockEntityType<LaserProjectorBlockEntity> createLaserProjector() {
		return BlockEntityType.Builder.of(LaserProjectorBlockEntity::new, BeatLampBlocks.LASER_PROJECTOR).build(null);
	}
	public static BlockEntityType<FogGeneratorBlockEntity> createFogGenerator() {
		return BlockEntityType.Builder.of(FogGeneratorBlockEntity::new, BeatLampBlocks.FOG_GENERATOR).build(null);
	}
	public static BlockEntityType<StageJukeboxBlockEntity> createStageJukebox() {
		return BlockEntityType.Builder.of(StageJukeboxBlockEntity::new, BeatLampBlocks.STAGE_JUKEBOX).build(null);
	}
	public static BlockEntityType<DmxConsoleBlockEntity> createDmxConsole() {
		return BlockEntityType.Builder.of(DmxConsoleBlockEntity::new, BeatLampBlocks.DMX_CONSOLE).build(null);
	}
}
