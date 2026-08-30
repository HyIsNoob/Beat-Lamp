package com.beatlamp.forge;

import java.util.List;

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

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(BeatLamp.MOD_ID)
public class BeatLampForge {
	public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BeatLamp.MOD_ID);

	public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_TABS.register("main", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
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

	public static SimpleChannel CHANNEL;

	public BeatLampForge() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		BeatLamp.LOGGER.info("Beat Lamp Forge initializing (1.21.1)");

		BeatLampBlocks.register();
		BeatLampBlockEntities.register();
		BeatLampItems.register();
		CREATIVE_TABS.register(modEventBus);

		registerPackets();
		MinecraftForge.EVENT_BUS.register(this);
	}

	private void registerPackets() {
		CHANNEL = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "main"))
			.networkProtocolVersion(1)
			.simpleChannel();

		CHANNEL.messageBuilder(LampConfigurePayload.class)
			.encoder((msg, buf) -> LampConfigurePayload.CODEC.encode(buf, msg))
			.decoder(LampConfigurePayload.CODEC::decode)
			.consumerMainThread((payload, ctx) -> {
				var player = ctx.getSender();
				if (player == null) return;
				Level level = player.level();
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
			}).add();

		CHANNEL.messageBuilder(StageLightConfigurePayload.class)
			.encoder((msg, buf) -> StageLightConfigurePayload.CODEC.encode(buf, msg))
			.decoder(StageLightConfigurePayload.CODEC::decode)
			.consumerMainThread((payload, ctx) -> {
				var player = ctx.getSender();
				if (player == null) return;
				Level level = player.level();
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
			}).add();

		CHANNEL.messageBuilder(LaserProjectorConfigurePayload.class)
			.encoder((msg, buf) -> LaserProjectorConfigurePayload.CODEC.encode(buf, msg))
			.decoder(LaserProjectorConfigurePayload.CODEC::decode)
			.consumerMainThread((payload, ctx) -> {
				var player = ctx.getSender();
				if (player == null) return;
				Level level = player.level();
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
			}).add();

		CHANNEL.messageBuilder(FountainConfigurePayload.class)
			.encoder((msg, buf) -> FountainConfigurePayload.CODEC.encode(buf, msg))
			.decoder(FountainConfigurePayload.CODEC::decode)
			.consumerMainThread((payload, ctx) -> {
				var player = ctx.getSender();
				if (player == null) return;
				Level level = player.level();
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
			}).add();

		CHANNEL.messageBuilder(FogGeneratorConfigurePayload.class)
			.encoder((msg, buf) -> FogGeneratorConfigurePayload.CODEC.encode(buf, msg))
			.decoder(FogGeneratorConfigurePayload.CODEC::decode)
			.consumerMainThread((payload, ctx) -> {
				var player = ctx.getSender();
				if (player == null) return;
				Level level = player.level();
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
			}).add();

		CHANNEL.messageBuilder(EmitterConfigurePayload.class)
			.encoder((msg, buf) -> EmitterConfigurePayload.CODEC.encode(buf, msg))
			.decoder(EmitterConfigurePayload.CODEC::decode)
			.consumerMainThread((payload, ctx) -> {
				var player = ctx.getSender();
				if (player == null) return;
				Level level = player.level();
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
			}).add();

		CHANNEL.messageBuilder(LampSourcePayload.class)
			.encoder((msg, buf) -> LampSourcePayload.CODEC.encode(buf, msg))
			.decoder(LampSourcePayload.CODEC::decode)
			.consumerMainThread((payload, ctx) -> {
				var player = ctx.getSender();
				if (player == null) return;
				Level level = player.level();
				if (level.getBlockEntity(payload.pos()) instanceof BeatLampBlockEntity lamp) {
					List<BlockPos> members = lamp.getManualGroup().size() >= 2 ? lamp.getManualGroup() : BeatLamp.floodFill(level, payload.pos());
					for (BlockPos member : members) {
						if (level.getBlockEntity(member) instanceof BeatLampBlockEntity l) {
							l.setSource(null);
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
			}).add();

		CHANNEL.messageBuilder(EmitterSignalPayload.class)
			.encoder((msg, buf) -> EmitterSignalPayload.CODEC.encode(buf, msg))
			.decoder(EmitterSignalPayload.CODEC::decode)
			.consumerMainThread((payload, ctx) -> {
				var player = ctx.getSender();
				if (player == null) return;
				Level level = player.level();
				if (level.getBlockEntity(payload.pos()) instanceof BeatEmitterBlockEntity emitter) {
					emitter.setSignal(payload.signal(), level.getGameTime());
				}
			}).add();

		CHANNEL.messageBuilder(FountainFirePayload.class)
			.encoder((msg, buf) -> FountainFirePayload.CODEC.encode(buf, msg))
			.decoder(FountainFirePayload.CODEC::decode)
			.consumerMainThread((payload, ctx) -> {
				var player = ctx.getSender();
				if (player != null && player.level() instanceof ServerLevel serverLevel) {
					if (serverLevel.getBlockEntity(payload.pos()) instanceof FountainBlockEntity fountain) {
						long gameTime = serverLevel.getGameTime();
						if (fountain.canFire(gameTime)) {
							fountain.markFired(gameTime);
						}
					}
				}
			}).add();

		CHANNEL.messageBuilder(DmxConsolePayload.class)
			.encoder((msg, buf) -> DmxConsolePayload.CODEC.encode(buf, msg))
			.decoder(DmxConsolePayload.CODEC::decode)
			.consumerMainThread((payload, ctx) -> {
				var player = ctx.getSender();
				if (player == null) return;
				Level level = player.level();
				if (level.getBlockEntity(payload.pos()) instanceof DmxConsoleBlockEntity dmx) {
					dmx.setBlackout(payload.blackout());
					dmx.setStrobeAll(payload.strobeAll());
					dmx.setMasterDimmer(payload.masterDimmer());
					dmx.setMasterSpeed(payload.masterSpeed());
				}
			}).add();
	}
}
