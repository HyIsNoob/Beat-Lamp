package com.beatlamp.fabric;

import com.beatlamp.BeatLamp;
import com.beatlamp.BeatLampBlockEntities;
import com.beatlamp.BeatLampBlocks;
import com.beatlamp.BeatLampItems;
import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.DmxConsoleBlockEntity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.block.RainbowLedBlockEntity;
import com.beatlamp.block.StageJukeboxBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;
import com.beatlamp.item.StageBlockItem;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BeatLampFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		// 1. Register Blocks
		BeatLampBlocks.BEAT_LAMP = Registry.register(BuiltInRegistries.BLOCK, id("beat_lamp"), BeatLampBlocks.createBeatLamp());
		BeatLampBlocks.RAINBOW_LED_BLOCK = Registry.register(BuiltInRegistries.BLOCK, id("rainbow_led_block"), BeatLampBlocks.createRainbowLedBlock());
		BeatLampBlocks.BEAT_EMITTER = Registry.register(BuiltInRegistries.BLOCK, id("beat_emitter"), BeatLampBlocks.createBeatEmitter());
		BeatLampBlocks.STAGE_LIGHT = Registry.register(BuiltInRegistries.BLOCK, id("stage_light"), BeatLampBlocks.createStageLight());
		BeatLampBlocks.FOUNTAIN = Registry.register(BuiltInRegistries.BLOCK, id("fountain"), BeatLampBlocks.createFountain());
		BeatLampBlocks.LASER_PROJECTOR = Registry.register(BuiltInRegistries.BLOCK, id("laser_projector"), BeatLampBlocks.createLaserProjector());
		BeatLampBlocks.FOG_GENERATOR = Registry.register(BuiltInRegistries.BLOCK, id("fog_generator"), BeatLampBlocks.createFogGenerator());
		BeatLampBlocks.STAGE_JUKEBOX = Registry.register(BuiltInRegistries.BLOCK, id("stage_jukebox"), BeatLampBlocks.createStageJukebox());
		BeatLampBlocks.DMX_CONSOLE = Registry.register(BuiltInRegistries.BLOCK, id("dmx_console"), BeatLampBlocks.createDmxConsole());
		BeatLampBlocks.DJ_DECK = Registry.register(BuiltInRegistries.BLOCK, id("dj_deck"), BeatLampBlocks.createDjDeck());
		BeatLampBlocks.STAGE_SPEAKER = Registry.register(BuiltInRegistries.BLOCK, id("stage_speaker"), BeatLampBlocks.createStageSpeaker());

		// 2. Register BlockEntities
		BeatLampBlockEntities.BEAT_LAMP = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("beat_lamp"),
			BlockEntityTypeHelper.create(BeatLampBlockEntity::new, BeatLampBlocks.BEAT_LAMP)
		);
		BeatLampBlockEntities.RAINBOW_LED = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("rainbow_led"),
			BlockEntityTypeHelper.create(RainbowLedBlockEntity::new, BeatLampBlocks.RAINBOW_LED_BLOCK)
		);
		BeatLampBlockEntities.BEAT_EMITTER = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("beat_emitter"),
			BlockEntityTypeHelper.create(BeatEmitterBlockEntity::new, BeatLampBlocks.BEAT_EMITTER)
		);
		BeatLampBlockEntities.STAGE_LIGHT = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("stage_light"),
			BlockEntityTypeHelper.create(StageLightBlockEntity::new, BeatLampBlocks.STAGE_LIGHT)
		);
		BeatLampBlockEntities.FOUNTAIN = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("fountain"),
			BlockEntityTypeHelper.create(FountainBlockEntity::new, BeatLampBlocks.FOUNTAIN)
		);
		BeatLampBlockEntities.LASER_PROJECTOR = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("laser_projector"),
			BlockEntityTypeHelper.create(LaserProjectorBlockEntity::new, BeatLampBlocks.LASER_PROJECTOR)
		);
		BeatLampBlockEntities.FOG_GENERATOR = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("fog_generator"),
			BlockEntityTypeHelper.create(FogGeneratorBlockEntity::new, BeatLampBlocks.FOG_GENERATOR)
		);
		BeatLampBlockEntities.STAGE_JUKEBOX = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("stage_jukebox"),
			BlockEntityTypeHelper.create(StageJukeboxBlockEntity::new, BeatLampBlocks.STAGE_JUKEBOX)
		);
		BeatLampBlockEntities.DMX_CONSOLE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("dmx_console"),
			BlockEntityTypeHelper.create(DmxConsoleBlockEntity::new, BeatLampBlocks.DMX_CONSOLE)
		);

		// 3. Register Items
		BeatLampItems.BEAT_LAMP = Registry.register(BuiltInRegistries.ITEM, id("beat_lamp"), new StageBlockItem(BeatLampBlocks.BEAT_LAMP, new Item.Properties(), "beat_lamp", false));
		BeatLampItems.RAINBOW_LED_BLOCK = Registry.register(BuiltInRegistries.ITEM, id("rainbow_led_block"), new StageBlockItem(BeatLampBlocks.RAINBOW_LED_BLOCK, new Item.Properties(), "rainbow_led_block", true));
		BeatLampItems.BEAT_EMITTER = Registry.register(BuiltInRegistries.ITEM, id("beat_emitter"), new StageBlockItem(BeatLampBlocks.BEAT_EMITTER, new Item.Properties(), "beat_emitter", false));
		BeatLampItems.STAGE_LIGHT = Registry.register(BuiltInRegistries.ITEM, id("stage_light"), new StageBlockItem(BeatLampBlocks.STAGE_LIGHT, new Item.Properties(), "stage_light", false));
		BeatLampItems.FOUNTAIN = Registry.register(BuiltInRegistries.ITEM, id("fountain"), new StageBlockItem(BeatLampBlocks.FOUNTAIN, new Item.Properties(), "fountain", false));
		BeatLampItems.LASER_PROJECTOR = Registry.register(BuiltInRegistries.ITEM, id("laser_projector"), new StageBlockItem(BeatLampBlocks.LASER_PROJECTOR, new Item.Properties(), "laser_projector", false));
		BeatLampItems.FOG_GENERATOR = Registry.register(BuiltInRegistries.ITEM, id("fog_generator"), new StageBlockItem(BeatLampBlocks.FOG_GENERATOR, new Item.Properties(), "fog_generator", false));
		BeatLampItems.STAGE_JUKEBOX = Registry.register(BuiltInRegistries.ITEM, id("stage_jukebox"), new StageBlockItem(BeatLampBlocks.STAGE_JUKEBOX, new Item.Properties(), "stage_jukebox", false));
		BeatLampItems.DMX_CONSOLE = Registry.register(BuiltInRegistries.ITEM, id("dmx_console"), new StageBlockItem(BeatLampBlocks.DMX_CONSOLE, new Item.Properties(), "dmx_console", false));
		BeatLampItems.DJ_DECK = Registry.register(BuiltInRegistries.ITEM, id("dj_deck"), new StageBlockItem(BeatLampBlocks.DJ_DECK, new Item.Properties(), "dj_deck", true));
		BeatLampItems.STAGE_SPEAKER = Registry.register(BuiltInRegistries.ITEM, id("stage_speaker"), new StageBlockItem(BeatLampBlocks.STAGE_SPEAKER, new Item.Properties(), "stage_speaker", true));
		BeatLampItems.LINKER = Registry.register(BuiltInRegistries.ITEM, id("linker"), BeatLampItems.createLinkerItem());
		BeatLampItems.CONTROLLER = Registry.register(BuiltInRegistries.ITEM, id("controller"), BeatLampItems.createControllerItem());

		// 4. Register Creative Tab
		BeatLampItems.TAB = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			id("main"),
			CreativeTabHelper.builder()
				.title(Component.translatable("itemGroup.beatlamp"))
				.icon(() -> new ItemStack(BeatLampItems.CONTROLLER))
				.displayItems((parameters, output) -> {
					output.accept(BeatLampItems.BEAT_LAMP);
					output.accept(BeatLampItems.RAINBOW_LED_BLOCK);
					output.accept(BeatLampItems.STAGE_LIGHT);
					output.accept(BeatLampItems.LASER_PROJECTOR);
					output.accept(BeatLampItems.FOUNTAIN);
					output.accept(BeatLampItems.FOG_GENERATOR);
					output.accept(BeatLampItems.STAGE_JUKEBOX);
					output.accept(BeatLampItems.DMX_CONSOLE);
					output.accept(BeatLampItems.DJ_DECK);
					output.accept(BeatLampItems.STAGE_SPEAKER);
					output.accept(BeatLampItems.BEAT_EMITTER);
					output.accept(BeatLampItems.LINKER);
					output.accept(BeatLampItems.CONTROLLER);
				})
				.build()
		);

		// 5. Register Server Packet Receivers
		registerNetworkReceivers();
	}

	private void registerNetworkReceivers() {
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
			com.beatlamp.fabric.network.FabricNetwork.LAMP_CONFIGURE_ID,
			(server, player, handler, buf, responseSender) -> com.beatlamp.fabric.network.FabricNetwork.handleServerPayload(player, com.beatlamp.fabric.network.FabricNetwork.LAMP_CONFIGURE_ID, buf)
		);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
			com.beatlamp.fabric.network.FabricNetwork.STAGE_LIGHT_CONFIGURE_ID,
			(server, player, handler, buf, responseSender) -> com.beatlamp.fabric.network.FabricNetwork.handleServerPayload(player, com.beatlamp.fabric.network.FabricNetwork.STAGE_LIGHT_CONFIGURE_ID, buf)
		);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
			com.beatlamp.fabric.network.FabricNetwork.FOUNTAIN_CONFIGURE_ID,
			(server, player, handler, buf, responseSender) -> com.beatlamp.fabric.network.FabricNetwork.handleServerPayload(player, com.beatlamp.fabric.network.FabricNetwork.FOUNTAIN_CONFIGURE_ID, buf)
		);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
			com.beatlamp.fabric.network.FabricNetwork.LASER_PROJECTOR_CONFIGURE_ID,
			(server, player, handler, buf, responseSender) -> com.beatlamp.fabric.network.FabricNetwork.handleServerPayload(player, com.beatlamp.fabric.network.FabricNetwork.LASER_PROJECTOR_CONFIGURE_ID, buf)
		);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
			com.beatlamp.fabric.network.FabricNetwork.FOG_GENERATOR_CONFIGURE_ID,
			(server, player, handler, buf, responseSender) -> com.beatlamp.fabric.network.FabricNetwork.handleServerPayload(player, com.beatlamp.fabric.network.FabricNetwork.FOG_GENERATOR_CONFIGURE_ID, buf)
		);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
			com.beatlamp.fabric.network.FabricNetwork.EMITTER_CONFIGURE_ID,
			(server, player, handler, buf, responseSender) -> com.beatlamp.fabric.network.FabricNetwork.handleServerPayload(player, com.beatlamp.fabric.network.FabricNetwork.EMITTER_CONFIGURE_ID, buf)
		);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
			com.beatlamp.fabric.network.FabricNetwork.EMITTER_SIGNAL_ID,
			(server, player, handler, buf, responseSender) -> com.beatlamp.fabric.network.FabricNetwork.handleServerPayload(player, com.beatlamp.fabric.network.FabricNetwork.EMITTER_SIGNAL_ID, buf)
		);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
			com.beatlamp.fabric.network.FabricNetwork.LAMP_SOURCE_ID,
			(server, player, handler, buf, responseSender) -> com.beatlamp.fabric.network.FabricNetwork.handleServerPayload(player, com.beatlamp.fabric.network.FabricNetwork.LAMP_SOURCE_ID, buf)
		);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
			com.beatlamp.fabric.network.FabricNetwork.FOUNTAIN_FIRE_ID,
			(server, player, handler, buf, responseSender) -> com.beatlamp.fabric.network.FabricNetwork.handleServerPayload(player, com.beatlamp.fabric.network.FabricNetwork.FOUNTAIN_FIRE_ID, buf)
		);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
			com.beatlamp.fabric.network.FabricNetwork.DMX_CONSOLE_ID,
			(server, player, handler, buf, responseSender) -> com.beatlamp.fabric.network.FabricNetwork.handleServerPayload(player, com.beatlamp.fabric.network.FabricNetwork.DMX_CONSOLE_ID, buf)
		);
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
			com.beatlamp.fabric.network.FabricNetwork.RAINBOW_LED_CONFIGURE_ID,
			(server, player, handler, buf, responseSender) -> com.beatlamp.fabric.network.FabricNetwork.handleServerPayload(player, com.beatlamp.fabric.network.FabricNetwork.RAINBOW_LED_CONFIGURE_ID, buf)
		);
	}

	private static ResourceLocation id(String path) {
		return new ResourceLocation(BeatLamp.MOD_ID, path);
	}
}
