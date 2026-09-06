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
}
