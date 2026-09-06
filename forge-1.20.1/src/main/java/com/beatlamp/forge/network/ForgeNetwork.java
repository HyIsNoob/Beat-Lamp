package com.beatlamp.forge.network;

import java.util.List;

import com.beatlamp.BeatLamp;
import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.DmxConsoleBlockEntity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;
import com.beatlamp.network.DmxConsolePayload;
import com.beatlamp.network.EmitterSignalPayload;
import com.beatlamp.network.FogGeneratorConfigurePayload;
import com.beatlamp.network.FountainConfigurePayload;
import com.beatlamp.network.FountainFirePayload;
import com.beatlamp.network.LampConfigurePayload;
import com.beatlamp.network.LampSourcePayload;
import com.beatlamp.network.LaserProjectorConfigurePayload;
import com.beatlamp.network.StageLightConfigurePayload;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ForgeNetwork {
	private static final String PROTOCOL_VERSION = "1";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		new ResourceLocation(BeatLamp.MOD_ID, "main"),
		() -> PROTOCOL_VERSION,
		PROTOCOL_VERSION::equals,
		PROTOCOL_VERSION::equals
	);

	private static int packetId = 0;

	public static void register() {
		CHANNEL.registerMessage(
			packetId++,
			LampConfigurePayload.class,
			LampConfigurePayload::write,
			LampConfigurePayload::read,
			(msg, ctxSupplier) -> {
				var ctx = ctxSupplier.get();
				ctx.enqueueWork(() -> {
					ServerPlayer player = ctx.getSender();
					if (player == null) return;
					ServerLevel level = player.serverLevel();
					BlockPos pos = msg.pos();

					if (msg.unlink()) {
						BeatLamp.unlinkGroup(level, pos);
						return;
					}

					BlockEntity be = level.getBlockEntity(pos);
					if (be instanceof BeatLampBlockEntity lamp) {
						List<BlockPos> group = lamp.getManualGroup().isEmpty() ? BeatLamp.floodFill(level, pos) : lamp.getManualGroup();
						for (BlockPos memberPos : group) {
							if (level.getBlockEntity(memberPos) instanceof BeatLampBlockEntity memberLamp) {
								memberLamp.applyConfig(
									msg.mode(), msg.sensitivity(), msg.speed(), msg.color(),
									msg.frameless(), msg.blackback(), msg.idleLight(), msg.reverse(),
									msg.particles(), msg.orientation(), msg.tempoPulse(), msg.dmxEnrolled(), msg.customName()
								);
							}
						}
					}
				});
				ctx.setPacketHandled(true);
			}
		);

		CHANNEL.registerMessage(
			packetId++,
			StageLightConfigurePayload.class,
			StageLightConfigurePayload::write,
			StageLightConfigurePayload::read,
			(msg, ctxSupplier) -> {
				var ctx = ctxSupplier.get();
				ctx.enqueueWork(() -> {
					ServerPlayer player = ctx.getSender();
					if (player == null) return;
					ServerLevel level = player.serverLevel();
					BlockPos pos = msg.pos();

					if (msg.unlink()) {
						BeatLamp.unlinkStageLightGroup(level, pos);
						return;
					}

					BlockEntity be = level.getBlockEntity(pos);
					if (be instanceof StageLightBlockEntity light) {
						List<BlockPos> group = light.getManualGroup().isEmpty() ? List.of(pos) : light.getManualGroup();
						for (BlockPos memberPos : group) {
							if (level.getBlockEntity(memberPos) instanceof StageLightBlockEntity memberLight) {
								memberLight.setMode(msg.mode());
								memberLight.setSensitivity(msg.sensitivity());
								memberLight.setSpeed(msg.speed());
								memberLight.setColor(msg.color());
								memberLight.setDmxEnrolled(msg.dmxEnrolled());
								memberLight.setCustomName(msg.customName());
							}
						}
					}
				});
				ctx.setPacketHandled(true);
			}
		);

		CHANNEL.registerMessage(
			packetId++,
			FountainConfigurePayload.class,
			FountainConfigurePayload::write,
			FountainConfigurePayload::read,
			(msg, ctxSupplier) -> {
				var ctx = ctxSupplier.get();
				ctx.enqueueWork(() -> {
					ServerPlayer player = ctx.getSender();
					if (player == null) return;
					ServerLevel level = player.serverLevel();
					BlockPos pos = msg.pos();

					if (msg.unlink()) {
						BeatLamp.unlinkFountainGroup(level, pos);
						return;
					}

					BlockEntity be = level.getBlockEntity(pos);
					if (be instanceof FountainBlockEntity fountain) {
						List<BlockPos> group = fountain.getManualGroup().isEmpty() ? List.of(pos) : fountain.getManualGroup();
						for (BlockPos memberPos : group) {
							if (level.getBlockEntity(memberPos) instanceof FountainBlockEntity memberFountain) {
								memberFountain.setColor(msg.color());
								memberFountain.setFireworkMode(msg.fireworkMode());
								memberFountain.setSprayThreshold(msg.sprayThreshold());
								memberFountain.setImpactThreshold(msg.impactThreshold());
								memberFountain.setSmokeEnabled(msg.smokeEnabled());
								memberFountain.setParticleType(msg.particleType());
								memberFountain.setDmxEnrolled(msg.dmxEnrolled());
								memberFountain.setCustomName(msg.customName());
							}
						}
					}
				});
				ctx.setPacketHandled(true);
			}
		);

		CHANNEL.registerMessage(
			packetId++,
			LaserProjectorConfigurePayload.class,
			LaserProjectorConfigurePayload::write,
			LaserProjectorConfigurePayload::read,
			(msg, ctxSupplier) -> {
				var ctx = ctxSupplier.get();
				ctx.enqueueWork(() -> {
					ServerPlayer player = ctx.getSender();
					if (player == null) return;
					ServerLevel level = player.serverLevel();
					BlockPos pos = msg.pos();

					if (msg.unlink()) {
						BeatLamp.unlinkLaserGroup(level, pos);
						return;
					}

					BlockEntity be = level.getBlockEntity(pos);
					if (be instanceof LaserProjectorBlockEntity laser) {
						List<BlockPos> group = laser.getManualGroup().isEmpty() ? List.of(pos) : laser.getManualGroup();
						for (BlockPos memberPos : group) {
							if (level.getBlockEntity(memberPos) instanceof LaserProjectorBlockEntity memberLaser) {
								memberLaser.setMode(msg.mode());
								memberLaser.setBeamCount(msg.beamCount());
								memberLaser.setSpread(msg.spread());
								memberLaser.setSpeed(msg.speed());
								memberLaser.setColor(msg.color());
								memberLaser.setDmxEnrolled(msg.dmxEnrolled());
								memberLaser.setCustomName(msg.customName());
							}
						}
					}
				});
				ctx.setPacketHandled(true);
			}
		);

		CHANNEL.registerMessage(
			packetId++,
			FogGeneratorConfigurePayload.class,
			FogGeneratorConfigurePayload::write,
			FogGeneratorConfigurePayload::read,
			(msg, ctxSupplier) -> {
				var ctx = ctxSupplier.get();
				ctx.enqueueWork(() -> {
					ServerPlayer player = ctx.getSender();
					if (player == null) return;
					ServerLevel level = player.serverLevel();
					BlockPos pos = msg.pos();

					if (msg.unlink()) {
						BeatLamp.unlinkFogGroup(level, pos);
						return;
					}

					BlockEntity be = level.getBlockEntity(pos);
					if (be instanceof FogGeneratorBlockEntity fog) {
						List<BlockPos> group = fog.getManualGroup().isEmpty() ? List.of(pos) : fog.getManualGroup();
						for (BlockPos memberPos : group) {
							if (level.getBlockEntity(memberPos) instanceof FogGeneratorBlockEntity memberFog) {
								memberFog.setDensity(msg.density());
								memberFog.setRadius(msg.radius());
								memberFog.setColor(msg.color());
								memberFog.setDmxEnrolled(msg.dmxEnrolled());
								memberFog.setCustomName(msg.customName());
							}
						}
					}
				});
				ctx.setPacketHandled(true);
			}
		);

		CHANNEL.registerMessage(
			packetId++,
			EmitterSignalPayload.class,
			EmitterSignalPayload::write,
			EmitterSignalPayload::read,
			(msg, ctxSupplier) -> {
				var ctx = ctxSupplier.get();
				ctx.enqueueWork(() -> {
					ServerPlayer player = ctx.getSender();
					if (player == null) return;
					ServerLevel level = player.serverLevel();
					BlockPos pos = msg.pos();

					if (msg.unlink()) {
						BeatLamp.unlinkEmitterGroup(level, pos);
						return;
					}

					BlockEntity be = level.getBlockEntity(pos);
					if (be instanceof BeatEmitterBlockEntity emitter) {
						List<BlockPos> group = emitter.getManualGroup().isEmpty() ? List.of(pos) : emitter.getManualGroup();
						for (BlockPos memberPos : group) {
							if (level.getBlockEntity(memberPos) instanceof BeatEmitterBlockEntity memberEmitter) {
								memberEmitter.applyConfig(msg.mode(), msg.threshold(), msg.inverted(), msg.dmxEnrolled(), msg.customName());
							}
						}
					}
				});
				ctx.setPacketHandled(true);
			}
		);

		CHANNEL.registerMessage(
			packetId++,
			LampSourcePayload.class,
			LampSourcePayload::write,
			LampSourcePayload::read,
			(msg, ctxSupplier) -> {
				var ctx = ctxSupplier.get();
				ctx.enqueueWork(() -> {
					ServerPlayer player = ctx.getSender();
					if (player == null) return;
					ServerLevel level = player.serverLevel();
					BlockPos targetPos = msg.targetPos();
					BlockPos sourcePos = msg.sourcePos();

					if (sourcePos == null) {
						BlockEntity be = level.getBlockEntity(targetPos);
						if (be instanceof BeatLampBlockEntity l) l.setSource(null);
						else if (be instanceof StageLightBlockEntity s) s.setSource(null);
						else if (be instanceof FountainBlockEntity f) f.setSource(null);
						else if (be instanceof LaserProjectorBlockEntity lp) lp.setSource(null);
						else if (be instanceof FogGeneratorBlockEntity fg) fg.setSource(null);
						else if (be instanceof BeatEmitterBlockEntity em) em.setSource(null);
					} else {
						BeatLamp.bindSource(level, targetPos, sourcePos, player, player.getMainHandItem());
					}
				});
				ctx.setPacketHandled(true);
			}
		);

		CHANNEL.registerMessage(
			packetId++,
			FountainFirePayload.class,
			FountainFirePayload::write,
			FountainFirePayload::read,
			(msg, ctxSupplier) -> {
				var ctx = ctxSupplier.get();
				ctx.enqueueWork(() -> {
					ServerPlayer player = ctx.getSender();
					if (player == null) return;
					ServerLevel level = player.serverLevel();
					BlockPos pos = msg.pos();

					BlockEntity be = level.getBlockEntity(pos);
					if (be instanceof FountainBlockEntity fountain) {
						if (fountain.canFire(level.getGameTime())) {
							fountain.markFired(level.getGameTime());
							BeatLamp.spawnFirework(level, pos, fountain.getColor());
						}
					}
				});
				ctx.setPacketHandled(true);
			}
		);

		CHANNEL.registerMessage(
			packetId++,
			DmxConsolePayload.class,
			DmxConsolePayload::write,
			DmxConsolePayload::read,
			(msg, ctxSupplier) -> {
				var ctx = ctxSupplier.get();
				ctx.enqueueWork(() -> {
					ServerPlayer player = ctx.getSender();
					if (player == null) return;
					ServerLevel level = player.serverLevel();
					BlockPos pos = msg.pos();

					BlockEntity be = level.getBlockEntity(pos);
					if (be instanceof DmxConsoleBlockEntity dmx) {
						dmx.setBlackout(msg.blackout());
						dmx.setStrobeAll(msg.strobeAll());
						dmx.setMasterDimmer(msg.masterDimmer());
						dmx.setMasterSpeed(msg.masterSpeed());
					}
				});
				ctx.setPacketHandled(true);
			}
		);
	}

	public static void sendToServer(Object message) {
		CHANNEL.sendToServer(message);
	}
}
