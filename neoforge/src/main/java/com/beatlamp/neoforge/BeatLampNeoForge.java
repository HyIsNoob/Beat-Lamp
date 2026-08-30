package com.beatlamp.neoforge;

import java.util.List;

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
import com.beatlamp.item.GroupLinkerItem;
import com.beatlamp.item.LampControllerItem;
import com.beatlamp.item.StageBlockItem;
import com.beatlamp.network.DmxConsolePayload;
import com.beatlamp.network.EmitterConfigurePayload;
import com.beatlamp.network.EmitterSignalPayload;
import com.beatlamp.network.FogGeneratorConfigurePayload;
import com.beatlamp.network.FountainConfigurePayload;
import com.beatlamp.network.FountainFirePayload;
import com.beatlamp.network.LampConfigurePayload;
import com.beatlamp.network.LampSourcePayload;
import com.beatlamp.network.LaserProjectorConfigurePayload;
import com.beatlamp.network.StageLightConfigurePayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(BeatLamp.MOD_ID)
public class BeatLampNeoForge {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, BeatLamp.MOD_ID);
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, BeatLamp.MOD_ID);
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BeatLamp.MOD_ID);
	public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BeatLamp.MOD_ID);
	public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BeatLamp.MOD_ID);

	// 1. Blocks
	public static final DeferredHolder<Block, BeatLampBlock> BEAT_LAMP_BLOCK = BLOCKS.register("beat_lamp", BeatLampBlocks::createBeatLamp);
	public static final DeferredHolder<Block, BeatEmitterBlock> BEAT_EMITTER_BLOCK = BLOCKS.register("beat_emitter", BeatLampBlocks::createBeatEmitter);
	public static final DeferredHolder<Block, StageLightBlock> STAGE_LIGHT_BLOCK = BLOCKS.register("stage_light", BeatLampBlocks::createStageLight);
	public static final DeferredHolder<Block, FountainBlock> FOUNTAIN_BLOCK = BLOCKS.register("fountain", BeatLampBlocks::createFountain);
	public static final DeferredHolder<Block, LaserProjectorBlock> LASER_PROJECTOR_BLOCK = BLOCKS.register("laser_projector", BeatLampBlocks::createLaserProjector);
	public static final DeferredHolder<Block, FogGeneratorBlock> FOG_GENERATOR_BLOCK = BLOCKS.register("fog_generator", BeatLampBlocks::createFogGenerator);
	public static final DeferredHolder<Block, StageJukeboxBlock> STAGE_JUKEBOX_BLOCK = BLOCKS.register("stage_jukebox", BeatLampBlocks::createStageJukebox);
	public static final DeferredHolder<Block, DmxConsoleBlock> DMX_CONSOLE_BLOCK = BLOCKS.register("dmx_console", BeatLampBlocks::createDmxConsole);
	public static final DeferredHolder<Block, DjDeckBlock> DJ_DECK_BLOCK = BLOCKS.register("dj_deck", BeatLampBlocks::createDjDeck);
	public static final DeferredHolder<Block, StageSpeakerBlock> STAGE_SPEAKER_BLOCK = BLOCKS.register("stage_speaker", BeatLampBlocks::createStageSpeaker);

	// 2. BlockEntities
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BeatLampBlockEntity>> BEAT_LAMP_BE = BLOCK_ENTITIES.register("beat_lamp", () -> BlockEntityType.Builder.of(BeatLampBlockEntity::new, BEAT_LAMP_BLOCK.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BeatEmitterBlockEntity>> BEAT_EMITTER_BE = BLOCK_ENTITIES.register("beat_emitter", () -> BlockEntityType.Builder.of(BeatEmitterBlockEntity::new, BEAT_EMITTER_BLOCK.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StageLightBlockEntity>> STAGE_LIGHT_BE = BLOCK_ENTITIES.register("stage_light", () -> BlockEntityType.Builder.of(StageLightBlockEntity::new, STAGE_LIGHT_BLOCK.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FountainBlockEntity>> FOUNTAIN_BE = BLOCK_ENTITIES.register("fountain", () -> BlockEntityType.Builder.of(FountainBlockEntity::new, FOUNTAIN_BLOCK.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaserProjectorBlockEntity>> LASER_PROJECTOR_BE = BLOCK_ENTITIES.register("laser_projector", () -> BlockEntityType.Builder.of(LaserProjectorBlockEntity::new, LASER_PROJECTOR_BLOCK.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FogGeneratorBlockEntity>> FOG_GENERATOR_BE = BLOCK_ENTITIES.register("fog_generator", () -> BlockEntityType.Builder.of(FogGeneratorBlockEntity::new, FOG_GENERATOR_BLOCK.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StageJukeboxBlockEntity>> STAGE_JUKEBOX_BE = BLOCK_ENTITIES.register("stage_jukebox", () -> BlockEntityType.Builder.of(StageJukeboxBlockEntity::new, STAGE_JUKEBOX_BLOCK.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DmxConsoleBlockEntity>> DMX_CONSOLE_BE = BLOCK_ENTITIES.register("dmx_console", () -> BlockEntityType.Builder.of(DmxConsoleBlockEntity::new, DMX_CONSOLE_BLOCK.get()).build(null));

	// 3. Data Components
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> ANCHOR_POS = DATA_COMPONENTS.register("anchor_pos", BeatLampItems::createAnchorPosComponent);
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> SOURCE_POS = DATA_COMPONENTS.register("source_pos", BeatLampItems::createSourcePosComponent);

	// 4. Items
	public static final DeferredHolder<Item, StageBlockItem> BEAT_LAMP_ITEM = ITEMS.register("beat_lamp", () -> new StageBlockItem(BEAT_LAMP_BLOCK.get(), new Item.Properties(), "beat_lamp", false));
	public static final DeferredHolder<Item, StageBlockItem> BEAT_EMITTER_ITEM = ITEMS.register("beat_emitter", () -> new StageBlockItem(BEAT_EMITTER_BLOCK.get(), new Item.Properties(), "beat_emitter", false));
	public static final DeferredHolder<Item, StageBlockItem> STAGE_LIGHT_ITEM = ITEMS.register("stage_light", () -> new StageBlockItem(STAGE_LIGHT_BLOCK.get(), new Item.Properties(), "stage_light", false));
	public static final DeferredHolder<Item, StageBlockItem> FOUNTAIN_ITEM = ITEMS.register("fountain", () -> new StageBlockItem(FOUNTAIN_BLOCK.get(), new Item.Properties(), "fountain", false));
	public static final DeferredHolder<Item, StageBlockItem> LASER_PROJECTOR_ITEM = ITEMS.register("laser_projector", () -> new StageBlockItem(LASER_PROJECTOR_BLOCK.get(), new Item.Properties(), "laser_projector", false));
	public static final DeferredHolder<Item, StageBlockItem> FOG_GENERATOR_ITEM = ITEMS.register("fog_generator", () -> new StageBlockItem(FOG_GENERATOR_BLOCK.get(), new Item.Properties(), "fog_generator", false));
	public static final DeferredHolder<Item, StageBlockItem> STAGE_JUKEBOX_ITEM = ITEMS.register("stage_jukebox", () -> new StageBlockItem(STAGE_JUKEBOX_BLOCK.get(), new Item.Properties(), "stage_jukebox", false));
	public static final DeferredHolder<Item, StageBlockItem> DMX_CONSOLE_ITEM = ITEMS.register("dmx_console", () -> new StageBlockItem(DMX_CONSOLE_BLOCK.get(), new Item.Properties(), "dmx_console", false));
	public static final DeferredHolder<Item, StageBlockItem> DJ_DECK_ITEM = ITEMS.register("dj_deck", () -> new StageBlockItem(DJ_DECK_BLOCK.get(), new Item.Properties(), "dj_deck", true));
	public static final DeferredHolder<Item, StageBlockItem> STAGE_SPEAKER_ITEM = ITEMS.register("stage_speaker", () -> new StageBlockItem(STAGE_SPEAKER_BLOCK.get(), new Item.Properties(), "stage_speaker", true));
	public static final DeferredHolder<Item, GroupLinkerItem> LINKER_ITEM = ITEMS.register("linker", BeatLampItems::createLinkerItem);
	public static final DeferredHolder<Item, LampControllerItem> CONTROLLER_ITEM = ITEMS.register("controller", BeatLampItems::createControllerItem);

	// 5. Creative Mode Tab
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register("main", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
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

	public BeatLampNeoForge(IEventBus modEventBus) {
		BeatLamp.LOGGER.info("Beat Lamp NeoForge initializing (1.21.1)");

		BeatLamp.initCommon();

		BLOCKS.register(modEventBus);
		BLOCK_ENTITIES.register(modEventBus);
		DATA_COMPONENTS.register(modEventBus);
		ITEMS.register(modEventBus);
		CREATIVE_TABS.register(modEventBus);

		modEventBus.addListener(this::commonSetup);
		modEventBus.addListener(this::registerPayloads);
	}

	private void commonSetup(FMLCommonSetupEvent event) {
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

			BeatLampItems.ANCHOR_POS = ANCHOR_POS.get();
			BeatLampItems.SOURCE_POS = SOURCE_POS.get();

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
	}

	private void registerPayloads(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");

		registrar.playToServer(LampConfigurePayload.ID, LampConfigurePayload.CODEC, this::handleLampConfig);
		registrar.playToServer(StageLightConfigurePayload.ID, StageLightConfigurePayload.CODEC, this::handleStageLightConfig);
		registrar.playToServer(FountainConfigurePayload.ID, FountainConfigurePayload.CODEC, this::handleFountainConfig);
		registrar.playToServer(LaserProjectorConfigurePayload.ID, LaserProjectorConfigurePayload.CODEC, this::handleLaserConfig);
		registrar.playToServer(FogGeneratorConfigurePayload.ID, FogGeneratorConfigurePayload.CODEC, this::handleFogConfig);
		registrar.playToServer(LampSourcePayload.ID, LampSourcePayload.CODEC, this::handleLampSource);
		registrar.playToServer(EmitterSignalPayload.ID, EmitterSignalPayload.CODEC, this::handleEmitterSignal);
		registrar.playToServer(EmitterConfigurePayload.ID, EmitterConfigurePayload.CODEC, this::handleEmitterConfig);
		registrar.playToServer(FountainFirePayload.ID, FountainFirePayload.CODEC, this::handleFountainFire);
		registrar.playToServer(DmxConsolePayload.ID, DmxConsolePayload.CODEC, this::handleDmxConsole);
	}

	private void handleDmxConsole(DmxConsolePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = context.player().level();
			if (level.getBlockEntity(payload.pos()) instanceof DmxConsoleBlockEntity dmx) {
				dmx.setBlackout(payload.blackout());
				dmx.setStrobeAll(payload.strobeAll());
				dmx.setMasterDimmer(payload.masterDimmer());
				dmx.setMasterSpeed(payload.masterSpeed());
			}
		});
	}

	private void handleEmitterConfig(EmitterConfigurePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = context.player().level();
			if (payload.unlink()) {
				BeatLamp.unlinkEmitterGroup(level, payload.pos());
				return;
			}
			List<BlockPos> members = null;
			if (level.getBlockEntity(payload.pos()) instanceof BeatEmitterBlockEntity emitter && emitter.getManualGroup().size() >= 2) {
				members = emitter.getManualGroup();
			}
			if (members == null) {
				members = List.of(payload.pos());
			}
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof BeatEmitterBlockEntity emitter) {
					emitter.applyConfig(payload.mode(), payload.threshold(), payload.inverted(), payload.dmxEnrolled(), payload.customName());
				}
			}
		});
	}

	private void handleFountainFire(FountainFirePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			ServerPlayer player = (ServerPlayer) context.player();
			Level level = player.level();
			if (level instanceof ServerLevel serverLevel && serverLevel.getBlockEntity(payload.pos()) instanceof FountainBlockEntity fountain) {
				long gameTime = serverLevel.getGameTime();
				if (fountain.canFire(gameTime)) {
					fountain.markFired(gameTime);
					BeatLamp.spawnFirework(serverLevel, payload.pos(), fountain.getColor());
				}
			}
		});
	}

	private void handleEmitterSignal(EmitterSignalPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = context.player().level();
			if (level.getBlockEntity(payload.pos()) instanceof BeatEmitterBlockEntity emitter) {
				emitter.setSignal(payload.signal(), level.getGameTime());
			}
		});
	}

	private void handleLampSource(LampSourcePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = context.player().level();
			if (level.getBlockEntity(payload.pos()) instanceof BeatLampBlockEntity lamp) {
				List<BlockPos> members = lamp.getManualGroup().size() >= 2 ? lamp.getManualGroup() : BeatLamp.floodFill(level, payload.pos());
				for (BlockPos member : members) {
					if (level.getBlockEntity(member) instanceof BeatLampBlockEntity beatLamp) {
						beatLamp.setSource(null);
					}
				}
			} else if (level.getBlockEntity(payload.pos()) instanceof StageLightBlockEntity light) {
				List<BlockPos> members = light.getManualGroup().size() >= 2 ? light.getManualGroup() : List.of(payload.pos());
				for (BlockPos member : members) {
					if (level.getBlockEntity(member) instanceof StageLightBlockEntity l) {
						l.setSource(null);
					}
				}
			} else if (level.getBlockEntity(payload.pos()) instanceof FountainBlockEntity fountain) {
				List<BlockPos> members = fountain.getManualGroup().size() >= 2 ? fountain.getManualGroup() : List.of(payload.pos());
				for (BlockPos member : members) {
					if (level.getBlockEntity(member) instanceof FountainBlockEntity f) {
						f.setSource(null);
					}
				}
			} else if (level.getBlockEntity(payload.pos()) instanceof LaserProjectorBlockEntity laser) {
				List<BlockPos> members = laser.getManualGroup().size() >= 2 ? laser.getManualGroup() : List.of(payload.pos());
				for (BlockPos member : members) {
					if (level.getBlockEntity(member) instanceof LaserProjectorBlockEntity l) {
						l.setSource(null);
					}
				}
			} else if (level.getBlockEntity(payload.pos()) instanceof FogGeneratorBlockEntity fog) {
				List<BlockPos> members = fog.getManualGroup().size() >= 2 ? fog.getManualGroup() : List.of(payload.pos());
				for (BlockPos member : members) {
					if (level.getBlockEntity(member) instanceof FogGeneratorBlockEntity f) {
						f.setSource(null);
					}
				}
			} else if (level.getBlockEntity(payload.pos()) instanceof BeatEmitterBlockEntity emitter) {
				List<BlockPos> members = emitter.getManualGroup().size() >= 2 ? emitter.getManualGroup() : List.of(payload.pos());
				for (BlockPos member : members) {
					if (level.getBlockEntity(member) instanceof BeatEmitterBlockEntity e) {
						e.setSource(null);
					}
				}
			}
		});
	}

	private void handleFogConfig(FogGeneratorConfigurePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = context.player().level();
			if (payload.unlink()) {
				BeatLamp.unlinkFogGroup(level, payload.pos());
				return;
			}
			List<BlockPos> members = null;
			if (level.getBlockEntity(payload.pos()) instanceof FogGeneratorBlockEntity fog && fog.getManualGroup().size() >= 2) {
				members = fog.getManualGroup();
			}
			if (members == null) {
				members = List.of(payload.pos());
			}
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof FogGeneratorBlockEntity fog) {
					fog.setDensity(payload.density());
					fog.setRadius(payload.radius());
					fog.setColor(payload.color());
					fog.setDmxEnrolled(payload.dmxEnrolled());
					fog.setCustomName(payload.customName());
				}
			}
		});
	}

	private void handleLaserConfig(LaserProjectorConfigurePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = context.player().level();
			if (payload.unlink()) {
				BeatLamp.unlinkLaserGroup(level, payload.pos());
				return;
			}
			List<BlockPos> members = null;
			if (level.getBlockEntity(payload.pos()) instanceof LaserProjectorBlockEntity laser && laser.getManualGroup().size() >= 2) {
				members = laser.getManualGroup();
			}
			if (members == null) {
				members = List.of(payload.pos());
			}
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof LaserProjectorBlockEntity laser) {
					laser.setMode(payload.mode());
					laser.setBeamCount(payload.beamCount());
					laser.setSpread(payload.spread());
					laser.setSpeed(payload.speed());
					laser.setColor(payload.color());
					laser.setDmxEnrolled(payload.dmxEnrolled());
					laser.setCustomName(payload.customName());
				}
			}
		});
	}

	private void handleFountainConfig(FountainConfigurePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = context.player().level();
			if (payload.unlink()) {
				BeatLamp.unlinkFountainGroup(level, payload.pos());
				return;
			}
			List<BlockPos> members = null;
			if (level.getBlockEntity(payload.pos()) instanceof FountainBlockEntity fountain && fountain.getManualGroup().size() >= 2) {
				members = fountain.getManualGroup();
			}
			if (members == null) {
				members = List.of(payload.pos());
			}
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof FountainBlockEntity fountain) {
					fountain.setFireworkMode(payload.fireworkMode());
					fountain.setSprayThreshold(payload.sprayThreshold());
					fountain.setImpactThreshold(payload.impactThreshold());
					fountain.setSmokeEnabled(payload.smokeEnabled());
					fountain.setParticleType(payload.particleType());
					fountain.setColor(payload.color());
					fountain.setDmxEnrolled(payload.dmxEnrolled());
					fountain.setCustomName(payload.customName());
				}
			}
		});
	}

	private void handleStageLightConfig(StageLightConfigurePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = context.player().level();
			if (payload.unlink()) {
				BeatLamp.unlinkStageLightGroup(level, payload.pos());
				return;
			}
			List<BlockPos> members = null;
			if (level.getBlockEntity(payload.pos()) instanceof StageLightBlockEntity light && light.getManualGroup().size() >= 2) {
				members = light.getManualGroup();
			}
			if (members == null) {
				members = List.of(payload.pos());
			}
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof StageLightBlockEntity light) {
					light.setMode(payload.mode());
					light.setSensitivity(payload.sensitivity());
					light.setSpeed(payload.speed());
					light.setColor(payload.color());
					light.setDmxEnrolled(payload.dmxEnrolled());
					light.setCustomName(payload.customName());
				}
			}
		});
	}

	private void handleLampConfig(LampConfigurePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = context.player().level();
			if (payload.unlink()) {
				BeatLamp.unlinkGroup(level, payload.pos());
				return;
			}
			List<BlockPos> members = null;
			if (level.getBlockEntity(payload.pos()) instanceof BeatLampBlockEntity lamp && lamp.getManualGroup().size() >= 2) {
				members = lamp.getManualGroup();
			}
			if (members == null) {
				members = BeatLamp.floodFill(level, payload.pos());
			}
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof BeatLampBlockEntity beatLamp) {
					beatLamp.applyConfig(payload.mode(), payload.sensitivity(), payload.speed(), payload.color(),
						payload.frameless(), payload.blackback(), payload.idleLight(), payload.reverse(),
						payload.particles(), payload.orientation(), payload.tempoPulse(), payload.dmxEnrolled(), payload.customName());
				}
			}
		});
	}
}
