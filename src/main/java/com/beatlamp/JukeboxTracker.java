package com.beatlamp;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class JukeboxTracker {
	private static final Map<ResourceKey<Level>, Set<BlockPos>> PLAYING = new ConcurrentHashMap<>();
	private static final double RANGE_SQR = 64.0 * 64.0;

	private JukeboxTracker() {
	}

	public static void setPlaying(Level level, BlockPos pos, boolean playing) {
		if (level == null) {
			return;
		}

		Set<BlockPos> positions = PLAYING.computeIfAbsent(level.dimension(), key -> ConcurrentHashMap.newKeySet());

		if (playing) {
			positions.add(pos.immutable());
		} else {
			positions.remove(pos);
		}
	}

	public static boolean isPlayingNear(Level level, BlockPos center) {
		Set<BlockPos> positions = PLAYING.get(level.dimension());

		if (positions == null || positions.isEmpty()) {
			return false;
		}

		Vec3 centerVec = Vec3.atCenterOf(center);

		for (BlockPos pos : positions) {
			if (centerVec.distanceToSqr(Vec3.atCenterOf(pos)) <= RANGE_SQR) {
				return true;
			}
		}

		return false;
	}

	public static boolean isPlayingNear(Level level, BlockPos center, BlockPos source) {
		if (source != null) {
			Set<BlockPos> positions = PLAYING.get(level.dimension());

			if (positions == null || !positions.contains(source)) {
				return false;
			}

			return Vec3.atCenterOf(center).distanceToSqr(Vec3.atCenterOf(source)) <= RANGE_SQR;
		}

		return isPlayingNear(level, center);
	}
}
