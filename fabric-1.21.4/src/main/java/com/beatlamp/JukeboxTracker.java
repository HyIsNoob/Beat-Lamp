package com.beatlamp;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.beatlamp.config.BeatLampConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class JukeboxTracker {
	private static final Map<ResourceKey<Level>, Set<BlockPos>> PLAYING = new ConcurrentHashMap<>();

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
		double rangeSqr = (double) BeatLampConfig.maxAudioRadius * (double) BeatLampConfig.maxAudioRadius;

		for (BlockPos pos : positions) {
			if (centerVec.distanceToSqr(Vec3.atCenterOf(pos)) <= rangeSqr) {
				return true;
			}
		}

		return false;
	}

	public static boolean isPlayingNear(Level level, BlockPos center, BlockPos source) {
		if (source != null) {
			double rangeSqr = (double) BeatLampConfig.maxAudioRadius * (double) BeatLampConfig.maxAudioRadius;
			if (Vec3.atCenterOf(center).distanceToSqr(Vec3.atCenterOf(source)) > rangeSqr) {
				return false;
			}

			Set<BlockPos> positions = PLAYING.get(level.dimension());
			if (positions != null && positions.contains(source)) {
				return true;
			}

			net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(source);
			if (be != null) {
				try {
					java.lang.reflect.Field dataField = be.getClass().getField("data");
					Object data = dataField.get(be);
					if (data != null) {
						java.lang.reflect.Field activeField = data.getClass().getField("active");
						java.lang.reflect.Field pausedField = data.getClass().getField("paused");
						java.lang.reflect.Field mutedField = data.getClass().getField("muted");
						java.lang.reflect.Field volField = data.getClass().getField("volume");
						boolean active = activeField.getBoolean(data);
						boolean paused = pausedField.getBoolean(data);
						boolean muted = mutedField.getBoolean(data);
						int vol = volField.getInt(data);
						if (active && !paused && !muted && vol > 0) {
							return true;
						}
					}
				} catch (Throwable ignored) {
				}
				net.minecraft.world.level.block.state.BlockState state = be.getBlockState();
				if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HAS_RECORD)
					&& state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HAS_RECORD)) {
					return true;
				}
			}

			return false;
		}

		return isPlayingNear(level, center);
	}
}
