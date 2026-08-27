package com.beatlamp;

import com.beatlamp.item.GroupLinkerItem;
import com.beatlamp.item.LampControllerItem;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BeatLampItems {
	public static final BlockItem BEAT_LAMP = new BlockItem(BeatLampBlocks.BEAT_LAMP, new Item.Properties());
	public static final BlockItem BEAT_EMITTER = new BlockItem(BeatLampBlocks.BEAT_EMITTER, new Item.Properties());
	public static final BlockItem STAGE_LIGHT = new BlockItem(BeatLampBlocks.STAGE_LIGHT, new Item.Properties());
	public static final BlockItem FOUNTAIN = new BlockItem(BeatLampBlocks.FOUNTAIN, new Item.Properties());
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
				output.accept(FOUNTAIN);
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
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "linker"), LINKER);
		Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "controller"), CONTROLLER);
	}
}
