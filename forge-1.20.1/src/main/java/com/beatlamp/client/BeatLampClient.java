package com.beatlamp.client;

import java.util.List;

import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.DmxConsoleBlockEntity;
import com.beatlamp.block.FogDensity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.FountainParticles;
import com.beatlamp.block.LampMode;
import com.beatlamp.block.LampOrientation;
import com.beatlamp.block.LampParticles;
import com.beatlamp.block.LaserMode;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;
import com.beatlamp.block.StageLightMode;
import com.beatlamp.client.audio.AudioAnalyzer;
import com.beatlamp.client.audio.JukeboxAudioTracker;
import com.beatlamp.client.config.BeatLampClientConfig;
import com.beatlamp.client.gui.DmxConsoleScreen;
import com.beatlamp.client.gui.EmitterConfigScreen;
import com.beatlamp.client.gui.FogGeneratorConfigScreen;
import com.beatlamp.client.gui.FountainConfigScreen;
import com.beatlamp.client.gui.LampConfigScreen;
import com.beatlamp.client.gui.LaserProjectorConfigScreen;
import com.beatlamp.client.gui.StageLightConfigScreen;
import com.beatlamp.network.FountainFirePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BeatLampClient {
	private static final LampParticles[] MIXED_TYPES = {
		LampParticles.NOTE, LampParticles.FIREWORK, LampParticles.GLOW, LampParticles.SOUL
	};

	private static final Direction[][] AXES = {
		{Direction.WEST, Direction.EAST},
		{Direction.DOWN, Direction.UP},
		{Direction.NORTH, Direction.SOUTH}
	};

	public static void tickLamp(BeatLampBlockEntity beatLamp) {
		Level level = beatLamp.getLevel();
		if (level == null) {
			return;
		}

		BlockPos blockPos = beatLamp.getBlockPos();
		if (DmxMasterTracker.isBlackoutNear(blockPos)) {
			beatLamp.pulse = 0.0F;
			beatLamp.smoothLevel = 0.0F;
			beatLamp.beatPulse = 0.0F;
			beatLamp.displayColor = 0;
			return;
		}

		if (DmxMasterTracker.isStrobeAllNear(blockPos)) {
			long gt = level.getGameTime();
			boolean flash = (gt % 4) < 2;
			beatLamp.displayColor = flash ? 0xFFFFFFFF : 0x00000000;
			beatLamp.pulse = flash ? 1.0F : 0.0F;
			beatLamp.smoothLevel = beatLamp.pulse;
			return;
		}

		Vec3 center = getGroupCenter(blockPos, beatLamp.getManualGroup());
		BlockPos source = beatLamp.getSource();

		if (!JukeboxAudioTracker.isAnyJukeboxPlayingNear(center, source)) {
			beatLamp.smoothLevel = 0.0F;
			beatLamp.pulse = 0.0F;
			beatLamp.beatPulse = 0.0F;
			beatLamp.barValue = 0.0F;
			beatLamp.spectrumLevel = 0.0F;
			beatLamp.peakLevel = 0.0F;
			beatLamp.displayColor = 0;
			return;
		}

		float sensitivity = beatLamp.getSensitivity();
		float speed = beatLamp.getSpeed() * DmxMasterTracker.getMasterSpeedNear(blockPos);

		LampMode mode = beatLamp.getMode();
		boolean tempoAssist = beatLamp.isTempoPulse();
		float rawLevel = JukeboxAudioTracker.getRawLevelAt(center, source);
		float audioLevel = JukeboxAudioTracker.getLevelAt(center, source);
		float target = Mth.clamp((tempoAssist ? audioLevel : rawLevel) * sensitivity, 0.0F, 1.0F);

		float diff = target - beatLamp.smoothLevel;
		if (tempoAssist) {
			beatLamp.smoothLevel += diff * (diff > 0.0F ? 0.5F : 0.15F);
		} else {
			beatLamp.smoothLevel += diff * (diff > 0.0F ? 0.55F : 0.25F);
		}

		beatLamp.pulse = beatLamp.smoothLevel;
		beatLamp.beatPulse = JukeboxAudioTracker.getBeatPulseAt(center, source);

		if (level.getGameTime() % 10 == 0) {
			resolveTopology(level, blockPos, beatLamp);
		}

		float time = JukeboxAudioTracker.getEffectTime() * speed;
		float energy = Mth.clamp(Math.max(beatLamp.pulse, beatLamp.beatPulse * 0.7F), 0.0F, 1.0F);

		switch (mode) {
			case PULSE -> {
				beatLamp.displayColor = resolveColor(beatLamp, blockPos, time, 0.0F, energy);
			}
			case RGB -> {
				float hue = (time * 1.5F + beatLamp.groupIndex * 12.0F) % 360.0F;
				beatLamp.displayColor = java.awt.Color.HSBtoRGB(hue / 360.0F, 0.9F, 1.0F);
			}
			case SPECTRUM -> {
				int band = Math.min(AudioAnalyzer.BAND_COUNT - 1, (int) (beatLamp.groupDistance * (AudioAnalyzer.BAND_COUNT / Math.max(1.0F, (float) beatLamp.groupSize))));
				float bandVal = JukeboxAudioTracker.getBandAt(center, band, source) * sensitivity;
				beatLamp.barValue = Mth.clamp(bandVal, 0.0F, 1.0F);
				beatLamp.displayColor = resolveColor(beatLamp, blockPos, time, (float) band / AudioAnalyzer.BAND_COUNT, energy);
			}
			case VU_METER -> {
				float normalized = beatLamp.groupSize > 1 ? (float) beatLamp.groupIndex / (beatLamp.groupSize - 1) : 0.0F;
				if (beatLamp.isReverse()) {
					normalized = 1.0F - normalized;
				}
				beatLamp.barValue = beatLamp.pulse >= normalized ? 1.0F : 0.0F;
				beatLamp.displayColor = resolveVuColor(normalized, energy);
			}
			case OSCILLOSCOPE, MATRIX_RAIN, RIPPLE, WAVE, SCAN -> {
				float phase = (time * 0.15F + beatLamp.groupIndex * 0.2F) % 1.0F;
				beatLamp.barValue = Mth.clamp(beatLamp.pulse * (0.5F + 0.5F * Mth.sin(phase * 6.28F)), 0.0F, 1.0F);
				beatLamp.displayColor = resolveColor(beatLamp, blockPos, time, phase, energy);
			}
		}
	}

	public static void tickStageLight(StageLightBlockEntity light) {
		Level level = light.getLevel();
		if (level == null) {
			return;
		}

		BlockPos blockPos = light.getBlockPos();
		if (DmxMasterTracker.isBlackoutNear(blockPos)) {
			light.beamIntensity = 0.0F;
			return;
		}

		if (DmxMasterTracker.isStrobeAllNear(blockPos)) {
			long gt = level.getGameTime();
			light.beamIntensity = ((gt % 4) < 2) ? 1.0F : 0.0F;
			return;
		}

		Vec3 center = getGroupCenter(blockPos, light.getManualGroup());
		BlockPos source = light.getSource();

		if (!JukeboxAudioTracker.isAnyJukeboxPlayingNear(center, source)) {
			light.beamIntensity = 0.0F;
			return;
		}

		float sensitivity = light.getSensitivity();
		boolean tempoAssist = light.isTempoPulse();
		float rawLevel = JukeboxAudioTracker.getRawLevelAt(center, source);
		float audioLevel = JukeboxAudioTracker.getLevelAt(center, source);
		float target = Mth.clamp((tempoAssist ? audioLevel : rawLevel) * sensitivity, 0.0F, 1.0F);

		float diff = target - light.beamIntensity;
		if (tempoAssist) {
			light.beamIntensity += diff * (diff > 0.0F ? 0.5F : 0.15F);
		} else {
			light.beamIntensity += diff * (diff > 0.0F ? 0.55F : 0.25F);
			if (light.beamIntensity < 0.02F) {
				light.beamIntensity = 0.0F;
			}
		}

		float beatPulse = (tempoAssist ? JukeboxAudioTracker.getBeatPulseAt(center, source) : JukeboxAudioTracker.getKickPulseAt(center, source)) * sensitivity;

		if (!light.getManualGroup().isEmpty()) {
			light.groupSize = light.getManualGroup().size();
			light.groupIndex = Math.max(0, light.getManualGroup().indexOf(blockPos));
		} else {
			light.groupSize = 1;
			light.groupIndex = 0;
		}

		if (light.getMode() == StageLightMode.BEAT_STEP) {
			if (beatPulse > 0.60F) {
				RandomSource random = level.getRandom();
				float[] discretePans = {-48.0F, -32.0F, -16.0F, 0.0F, 16.0F, 32.0F, 48.0F};
				float[] discreteTilts = {15.0F, 26.0F, 38.0F, 50.0F};
				light.targetYaw = discretePans[random.nextInt(discretePans.length)];
				light.targetPitch = discreteTilts[random.nextInt(discreteTilts.length)];
			}
			light.currentYaw = Mth.lerp(0.28F, light.currentYaw, light.targetYaw);
			light.currentPitch = Mth.lerp(0.28F, light.currentPitch, light.targetPitch);
		}
	}

	public static void tickLaserProjector(LaserProjectorBlockEntity laser) {
		Level level = laser.getLevel();
		if (level == null) {
			return;
		}

		BlockPos blockPos = laser.getBlockPos();
		if (DmxMasterTracker.isBlackoutNear(blockPos)) {
			laser.beamIntensity = 0.0F;
			return;
		}

		if (DmxMasterTracker.isStrobeAllNear(blockPos)) {
			long gt = level.getGameTime();
			laser.beamIntensity = ((gt % 4) < 2) ? 1.0F : 0.0F;
			return;
		}

		Vec3 center = getGroupCenter(blockPos, laser.getManualGroup());
		BlockPos source = laser.getSource();

		if (!JukeboxAudioTracker.isAnyJukeboxPlayingNear(center, source)) {
			laser.beamIntensity = 0.0F;
			return;
		}

		float audioLevel = JukeboxAudioTracker.getLevelAt(center, source);
		float beatPulse = JukeboxAudioTracker.getBeatPulseAt(center, source);
		laser.beamIntensity = Mth.clamp(Math.max(audioLevel, beatPulse * 0.9F), 0.0F, 1.0F);
	}

	public static void tickFountain(FountainBlockEntity fountain) {
		Level level = fountain.getLevel();
		if (level == null) {
			return;
		}

		BlockPos blockPos = fountain.getBlockPos();
		if (DmxMasterTracker.isBlackoutNear(blockPos)) {
			fountain.fountainEnergy = 0.0F;
			fountain.fountainImpact = 0.0F;
			return;
		}

		Vec3 center = getGroupCenter(blockPos, fountain.getManualGroup());
		BlockPos source = fountain.getSource();

		if (!JukeboxAudioTracker.isAnyJukeboxPlayingNear(center, source)) {
			fountain.fountainEnergy = 0.0F;
			fountain.fountainImpact = 0.0F;
			return;
		}

		float audioLevel = JukeboxAudioTracker.getLevelAt(center, source);
		float beatPulse = JukeboxAudioTracker.getBeatPulseAt(center, source);
		float energy = Mth.clamp(Math.max(audioLevel, beatPulse * 0.85F), 0.0F, 1.0F);
		float impact = JukeboxAudioTracker.getImpactPulseAt(center, source);
		fountain.fountainEnergy = energy;
		fountain.fountainImpact = impact;

		RandomSource random = level.getRandom();
		double originX = blockPos.getX() + 0.5;
		double originY = blockPos.getY() + 1.02;
		double originZ = blockPos.getZ() + 0.5;

		int color = fountain.getColor();
		if (color == BeatLampBlockEntity.COLOR_OLED) {
			float hue = (JukeboxAudioTracker.getEffectTime() * 3.0F % 360.0F) / 360.0F;
			color = java.awt.Color.HSBtoRGB(hue, 0.85F, 1.0F);
		}
		float r = ((color >> 16) & 0xFF) / 255.0F;
		float g = ((color >> 8) & 0xFF) / 255.0F;
		float b = (color & 0xFF) / 255.0F;
		net.minecraft.core.particles.DustParticleOptions dust = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(r, g, b), 1.4F);

		FountainParticles pType = fountain.getParticleType();
		float sprayThreshold = fountain.getSprayThreshold();
		float densityMul = BeatLampClientConfig.particleDensityMultiplier;

		if (energy >= sprayThreshold && densityMul > 0.01F) {
			float activeIntensity = (energy - sprayThreshold) / Math.max(0.001F, 1.0F - sprayThreshold);
			int count = Math.max(1, Math.round((1 + (int) (activeIntensity * 4.0F)) * densityMul));
			for (int i = 0; i < count; i++) {
				double px = originX + (random.nextDouble() - 0.5) * 0.22;
				double pz = originZ + (random.nextDouble() - 0.5) * 0.22;
				double vx = (random.nextDouble() - 0.5) * 0.04;
				double vy = 0.20 + activeIntensity * 0.48 + random.nextDouble() * 0.15;
				double vz = (random.nextDouble() - 0.5) * 0.04;

				spawnFountainParticle(level, pType, px, originY, pz, vx, vy, vz, dust, random);
			}

			if (fountain.isSmokeEnabled() && random.nextInt(4) == 0) {
				level.addParticle(ParticleTypes.SMOKE, originX, originY, originZ, 0.0, 0.06, 0.0);
			}
		}

		if (beatPulse > 0.5F && energy >= sprayThreshold * 0.8F && densityMul > 0.01F) {
			int beatSparks = Math.max(1, Math.round((2 + (int) (beatPulse * 4.0F)) * densityMul));
			for (int i = 0; i < beatSparks; i++) {
				double px = originX + (random.nextDouble() - 0.5) * 0.18;
				double pz = originZ + (random.nextDouble() - 0.5) * 0.18;
				double vx = (random.nextDouble() - 0.5) * 0.08;
				double vy = 0.38 + beatPulse * 0.42 + random.nextDouble() * 0.18;
				double vz = (random.nextDouble() - 0.5) * 0.08;

				level.addParticle(ParticleTypes.FIREWORK, px, originY, pz, vx, vy, vz);
				spawnFountainParticle(level, pType, px, originY, pz, vx * 0.8, vy * 0.9, vz * 0.8, dust, random);
			}
		}

		float dropThreshold = fountain.getImpactThreshold();
		if (dropThreshold < 0.98F && impact >= dropThreshold && densityMul > 0.01F) {
			int burstCount = Math.max(4, Math.round(48 * densityMul));
			for (int i = 0; i < burstCount; i++) {
				double angle = random.nextDouble() * Math.PI * 2.0;
				double spread = 0.08 + random.nextDouble() * 0.28;
				double vx = Math.cos(angle) * spread;
				double vy = 0.35 + random.nextDouble() * 0.55;
				double vz = Math.sin(angle) * spread;

				level.addParticle(ParticleTypes.FIREWORK, originX, originY + 0.1, originZ, vx, vy, vz);
				if (i % 2 == 0) {
					level.addParticle(dust, originX, originY + 0.1, originZ, vx * 0.8, vy * 0.9, vz * 0.8);
				}
				if (i % 3 == 0) {
					level.addParticle(ParticleTypes.GLOW, originX, originY + 0.1, originZ, vx * 0.6, vy * 0.8, vz * 0.6);
				}
			}

			if (fountain.isFireworkMode()) {
				long now = level.getGameTime();
				if (now - fountain.lastFireSend >= 20L) {
					fountain.lastFireSend = now;
					PlatformNetwork.sendToServer(new FountainFirePayload(blockPos));
				}
			}
		}
	}

	public static void tickFogGenerator(FogGeneratorBlockEntity fog) {
		Level level = fog.getLevel();
		if (level == null) {
			return;
		}

		BlockPos blockPos = fog.getBlockPos();
		if (DmxMasterTracker.isBlackoutNear(blockPos)) {
			return;
		}

		Vec3 center = getGroupCenter(blockPos, fog.getManualGroup());
		BlockPos source = fog.getSource();
		float beatPulse = JukeboxAudioTracker.getBeatPulseAt(center, source);

		if (BeatLampClientConfig.particleDensityMultiplier <= 0.01F) {
			return;
		}

		RandomSource random = level.getRandom();
		int baseCount = Math.max(1, Math.round(fog.getDensity().getMultiplier() * BeatLampClientConfig.particleDensityMultiplier));
		int count = baseCount;
		if (beatPulse > 0.55F) {
			count += Math.max(1, Math.round((2 + (int) (beatPulse * 3)) * BeatLampClientConfig.particleDensityMultiplier));
		}

		double originX = blockPos.getX() + 0.5;
		double originY = blockPos.getY() + 0.5;
		double originZ = blockPos.getZ() + 0.5;

		int color = fog.getColor();
		float r = 1.0F, g = 1.0F, b = 1.0F;
		boolean isColored = color != BeatLampBlockEntity.COLOR_OLED;
		if (isColored) {
			r = ((color >> 16) & 0xFF) / 255.0F;
			g = ((color >> 8) & 0xFF) / 255.0F;
			b = (color & 0xFF) / 255.0F;
		}

		double speedMultiplier = 0.08 * (fog.getRadius() / 8.0);

		for (int i = 0; i < count; i++) {
			double px = originX + (random.nextDouble() - 0.5) * 0.35;
			double py = originY + (random.nextDouble() - 0.5) * 0.2;
			double pz = originZ + (random.nextDouble() - 0.5) * 0.35;

			double angle = random.nextDouble() * Math.PI * 2.0;
			double speed = speedMultiplier * (0.35 + random.nextDouble() * 0.65);
			double vx = Math.cos(angle) * speed;
			double vy = 0.008 + random.nextDouble() * 0.015 + (beatPulse > 0.55F ? 0.06 : 0.0);
			double vz = Math.sin(angle) * speed;

			level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, vx, vy, vz);

			if (i % 2 == 0) {
				level.addParticle(ParticleTypes.CLOUD, px, py, pz, vx * 0.7, vy * 0.5, vz * 0.7);
			}

			if (isColored) {
				level.addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(r, g, b), 2.2F), px, py, pz, vx * 0.9, vy, vz * 0.9);
				if (i % 3 == 0) {
					level.addParticle(ParticleTypes.GLOW, px, py, pz, vx * 0.5, vy, vz * 0.5);
				}
			}
		}
	}

	public static void tickEmitter(BeatEmitterBlockEntity emitter) {
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

		if (!JukeboxAudioTracker.isAnyJukeboxPlayingNear(center, source)) {
			int finalSignal = emitter.isInverted() ? 15 : 0;
			emitter.setSignal(finalSignal, level.getGameTime());
			return;
		}

		float audioLevel = JukeboxAudioTracker.getLevelAt(center, source);
		float beatPulse = JukeboxAudioTracker.getBeatPulseAt(center, source);
		float kickPulse = JukeboxAudioTracker.getKickPulseAt(center, source);
		float snarePulse = JukeboxAudioTracker.getSnarePulseAt(center, source);
		float hihatPulse = JukeboxAudioTracker.getHihatPulseAt(center, source);
		float impactPulse = JukeboxAudioTracker.getImpactPulseAt(center, source);

		float threshold = emitter.getThreshold();
		int rawSignal = 0;

		switch (emitter.getMode()) {
			case PULSE -> rawSignal = beatPulse >= threshold ? 15 : 0;
			case DROP -> rawSignal = impactPulse >= threshold ? 15 : 0;
			case ENERGY -> {
				float energy = Mth.clamp(Math.max(audioLevel, beatPulse * 0.85F) * (1.0F / Math.max(0.1F, threshold)), 0.0F, 1.0F);
				rawSignal = Math.round(energy * 15.0F);
			}
			case KICK -> rawSignal = kickPulse >= threshold ? 15 : 0;
			case SNARE -> rawSignal = snarePulse >= threshold ? 15 : 0;
			case HIHAT -> rawSignal = hihatPulse >= threshold ? 15 : 0;
		}

		int finalSignal = emitter.isInverted() ? (15 - rawSignal) : rawSignal;
		long gameTime = level.getGameTime();
		emitter.setSignal(finalSignal, gameTime);
	}

	private static void spawnFountainParticle(Level level, FountainParticles type, double x, double y, double z, double vx, double vy, double vz, net.minecraft.core.particles.DustParticleOptions dust, RandomSource random) {
		switch (type) {
			case FLAME -> level.addParticle(ParticleTypes.FLAME, x, y, z, vx, vy, vz);
			case SOUL_FLAME -> level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, vx, vy, vz);
			case FIREWORK -> level.addParticle(ParticleTypes.FIREWORK, x, y, z, vx, vy, vz);
			case GLOW -> level.addParticle(ParticleTypes.GLOW, x, y, z, vx, vy, vz);
			case SPARK -> level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, vx, vy, vz);
			case DUST -> level.addParticle(dust, x, y, z, vx, vy, vz);
			case MIXED -> {
				int r = random.nextInt(4);
				if (r == 0) level.addParticle(ParticleTypes.FLAME, x, y, z, vx, vy, vz);
				else if (r == 1) level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, vx, vy, vz);
				else if (r == 2) level.addParticle(ParticleTypes.FIREWORK, x, y, z, vx, vy, vz);
				else level.addParticle(dust, x, y, z, vx, vy, vz);
			}
		}
	}

	private static void resolveTopology(Level level, BlockPos pos, BeatLampBlockEntity lamp) {
		List<BlockPos> group = lamp.getManualGroup();
		if (!group.isEmpty()) {
			lamp.groupSize = group.size();
			lamp.groupIndex = Math.max(0, group.indexOf(pos));
			Vec3 start = Vec3.atCenterOf(group.get(0));
			lamp.groupDistance = (float) start.distanceTo(Vec3.atCenterOf(pos));
		} else {
			lamp.groupSize = 1;
			lamp.groupIndex = 0;
			lamp.groupDistance = 0.0F;
		}
	}

	private static Vec3 getGroupCenter(BlockPos pos, List<BlockPos> group) {
		if (group.isEmpty()) {
			return Vec3.atCenterOf(pos);
		}
		double sumX = 0, sumY = 0, sumZ = 0;
		for (BlockPos p : group) {
			sumX += p.getX() + 0.5;
			sumY += p.getY() + 0.5;
			sumZ += p.getZ() + 0.5;
		}
		return new Vec3(sumX / group.size(), sumY / group.size(), sumZ / group.size());
	}

	private static int resolveColor(BeatLampBlockEntity lamp, BlockPos pos, float time, float offset, float energy) {
		int color = lamp.getColor();
		if (color == BeatLampBlockEntity.COLOR_OLED) {
			float hue = (time * 1.5F + offset * 360.0F) % 360.0F;
			return java.awt.Color.HSBtoRGB(hue / 360.0F, 0.9F, 1.0F);
		}
		return color;
	}

	private static int resolveVuColor(float normalized, float energy) {
		if (normalized < 0.6F) {
			return 0x00FF00;
		} else if (normalized < 0.85F) {
			return 0xFFFF00;
		} else {
			return 0xFF0000;
		}
	}

	public static void init() {
		BeatLampBlockEntity.clientTicker = BeatLampClient::tickLamp;
		BeatLampBlockEntity.controllerUser = lamp -> Minecraft.getInstance().setScreen(new LampConfigScreen(lamp));

		StageLightBlockEntity.clientTicker = BeatLampClient::tickStageLight;
		StageLightBlockEntity.controllerUser = light -> Minecraft.getInstance().setScreen(new StageLightConfigScreen(light));

		LaserProjectorBlockEntity.clientTicker = BeatLampClient::tickLaserProjector;
		LaserProjectorBlockEntity.controllerUser = laser -> Minecraft.getInstance().setScreen(new LaserProjectorConfigScreen(laser));

		FountainBlockEntity.clientTicker = BeatLampClient::tickFountain;
		FountainBlockEntity.controllerUser = fountain -> Minecraft.getInstance().setScreen(new FountainConfigScreen(fountain));

		FogGeneratorBlockEntity.clientTicker = BeatLampClient::tickFogGenerator;
		FogGeneratorBlockEntity.controllerUser = fog -> Minecraft.getInstance().setScreen(new FogGeneratorConfigScreen(fog));

		BeatEmitterBlockEntity.clientTicker = BeatLampClient::tickEmitter;
		BeatEmitterBlockEntity.controllerUser = emitter -> Minecraft.getInstance().setScreen(new EmitterConfigScreen(emitter));

		DmxConsoleBlockEntity.controllerUser = dmx -> Minecraft.getInstance().setScreen(new DmxConsoleScreen(dmx));
	}
}
