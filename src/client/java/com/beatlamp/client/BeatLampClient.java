package com.beatlamp.client;

import java.util.List;

import com.beatlamp.BeatLampBlockEntities;
import com.beatlamp.BeatLampBlocks;
import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.LampMode;
import com.beatlamp.block.LampOrientation;
import com.beatlamp.block.LampParticles;
import com.beatlamp.client.audio.AudioAnalyzer;
import com.beatlamp.client.audio.JukeboxAudioTracker;
import com.beatlamp.client.gui.LampConfigScreen;
import com.beatlamp.client.render.BeatLampRenderer;
import com.beatlamp.client.render.LampOutlineRenderer;
import com.beatlamp.network.EmitterSignalPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BeatLampClient implements ClientModInitializer {
	private static final LampParticles[] MIXED_TYPES = {
		LampParticles.NOTE, LampParticles.END_ROD, LampParticles.FIREWORK, LampParticles.GLOW
	};

	private static final Direction[][] AXES = {
		{Direction.WEST, Direction.EAST},
		{Direction.DOWN, Direction.UP},
		{Direction.NORTH, Direction.SOUTH}
	};

	private static final int TOPOLOGY_REFRESH_TICKS = 10;

	@Override
	public void onInitializeClient() {
		BeatLampClientConfig.load();

		BlockRenderLayerMap.INSTANCE.putBlock(BeatLampBlocks.BEAT_LAMP, RenderType.cutout());
		BlockEntityRenderers.register(BeatLampBlockEntities.BEAT_LAMP, BeatLampRenderer::new);

		BeatLampBlockEntity.clientTicker = BeatLampClient::tickLamp;
		BeatEmitterBlockEntity.clientTicker = BeatLampClient::tickEmitter;
		BeatLampBlockEntity.controllerUser = beatLamp -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.execute(() -> {
				if (minecraft.screen == null && minecraft.player != null) {
					minecraft.setScreen(new LampConfigScreen(beatLamp));
				}
			});
		};

		WorldRenderEvents.AFTER_TRANSLUCENT.register(LampOutlineRenderer::render);

		ClientTickEvents.END_CLIENT_TICK.register(client -> JukeboxAudioTracker.clientTick());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> JukeboxAudioTracker.clear());
	}

	private static void tickLamp(BeatLampBlockEntity beatLamp) {
		Level level = beatLamp.getLevel();
		if (level == null) {
			return;
		}

		BlockPos blockPos = beatLamp.getBlockPos();
		Vec3 center = Vec3.atCenterOf(blockPos);
		BlockPos source = beatLamp.getSource();
		float sensitivity = beatLamp.getSensitivity();
		float speed = beatLamp.getSpeed();

		float target = Mth.clamp(JukeboxAudioTracker.getLevelAt(center, source) * sensitivity, 0.0F, 1.0F);
		float diff = target - beatLamp.smoothLevel;
		beatLamp.smoothLevel += diff * (diff > 0.0F ? 0.5F : 0.15F);
		beatLamp.pulse = beatLamp.smoothLevel;
		beatLamp.beatPulse = JukeboxAudioTracker.getBeatPulseAt(center, source);

		LampMode mode = beatLamp.getMode();

		if (needsTopology(mode) && shouldRefreshTopology(level, blockPos)) {
			updateGroupInfo(level, beatLamp, blockPos);
		}

		float time = JukeboxAudioTracker.getEffectTime() * speed;
		float energy = Mth.clamp(Math.max(beatLamp.pulse, beatLamp.beatPulse * 0.8F), 0.0F, 1.0F);

		switch (mode) {
			case PULSE -> beatLamp.displayColor = resolveColor(beatLamp, blockPos, time, -1.0F, energy);
			case RGB -> {
				float hue = ((time * 3 + (blockPos.getX() + blockPos.getZ()) * 6) % 360) / 360.0F;
				beatLamp.displayColor = java.awt.Color.HSBtoRGB(hue, 0.85F, 1.0F);
			}
			case SPECTRUM -> {
				int band = Math.min(AudioAnalyzer.BAND_COUNT - 1, beatLamp.bandIndex * AudioAnalyzer.BAND_COUNT / Math.max(1, beatLamp.bandCount));
				float bandTarget = Mth.clamp(JukeboxAudioTracker.getBandAt(center, band, source) * sensitivity, 0.0F, 1.0F);
				float bandDiff = bandTarget - beatLamp.spectrumLevel;
				beatLamp.spectrumLevel += bandDiff * (bandDiff > 0.0F ? 0.55F : 0.2F);

				int rows = Math.max(1, beatLamp.columnSize);
				float coverage = beatLamp.spectrumLevel * rows - beatLamp.columnIndex;
				beatLamp.barValue = Mth.clamp(coverage, 0.0F, 1.0F);
				beatLamp.displayColor = resolveColor(beatLamp, blockPos, time, (float) band / (AudioAnalyzer.BAND_COUNT - 1), energy);
			}
			case RIPPLE -> {
				float phase = (time * 0.15F - beatLamp.groupDistance * 0.35F) % 1.0F;
				if (phase < 0.0F) {
					phase += 1.0F;
				}

				float ring = Mth.clamp(1.0F - Math.abs(phase - 0.5F) * 4.0F, 0.0F, 1.0F);
				float value = Mth.clamp(ring * (0.35F + beatLamp.pulse * 0.65F), 0.0F, 1.0F);
				beatLamp.barValue = value;
				beatLamp.displayColor = resolveColor(beatLamp, blockPos, time, beatLamp.groupDistance * 0.15F, energy);
			}
			case WAVE -> {
				float wave = (float) (0.5 + 0.5 * Math.sin(time * 0.25F - beatLamp.groupIndex * 0.7F));
				float value = Mth.clamp(wave * (0.3F + beatLamp.pulse * 0.7F), 0.0F, 1.0F);
				beatLamp.barValue = value;
				beatLamp.displayColor = resolveColor(beatLamp, blockPos, time, (float) beatLamp.groupIndex / Math.max(1, beatLamp.groupSize), energy);
			}
			case SCAN -> {
				float cycle = (time * 0.08F) % 2.0F;
				float position = cycle < 1.0F ? cycle : 2.0F - cycle;
				float lampPos = beatLamp.groupSize > 1 ? (float) beatLamp.groupIndex / (beatLamp.groupSize - 1) : 0.5F;
				float scan = Mth.clamp(1.0F - Math.abs(position - lampPos) * beatLamp.groupSize * 0.5F, 0.0F, 1.0F);
				float value = Mth.clamp(scan * (0.4F + beatLamp.pulse * 0.6F), 0.0F, 1.0F);
				beatLamp.barValue = value;
				beatLamp.displayColor = resolveColor(beatLamp, blockPos, time, position, energy);
			}
		}

		if (beatLamp.beatPulse > 0.35F && beatLamp.getParticles() != LampParticles.OFF) {
			int threshold = (int) Mth.lerp(Mth.clamp(beatLamp.beatPulse, 0.0F, 1.0F), 30.0F, 8.0F);

			if (level.getRandom().nextInt(threshold) == 0) {
				spawnParticle(level, blockPos, beatLamp);
			}
		}
	}

	private static void tickEmitter(BeatEmitterBlockEntity emitter) {
		Level level = emitter.getLevel();
		if (level == null) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || minecraft.player.distanceToSqr(
			emitter.getBlockPos().getX() + 0.5, emitter.getBlockPos().getY() + 0.5, emitter.getBlockPos().getZ() + 0.5
		) >= 4096.0) {
			return;
		}

		BlockPos blockPos = emitter.getBlockPos();
		Vec3 center = Vec3.atCenterOf(blockPos);
		BlockPos source = emitter.getSource();

		float audioLevel = JukeboxAudioTracker.getLevelAt(center, source);
		float beat = JukeboxAudioTracker.getBeatPulseAt(center, source);
		float energy = Mth.clamp(Math.max(audioLevel, beat * 0.8F), 0.0F, 1.0F);
		int signal = Math.round(energy * 15.0F);
		long gameTime = level.getGameTime();

		if (emitter.shouldSendSignal(gameTime, signal)) {
			ClientPlayNetworking.send(new EmitterSignalPayload(blockPos, signal));
			emitter.markSent(gameTime, signal);
		}
	}

	private static void spawnParticle(Level level, BlockPos blockPos, BeatLampBlockEntity beatLamp) {
		RandomSource random = level.getRandom();
		LampParticles type = beatLamp.getParticles();

		if (type == LampParticles.MIXED) {
			type = MIXED_TYPES[random.nextInt(MIXED_TYPES.length)];
		}

		Direction face = randomExposedFace(level, blockPos, random);
		double u = random.nextDouble();
		double v = random.nextDouble();
		double x = blockPos.getX() + 0.5;
		double y = blockPos.getY() + 0.5;
		double z = blockPos.getZ() + 0.5;

		switch (face) {
			case WEST -> { x = blockPos.getX() - 0.06; y += u - 0.5; z += v - 0.5; }
			case EAST -> { x = blockPos.getX() + 1.06; y += u - 0.5; z += v - 0.5; }
			case DOWN -> { y = blockPos.getY() - 0.06; x += u - 0.5; z += v - 0.5; }
			case UP -> { y = blockPos.getY() + 1.06; x += u - 0.5; z += v - 0.5; }
			case NORTH -> { z = blockPos.getZ() - 0.06; x += u - 0.5; y += v - 0.5; }
			case SOUTH -> { z = blockPos.getZ() + 1.06; x += u - 0.5; y += v - 0.5; }
		}

		double speed = 0.02 + random.nextDouble() * 0.04;
		double vx = face.getStepX() * speed + (random.nextDouble() - 0.5) * 0.015;
		double vy = face.getStepY() * speed + (random.nextDouble() - 0.5) * 0.015;
		double vz = face.getStepZ() * speed + (random.nextDouble() - 0.5) * 0.015;

		switch (type) {
			case NOTE -> {
				int display = beatLamp.displayColor;

				if (display == 0) {
					level.addParticle(ParticleTypes.NOTE, x, y, z, 0.5, 0.0, 0.0);
				} else {
					float[] hsb = java.awt.Color.RGBtoHSB((display >> 16) & 0xFF, (display >> 8) & 0xFF, display & 0xFF, null);
					level.addParticle(ParticleTypes.NOTE, x, y, z, hsb[0], 0.0, 0.0);
				}
			}
			case END_ROD -> level.addParticle(ParticleTypes.END_ROD, x, y, z, vx, vy + 0.01, vz);
			case FIREWORK -> level.addParticle(ParticleTypes.FIREWORK, x, y, z, vx, vy + 0.02, vz);
			case GLOW -> level.addParticle(ParticleTypes.GLOW, x, y, z, vx, vy + 0.01, vz);
			default -> {
			}
		}
	}

	private static Direction randomExposedFace(Level level, BlockPos blockPos, RandomSource random) {
		int exposed = 0;
		Direction best = Direction.UP;

		for (Direction direction : Direction.values()) {
			if (!level.getBlockState(blockPos.relative(direction)).is(BeatLampBlocks.BEAT_LAMP)) {
				exposed++;
				if (random.nextInt(exposed) == 0) {
					best = direction;
				}
			}
		}

		return best;
	}

	private static int resolveColor(BeatLampBlockEntity beatLamp, BlockPos blockPos, float time, float hueOffset, float energy) {
		if (energy < 0.03F) {
			return 0;
		}

		int color = beatLamp.getColor();

		if (color == BeatLampBlockEntity.COLOR_OLED) {
			float hue = ((time * 2 + (blockPos.getX() + blockPos.getZ()) * 4) % 360) / 360.0F;

			if (hueOffset >= 0.0F) {
				hue = (hue + hueOffset) % 1.0F;
			}

			float value = Math.min(1.0F, 0.35F + energy * 0.9F);
			return java.awt.Color.HSBtoRGB(hue, 0.9F, value);
		}

		float[] hsb = java.awt.Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
		float hue = hsb[0];

		if (hueOffset >= 0.0F) {
			hue = (hsb[0] + hueOffset) % 1.0F;
		}

		float value = hsb[2] * Mth.clamp(energy * 1.25F, 0.0F, 1.0F);
		return java.awt.Color.HSBtoRGB(hue, hsb[1], value);
	}

	private static Direction axisNegative(Direction.Axis axis) {
		return switch (axis) {
			case X -> Direction.WEST;
			case Y -> Direction.DOWN;
			default -> Direction.NORTH;
		};
	}

	private static boolean needsTopology(LampMode mode) {
		return mode == LampMode.SPECTRUM || mode == LampMode.RIPPLE || mode == LampMode.WAVE || mode == LampMode.SCAN;
	}

	private static boolean shouldRefreshTopology(Level level, BlockPos blockPos) {
		long stagger = (long) blockPos.getX() * 31L + (long) blockPos.getY() * 17L + (long) blockPos.getZ() * 13L;
		return Math.floorMod(level.getGameTime() + stagger, TOPOLOGY_REFRESH_TICKS) == 0L;
	}

	private static void updateGroupInfo(Level level, BeatLampBlockEntity beatLamp, BlockPos blockPos) {
		List<BlockPos> manualGroup = beatLamp.getManualGroup();

		if (manualGroup.size() >= 2) {
			applyManualGroup(beatLamp, blockPos, manualGroup);
			return;
		}

		Direction bestAxis;
		int bestIndex;
		int bestLength;
		Direction.Axis forced = beatLamp.getOrientation().getAxis();

		if (forced != null) {
			bestAxis = axisNegative(forced);
			bestIndex = countRun(level, blockPos, bestAxis);
			bestLength = bestIndex + 1 + countRun(level, blockPos, bestAxis.getOpposite());
		} else {
			bestIndex = 0;
			bestLength = 1;
			bestAxis = Direction.WEST;

			for (Direction[] axis : AXES) {
				int behind = countRun(level, blockPos, axis[0]);
				int ahead = countRun(level, blockPos, axis[1]);
				int length = behind + 1 + ahead;

				if (length > bestLength) {
					bestLength = length;
					bestIndex = behind;
					bestAxis = axis[0];
				}
			}
		}

		beatLamp.groupIndex = bestIndex;
		beatLamp.groupSize = bestLength;

		if (bestLength > 1) {
			BlockPos start = blockPos.relative(bestAxis, bestIndex);
			double centerX = 0.0;
			double centerY = 0.0;
			double centerZ = 0.0;
			BlockPos cursor = start;

			for (int i = 0; i < bestLength; i++) {
				centerX += cursor.getX();
				centerY += cursor.getY();
				centerZ += cursor.getZ();
				cursor = cursor.relative(bestAxis.getOpposite());
			}

			centerX /= bestLength;
			centerY /= bestLength;
			centerZ /= bestLength;
			beatLamp.groupDistance = (float) Math.sqrt(
				Math.pow(blockPos.getX() - centerX, 2) + Math.pow(blockPos.getY() - centerY, 2) + Math.pow(blockPos.getZ() - centerZ, 2)
			);
		} else {
			beatLamp.groupDistance = 0.0F;
		}

		updateBandAndColumnInfo(level, beatLamp, blockPos);
	}

	private static int compareByAxis(BlockPos a, BlockPos b, Direction.Axis axis) {
		int c = Integer.compare(axis.choose(a.getX(), a.getY(), a.getZ()), axis.choose(b.getX(), b.getY(), b.getZ()));
		if (c != 0) {
			return c;
		}

		c = Integer.compare(a.getY(), b.getY());
		if (c != 0) {
			return c;
		}

		c = Integer.compare(a.getX(), b.getX());
		if (c != 0) {
			return c;
		}

		return Integer.compare(a.getZ(), b.getZ());
	}

	private static void applyManualGroup(BeatLampBlockEntity beatLamp, BlockPos blockPos, List<BlockPos> members) {
		double centerX = 0.0;
		double centerY = 0.0;
		double centerZ = 0.0;

		for (BlockPos member : members) {
			centerX += member.getX();
			centerY += member.getY();
			centerZ += member.getZ();
		}

		centerX /= members.size();
		centerY /= members.size();
		centerZ /= members.size();

		Direction.Axis forced = beatLamp.getOrientation().getAxis();
		int index = 0;

		if (forced != null) {
			for (BlockPos member : members) {
				if (!member.equals(blockPos) && compareByAxis(member, blockPos, forced) < 0) {
					index++;
				}
			}
		} else {
			for (int i = 0; i < members.size(); i++) {
				if (members.get(i).equals(blockPos)) {
					index = i;
					break;
				}
			}
		}

		beatLamp.groupIndex = index;
		beatLamp.groupSize = members.size();
		beatLamp.groupDistance = (float) Math.sqrt(
			Math.pow(blockPos.getX() - centerX, 2) + Math.pow(blockPos.getY() - centerY, 2) + Math.pow(blockPos.getZ() - centerZ, 2)
		);
		beatLamp.bandIndex = index;
		beatLamp.bandCount = members.size();

		Level level = beatLamp.getLevel();

		if (level != null && forced != Direction.Axis.Y) {
			updateColumnInfo(level, beatLamp, blockPos);
		} else {
			beatLamp.columnIndex = 0;
			beatLamp.columnSize = 1;
		}
	}

	private static void updateBandAndColumnInfo(Level level, BeatLampBlockEntity beatLamp, BlockPos blockPos) {
		int below = countRun(level, blockPos, Direction.DOWN);
		int above = countRun(level, blockPos, Direction.UP);

		beatLamp.columnIndex = below;
		beatLamp.columnSize = below + 1 + above;

		BlockPos base = blockPos.relative(Direction.DOWN, below);
		Direction.Axis forced = beatLamp.getOrientation().getAxis();

		if (forced == null) {
			int westEast = countRun(level, base, Direction.WEST) + 1 + countRun(level, base, Direction.EAST);
			int northSouth = countRun(level, base, Direction.NORTH) + 1 + countRun(level, base, Direction.SOUTH);

			if (westEast >= northSouth && westEast > 1) {
				beatLamp.bandIndex = countRun(level, base, Direction.WEST);
				beatLamp.bandCount = westEast;
			} else if (northSouth > 1) {
				beatLamp.bandIndex = countRun(level, base, Direction.NORTH);
				beatLamp.bandCount = northSouth;
			} else {
				beatLamp.bandIndex = 0;
				beatLamp.bandCount = 1;
			}
		} else {
			Direction negative = axisNegative(forced);
			beatLamp.bandIndex = countRun(level, base, negative);
			beatLamp.bandCount = beatLamp.bandIndex + 1 + countRun(level, base, negative.getOpposite());

			if (forced == Direction.Axis.Y) {
				beatLamp.columnIndex = 0;
				beatLamp.columnSize = 1;
			}
		}
	}

	private static void updateColumnInfo(Level level, BeatLampBlockEntity beatLamp, BlockPos blockPos) {
		int below = countRun(level, blockPos, Direction.DOWN);
		int above = countRun(level, blockPos, Direction.UP);

		beatLamp.columnIndex = below;
		beatLamp.columnSize = below + 1 + above;
	}

	private static int countRun(Level level, BlockPos blockPos, Direction direction) {
		int count = 0;
		BlockPos current = blockPos.relative(direction);

		while (count < 64 && level.getBlockState(current).is(BeatLampBlocks.BEAT_LAMP)) {
			count++;
			current = current.relative(direction);
		}

		return count;
	}
}
