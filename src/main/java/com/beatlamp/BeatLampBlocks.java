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

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BeatLampBlocks {
	public static final BeatLampBlock BEAT_LAMP = new BeatLampBlock(
		BlockBehaviour.Properties.of()
			.strength(0.3F)
			.sound(SoundType.GLASS)
			.lightLevel(state -> state.getValue(BeatLampBlock.LIT) ? 15 : 0)
			.noOcclusion()
	);

	public static final BeatEmitterBlock BEAT_EMITTER = new BeatEmitterBlock(BeatEmitterBlock.emitterProperties());

	public static final StageLightBlock STAGE_LIGHT = new StageLightBlock(
		BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.METAL)
	);

	public static final FountainBlock FOUNTAIN = new FountainBlock(
		BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.STONE)
	);

	public static final LaserProjectorBlock LASER_PROJECTOR = new LaserProjectorBlock(
		BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.METAL)
	);

	public static final FogGeneratorBlock FOG_GENERATOR = new FogGeneratorBlock(
		BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.STONE)
	);

	public static final StageJukeboxBlock STAGE_JUKEBOX = new StageJukeboxBlock(
		BlockBehaviour.Properties.of().strength(2.0F, 6.0F).sound(SoundType.WOOD)
	);

	public static final DmxConsoleBlock DMX_CONSOLE = new DmxConsoleBlock(
		BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.METAL)
	);

	public static final DjDeckBlock DJ_DECK = new DjDeckBlock(
		BlockBehaviour.Properties.of().strength(1.2F).sound(SoundType.METAL)
	);

	public static final StageSpeakerBlock STAGE_SPEAKER = new StageSpeakerBlock(
		BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.WOOD)
	);

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "beat_lamp"), BEAT_LAMP);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "beat_emitter"), BEAT_EMITTER);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "stage_light"), STAGE_LIGHT);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "fountain"), FOUNTAIN);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "laser_projector"), LASER_PROJECTOR);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "fog_generator"), FOG_GENERATOR);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "stage_jukebox"), STAGE_JUKEBOX);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "dmx_console"), DMX_CONSOLE);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "dj_deck"), DJ_DECK);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "stage_speaker"), STAGE_SPEAKER);
	}
}
