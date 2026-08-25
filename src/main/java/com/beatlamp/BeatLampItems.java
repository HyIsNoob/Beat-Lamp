package com.beatlamp;

import com.beatlamp.item.LampControllerItem;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class BeatLampItems {
	public static final BlockItem BEAT_LAMP = new BlockItem(BeatLampBlocks.BEAT_LAMP, new Item.Properties());
	public static final LampControllerItem CONTROLLER = new LampControllerItem(new Item.Properties().stacksTo(1));

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
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "controller"), CONTROLLER);
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COLORED_BLOCKS).register(entries -> entries.accept(BEAT_LAMP));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(BEAT_LAMP));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(CONTROLLER));
	}
}
