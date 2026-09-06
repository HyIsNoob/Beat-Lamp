package com.beatlamp.fabric;

import java.util.List;

import com.beatlamp.BeatLamp;
import com.beatlamp.BeatLampBlockEntities;
import com.beatlamp.BeatLampBlocks;
import com.beatlamp.BeatLampItems;
import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;
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

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BeatLampFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		BeatLamp.initCommon();

		// 1. Register Blocks
		BeatLampBlocks.BEAT_LAMP = Registry.register(BuiltInRegistries.BLOCK, BeatLamp.id("beat_lamp"), BeatLampBlocks.createBeatLamp());
		BeatLampBlocks.BEAT_EMITTER = Registry.register(BuiltInRegistries.BLOCK, BeatLamp.id("beat_emitter"), BeatLampBlocks.createBeatEmitter());
		BeatLampBlocks.STAGE_LIGHT = Registry.register(BuiltInRegistries.BLOCK, BeatLamp.id("stage_light"), BeatLampBlocks.createStageLight());
		BeatLampBlocks.FOUNTAIN = Registry.register(BuiltInRegistries.BLOCK, BeatLamp.id("fountain"), BeatLampBlocks.createFountain());
		BeatLampBlocks.LASER_PROJECTOR = Registry.register(BuiltInRegistries.BLOCK, BeatLamp.id("laser_projector"), BeatLampBlocks.createLaserProjector());
		BeatLampBlocks.FOG_GENERATOR = Registry.register(BuiltInRegistries.BLOCK, BeatLamp.id("fog_generator"), BeatLampBlocks.createFogGenerator());
		BeatLampBlocks.STAGE_JUKEBOX = Registry.register(BuiltInRegistries.BLOCK, BeatLamp.id("stage_jukebox"), BeatLampBlocks.createStageJukebox());
		BeatLampBlocks.DMX_CONSOLE = Registry.register(BuiltInRegistries.BLOCK, BeatLamp.id("dmx_console"), BeatLampBlocks.createDmxConsole());
		BeatLampBlocks.DJ_DECK = Registry.register(BuiltInRegistries.BLOCK, BeatLamp.id("dj_deck"), BeatLampBlocks.createDjDeck());
		BeatLampBlocks.STAGE_SPEAKER = Registry.register(BuiltInRegistries.BLOCK, BeatLamp.id("stage_speaker"), BeatLampBlocks.createStageSpeaker());

		// 2. Register BlockEntities
		BeatLampBlockEntities.BEAT_LAMP = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BeatLamp.id("beat_lamp"), BeatLampBlockEntities.createBeatLamp());
		BeatLampBlockEntities.BEAT_EMITTER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BeatLamp.id("beat_emitter"), BeatLampBlockEntities.createBeatEmitter());
		BeatLampBlockEntities.STAGE_LIGHT = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BeatLamp.id("stage_light"), BeatLampBlockEntities.createStageLight());
		BeatLampBlockEntities.FOUNTAIN = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BeatLamp.id("fountain"), BeatLampBlockEntities.createFountain());
		BeatLampBlockEntities.LASER_PROJECTOR = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BeatLamp.id("laser_projector"), BeatLampBlockEntities.createLaserProjector());
		BeatLampBlockEntities.FOG_GENERATOR = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BeatLamp.id("fog_generator"), BeatLampBlockEntities.createFogGenerator());
		BeatLampBlockEntities.STAGE_JUKEBOX = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BeatLamp.id("stage_jukebox"), BeatLampBlockEntities.createStageJukebox());
		BeatLampBlockEntities.DMX_CONSOLE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BeatLamp.id("dmx_console"), BeatLampBlockEntities.createDmxConsole());

		// 3. Register Data Components
		BeatLampItems.ANCHOR_POS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, BeatLamp.id("anchor_pos"), BeatLampItems.createAnchorPosComponent());
		BeatLampItems.SOURCE_POS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, BeatLamp.id("source_pos"), BeatLampItems.createSourcePosComponent());

		// 4. Register Items
		BeatLampItems.BEAT_LAMP = Registry.register(BuiltInRegistries.ITEM, BeatLamp.id("beat_lamp"), BeatLampItems.createBeatLampItem());
		BeatLampItems.BEAT_EMITTER = Registry.register(BuiltInRegistries.ITEM, BeatLamp.id("beat_emitter"), BeatLampItems.createBeatEmitterItem());
		BeatLampItems.STAGE_LIGHT = Registry.register(BuiltInRegistries.ITEM, BeatLamp.id("stage_light"), BeatLampItems.createStageLightItem());
		BeatLampItems.FOUNTAIN = Registry.register(BuiltInRegistries.ITEM, BeatLamp.id("fountain"), BeatLampItems.createFountainItem());
		BeatLampItems.LASER_PROJECTOR = Registry.register(BuiltInRegistries.ITEM, BeatLamp.id("laser_projector"), BeatLampItems.createLaserProjectorItem());
		BeatLampItems.FOG_GENERATOR = Registry.register(BuiltInRegistries.ITEM, BeatLamp.id("fog_generator"), BeatLampItems.createFogGeneratorItem());
		BeatLampItems.STAGE_JUKEBOX = Registry.register(BuiltInRegistries.ITEM, BeatLamp.id("stage_jukebox"), BeatLampItems.createStageJukeboxItem());
		BeatLampItems.DMX_CONSOLE = Registry.register(BuiltInRegistries.ITEM, BeatLamp.id("dmx_console"), BeatLampItems.createDmxConsoleItem());
		BeatLampItems.DJ_DECK = Registry.register(BuiltInRegistries.ITEM, BeatLamp.id("dj_deck"), BeatLampItems.createDjDeckItem());
		BeatLampItems.STAGE_SPEAKER = Registry.register(BuiltInRegistries.ITEM, BeatLamp.id("stage_speaker"), BeatLampItems.createStageSpeakerItem());
		BeatLampItems.LINKER = Registry.register(BuiltInRegistries.ITEM, BeatLamp.id("linker"), BeatLampItems.createLinkerItem());
		BeatLampItems.CONTROLLER = Registry.register(BuiltInRegistries.ITEM, BeatLamp.id("controller"), BeatLampItems.createControllerItem());

		// 5. Register Creative Tab
		BeatLampItems.TAB = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			BeatLamp.id("main"),
			FabricItemGroup.builder()
				.title(Component.translatable("itemGroup.beatlamp"))
				.icon(() -> new ItemStack(BeatLampItems.CONTROLLER))
				.displayItems((parameters, output) -> {
					output.accept(BeatLampItems.BEAT_LAMP);
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

		// Register Packets on Fabric
		PayloadTypeRegistry.playC2S().register(LampConfigurePayload.ID, LampConfigurePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(StageLightConfigurePayload.ID, StageLightConfigurePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FountainConfigurePayload.ID, FountainConfigurePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(LaserProjectorConfigurePayload.ID, LaserProjectorConfigurePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FogGeneratorConfigurePayload.ID, FogGeneratorConfigurePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(LampSourcePayload.ID, LampSourcePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(EmitterSignalPayload.ID, EmitterSignalPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(EmitterConfigurePayload.ID, EmitterConfigurePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FountainFirePayload.ID, FountainFirePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(DmxConsolePayload.ID, DmxConsolePayload.CODEC);

		// 1. Lamp config receiver
		ServerPlayNetworking.registerGlobalReceiver(LampConfigurePayload.ID, (payload, context) -> {
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
					beatLamp.applyConfig(
						payload.mode(),
						payload.sensitivity(),
						payload.speed(),
						payload.color(),
						payload.frameless(),
						payload.blackback(),
						payload.idleLight(),
						payload.reverse(),
						payload.particles(),
						payload.orientation(),
						payload.tempoPulse(),
						payload.dmxEnrolled(),
						payload.customName()
					);
				}
			}
		});

		// 2. Stage Light config receiver
		ServerPlayNetworking.registerGlobalReceiver(StageLightConfigurePayload.ID, (payload, context) -> {
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

		// 3. Fountain config receiver
		ServerPlayNetworking.registerGlobalReceiver(FountainConfigurePayload.ID, (payload, context) -> {
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

		// 4. Laser Projector config receiver
		ServerPlayNetworking.registerGlobalReceiver(LaserProjectorConfigurePayload.ID, (payload, context) -> {
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

		// 5. Fog Generator config receiver
		ServerPlayNetworking.registerGlobalReceiver(FogGeneratorConfigurePayload.ID, (payload, context) -> {
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

		// 6. Source clear receiver
		ServerPlayNetworking.registerGlobalReceiver(LampSourcePayload.ID, (payload, context) -> {
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

		// 7. Emitter signal receiver
		ServerPlayNetworking.registerGlobalReceiver(EmitterSignalPayload.ID, (payload, context) -> {
			Level level = context.player().level();

			if (level.getBlockEntity(payload.pos()) instanceof BeatEmitterBlockEntity emitter) {
				emitter.setSignal(payload.signal(), level.getGameTime());
			}
		});

		// 7b. Emitter config receiver
		ServerPlayNetworking.registerGlobalReceiver(EmitterConfigurePayload.ID, (payload, context) -> {
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
					emitter.applyConfig(
						payload.mode(),
						payload.threshold(),
						payload.inverted(),
						payload.dmxEnrolled(),
						payload.customName()
					);
				}
			}
		});

		// 8. Fountain fire payload receiver
		ServerPlayNetworking.registerGlobalReceiver(FountainFirePayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			Level level = player.level();
			if (level instanceof ServerLevel serverLevel && serverLevel.getBlockEntity(payload.pos()) instanceof FountainBlockEntity fountain) {
				long gameTime = serverLevel.getGameTime();
				if (fountain.canFire(gameTime)) {
					fountain.markFired(gameTime);
					BeatLamp.spawnFirework(serverLevel, payload.pos(), fountain.getColor());
				}
			}
		});

		// 9. DMX Console config receiver
		ServerPlayNetworking.registerGlobalReceiver(DmxConsolePayload.ID, (payload, context) -> {
			Level level = context.player().level();
			if (level.getBlockEntity(payload.pos()) instanceof com.beatlamp.block.DmxConsoleBlockEntity dmx) {
				dmx.setBlackout(payload.blackout());
				dmx.setStrobeAll(payload.strobeAll());
				dmx.setMasterDimmer(payload.masterDimmer());
				dmx.setMasterSpeed(payload.masterSpeed());
			}
		});
	}
}
