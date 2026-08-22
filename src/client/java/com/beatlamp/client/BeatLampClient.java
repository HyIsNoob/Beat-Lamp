package com.beatlamp.client;

import java.util.List;

import com.beatlamp.BeatLampBlockEntities;
import com.beatlamp.BeatLampBlocks;
import com.beatlamp.BeatLampItems;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.LampMode;
import com.beatlamp.block.LampParticles;
import com.beatlamp.client.audio.AudioAnalyzer;
import com.beatlamp.client.audio.JukeboxAudioTracker;
import com.beatlamp.client.gui.LampConfigScreen;
import com.beatlamp.client.render.BeatLampRenderer;
import com.beatlamp.client.render.LampOutlineRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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

	@Override
	public void onInitializeClient() {
		BlockRenderLayerMap.INSTANCE.putBlock(BeatLampBlocks.BEAT_LAMP, RenderType.cutout());
		BlockEntityRenderers.register(BeatLampBlockEntities.BEAT_LAMP, BeatLampRenderer::new);

		BeatLampBlockEntity.clientTicker = BeatLampClient::tickLamp;
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
		float sensitivity = beatLamp.getSensitivity();
		float speed = beatLamp.getSpeed();

		float target = Mth.clamp(JukeboxAudioTracker.getLevelAt(center) * sensitivity, 0.0F, 1.0F);
		float diff = target - beatLamp.smoothLevel;
		beatLamp.smoothLevel += diff * (diff > 0.0F ? 0.5F : 0.15F);
		beatLamp.pulse = beatLamp.smoothLevel;
		beatLamp.beatPulse = JukeboxAudioTracker.getBeatPulseAt(center);

		updateGroupInfo(level, beatLamp, blockPos);

		LampMode mode = beatLamp.getMode();
		float time = level.getGameTime() * speed;

		switch (mode) {
			case PULSE -> beatLamp.displayColor = resolveColor(beatLamp, blockPos, time);
			case RGB -> {
				float hue = ((time * 3 + (blockPos.getX() + blockPos.getZ()) * 6) % 360) / 360.0F;
				beatLamp.displayColor = java.awt.Color.HSBtoRGB(hue, 0.85F, 1.0F);
			}
			case SPECTRUM -> {
				float[] spectrum = JukeboxAudioTracker.getSpectrumAt(center);
				int band = Math.min(AudioAnalyzer.BAND_COUNT - 1, beatLamp.bandIndex * AudioAnalyzer.BAND_COUNT / Math.max(1, beatLamp.bandCount));
				float bandTarget = Mth.clamp(spectrum[band] * sensitivity, 0.0F, 1.0F);
				float bandDiff = bandTarget - beatLamp.spectrumLevel;
				beatLamp.spectrumLevel += bandDiff * (bandDiff > 0.0F ? 0.55F : 0.2F);

				int rows = Math.max(1, beatLamp.columnSize);
				float coverage = beatLamp.spectrumLevel * rows - beatLamp.columnIndex;
				beatLamp.barValue = Mth.clamp(coverage, 0.0F, 1.0F);
				beatLamp.displayColor = resolveColor(beatLamp, blockPos, time, (float) band / (AudioAnalyzer.BAND_COUNT - 1));
			}
			case RIPPLE -> {
				float phase = (time * 0.15F - beatLamp.groupDistance * 0.35F) % 1.0F;
				if (phase < 0.0F) {
					phase += 1.0F;
				}

				float ring = Mth.clamp(1.0F - Math.abs(phase - 0.5F) * 4.0F, 0.0F, 1.0F);
				float value = Mth.clamp(ring * (0.35F + beatLamp.pulse * 0.65F), 0.0F, 1.0F);
				beatLamp.barValue = value;
				beatLamp.displayColor = resolveColor(beatLamp, blockPos, time, beatLamp.groupDistance * 0.15F);
			}
			case WAVE -> {
				float wave = (float) (0.5 + 0.5 * Math.sin(time * 0.25F - beatLamp.groupIndex * 0.7F));
				float value = Mth.clamp(wave * (0.3F + beatLamp.pulse * 0.7F), 0.0F, 1.0F);
				beatLamp.barValue = value;
				beatLamp.displayColor = resolveColor(beatLamp, blockPos, time, (float) beatLamp.groupIndex / Math.max(1, beatLamp.groupSize));
			}
			case SCAN -> {
				float cycle = (time * 0.08F) % 2.0F;
				float position = cycle < 1.0F ? cycle : 2.0F - cycle;
				float lampPos = beatLamp.groupSize > 1 ? (float) beatLamp.groupIndex / (beatLamp.groupSize - 1) : 0.5F;
				float scan = Mth.clamp(1.0F - Math.abs(position - lampPos) * beatLamp.groupSize * 0.5F, 0.0F, 1.0F);
				float value = Mth.clamp(scan * (0.4F + beatLamp.pulse * 0.6F), 0.0F, 1.0F);
				beatLamp.barValue = value;
				beatLamp.displayColor = resolveColor(beatLamp, blockPos, time, position);
			}
		}

		if (beatLamp.beatPulse > 0.85F && beatLamp.getParticles() != LampParticles.OFF && level.getRandom().nextInt(12) == 0) {
			spawnParticle(level, blockPos, beatLamp);
		}
	}

	private static void spawnParticle(Level level, BlockPos blockPos, BeatLampBlockEntity beatLamp) {
		RandomSource random = level.getRandom();
		LampParticles type = beatLamp.getParticles();

		if (type == LampParticles.MIXED) {
			type = MIXED_TYPES[random.nextInt(MIXED_TYPES.length)];
		}

		double x = blockPos.getX() + 0.2 + random.nextDouble() * 0.6;
		double y = blockPos.getY() + 1.05 + random.nextDouble() * 0.2;
		double z = blockPos.getZ() + 0.2 + random.nextDouble() * 0.6;

		switch (type) {
			case NOTE -> {
				float[] hsb = java.awt.Color.RGBtoHSB(
					(beatLamp.displayColor >> 16) & 0xFF, (beatLamp.displayColor >> 8) & 0xFF, beatLamp.displayColor & 0xFF, null
				);
				level.addParticle(ParticleTypes.NOTE, x, y, z, hsb[0], 0.0, 0.0);
			}
			case END_ROD -> level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.03, 0.0);
			case FIREWORK -> level.addParticle(ParticleTypes.FIREWORK, x, y, z, 0.0, 0.05, 0.0);
			case GLOW -> level.addParticle(ParticleTypes.GLOW, x, y, z, 0.0, 0.02, 0.0);
			default -> {
			}
		}
	}

	private static int resolveColor(BeatLampBlockEntity beatLamp, BlockPos blockPos, float time) {
		return resolveColor(beatLamp, blockPos, time, -1.0F);
	}

	private static int resolveColor(BeatLampBlockEntity beatLamp, BlockPos blockPos, float time, float hueOffset) {
		int color = beatLamp.getColor();

		if (color == BeatLampBlockEntity.COLOR_OLED) {
			float hue = ((time * 2 + (blockPos.getX() + blockPos.getZ()) * 4) % 360) / 360.0F;
			if (hueOffset >= 0.0F) {
				hue = (hue + hueOffset) % 1.0F;
			}

			return java.awt.Color.HSBtoRGB(hue, 0.9F, 1.0F);
		}

		if (hueOffset >= 0.0F) {
			float[] hsb = java.awt.Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
			return java.awt.Color.HSBtoRGB((hsb[0] + hueOffset) % 1.0F, hsb[1], hsb[2]);
		}

		return color;
	}

	private static void updateGroupInfo(Level level, BeatLampBlockEntity beatLamp, BlockPos blockPos) {
		List<BlockPos> manualGroup = beatLamp.getManualGroup();

		if (manualGroup.size() >= 2) {
			applyManualGroup(beatLamp, blockPos, manualGroup);
			return;
		}

		int bestIndex = 0;
		int bestLength = 1;
		Direction bestAxis = Direction.WEST;

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

	private static void applyManualGroup(BeatLampBlockEntity beatLamp, BlockPos blockPos, List<BlockPos> members) {
		int index = 0;
		double centerX = 0.0;
		double centerY = 0.0;
		double centerZ = 0.0;

		for (int i = 0; i < members.size(); i++) {
			BlockPos member = members.get(i);

			if (member.equals(blockPos)) {
				index = i;
			}

			centerX += member.getX();
			centerY += member.getY();
			centerZ += member.getZ();
		}

		centerX /= members.size();
		centerY /= members.size();
		centerZ /= members.size();

		beatLamp.groupIndex = index;
		beatLamp.groupSize = members.size();
		beatLamp.groupDistance = (float) Math.sqrt(
			Math.pow(blockPos.getX() - centerX, 2) + Math.pow(blockPos.getY() - centerY, 2) + Math.pow(blockPos.getZ() - centerZ, 2)
		);

		Level level = beatLamp.getLevel();
		if (level != null) {
			beatLamp.bandIndex = index;
			beatLamp.bandCount = members.size();
			updateColumnInfo(level, beatLamp, blockPos);
		}
	}

	private static void updateBandAndColumnInfo(Level level, BeatLampBlockEntity beatLamp, BlockPos blockPos) {
		int below = countRun(level, blockPos, Direction.DOWN);
		int above = countRun(level, blockPos, Direction.UP);

		beatLamp.columnIndex = below;
		beatLamp.columnSize = below + 1 + above;

		BlockPos base = blockPos.relative(Direction.DOWN, below);
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

	private static final Direction[][] AXES = {
		{Direction.WEST, Direction.EAST},
		{Direction.DOWN, Direction.UP},
		{Direction.NORTH, Direction.SOUTH}
	};
}
