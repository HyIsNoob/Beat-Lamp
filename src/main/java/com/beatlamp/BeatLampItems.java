package com.beatlamp;

import com.beatlamp.item.GroupLinkerItem;
import com.beatlamp.item.LampControllerItem;
import com.beatlamp.item.StageBlockItem;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BeatLampItems {
	public static final StageBlockItem BEAT_LAMP = new StageBlockItem(BeatLampBlocks.BEAT_LAMP, new Item.Properties(), "beat_lamp", false);
	public static final StageBlockItem BEAT_EMITTER = new StageBlockItem(BeatLampBlocks.BEAT_EMITTER, new Item.Properties(), "beat_emitter", false);
	public static final StageBlockItem STAGE_LIGHT = new StageBlockItem(BeatLampBlocks.STAGE_LIGHT, new Item.Properties(), "stage_light", false);
	public static final StageBlockItem FOUNTAIN = new StageBlockItem(BeatLampBlocks.FOUNTAIN, new Item.Properties(), "fountain", false);
	public static final StageBlockItem LASER_PROJECTOR = new StageBlockItem(BeatLampBlocks.LASER_PROJECTOR, new Item.Properties(), "laser_projector", false);
	public static final StageBlockItem FOG_GENERATOR = new StageBlockItem(BeatLampBlocks.FOG_GENERATOR, new Item.Properties(), "fog_generator", false);
	public static final StageBlockItem STAGE_JUKEBOX = new StageBlockItem(BeatLampBlocks.STAGE_JUKEBOX, new Item.Properties(), "stage_jukebox", false);
	public static final StageBlockItem DMX_CONSOLE = new StageBlockItem(BeatLampBlocks.DMX_CONSOLE, new Item.Properties(), "dmx_console", false);
	public static final StageBlockItem DJ_DECK = new StageBlockItem(BeatLampBlocks.DJ_DECK, new Item.Properties(), "dj_deck", true);
	public static final StageBlockItem STAGE_SPEAKER = new StageBlockItem(BeatLampBlocks.STAGE_SPEAKER, new Item.Properties(), "stage_speaker", true);

	public static final GroupLinkerItem LINKER = new GroupLinkerItem(new Item.Properties().stacksTo(1));
	public static final LampControllerItem CONTROLLER = new LampControllerItem(new Item.Properties().stacksTo(1));

	public static final CreativeModeTab TAB = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB,
		ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "main"),
		FabricItemGroup.builder()
			.title(Component.translatable("itemGroup.beatlamp"))
			.icon(() -> new ItemStack(CONTROLLER))
			.displayItems((parameters, output) -> {
				output.accept(BEAT_LAMP);
				output.accept(STAGE_LIGHT);
				output.accept(LASER_PROJECTOR);
				output.accept(FOUNTAIN);
				output.accept(FOG_GENERATOR);
				output.accept(STAGE_JUKEBOX);
				output.accept(DMX_CONSOLE);
				output.accept(DJ_DECK);
				output.accept(STAGE_SPEAKER);
				output.accept(BEAT_EMITTER);
				output.accept(LINKER);
				output.accept(CONTROLLER);
			})
			.build()
	);

	public static final DataComponentType<BlockPos> ANCHOR_POS = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "anchor_pos"),
		DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).build()
	);

	public static final DataComponentType<BlockPos> SOURCE_POS = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "source_pos"),
		DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).build()
	);

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "beat_lamp"), BEAT_LAMP);
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "beat_emitter"), BEAT_EMITTER);
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "stage_light"), STAGE_LIGHT);
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "fountain"), FOUNTAIN);
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "laser_projector"), LASER_PROJECTOR);
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "fog_generator"), FOG_GENERATOR);
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "stage_jukebox"), STAGE_JUKEBOX);
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "dmx_console"), DMX_CONSOLE);
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "dj_deck"), DJ_DECK);
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "stage_speaker"), STAGE_SPEAKER);
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "linker"), LINKER);
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "controller"), CONTROLLER);
	}
}
