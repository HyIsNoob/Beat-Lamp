package com.beatlamp.client.gui;

import com.beatlamp.client.config.BeatLampClientConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class BeatLampClientSettingsScreen extends Screen {
	private Button masterEffectsBtn;
	private Button audioEngineBtn;
	private Button beamQualityBtn;
	private Button laserBeamsBtn;
	private Button particleDensityBtn;
	private Button antiStrobeBtn;

	public BeatLampClientSettingsScreen() {
		super(Component.translatable("screen.beatlamp.client_settings.title"));
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int startY = Math.max(38, this.height / 2 - 75);
		int btnWidth = 150;
		int btnHeight = 20;

		// Row 0: Master Effects (Left) & Audio Engine (Right)
		this.masterEffectsBtn = this.addRenderableWidget(
			Button.builder(this.stageEffectsLabel(), button -> {
				BeatLampClientConfig.enableStageEffects = !BeatLampClientConfig.enableStageEffects;
				BeatLampClientConfig.save();
				button.setMessage(this.stageEffectsLabel());
			}).bounds(centerX - 155, startY, btnWidth, btnHeight).build()
		);

		this.audioEngineBtn = this.addRenderableWidget(
			Button.builder(this.audioEngineLabel(), button -> {
				BeatLampClientConfig.setQualityProfile(BeatLampClientConfig.getQualityProfile().next());
				button.setMessage(this.audioEngineLabel());
			}).bounds(centerX + 5, startY, btnWidth, btnHeight).build()
		);

		// Row 1: Volumetric Beams (Left) & 3D Lasers (Right)
		this.beamQualityBtn = this.addRenderableWidget(
			Button.builder(this.beamQualityLabel(), button -> {
				BeatLampClientConfig.beamQuality = BeatLampClientConfig.beamQuality.next();
				BeatLampClientConfig.save();
				button.setMessage(this.beamQualityLabel());
			}).bounds(centerX - 155, startY + 24, btnWidth, btnHeight).build()
		);

		this.laserBeamsBtn = this.addRenderableWidget(
			Button.builder(this.laserBeamsLabel(), button -> {
				BeatLampClientConfig.enableLaserBeams = !BeatLampClientConfig.enableLaserBeams;
				BeatLampClientConfig.save();
				button.setMessage(this.laserBeamsLabel());
			}).bounds(centerX + 5, startY + 24, btnWidth, btnHeight).build()
		);

		// Row 2: Particle Density (Left) & Anti-Strobe (Right)
		this.particleDensityBtn = this.addRenderableWidget(
			Button.builder(this.particleDensityLabel(), button -> {
				this.cycleParticleDensity();
				button.setMessage(this.particleDensityLabel());
			}).bounds(centerX - 155, startY + 48, btnWidth, btnHeight).build()
		);

		this.antiStrobeBtn = this.addRenderableWidget(
			Button.builder(this.antiStrobeLabel(), button -> {
				BeatLampClientConfig.antiStrobe = !BeatLampClientConfig.antiStrobe;
				BeatLampClientConfig.save();
				button.setMessage(this.antiStrobeLabel());
			}).bounds(centerX + 5, startY + 48, btnWidth, btnHeight).build()
		);

		// Row 3: Effect Render Distance Slider (Full width 310)
		this.addRenderableWidget(new DistanceSlider(centerX - 155, startY + 74, 310, btnHeight));

		// Row 4: Done Button
		this.addRenderableWidget(
			Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(centerX - 100, startY + 108, 200, btnHeight)
				.build()
		);
	}

	private Component stageEffectsLabel() {
		Component state = BeatLampClientConfig.enableStageEffects
			? Component.translatable("screen.beatlamp.toggle.on").withStyle(ChatFormatting.GREEN)
			: Component.translatable("screen.beatlamp.toggle.off").withStyle(ChatFormatting.RED);
		return Component.translatable("screen.beatlamp.client_settings.stage_effects", state);
	}

	private Component audioEngineLabel() {
		String key = switch (BeatLampClientConfig.getQualityProfile()) {
			case STUDIO -> "screen.beatlamp.client_settings.audio_engine.studio";
			case LITE -> "screen.beatlamp.client_settings.audio_engine.lite";
			case OFF -> "screen.beatlamp.client_settings.audio_engine.off";
		};
		ChatFormatting color = switch (BeatLampClientConfig.getQualityProfile()) {
			case STUDIO -> ChatFormatting.AQUA;
			case LITE -> ChatFormatting.YELLOW;
			case OFF -> ChatFormatting.RED;
		};
		return Component.translatable("screen.beatlamp.client_settings.audio_engine", Component.translatable(key).withStyle(color));
	}

	private Component beamQualityLabel() {
		String key = switch (BeatLampClientConfig.beamQuality) {
			case HIGH -> "screen.beatlamp.client_settings.beam_quality.high";
			case MEDIUM -> "screen.beatlamp.client_settings.beam_quality.medium";
			case OFF -> "screen.beatlamp.client_settings.beam_quality.off";
		};
		ChatFormatting color = switch (BeatLampClientConfig.beamQuality) {
			case HIGH -> ChatFormatting.GREEN;
			case MEDIUM -> ChatFormatting.YELLOW;
			case OFF -> ChatFormatting.RED;
		};
		return Component.translatable("screen.beatlamp.client_settings.beam_quality", Component.translatable(key).withStyle(color));
	}

	private Component laserBeamsLabel() {
		Component state = BeatLampClientConfig.enableLaserBeams
			? Component.translatable("screen.beatlamp.toggle.on").withStyle(ChatFormatting.GREEN)
			: Component.translatable("screen.beatlamp.toggle.off").withStyle(ChatFormatting.RED);
		return Component.translatable("screen.beatlamp.client_settings.laser_beams", state);
	}

	private Component particleDensityLabel() {
		float density = BeatLampClientConfig.particleDensityMultiplier;
		String key;
		ChatFormatting color;
		if (density >= 0.9F) {
			key = "screen.beatlamp.client_settings.particle_density.full";
			color = ChatFormatting.GREEN;
		} else if (density >= 0.45F) {
			key = "screen.beatlamp.client_settings.particle_density.half";
			color = ChatFormatting.YELLOW;
		} else if (density >= 0.15F) {
			key = "screen.beatlamp.client_settings.particle_density.quarter";
			color = ChatFormatting.GOLD;
		} else {
			key = "screen.beatlamp.client_settings.particle_density.off";
			color = ChatFormatting.RED;
		}
		return Component.translatable("screen.beatlamp.client_settings.particle_density", Component.translatable(key).withStyle(color));
	}

	private void cycleParticleDensity() {
		float density = BeatLampClientConfig.particleDensityMultiplier;
		if (density >= 0.9F) {
			BeatLampClientConfig.particleDensityMultiplier = 0.5F;
			BeatLampClientConfig.enableFogParticles = true;
		} else if (density >= 0.45F) {
			BeatLampClientConfig.particleDensityMultiplier = 0.25F;
			BeatLampClientConfig.enableFogParticles = true;
		} else if (density >= 0.15F) {
			BeatLampClientConfig.particleDensityMultiplier = 0.0F;
			BeatLampClientConfig.enableFogParticles = false;
		} else {
			BeatLampClientConfig.particleDensityMultiplier = 1.0F;
			BeatLampClientConfig.enableFogParticles = true;
		}
		BeatLampClientConfig.save();
	}

	private Component antiStrobeLabel() {
		Component state = BeatLampClientConfig.antiStrobe
			? Component.translatable("screen.beatlamp.toggle.on").withStyle(ChatFormatting.GREEN)
			: Component.translatable("screen.beatlamp.toggle.off").withStyle(ChatFormatting.GRAY);
		return Component.translatable("screen.beatlamp.client_settings.anti_strobe", state);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		this.renderBackground(graphics, mouseX, mouseY, delta);
		super.render(graphics, mouseX, mouseY, delta);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, Math.max(16, this.height / 2 - 97), 0xFFFFFF);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static final class DistanceSlider extends AbstractSliderButton {
		DistanceSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(), (BeatLampClientConfig.beamRenderDistance - 16.0) / (128.0 - 16.0));
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int dist = (int) Math.round(Mth.lerp(this.value, 16.0, 128.0));
			dist = Math.max(16, (dist / 8) * 8);
			Component val = Component.translatable("screen.beatlamp.client_settings.render_distance.format", dist);
			this.setMessage(Component.translatable("screen.beatlamp.client_settings.render_distance").append(": ").append(val));
		}

		@Override
		protected void applyValue() {
			int dist = (int) Math.round(Mth.lerp(this.value, 16.0, 128.0));
			dist = Math.max(16, (dist / 8) * 8);
			BeatLampClientConfig.beamRenderDistance = dist;
			BeatLampClientConfig.save();
		}
	}
}
