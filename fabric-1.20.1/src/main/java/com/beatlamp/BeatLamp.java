package com.beatlamp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BeatLamp {
	public static final String MOD_ID = "beatlamp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final int LINK_RANGE = 48;

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	public static void initCommon() {
		LOGGER.info("Beat Lamp common initializing (1.20.1)");
		BeatLampConfig.load();
	}

	public static void spawnFirework(ServerLevel level, BlockPos pos, int color) {
		List<Integer> colors = new ArrayList<>();

		if (color == BeatLampBlockEntity.COLOR_OLED) {
			for (DyeColor dye : DyeColor.values()) {
				colors.add(dye.getFireworkColor());
			}
		} else {
			colors.add(color);
		}

		ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
		CompoundTag tag = stack.getOrCreateTagElement("Fireworks");
		ListTag explosions = new ListTag();
		CompoundTag explosionTag = new CompoundTag();
		explosionTag.putByte("Type", (byte) level.random.nextInt(5));
		explosionTag.putIntArray("Colors", colors);
		explosionTag.putBoolean("Flicker", level.random.nextBoolean());
		explosionTag.putBoolean("Trail", true);
		explosions.add(explosionTag);
		tag.put("Explosions", explosions);
		tag.putByte("Flight", (byte) 1);

		FireworkRocketEntity rocket = new FireworkRocketEntity(
			level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack
		);
		level.addFreshEntity(rocket);
	}

	public static void handleLink(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack linker) {
		BlockPos anchor = linker.hasTag() && linker.getTag().contains("AnchorPos") ? BlockPos.of(linker.getTag().getLong("AnchorPos")) : null;

		if (anchor == null) {
			BlockEntity startBe = level.getBlockEntity(pos);
			if (!(startBe instanceof BeatLampBlockEntity || startBe instanceof StageLightBlockEntity
				|| startBe instanceof FountainBlockEntity || startBe instanceof LaserProjectorBlockEntity
				|| startBe instanceof FogGeneratorBlockEntity || startBe instanceof BeatEmitterBlockEntity)) {
				message(player, "message.beatlamp.link.invalid_start");
				return;
			}

			linker.getOrCreateTag().putLong("AnchorPos", pos.asLong());
			message(player, "message.beatlamp.link.anchor");
			return;
		}

		if (anchor.equals(pos)) {
			linker.getOrCreateTag().remove("AnchorPos");
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
					if (lamps.size() >= 256) {
						message(player, "message.beatlamp.link.full", 256);
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
					if (lights.size() >= 256) {
						message(player, "message.beatlamp.link.full", 256);
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
					if (fountains.size() >= 256) {
						message(player, "message.beatlamp.link.full", 256);
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
					if (lasers.size() >= 256) {
						message(player, "message.beatlamp.link.full", 256);
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
					if (fogs.size() >= 256) {
						message(player, "message.beatlamp.link.full", 256);
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
					if (emitters.size() >= 256) {
						message(player, "message.beatlamp.link.full", 256);
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

		linker.getOrCreateTag().remove("AnchorPos");
		if (positions.size() >= 2) {
			message(player, "message.beatlamp.link.success", positions.size());
		} else {
			message(player, "message.beatlamp.link.single");
		}
	}

	public static void handleSourceSelect(ServerPlayer player, BlockPos jukeboxPos, ItemStack controller) {
		controller.getOrCreateTag().putLong("SourcePos", jukeboxPos.asLong());
		message(player, "message.beatlamp.source.selected", jukeboxPos.toShortString());
	}

	public static void bindSource(ServerLevel level, BlockPos targetPos, BlockPos jukeboxPos, ServerPlayer player, ItemStack controller) {
		BlockEntity be = level.getBlockEntity(targetPos);
		if (be instanceof BeatLampBlockEntity) {
			List<BlockPos> members = ((BeatLampBlockEntity) be).getManualGroup().size() >= 2 ? ((BeatLampBlockEntity) be).getManualGroup() : floodFill(level, targetPos);
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof BeatLampBlockEntity memberLamp) {
					memberLamp.setSource(jukeboxPos);
				}
			}
			message(player, "message.beatlamp.source.bound", members.size(), jukeboxPos.toShortString());
		} else if (be instanceof StageLightBlockEntity) {
			List<BlockPos> members = ((StageLightBlockEntity) be).getManualGroup().size() >= 2 ? ((StageLightBlockEntity) be).getManualGroup() : List.of(targetPos);
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof StageLightBlockEntity memberLight) {
					memberLight.setSource(jukeboxPos);
				}
			}
			message(player, "message.beatlamp.source.bound", members.size(), jukeboxPos.toShortString());
		} else if (be instanceof FountainBlockEntity) {
			List<BlockPos> members = ((FountainBlockEntity) be).getManualGroup().size() >= 2 ? ((FountainBlockEntity) be).getManualGroup() : List.of(targetPos);
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof FountainBlockEntity f) {
					f.setSource(jukeboxPos);
				}
			}
			message(player, "message.beatlamp.source.bound", members.size(), jukeboxPos.toShortString());
		} else if (be instanceof LaserProjectorBlockEntity) {
			List<BlockPos> members = ((LaserProjectorBlockEntity) be).getManualGroup().size() >= 2 ? ((LaserProjectorBlockEntity) be).getManualGroup() : List.of(targetPos);
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof LaserProjectorBlockEntity l) {
					l.setSource(jukeboxPos);
				}
			}
			message(player, "message.beatlamp.source.bound", members.size(), jukeboxPos.toShortString());
		} else if (be instanceof FogGeneratorBlockEntity) {
			List<BlockPos> members = ((FogGeneratorBlockEntity) be).getManualGroup().size() >= 2 ? ((FogGeneratorBlockEntity) be).getManualGroup() : List.of(targetPos);
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof FogGeneratorBlockEntity f) {
					f.setSource(jukeboxPos);
				}
			}
			message(player, "message.beatlamp.source.bound", members.size(), jukeboxPos.toShortString());
		} else if (be instanceof BeatEmitterBlockEntity) {
			List<BlockPos> members = ((BeatEmitterBlockEntity) be).getManualGroup().size() >= 2 ? ((BeatEmitterBlockEntity) be).getManualGroup() : List.of(targetPos);
			for (BlockPos member : members) {
				if (level.getBlockEntity(member) instanceof BeatEmitterBlockEntity e) {
					e.setSource(jukeboxPos);
				}
			}
			message(player, "message.beatlamp.source.bound", members.size(), jukeboxPos.toShortString());
		}
	}

	public static void unlinkGroup(Level level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof BeatLampBlockEntity lamp) {
			List<BlockPos> group = new ArrayList<>(lamp.getManualGroup());
			for (BlockPos member : group) {
				if (level.getBlockEntity(member) instanceof BeatLampBlockEntity memberLamp) {
					memberLamp.clearManualGroup();
				}
			}
		}
	}

	public static void unlinkStageLightGroup(Level level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof StageLightBlockEntity light) {
			List<BlockPos> group = new ArrayList<>(light.getManualGroup());
			for (BlockPos member : group) {
				if (level.getBlockEntity(member) instanceof StageLightBlockEntity memberLight) {
					memberLight.clearManualGroup();
				}
			}
		}
	}

	public static void unlinkLaserGroup(Level level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof LaserProjectorBlockEntity laser) {
			List<BlockPos> group = new ArrayList<>(laser.getManualGroup());
			for (BlockPos member : group) {
				if (level.getBlockEntity(member) instanceof LaserProjectorBlockEntity l) {
					l.clearManualGroup();
				}
			}
		}
	}

	public static void unlinkFogGroup(Level level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof FogGeneratorBlockEntity fog) {
			List<BlockPos> group = new ArrayList<>(fog.getManualGroup());
			for (BlockPos member : group) {
				if (level.getBlockEntity(member) instanceof FogGeneratorBlockEntity f) {
					f.clearManualGroup();
				}
			}
		}
	}

	public static void unlinkFountainGroup(Level level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof FountainBlockEntity fountain) {
			List<BlockPos> group = new ArrayList<>(fountain.getManualGroup());
			for (BlockPos member : group) {
				if (level.getBlockEntity(member) instanceof FountainBlockEntity f) {
					f.clearManualGroup();
				}
			}
		}
	}

	public static void unlinkEmitterGroup(Level level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof BeatEmitterBlockEntity emitter) {
			List<BlockPos> group = new ArrayList<>(emitter.getManualGroup());
			for (BlockPos member : group) {
				if (level.getBlockEntity(member) instanceof BeatEmitterBlockEntity e) {
					e.clearManualGroup();
				}
			}
		}
	}

	public static List<BlockPos> floodFill(Level level, BlockPos origin) {
		List<BlockPos> result = new ArrayList<>();
		Set<BlockPos> visited = new HashSet<>();
		Queue<BlockPos> queue = new ArrayDeque<>();

		queue.add(origin);
		visited.add(origin);

		while (!queue.isEmpty() && result.size() < 256) {
			BlockPos current = queue.poll();
			result.add(current);

			for (Direction direction : Direction.values()) {
				BlockPos neighbor = current.relative(direction);
				if (!visited.contains(neighbor) && level.getBlockEntity(neighbor) instanceof BeatLampBlockEntity) {
					visited.add(neighbor);
					queue.add(neighbor);
				}
			}
		}
		return result;
	}

	private static void message(ServerPlayer player, String key, Object... args) {
		player.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.AQUA), true);
	}
}
