package com.beatlamp.forge;

import com.beatlamp.BeatLamp;
import com.beatlamp.BeatLampBlockEntities;
import com.beatlamp.BeatLampBlocks;
import com.beatlamp.BeatLampItems;
import com.beatlamp.block.BeatEmitterBlock;
import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlock;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.DjDeckBlock;
import com.beatlamp.block.DmxConsoleBlock;
import com.beatlamp.block.DmxConsoleBlockEntity;
import com.beatlamp.block.FogGeneratorBlock;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.block.FountainBlock;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.LaserProjectorBlock;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.block.StageJukeboxBlock;
import com.beatlamp.block.StageJukeboxBlockEntity;
import com.beatlamp.block.StageLightBlock;
import com.beatlamp.block.StageLightBlockEntity;
import com.beatlamp.block.StageSpeakerBlock;
import com.beatlamp.forge.network.ForgeNetwork;
import com.beatlamp.item.GroupLinkerItem;
import com.beatlamp.item.LampControllerItem;
import com.beatlamp.item.StageBlockItem;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(BeatLamp.MOD_ID)
public class BeatLampForge {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BeatLamp.MOD_ID);
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BeatLamp.MOD_ID);
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BeatLamp.MOD_ID);
	public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(
		new net.minecraft.resources.ResourceLocation("minecraft", "creative_mode_tab"),
		BeatLamp.MOD_ID
	);

	// Blocks
	public static final RegistryObject<BeatLampBlock> BEAT_LAMP_BLOCK = BLOCKS.register("beat_lamp", BeatLampBlocks::createBeatLamp);
	public static final RegistryObject<BeatEmitterBlock> BEAT_EMITTER_BLOCK = BLOCKS.register("beat_emitter", BeatLampBlocks::createBeatEmitter);
	public static final RegistryObject<StageLightBlock> STAGE_LIGHT_BLOCK = BLOCKS.register("stage_light", BeatLampBlocks::createStageLight);
	public static final RegistryObject<FountainBlock> FOUNTAIN_BLOCK = BLOCKS.register("fountain", BeatLampBlocks::createFountain);
	public static final RegistryObject<LaserProjectorBlock> LASER_PROJECTOR_BLOCK = BLOCKS.register("laser_projector", BeatLampBlocks::createLaserProjector);
	public static final RegistryObject<FogGeneratorBlock> FOG_GENERATOR_BLOCK = BLOCKS.register("fog_generator", BeatLampBlocks::createFogGenerator);
	public static final RegistryObject<StageJukeboxBlock> STAGE_JUKEBOX_BLOCK = BLOCKS.register("stage_jukebox", BeatLampBlocks::createStageJukebox);
	public static final RegistryObject<DmxConsoleBlock> DMX_CONSOLE_BLOCK = BLOCKS.register("dmx_console", BeatLampBlocks::createDmxConsole);
	public static final RegistryObject<DjDeckBlock> DJ_DECK_BLOCK = BLOCKS.register("dj_deck", BeatLampBlocks::createDjDeck);
	public static final RegistryObject<StageSpeakerBlock> STAGE_SPEAKER_BLOCK = BLOCKS.register("stage_speaker", BeatLampBlocks::createStageSpeaker);

	// Block Entities
	public static final RegistryObject<BlockEntityType<BeatLampBlockEntity>> BEAT_LAMP_BE = BLOCK_ENTITIES.register(
		"beat_lamp", () -> BlockEntityTypeHelper.create(BeatLampBlockEntity::new, BEAT_LAMP_BLOCK.get())
	);
	public static final RegistryObject<BlockEntityType<BeatEmitterBlockEntity>> BEAT_EMITTER_BE = BLOCK_ENTITIES.register(
		"beat_emitter", () -> BlockEntityTypeHelper.create(BeatEmitterBlockEntity::new, BEAT_EMITTER_BLOCK.get())
	);
	public static final RegistryObject<BlockEntityType<StageLightBlockEntity>> STAGE_LIGHT_BE = BLOCK_ENTITIES.register(
		"stage_light", () -> BlockEntityTypeHelper.create(StageLightBlockEntity::new, STAGE_LIGHT_BLOCK.get())
	);
	public static final RegistryObject<BlockEntityType<FountainBlockEntity>> FOUNTAIN_BE = BLOCK_ENTITIES.register(
		"fountain", () -> BlockEntityTypeHelper.create(FountainBlockEntity::new, FOUNTAIN_BLOCK.get())
	);
	public static final RegistryObject<BlockEntityType<LaserProjectorBlockEntity>> LASER_PROJECTOR_BE = BLOCK_ENTITIES.register(
		"laser_projector", () -> BlockEntityTypeHelper.create(LaserProjectorBlockEntity::new, LASER_PROJECTOR_BLOCK.get())
	);
	public static final RegistryObject<BlockEntityType<FogGeneratorBlockEntity>> FOG_GENERATOR_BE = BLOCK_ENTITIES.register(
		"fog_generator", () -> BlockEntityTypeHelper.create(FogGeneratorBlockEntity::new, FOG_GENERATOR_BLOCK.get())
	);
	public static final RegistryObject<BlockEntityType<StageJukeboxBlockEntity>> STAGE_JUKEBOX_BE = BLOCK_ENTITIES.register(
		"stage_jukebox", () -> BlockEntityTypeHelper.create(StageJukeboxBlockEntity::new, STAGE_JUKEBOX_BLOCK.get())
	);
	public static final RegistryObject<BlockEntityType<DmxConsoleBlockEntity>> DMX_CONSOLE_BE = BLOCK_ENTITIES.register(
		"dmx_console", () -> BlockEntityTypeHelper.create(DmxConsoleBlockEntity::new, DMX_CONSOLE_BLOCK.get())
	);

	// Items
	public static final RegistryObject<StageBlockItem> BEAT_LAMP_ITEM = ITEMS.register("beat_lamp", () -> new StageBlockItem(BEAT_LAMP_BLOCK.get(), new Item.Properties(), "beat_lamp", false));
	public static final RegistryObject<StageBlockItem> BEAT_EMITTER_ITEM = ITEMS.register("beat_emitter", () -> new StageBlockItem(BEAT_EMITTER_BLOCK.get(), new Item.Properties(), "beat_emitter", false));
	public static final RegistryObject<StageBlockItem> STAGE_LIGHT_ITEM = ITEMS.register("stage_light", () -> new StageBlockItem(STAGE_LIGHT_BLOCK.get(), new Item.Properties(), "stage_light", false));
	public static final RegistryObject<StageBlockItem> FOUNTAIN_ITEM = ITEMS.register("fountain", () -> new StageBlockItem(FOUNTAIN_BLOCK.get(), new Item.Properties(), "fountain", false));
	public static final RegistryObject<StageBlockItem> LASER_PROJECTOR_ITEM = ITEMS.register("laser_projector", () -> new StageBlockItem(LASER_PROJECTOR_BLOCK.get(), new Item.Properties(), "laser_projector", false));
	public static final RegistryObject<StageBlockItem> FOG_GENERATOR_ITEM = ITEMS.register("fog_generator", () -> new StageBlockItem(FOG_GENERATOR_BLOCK.get(), new Item.Properties(), "fog_generator", false));
	public static final RegistryObject<StageBlockItem> STAGE_JUKEBOX_ITEM = ITEMS.register("stage_jukebox", () -> new StageBlockItem(STAGE_JUKEBOX_BLOCK.get(), new Item.Properties(), "stage_jukebox", false));
	public static final RegistryObject<StageBlockItem> DMX_CONSOLE_ITEM = ITEMS.register("dmx_console", () -> new StageBlockItem(DMX_CONSOLE_BLOCK.get(), new Item.Properties(), "dmx_console", false));
	public static final RegistryObject<StageBlockItem> DJ_DECK_ITEM = ITEMS.register("dj_deck", () -> new StageBlockItem(DJ_DECK_BLOCK.get(), new Item.Properties(), "dj_deck", true));
	public static final RegistryObject<StageBlockItem> STAGE_SPEAKER_ITEM = ITEMS.register("stage_speaker", () -> new StageBlockItem(STAGE_SPEAKER_BLOCK.get(), new Item.Properties(), "stage_speaker", true));
	public static final RegistryObject<GroupLinkerItem> LINKER_ITEM = ITEMS.register("linker", BeatLampItems::createLinkerItem);
	public static final RegistryObject<LampControllerItem> CONTROLLER_ITEM = ITEMS.register("controller", BeatLampItems::createControllerItem);

	// Creative Tab
	public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_TABS.register("main", () -> CreativeTabHelper.builder()
		.title(Component.translatable("itemGroup.beatlamp"))
		.icon(() -> new ItemStack(CONTROLLER_ITEM.get()))
		.displayItems((parameters, output) -> {
			output.accept(BEAT_LAMP_ITEM.get());
			output.accept(STAGE_LIGHT_ITEM.get());
			output.accept(LASER_PROJECTOR_ITEM.get());
			output.accept(FOUNTAIN_ITEM.get());
			output.accept(FOG_GENERATOR_ITEM.get());
			output.accept(STAGE_JUKEBOX_ITEM.get());
			output.accept(DMX_CONSOLE_ITEM.get());
			output.accept(DJ_DECK_ITEM.get());
			output.accept(STAGE_SPEAKER_ITEM.get());
			output.accept(BEAT_EMITTER_ITEM.get());
			output.accept(LINKER_ITEM.get());
			output.accept(CONTROLLER_ITEM.get());
		})
		.build()
	);

	public BeatLampForge() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		BeatLamp.LOGGER.info("Beat Lamp Forge initializing (1.20.1)");
		BeatLamp.initCommon();

		BLOCKS.register(modEventBus);
		BLOCK_ENTITIES.register(modEventBus);
		ITEMS.register(modEventBus);
		CREATIVE_TABS.register(modEventBus);

		modEventBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) -> {
			event.enqueueWork(() -> {
				BeatLampBlocks.BEAT_LAMP = BEAT_LAMP_BLOCK.get();
				BeatLampBlocks.BEAT_EMITTER = BEAT_EMITTER_BLOCK.get();
				BeatLampBlocks.STAGE_LIGHT = STAGE_LIGHT_BLOCK.get();
				BeatLampBlocks.FOUNTAIN = FOUNTAIN_BLOCK.get();
				BeatLampBlocks.LASER_PROJECTOR = LASER_PROJECTOR_BLOCK.get();
				BeatLampBlocks.FOG_GENERATOR = FOG_GENERATOR_BLOCK.get();
				BeatLampBlocks.STAGE_JUKEBOX = STAGE_JUKEBOX_BLOCK.get();
				BeatLampBlocks.DMX_CONSOLE = DMX_CONSOLE_BLOCK.get();
				BeatLampBlocks.DJ_DECK = DJ_DECK_BLOCK.get();
				BeatLampBlocks.STAGE_SPEAKER = STAGE_SPEAKER_BLOCK.get();

				BeatLampBlockEntities.BEAT_LAMP = BEAT_LAMP_BE.get();
				BeatLampBlockEntities.BEAT_EMITTER = BEAT_EMITTER_BE.get();
				BeatLampBlockEntities.STAGE_LIGHT = STAGE_LIGHT_BE.get();
				BeatLampBlockEntities.FOUNTAIN = FOUNTAIN_BE.get();
				BeatLampBlockEntities.LASER_PROJECTOR = LASER_PROJECTOR_BE.get();
				BeatLampBlockEntities.FOG_GENERATOR = FOG_GENERATOR_BE.get();
				BeatLampBlockEntities.STAGE_JUKEBOX = STAGE_JUKEBOX_BE.get();
				BeatLampBlockEntities.DMX_CONSOLE = DMX_CONSOLE_BE.get();

				BeatLampItems.BEAT_LAMP = BEAT_LAMP_ITEM.get();
				BeatLampItems.BEAT_EMITTER = BEAT_EMITTER_ITEM.get();
				BeatLampItems.STAGE_LIGHT = STAGE_LIGHT_ITEM.get();
				BeatLampItems.FOUNTAIN = FOUNTAIN_ITEM.get();
				BeatLampItems.LASER_PROJECTOR = LASER_PROJECTOR_ITEM.get();
				BeatLampItems.FOG_GENERATOR = FOG_GENERATOR_ITEM.get();
				BeatLampItems.STAGE_JUKEBOX = STAGE_JUKEBOX_ITEM.get();
				BeatLampItems.DMX_CONSOLE = DMX_CONSOLE_ITEM.get();
				BeatLampItems.DJ_DECK = DJ_DECK_ITEM.get();
				BeatLampItems.STAGE_SPEAKER = STAGE_SPEAKER_ITEM.get();
				BeatLampItems.LINKER = LINKER_ITEM.get();
				BeatLampItems.CONTROLLER = CONTROLLER_ITEM.get();
				BeatLampItems.TAB = TAB.get();
			});
		});

		ForgeNetwork.register();
		MinecraftForge.EVENT_BUS.register(this);
	}
}
