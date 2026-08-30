package com.beatlamp;

import com.beatlamp.item.GroupLinkerItem;
import com.beatlamp.item.LampControllerItem;
import com.beatlamp.item.StageBlockItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

public class BeatLampItems {
	public static StageBlockItem BEAT_LAMP;
	public static StageBlockItem BEAT_EMITTER;
	public static StageBlockItem STAGE_LIGHT;
	public static StageBlockItem FOUNTAIN;
	public static StageBlockItem LASER_PROJECTOR;
	public static StageBlockItem FOG_GENERATOR;
	public static StageBlockItem STAGE_JUKEBOX;
	public static StageBlockItem DMX_CONSOLE;
	public static StageBlockItem DJ_DECK;
	public static StageBlockItem STAGE_SPEAKER;

	public static GroupLinkerItem LINKER;
	public static LampControllerItem CONTROLLER;

	public static CreativeModeTab TAB;
	public static DataComponentType<BlockPos> ANCHOR_POS;
	public static DataComponentType<BlockPos> SOURCE_POS;

	public static DataComponentType<BlockPos> createAnchorPosComponent() {
		return DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).build();
	}

	public static DataComponentType<BlockPos> createSourcePosComponent() {
		return DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).build();
	}

	public static StageBlockItem createBeatLampItem() { return new StageBlockItem(BeatLampBlocks.BEAT_LAMP, new Item.Properties(), "beat_lamp", false); }
	public static StageBlockItem createBeatEmitterItem() { return new StageBlockItem(BeatLampBlocks.BEAT_EMITTER, new Item.Properties(), "beat_emitter", false); }
	public static StageBlockItem createStageLightItem() { return new StageBlockItem(BeatLampBlocks.STAGE_LIGHT, new Item.Properties(), "stage_light", false); }
	public static StageBlockItem createFountainItem() { return new StageBlockItem(BeatLampBlocks.FOUNTAIN, new Item.Properties(), "fountain", false); }
	public static StageBlockItem createLaserProjectorItem() { return new StageBlockItem(BeatLampBlocks.LASER_PROJECTOR, new Item.Properties(), "laser_projector", false); }
	public static StageBlockItem createFogGeneratorItem() { return new StageBlockItem(BeatLampBlocks.FOG_GENERATOR, new Item.Properties(), "fog_generator", false); }
	public static StageBlockItem createStageJukeboxItem() { return new StageBlockItem(BeatLampBlocks.STAGE_JUKEBOX, new Item.Properties(), "stage_jukebox", false); }
	public static StageBlockItem createDmxConsoleItem() { return new StageBlockItem(BeatLampBlocks.DMX_CONSOLE, new Item.Properties(), "dmx_console", false); }
	public static StageBlockItem createDjDeckItem() { return new StageBlockItem(BeatLampBlocks.DJ_DECK, new Item.Properties(), "dj_deck", true); }
	public static StageBlockItem createStageSpeakerItem() { return new StageBlockItem(BeatLampBlocks.STAGE_SPEAKER, new Item.Properties(), "stage_speaker", true); }
	public static GroupLinkerItem createLinkerItem() { return new GroupLinkerItem(new Item.Properties().stacksTo(1)); }
	public static LampControllerItem createControllerItem() { return new LampControllerItem(new Item.Properties().stacksTo(1)); }
}
