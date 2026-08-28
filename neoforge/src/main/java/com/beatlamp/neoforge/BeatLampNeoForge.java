package com.beatlamp.neoforge;

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
import com.beatlamp.network.EmitterSignalPayload;
import com.beatlamp.network.FogGeneratorConfigurePayload;
import com.beatlamp.network.FountainConfigurePayload;
import com.beatlamp.network.FountainFirePayload;
import com.beatlamp.network.LampConfigurePayload;
import com.beatlamp.network.LampSourcePayload;
import com.beatlamp.network.LaserProjectorConfigurePayload;
import com.beatlamp.network.StageLightConfigurePayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(BeatLamp.MOD_ID)
public class BeatLampNeoForge {
	public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BeatLamp.MOD_ID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
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

	public BeatLampNeoForge(IEventBus modEventBus) {
		BeatLamp.LOGGER.info("Beat Lamp NeoForge initializing (1.21.1)");

		BeatLampBlocks.register();
		BeatLampBlockEntities.register();
		BeatLampItems.register();
		CREATIVE_TABS.register(modEventBus);

		modEventBus.addListener(this::registerPayloads);
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
		registrar.playToServer(FountainFirePayload.ID, FountainFirePayload.CODEC, this::handleFountainFire);
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
						payload.particles(), payload.orientation());
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
				}
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

	private void handleFountainFire(FountainFirePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer player && player.level() instanceof ServerLevel serverLevel) {
				if (serverLevel.getBlockEntity(payload.pos()) instanceof FountainBlockEntity fountain) {
					long gameTime = serverLevel.getGameTime();
					if (fountain.canFire(gameTime)) {
						fountain.markFired(gameTime);
					}
				}
			}
		});
	}
}
