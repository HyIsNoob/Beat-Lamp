package com.beatlamp.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.beatlamp.block.DmxConsoleBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class DmxMasterTracker {
	private static final Map<BlockPos, DmxConsoleBlockEntity> ACTIVE_CONSOLES = new ConcurrentHashMap<>();
	public static final double DMX_RANGE = 64.0;

	public static void register(DmxConsoleBlockEntity console) {
		if (console.getBlockPos() != null) {
			ACTIVE_CONSOLES.put(console.getBlockPos(), console);
		}
	}

	public static void unregister(BlockPos pos) {
		ACTIVE_CONSOLES.remove(pos);
	}

	public static void clear() {
		ACTIVE_CONSOLES.clear();
	}

	public static boolean isBlackoutNear(BlockPos pos) {
		Vec3 center = Vec3.atCenterOf(pos);
		for (DmxConsoleBlockEntity console : ACTIVE_CONSOLES.values()) {
			if (console.isRemoved() || console.getLevel() == null) {
				continue;
			}
			if (Vec3.atCenterOf(console.getBlockPos()).distanceTo(center) <= DMX_RANGE) {
				if (console.isBlackout()) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isStrobeAllNear(BlockPos pos) {
		Vec3 center = Vec3.atCenterOf(pos);
		for (DmxConsoleBlockEntity console : ACTIVE_CONSOLES.values()) {
			if (console.isRemoved() || console.getLevel() == null) {
				continue;
			}
			if (Vec3.atCenterOf(console.getBlockPos()).distanceTo(center) <= DMX_RANGE) {
				if (console.isStrobeAll()) {
					return true;
				}
			}
		}
		return false;
	}

	public static float getMasterDimmerNear(BlockPos pos) {
		Vec3 center = Vec3.atCenterOf(pos);
		float dimmer = 1.0F;
		for (DmxConsoleBlockEntity console : ACTIVE_CONSOLES.values()) {
			if (console.isRemoved() || console.getLevel() == null) {
				continue;
			}
			if (Vec3.atCenterOf(console.getBlockPos()).distanceTo(center) <= DMX_RANGE) {
				if (console.isBlackout()) {
					return 0.0F;
				}
				dimmer = Math.min(dimmer, console.getMasterDimmer());
			}
		}
		return dimmer;
	}

	public static float getMasterSpeedNear(BlockPos pos) {
		Vec3 center = Vec3.atCenterOf(pos);
		for (DmxConsoleBlockEntity console : ACTIVE_CONSOLES.values()) {
			if (console.isRemoved() || console.getLevel() == null) {
				continue;
			}
			if (Vec3.atCenterOf(console.getBlockPos()).distanceTo(center) <= DMX_RANGE) {
				return console.getMasterSpeed();
			}
		}
		return 1.0F;
	}
}
