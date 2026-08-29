package com.beatlamp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

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

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeatLamp implements ModInitializer {
	public static final String MOD_ID = "beatlamp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final int MAX_GROUP_SIZE = 512;
	public static final int LINK_RANGE = 48;

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Beat Lamp initializing (1.21.1)");

		BeatLampBlocks.register();
		BeatLampBlockEntities.register();
		BeatLampItems.register();

		// Register Packets
		PayloadTypeRegistry.playC2S().register(LampConfigurePayload.ID, LampConfigurePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(StageLightConfigurePayload.ID, StageLightConfigurePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FountainConfigurePayload.ID, FountainConfigurePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(LaserProjectorConfigurePayload.ID, LaserProjectorConfigurePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FogGeneratorConfigurePayload.ID, FogGeneratorConfigurePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(LampSourcePayload.ID, LampSourcePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(EmitterSignalPayload.ID, EmitterSignalPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FountainFirePayload.ID, FountainFirePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(com.beatlamp.network.DmxConsolePayload.ID, com.beatlamp.network.DmxConsolePayload.CODEC);

		// 1. Lamp config receiver
		ServerPlayNetworking.registerGlobalReceiver(LampConfigurePayload.ID, (payload, context) -> {
			Level level = context.player().level();

			if (payload.unlink()) {
				unlinkGroup(level, payload.pos());
				return;
			}

			List<BlockPos> members = null;
			if (level.getBlockEntity(payload.pos()) instanceof BeatLampBlockEntity lamp && lamp.getManualGroup().size() >= 2) {
				members = lamp.getManualGroup();
			}

			if (members == null) {
				members = floodFill(level, payload.pos());
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
				unlinkStageLightGroup(level, payload.pos());
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
				unlinkFountainGroup(level, payload.pos());
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
				unlinkLaserGroup(level, payload.pos());
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
				unlinkFogGroup(level, payload.pos());
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
				List<BlockPos> members = lamp.getManualGroup().size() >= 2 ? lamp.getManualGroup() : floodFill(level, payload.pos());
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

		// 7. Emitter signal receiver
		ServerPlayNetworking.registerGlobalReceiver(EmitterSignalPayload.ID, (payload, context) -> {
			Level level = context.player().level();

			if (level.getBlockEntity(payload.pos()) instanceof BeatEmitterBlockEntity emitter) {
				emitter.setSignal(payload.signal(), level.getGameTime());
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
					spawnFirework(serverLevel, payload.pos(), fountain.getColor());
				}
			}
		});

		// 9. DMX Console config receiver
		ServerPlayNetworking.registerGlobalReceiver(com.beatlamp.network.DmxConsolePayload.ID, (payload, context) -> {
			Level level = context.player().level();
			if (level.getBlockEntity(payload.pos()) instanceof com.beatlamp.block.DmxConsoleBlockEntity dmx) {
				dmx.setBlackout(payload.blackout());
				dmx.setStrobeAll(payload.strobeAll());
				dmx.setMasterDimmer(payload.masterDimmer());
				dmx.setMasterSpeed(payload.masterSpeed());
			}
		});
	}

	private static void spawnFirework(ServerLevel level, BlockPos pos, int color) {
		java.util.List<Integer> colors = new java.util.ArrayList<>();

		if (color == BeatLampBlockEntity.COLOR_OLED) {
			for (net.minecraft.world.item.DyeColor dye : net.minecraft.world.item.DyeColor.values()) {
				colors.add(dye.getFireworkColor());
			}
		} else {
			colors.add(color);
		}

		net.minecraft.world.item.component.FireworkExplosion explosion = new net.minecraft.world.item.component.FireworkExplosion(
			net.minecraft.world.item.component.FireworkExplosion.Shape.values()[level.random.nextInt(net.minecraft.world.item.component.FireworkExplosion.Shape.values().length)],
			new it.unimi.dsi.fastutil.ints.IntArrayList(colors),
			new it.unimi.dsi.fastutil.ints.IntArrayList(),
			level.random.nextBoolean(),
			true
		);

		net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET);
		stack.set(net.minecraft.core.component.DataComponents.FIREWORKS, new net.minecraft.world.item.component.Fireworks(1, java.util.List.of(explosion)));

		net.minecraft.world.entity.projectile.FireworkRocketEntity rocket = new net.minecraft.world.entity.projectile.FireworkRocketEntity(
			level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack
		);
		level.addFreshEntity(rocket);
	}

	public static void handleLink(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack linker) {
		BlockPos anchor = linker.get(BeatLampItems.ANCHOR_POS);

		if (anchor == null) {
			BlockEntity startBe = level.getBlockEntity(pos);
			if (!(startBe instanceof BeatLampBlockEntity || startBe instanceof StageLightBlockEntity
				|| startBe instanceof FountainBlockEntity || startBe instanceof LaserProjectorBlockEntity
				|| startBe instanceof FogGeneratorBlockEntity || startBe instanceof BeatEmitterBlockEntity)) {
				message(player, "message.beatlamp.link.invalid_start");
				return;
			}

			linker.set(BeatLampItems.ANCHOR_POS, pos.immutable());
			message(player, "message.beatlamp.link.anchor");
			return;
		}

		if (anchor.equals(pos)) {
			linker.remove(BeatLampItems.ANCHOR_POS);
			message(player, "message.beatlamp.link.cancel");
			return;
		}

		if (anchor.distSqr(pos) > (double) LINK_RANGE * LINK_RANGE) {
			message(player, "message.beatlamp.link.too_far", LINK_RANGE);
			return;
		}

		BlockEntity anchorBe = level.getBlockEntity(anchor);
		if (anchorBe == null) {
			linker.remove(BeatLampItems.ANCHOR_POS);
			message(player, "message.beatlamp.link.cancel");
			return;
		}

		int minX = Math.min(anchor.getX(), pos.getX());
		int minY = Math.min(anchor.getY(), pos.getY());
		int minZ = Math.min(anchor.getZ(), pos.getZ());
		int maxX = Math.max(anchor.getX(), pos.getX());
		int maxY = Math.max(anchor.getY(), pos.getY());
		int maxZ = Math.max(anchor.getZ(), pos.getZ());

		List<BlockPos> positions = new ArrayList<>();

		if (anchorBe instanceof BeatLampBlockEntity) {
			List<BeatLampBlockEntity> lamps = new ArrayList<>();
			for (BlockPos memberPos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
				if (level.getBlockEntity(memberPos) instanceof BeatLampBlockEntity lamp) {
					lamps.add(lamp);
					positions.add(lamp.getBlockPos().immutable());
					if (lamps.size() >= MAX_GROUP_SIZE) {
						message(player, "message.beatlamp.link.full", MAX_GROUP_SIZE);
						break;
					}
				}
			}
			if (lamps.size() >= 2) {
				for (BeatLampBlockEntity member : lamps) {
					member.setManualGroup(positions);
				}
			}
		} else if (anchorBe instanceof StageLightBlockEntity) {
			List<StageLightBlockEntity> lights = new ArrayList<>();
			for (BlockPos memberPos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
				if (level.getBlockEntity(memberPos) instanceof StageLightBlockEntity light) {
					lights.add(light);
					positions.add(light.getBlockPos().immutable());
					if (lights.size() >= MAX_GROUP_SIZE) {
						message(player, "message.beatlamp.link.full", MAX_GROUP_SIZE);
						break;
					}
				}
			}
			if (lights.size() >= 2) {
				for (StageLightBlockEntity member : lights) {
					member.setManualGroup(positions);
				}
			}
		} else if (anchorBe instanceof FountainBlockEntity) {
			List<FountainBlockEntity> fountains = new ArrayList<>();
			for (BlockPos memberPos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
				if (level.getBlockEntity(memberPos) instanceof FountainBlockEntity fountain) {
					fountains.add(fountain);
					positions.add(fountain.getBlockPos().immutable());
					if (fountains.size() >= MAX_GROUP_SIZE) {
						message(player, "message.beatlamp.link.full", MAX_GROUP_SIZE);
						break;
					}
				}
			}
			if (fountains.size() >= 2) {
				for (FountainBlockEntity member : fountains) {
					member.setManualGroup(positions);
				}
			}
		} else if (anchorBe instanceof LaserProjectorBlockEntity) {
			List<LaserProjectorBlockEntity> lasers = new ArrayList<>();
			for (BlockPos memberPos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
				if (level.getBlockEntity(memberPos) instanceof LaserProjectorBlockEntity laser) {
					lasers.add(laser);
					positions.add(laser.getBlockPos().immutable());
					if (lasers.size() >= MAX_GROUP_SIZE) {
						message(player, "message.beatlamp.link.full", MAX_GROUP_SIZE);
						break;
					}
				}
			}
			if (lasers.size() >= 2) {
				for (LaserProjectorBlockEntity member : lasers) {
					member.setManualGroup(positions);
				}
			}
		} else if (anchorBe instanceof FogGeneratorBlockEntity) {
			List<FogGeneratorBlockEntity> fogs = new ArrayList<>();
			for (BlockPos memberPos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
				if (level.getBlockEntity(memberPos) instanceof FogGeneratorBlockEntity fog) {
					fogs.add(fog);
					positions.add(fog.getBlockPos().immutable());
					if (fogs.size() >= MAX_GROUP_SIZE) {
						message(player, "message.beatlamp.link.full", MAX_GROUP_SIZE);
						break;
					}
				}
			}
			if (fogs.size() >= 2) {
				for (FogGeneratorBlockEntity member : fogs) {
					member.setManualGroup(positions);
				}
			}
		}

		if (positions.size() < 2) {
			message(player, "message.beatlamp.link.empty");
			return;
		}

		linker.remove(BeatLampItems.ANCHOR_POS);
		message(player, "message.beatlamp.link.added", positions.size());
	}

	public static void unlinkGroup(Level level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof BeatLampBlockEntity lamp)) {
			return;
		}

		List<BlockPos> targets = lamp.getManualGroup().size() >= 2 ? lamp.getManualGroup() : List.of(pos);
		for (BlockPos target : targets) {
			if (level.getBlockEntity(target) instanceof BeatLampBlockEntity other) {
				other.clearManualGroup();
			}
		}
	}

	public static void unlinkStageLightGroup(Level level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof StageLightBlockEntity light)) {
			return;
		}

		List<BlockPos> targets = light.getManualGroup().size() >= 2 ? light.getManualGroup() : List.of(pos);
		for (BlockPos target : targets) {
			if (level.getBlockEntity(target) instanceof StageLightBlockEntity other) {
				other.clearManualGroup();
			}
		}
	}

	public static void unlinkFountainGroup(Level level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof FountainBlockEntity fountain)) {
			return;
		}

		List<BlockPos> targets = fountain.getManualGroup().size() >= 2 ? fountain.getManualGroup() : List.of(pos);
		for (BlockPos target : targets) {
			if (level.getBlockEntity(target) instanceof FountainBlockEntity other) {
				other.clearManualGroup();
			}
		}
	}

	public static void unlinkLaserGroup(Level level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof LaserProjectorBlockEntity laser)) {
			return;
		}

		List<BlockPos> targets = laser.getManualGroup().size() >= 2 ? laser.getManualGroup() : List.of(pos);
		for (BlockPos target : targets) {
			if (level.getBlockEntity(target) instanceof LaserProjectorBlockEntity other) {
				other.clearManualGroup();
			}
		}
	}

	public static void unlinkFogGroup(Level level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof FogGeneratorBlockEntity fog)) {
			return;
		}

		List<BlockPos> targets = fog.getManualGroup().size() >= 2 ? fog.getManualGroup() : List.of(pos);
		for (BlockPos target : targets) {
			if (level.getBlockEntity(target) instanceof FogGeneratorBlockEntity other) {
				other.clearManualGroup();
			}
		}
	}

	public static void bindTarget(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack controller, BlockPos source) {
		if (level.getBlockEntity(pos) instanceof BeatEmitterBlockEntity emitter) {
			emitter.setSource(source);
			message(player, "message.beatlamp.source.bound_emitter");
			return;
		}

		if (level.getBlockEntity(pos) instanceof StageLightBlockEntity light) {
			List<BlockPos> members = light.getManualGroup().size() >= 2 ? light.getManualGroup() : List.of(pos);
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof StageLightBlockEntity l) {
					l.setSource(source);
				}
			}
			message(player, "message.beatlamp.source.bound", members.size());
			return;
		}

		if (level.getBlockEntity(pos) instanceof FountainBlockEntity fountain) {
			List<BlockPos> members = fountain.getManualGroup().size() >= 2 ? fountain.getManualGroup() : List.of(pos);
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof FountainBlockEntity f) {
					f.setSource(source);
				}
			}
			message(player, "message.beatlamp.source.bound", members.size());
			return;
		}

		if (level.getBlockEntity(pos) instanceof LaserProjectorBlockEntity laser) {
			List<BlockPos> members = laser.getManualGroup().size() >= 2 ? laser.getManualGroup() : List.of(pos);
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof LaserProjectorBlockEntity l) {
					l.setSource(source);
				}
			}
			message(player, "message.beatlamp.source.bound", members.size());
			return;
		}

		if (level.getBlockEntity(pos) instanceof FogGeneratorBlockEntity fog) {
			List<BlockPos> members = fog.getManualGroup().size() >= 2 ? fog.getManualGroup() : List.of(pos);
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof FogGeneratorBlockEntity f) {
					f.setSource(source);
				}
			}
			message(player, "message.beatlamp.source.bound", members.size());
			return;
		}

		if (level.getBlockEntity(pos) instanceof BeatLampBlockEntity) {
			bindSource(level, pos, player, controller, source);
		}
	}

	public static void toggleTarget(ServerPlayer player, BlockPos pos) {
		if (player.level().getBlockEntity(pos) instanceof BeatEmitterBlockEntity emitter) {
			emitter.togglePulseMode();
			message(player, emitter.isPulseMode() ? "message.beatlamp.emitter.mode.pulse" : "message.beatlamp.emitter.mode.level");
		}
	}

	public static void handleSourceSelect(ServerPlayer player, BlockPos jukeboxPos, ItemStack controller) {
		BlockPos current = controller.get(BeatLampItems.SOURCE_POS);

		if (jukeboxPos.equals(current)) {
			controller.remove(BeatLampItems.SOURCE_POS);
			message(player, "message.beatlamp.source.cancel");
			return;
		}

		controller.set(BeatLampItems.SOURCE_POS, jukeboxPos.immutable());
		message(player, "message.beatlamp.source.selected");
	}

	public static void bindSource(ServerLevel level, BlockPos lampPos, ServerPlayer player, ItemStack controller, BlockPos source) {
		List<BlockPos> members = null;

		if (level.getBlockEntity(lampPos) instanceof BeatLampBlockEntity lamp && lamp.getManualGroup().size() >= 2) {
			members = lamp.getManualGroup();
		}

		if (members == null) {
			members = floodFill(level, lampPos);
		}

		for (BlockPos member : members) {
			if (level.getBlockEntity(member) instanceof BeatLampBlockEntity beatLamp) {
				beatLamp.setSource(source);
			}
		}

		controller.remove(BeatLampItems.SOURCE_POS);
		message(player, "message.beatlamp.source.bound", members.size());
	}

	private static void message(ServerPlayer player, String key, Object... args) {
		player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.GOLD), true);
	}

	public static List<BlockPos> floodFill(Level level, BlockPos start) {
		List<BlockPos> result = new ArrayList<>();
		Set<BlockPos> visited = new HashSet<>();
		Queue<BlockPos> queue = new ArrayDeque<>();

		queue.add(start.immutable());
		visited.add(start.immutable());

		while (!queue.isEmpty() && result.size() < MAX_GROUP_SIZE) {
			BlockPos current = queue.poll();
			result.add(current);

			for (Direction dir : Direction.values()) {
				BlockPos neighbor = current.relative(dir);
				if (!visited.contains(neighbor) && level.getBlockEntity(neighbor) instanceof BeatLampBlockEntity) {
					visited.add(neighbor);
					queue.add(neighbor);
				}
			}
		}

		return result;
	}
}
