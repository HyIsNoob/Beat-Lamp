package com.beatlamp;

import com.beatlamp.block.BeatEmitterBlock;
import com.beatlamp.block.BeatLampBlock;
import com.beatlamp.block.DjDeckBlock;
import com.beatlamp.block.DmxConsoleBlock;
import com.beatlamp.block.FogGeneratorBlock;
import com.beatlamp.block.FountainBlock;
import com.beatlamp.block.LaserProjectorBlock;
import com.beatlamp.block.StageJukeboxBlock;
import com.beatlamp.block.StageLightBlock;
import com.beatlamp.block.StageSpeakerBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BeatLampBlocks {
	public static BeatLampBlock BEAT_LAMP;
	public static BeatEmitterBlock BEAT_EMITTER;
	public static StageLightBlock STAGE_LIGHT;
	public static FountainBlock FOUNTAIN;
	public static LaserProjectorBlock LASER_PROJECTOR;
	public static FogGeneratorBlock FOG_GENERATOR;
	public static StageJukeboxBlock STAGE_JUKEBOX;
	public static DmxConsoleBlock DMX_CONSOLE;
	public static DjDeckBlock DJ_DECK;
	public static StageSpeakerBlock STAGE_SPEAKER;

	public static BeatLampBlock createBeatLamp() {
		return new BeatLampBlock(BlockBehaviour.Properties.of()
			.strength(0.6F, 3.0F)
			.sound(SoundType.GLASS)
			.lightLevel(state -> 15));
	}

	public static BeatEmitterBlock createBeatEmitter() {
		return new BeatEmitterBlock(BlockBehaviour.Properties.of()
			.strength(1.5F, 6.0F)
			.sound(SoundType.METAL));
	}

	public static StageLightBlock createStageLight() {
		return new StageLightBlock(BlockBehaviour.Properties.of()
			.strength(1.5F, 6.0F)
			.sound(SoundType.METAL)
			.noOcclusion());
	}

	public static FountainBlock createFountain() {
		return new FountainBlock(BlockBehaviour.Properties.of()
			.strength(1.5F, 6.0F)
			.sound(SoundType.METAL)
			.noOcclusion());
	}

	public static LaserProjectorBlock createLaserProjector() {
		return new LaserProjectorBlock(BlockBehaviour.Properties.of()
			.strength(1.5F, 6.0F)
			.sound(SoundType.METAL)
			.noOcclusion());
	}

	public static FogGeneratorBlock createFogGenerator() {
		return new FogGeneratorBlock(BlockBehaviour.Properties.of()
			.strength(1.5F, 6.0F)
			.sound(SoundType.METAL)
			.noOcclusion());
	}

	public static StageJukeboxBlock createStageJukebox() {
		return new StageJukeboxBlock(BlockBehaviour.Properties.of()
			.strength(2.0F, 6.0F)
			.sound(SoundType.STONE));
	}

	public static DmxConsoleBlock createDmxConsole() {
		return new DmxConsoleBlock(BlockBehaviour.Properties.of()
			.strength(1.5F, 6.0F)
			.sound(SoundType.METAL)
			.noOcclusion());
	}

	public static DjDeckBlock createDjDeck() {
		return new DjDeckBlock(BlockBehaviour.Properties.of()
			.strength(1.5F, 6.0F)
			.sound(SoundType.METAL)
			.noOcclusion());
	}

	public static StageSpeakerBlock createStageSpeaker() {
		return new StageSpeakerBlock(BlockBehaviour.Properties.of()
			.strength(1.5F, 6.0F)
			.sound(SoundType.WOOD));
	}
}
