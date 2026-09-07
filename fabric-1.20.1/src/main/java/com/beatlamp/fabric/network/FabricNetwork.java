package com.beatlamp.fabric.network;

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

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class FabricNetwork {
	public static final ResourceLocation LAMP_CONFIGURE_ID = new ResourceLocation(BeatLamp.MOD_ID, "lamp_configure");
	public static final ResourceLocation STAGE_LIGHT_CONFIGURE_ID = new ResourceLocation(BeatLamp.MOD_ID, "stage_light_configure");
	public static final ResourceLocation FOUNTAIN_CONFIGURE_ID = new ResourceLocation(BeatLamp.MOD_ID, "fountain_configure");
	public static final ResourceLocation LASER_PROJECTOR_CONFIGURE_ID = new ResourceLocation(BeatLamp.MOD_ID, "laser_projector_configure");
	public static final ResourceLocation FOG_GENERATOR_CONFIGURE_ID = new ResourceLocation(BeatLamp.MOD_ID, "fog_generator_configure");
	public static final ResourceLocation EMITTER_SIGNAL_ID = new ResourceLocation(BeatLamp.MOD_ID, "emitter_signal");
	public static final ResourceLocation LAMP_SOURCE_ID = new ResourceLocation(BeatLamp.MOD_ID, "lamp_source");
	public static final ResourceLocation FOUNTAIN_FIRE_ID = new ResourceLocation(BeatLamp.MOD_ID, "fountain_fire");
	public static final ResourceLocation DMX_CONSOLE_ID = new ResourceLocation(BeatLamp.MOD_ID, "dmx_console");

	private FabricNetwork() {
	}

	public static boolean handleServerPayload(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buf) {
		if (LAMP_CONFIGURE_ID.equals(id)) {
			LampConfigurePayload msg = LampConfigurePayload.read(buf);
			player.server.execute(() -> {
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
			return true;
		}

		if (STAGE_LIGHT_CONFIGURE_ID.equals(id)) {
			StageLightConfigurePayload msg = StageLightConfigurePayload.read(buf);
			player.server.execute(() -> {
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
							memberLight.setTempoPulse(msg.tempoPulse());
							memberLight.setDmxEnrolled(msg.dmxEnrolled());
							memberLight.setCustomName(msg.customName());
						}
					}
				}
			});
			return true;
		}

		if (FOUNTAIN_CONFIGURE_ID.equals(id)) {
			FountainConfigurePayload msg = FountainConfigurePayload.read(buf);
			player.server.execute(() -> {
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
			return true;
		}

		if (LASER_PROJECTOR_CONFIGURE_ID.equals(id)) {
			LaserProjectorConfigurePayload msg = LaserProjectorConfigurePayload.read(buf);
			player.server.execute(() -> {
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
			return true;
		}

		if (FOG_GENERATOR_CONFIGURE_ID.equals(id)) {
			FogGeneratorConfigurePayload msg = FogGeneratorConfigurePayload.read(buf);
			player.server.execute(() -> {
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
			return true;
		}

		if (EMITTER_SIGNAL_ID.equals(id)) {
			EmitterSignalPayload msg = EmitterSignalPayload.read(buf);
			player.server.execute(() -> {
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
			return true;
		}

		if (LAMP_SOURCE_ID.equals(id)) {
			LampSourcePayload msg = LampSourcePayload.read(buf);
			player.server.execute(() -> {
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
			return true;
		}

		if (FOUNTAIN_FIRE_ID.equals(id)) {
			FountainFirePayload msg = FountainFirePayload.read(buf);
			player.server.execute(() -> {
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
			return true;
		}

		if (DMX_CONSOLE_ID.equals(id)) {
			DmxConsolePayload msg = DmxConsolePayload.read(buf);
			player.server.execute(() -> {
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
			return true;
		}

		return false;
	}

	public static void sendToServer(Object message) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		ResourceLocation channelId;

		if (message instanceof LampConfigurePayload payload) {
			channelId = LAMP_CONFIGURE_ID;
			payload.write(buf);
		} else if (message instanceof StageLightConfigurePayload payload) {
			channelId = STAGE_LIGHT_CONFIGURE_ID;
			payload.write(buf);
		} else if (message instanceof FountainConfigurePayload payload) {
			channelId = FOUNTAIN_CONFIGURE_ID;
			payload.write(buf);
		} else if (message instanceof LaserProjectorConfigurePayload payload) {
			channelId = LASER_PROJECTOR_CONFIGURE_ID;
			payload.write(buf);
		} else if (message instanceof FogGeneratorConfigurePayload payload) {
			channelId = FOG_GENERATOR_CONFIGURE_ID;
			payload.write(buf);
		} else if (message instanceof EmitterSignalPayload payload) {
			channelId = EMITTER_SIGNAL_ID;
			payload.write(buf);
		} else if (message instanceof LampSourcePayload payload) {
			channelId = LAMP_SOURCE_ID;
			payload.write(buf);
		} else if (message instanceof FountainFirePayload payload) {
			channelId = FOUNTAIN_FIRE_ID;
			payload.write(buf);
		} else if (message instanceof DmxConsolePayload payload) {
			channelId = DMX_CONSOLE_ID;
			payload.write(buf);
		} else {
			throw new IllegalArgumentException("Unknown payload type: " + message.getClass());
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() != null) {
			mc.getConnection().send(new ServerboundCustomPayloadPacket(channelId, buf));
		}
	}
}
