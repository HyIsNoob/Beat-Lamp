package com.beatlamp.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.beatlamp.block.DmxConsoleBlockEntity;

import net.minecraft.core.BlockPos;

public class DmxMasterTracker {
	private static final Map<BlockPos, DmxConsoleBlockEntity> ACTIVE_CONSOLES = new ConcurrentHashMap<>();

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

	private static double getRangeSqr() {
		double r = 64.0;
		return r * r;
	}

	public static boolean isBlackoutNear(BlockPos pos) {
		if (ACTIVE_CONSOLES.isEmpty()) {
			return false;
		}

		double rangeSqr = getRangeSqr();
		for (DmxConsoleBlockEntity console : ACTIVE_CONSOLES.values()) {
			if (console.isRemoved() || console.getLevel() == null) {
				continue;
			}

			BlockPos cPos = console.getBlockPos();
			long dx = pos.getX() - cPos.getX();
			long dy = pos.getY() - cPos.getY();
			long dz = pos.getZ() - cPos.getZ();

			if (dx * dx + dy * dy + dz * dz <= rangeSqr) {
				if (console.isBlackout()) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isStrobeAllNear(BlockPos pos) {
		if (ACTIVE_CONSOLES.isEmpty()) {
			return false;
		}

		double rangeSqr = getRangeSqr();
		for (DmxConsoleBlockEntity console : ACTIVE_CONSOLES.values()) {
			if (console.isRemoved() || console.getLevel() == null) {
				continue;
			}

			BlockPos cPos = console.getBlockPos();
			long dx = pos.getX() - cPos.getX();
			long dy = pos.getY() - cPos.getY();
			long dz = pos.getZ() - cPos.getZ();

			if (dx * dx + dy * dy + dz * dz <= rangeSqr) {
				if (console.isStrobeAll()) {
					return true;
				}
			}
		}
		return false;
	}

	public static float getMasterDimmerNear(BlockPos pos) {
		if (ACTIVE_CONSOLES.isEmpty()) {
			return 1.0F;
		}

		float dimmer = 1.0F;
		double rangeSqr = getRangeSqr();
		for (DmxConsoleBlockEntity console : ACTIVE_CONSOLES.values()) {
			if (console.isRemoved() || console.getLevel() == null) {
				continue;
			}

			BlockPos cPos = console.getBlockPos();
			long dx = pos.getX() - cPos.getX();
			long dy = pos.getY() - cPos.getY();
			long dz = pos.getZ() - cPos.getZ();

			if (dx * dx + dy * dy + dz * dz <= rangeSqr) {
				dimmer = Math.min(dimmer, console.getMasterDimmer());
			}
		}
		return dimmer;
	}

	public static float getMasterSpeedNear(BlockPos pos) {
		if (ACTIVE_CONSOLES.isEmpty()) {
			return 1.0F;
		}

		double rangeSqr = getRangeSqr();
		for (DmxConsoleBlockEntity console : ACTIVE_CONSOLES.values()) {
			if (console.isRemoved() || console.getLevel() == null) {
				continue;
			}

			BlockPos cPos = console.getBlockPos();
			long dx = pos.getX() - cPos.getX();
			long dy = pos.getY() - cPos.getY();
			long dz = pos.getZ() - cPos.getZ();

			if (dx * dx + dy * dy + dz * dz <= rangeSqr) {
				return console.getMasterSpeed();
			}
		}
		return 1.0F;
	}
}
