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

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

	private static BlockBehaviour.Properties blockProps(String name) {
		return BlockBehaviour.Properties.of()
			.setId(ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, name)));
	}

	public static BeatLampBlock createBeatLamp() {
		return new BeatLampBlock(
			blockProps("beat_lamp")
				.strength(0.3F)
				.sound(SoundType.GLASS)
				.lightLevel(state -> state.getValue(BeatLampBlock.LIT) ? 15 : 0)
				.noOcclusion()
		);
	}

	public static BeatEmitterBlock createBeatEmitter() {
		return new BeatEmitterBlock(BeatEmitterBlock.emitterProperties());
	}

	public static StageLightBlock createStageLight() {
		return new StageLightBlock(blockProps("stage_light").strength(0.5F).sound(SoundType.METAL));
	}

	public static FountainBlock createFountain() {
		return new FountainBlock(blockProps("fountain").strength(0.5F).sound(SoundType.STONE));
	}

	public static LaserProjectorBlock createLaserProjector() {
		return new LaserProjectorBlock(blockProps("laser_projector").strength(0.5F).sound(SoundType.METAL));
	}

	public static FogGeneratorBlock createFogGenerator() {
		return new FogGeneratorBlock(blockProps("fog_generator").strength(0.5F).sound(SoundType.STONE));
	}

	public static StageJukeboxBlock createStageJukebox() {
		return new StageJukeboxBlock(blockProps("stage_jukebox").strength(2.0F, 6.0F).sound(SoundType.WOOD));
	}

	public static DmxConsoleBlock createDmxConsole() {
		return new DmxConsoleBlock(blockProps("dmx_console").strength(1.5F).sound(SoundType.METAL));
	}

	public static DjDeckBlock createDjDeck() {
		return new DjDeckBlock(blockProps("dj_deck").strength(1.2F).sound(SoundType.METAL));
	}

	public static StageSpeakerBlock createStageSpeaker() {
		return new StageSpeakerBlock(blockProps("stage_speaker").strength(1.5F).sound(SoundType.WOOD));
	}
}
