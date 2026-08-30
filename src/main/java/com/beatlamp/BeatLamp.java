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
import com.beatlamp.config.BeatLampConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeatLamp {
	public static final String MOD_ID = "beatlamp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final int LINK_RANGE = 48;

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	public static void initCommon() {
		LOGGER.info("Beat Lamp common initializing (1.21.1)");

		BeatLampConfig.load();
		BeatLampBlocks.register();
		BeatLampBlockEntities.register();
		BeatLampItems.register();
	}

	public static void spawnFirework(ServerLevel level, BlockPos pos, int color) {
		if (!BeatLampConfig.enablePhysicalFireworks) {
			return;
		}

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

		if (Math.abs(anchor.getX() - pos.getX()) > LINK_RANGE
			|| Math.abs(anchor.getY() - pos.getY()) > LINK_RANGE
			|| Math.abs(anchor.getZ() - pos.getZ()) > LINK_RANGE) {
			message(player, "message.beatlamp.link.too_far", LINK_RANGE);
			return;
		}

		BlockEntity anchorBe = level.getBlockEntity(anchor);
		BlockEntity targetBe = level.getBlockEntity(pos);

		if (anchorBe == null || targetBe == null || anchorBe.getClass() != targetBe.getClass()) {
			message(player, "message.beatlamp.link.type_mismatch");
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
					if (lamps.size() >= BeatLampConfig.maxGroupLinkSize) {
						message(player, "message.beatlamp.link.full", BeatLampConfig.maxGroupLinkSize);
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
					if (lights.size() >= BeatLampConfig.maxGroupLinkSize) {
						message(player, "message.beatlamp.link.full", BeatLampConfig.maxGroupLinkSize);
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
					if (fountains.size() >= BeatLampConfig.maxGroupLinkSize) {
						message(player, "message.beatlamp.link.full", BeatLampConfig.maxGroupLinkSize);
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
					if (lasers.size() >= BeatLampConfig.maxGroupLinkSize) {
						message(player, "message.beatlamp.link.full", BeatLampConfig.maxGroupLinkSize);
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
					if (fogs.size() >= BeatLampConfig.maxGroupLinkSize) {
						message(player, "message.beatlamp.link.full", BeatLampConfig.maxGroupLinkSize);
						break;
					}
				}
			}
			if (fogs.size() >= 2) {
				for (FogGeneratorBlockEntity member : fogs) {
					member.setManualGroup(positions);
				}
			}
		} else if (anchorBe instanceof BeatEmitterBlockEntity) {
			List<BeatEmitterBlockEntity> emitters = new ArrayList<>();
			for (BlockPos memberPos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
				if (level.getBlockEntity(memberPos) instanceof BeatEmitterBlockEntity emitter) {
					emitters.add(emitter);
					positions.add(emitter.getBlockPos().immutable());
					if (emitters.size() >= BeatLampConfig.maxGroupLinkSize) {
						message(player, "message.beatlamp.link.full", BeatLampConfig.maxGroupLinkSize);
						break;
					}
				}
			}
			if (emitters.size() >= 2) {
				for (BeatEmitterBlockEntity member : emitters) {
					member.setManualGroup(positions);
				}
			}
		}

		linker.remove(BeatLampItems.ANCHOR_POS);
		if (positions.size() < 2) {
			message(player, "message.beatlamp.link.single");
		} else {
			message(player, "message.beatlamp.link.success", positions.size());
		}
	}

	public static void unlinkGroup(Level level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof BeatLampBlockEntity lamp)) {
			return;
		}

		List<BlockPos> targets = lamp.getManualGroup().size() >= 2 ? lamp.getManualGroup() : floodFill(level, pos);
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

	public static void unlinkEmitterGroup(Level level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof BeatEmitterBlockEntity emitter)) {
			return;
		}

		List<BlockPos> targets = emitter.getManualGroup().size() >= 2 ? emitter.getManualGroup() : List.of(pos);
		for (BlockPos target : targets) {
			if (level.getBlockEntity(target) instanceof BeatEmitterBlockEntity other) {
				other.clearManualGroup();
			}
		}
	}

	public static void bindTarget(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack controller, BlockPos source) {
		if (level.getBlockEntity(pos) instanceof BeatEmitterBlockEntity emitter) {
			List<BlockPos> members = emitter.getManualGroup().size() >= 2 ? emitter.getManualGroup() : List.of(pos);
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof BeatEmitterBlockEntity e) {
					e.setSource(source);
				}
			}
			message(player, "message.beatlamp.source.bound", members.size());
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

		bindSource(level, pos, player, controller, source);
	}

	public static void toggleTarget(ServerPlayer player, BlockPos pos) {
		Level level = player.level();
		if (level.getBlockEntity(pos) instanceof BeatEmitterBlockEntity emitter) {
			emitter.setInverted(!emitter.isInverted());
			String key = emitter.isInverted() ? "message.beatlamp.emitter.inverted" : "message.beatlamp.emitter.normal";
			message(player, key);
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

		while (!queue.isEmpty() && result.size() < BeatLampConfig.maxGroupLinkSize) {
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
