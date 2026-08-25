package com.beatlamp.client.gui;

import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.LampMode;
import com.beatlamp.block.LampOrientation;
import com.beatlamp.block.LampParticles;
import com.beatlamp.client.BeatLampClientConfig;
import com.beatlamp.network.LampConfigurePayload;
import com.beatlamp.network.LampSourcePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

public class LampConfigScreen extends Screen {
	private static final int[] PALETTE = buildPalette();

	private final BlockPos pos;
	private LampMode mode;
	private float sensitivity;
	private float speed;
	private int color;
	private boolean frameless;
	private boolean blackback;
	private boolean idleLight;
	private LampParticles particles;
	private LampOrientation orientation;
	private boolean unlink;

	private Button modeButton;
	private Button colorButton;
	private Button framelessButton;
	private Button blackbackButton;
	private Button idleLightButton;
	private Button sourceButton;
	private Button particlesButton;
	private Button orientationButton;
	private ValueSlider sensitivitySlider;
	private ValueSlider speedSlider;
	private BlockPos sourcePos;

	public LampConfigScreen(BeatLampBlockEntity beatLamp) {
		super(Component.translatable("screen.beatlamp.config"));
		this.pos = beatLamp.getBlockPos().immutable();
		this.mode = beatLamp.getMode();
		this.sensitivity = beatLamp.getSensitivity();
		this.speed = beatLamp.getSpeed();
		this.color = beatLamp.getColor();
		this.frameless = beatLamp.isFrameless();
		this.blackback = beatLamp.isBlackback();
		this.idleLight = beatLamp.isIdleLight();
		this.particles = beatLamp.getParticles();
		this.orientation = beatLamp.getOrientation();
		this.sourcePos = beatLamp.getSource() == null ? null : beatLamp.getSource().immutable();
	}

	private static int[] buildPalette() {
		DyeColor[] dyes = DyeColor.values();
		int[] palette = new int[dyes.length + 1];
		palette[0] = BeatLampBlockEntity.COLOR_OLED;

		for (int i = 0; i < dyes.length; i++) {
			palette[i + 1] = dyes[i].getFireworkColor();
		}

		return palette;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int y = this.height / 2 - 100;

		this.modeButton = this.addRenderableWidget(
			Button.builder(this.modeLabel(), button -> {
				this.mode = this.mode.next();
				button.setMessage(this.modeLabel());
			}).bounds(centerX - 100, y, 200, 20).build()
		);

		this.sensitivitySlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 24, Component.translatable("screen.beatlamp.sensitivity"), this.sensitivity, 0.25, 3.0) {
				@Override
				protected void applyValue() {
					LampConfigScreen.this.sensitivity = (float) Mth.lerp(this.value, 0.25, 3.0);
				}
			}
		);

		this.speedSlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 48, Component.translatable("screen.beatlamp.speed"), this.speed, 0.25, 3.0) {
				@Override
				protected void applyValue() {
					LampConfigScreen.this.speed = (float) Mth.lerp(this.value, 0.25, 3.0);
				}
			}
		);

		this.colorButton = this.addRenderableWidget(
			Button.builder(this.colorLabel(), button -> {
				int index = this.colorIndex();
				this.color = PALETTE[(index + 1) % PALETTE.length];
				button.setMessage(this.colorLabel());
			}).bounds(centerX - 100, y + 72, 200, 20).build()
		);

		this.framelessButton = this.addRenderableWidget(
			Button.builder(this.framelessLabel(), button -> {
				this.frameless = !this.frameless;
				button.setMessage(this.framelessLabel());
			}).bounds(centerX - 100, y + 96, 98, 20).build()
		);

		this.blackbackButton = this.addRenderableWidget(
			Button.builder(this.blackbackLabel(), button -> {
				this.blackback = !this.blackback;
				button.setMessage(this.blackbackLabel());
			}).bounds(centerX + 2, y + 96, 98, 20).build()
		);

		this.orientationButton = this.addRenderableWidget(
			Button.builder(this.orientationLabel(), button -> {
				this.orientation = this.orientation.next();
				button.setMessage(this.orientationLabel());
			}).bounds(centerX - 100, y + 120, 98, 20).build()
		);

		this.particlesButton = this.addRenderableWidget(
			Button.builder(this.particlesLabel(), button -> {
				this.particles = this.particles.next();
				button.setMessage(this.particlesLabel());
			}).bounds(centerX + 2, y + 120, 98, 20).build()
		);

		this.idleLightButton = this.addRenderableWidget(
			Button.builder(this.idleLightLabel(), button -> {
				this.idleLight = !this.idleLight;
				button.setMessage(this.idleLightLabel());
			}).bounds(centerX - 100, y + 144, 98, 20).build()
		);

		this.sourceButton = this.addRenderableWidget(
			Button.builder(this.sourceLabel(), button -> {
				if (this.sourcePos != null) {
					ClientPlayNetworking.send(new LampSourcePayload(this.pos));
					this.sourcePos = null;
					button.setMessage(this.sourceLabel());
				}
			}).bounds(centerX + 2, y + 144, 98, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("screen.beatlamp.beatquality").append(": ").append(Component.translatable(BeatLampClientConfig.highQualityBeat ? "screen.beatlamp.beatquality.high" : "screen.beatlamp.beatquality.low")), button -> {
				BeatLampClientConfig.highQualityBeat = !BeatLampClientConfig.highQualityBeat;
				BeatLampClientConfig.save();
				button.setMessage(Component.translatable("screen.beatlamp.beatquality").append(": ").append(Component.translatable(BeatLampClientConfig.highQualityBeat ? "screen.beatlamp.beatquality.high" : "screen.beatlamp.beatquality.low")));
			}).bounds(centerX - 100, y + 168, 98, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("screen.beatlamp.reset"), button -> this.resetToDefault())
				.bounds(centerX + 2, y + 168, 98, 20)
				.build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("screen.beatlamp.unlink"), button -> {
				this.unlink = true;
				this.onClose();
			}).bounds(centerX - 100, y + 192, 98, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(centerX + 2, y + 192, 98, 20)
				.build()
		);
	}

	private void resetToDefault() {
		this.mode = LampMode.PULSE;
		this.sensitivity = 1.0F;
		this.speed = 1.0F;
		this.color = BeatLampBlockEntity.COLOR_OLED;
		this.frameless = true;
		this.blackback = true;
		this.idleLight = false;
		this.particles = LampParticles.NOTE;
		this.orientation = LampOrientation.AUTO;

		this.modeButton.setMessage(this.modeLabel());
		this.colorButton.setMessage(this.colorLabel());
		this.framelessButton.setMessage(this.framelessLabel());
		this.blackbackButton.setMessage(this.blackbackLabel());
		this.idleLightButton.setMessage(this.idleLightLabel());
		this.particlesButton.setMessage(this.particlesLabel());
		this.orientationButton.setMessage(this.orientationLabel());
		this.sensitivitySlider.reset(1.0F, 0.25, 3.0);
		this.speedSlider.reset(1.0F, 0.25, 3.0);
	}

	private Component modeLabel() {
		return Component.translatable("screen.beatlamp.mode").append(": ").append(Component.translatable("screen.beatlamp.mode." + this.mode.getSerializedName()));
	}

	private Component colorLabel() {
		if (this.color == BeatLampBlockEntity.COLOR_OLED) {
			return Component.translatable("screen.beatlamp.color").append(": ").append(Component.translatable("screen.beatlamp.color.oled"));
		}

		return Component.translatable("screen.beatlamp.color").append(": #").append(Integer.toHexString(this.color).toUpperCase());
	}

	private Component framelessLabel() {
		return Component.translatable("screen.beatlamp.frameless").append(": ").append(Component.translatable(this.frameless ? "gui.yes" : "gui.no"));
	}

	private Component blackbackLabel() {
		return Component.translatable("screen.beatlamp.solidback").append(": ").append(Component.translatable(this.blackback ? "gui.yes" : "gui.no"));
	}

	private Component idleLightLabel() {
		return Component.translatable("screen.beatlamp.idlelight").append(": ").append(Component.translatable(this.idleLight ? "gui.yes" : "gui.no"));
	}

	private Component sourceLabel() {
		if (this.sourcePos == null) {
			return Component.translatable("screen.beatlamp.source.none");
		}

		return Component.translatable("screen.beatlamp.source.bound", this.sourcePos.getX(), this.sourcePos.getY(), this.sourcePos.getZ());
	}

	private Component particlesLabel() {
		return Component.translatable("screen.beatlamp.particles")
			.append(": ")
			.append(Component.translatable("screen.beatlamp.particles." + this.particles.getSerializedName()));
	}

	private Component orientationLabel() {
		return Component.translatable("screen.beatlamp.orientation")
			.append(": ")
			.append(Component.translatable("screen.beatlamp.orientation." + this.orientation.getSerializedName()));
	}

	private int colorIndex() {
		for (int i = 0; i < PALETTE.length; i++) {
			if (PALETTE[i] == this.color) {
				return i;
			}
		}

		return 0;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderTransparentBackground(guiGraphics);
		guiGraphics.drawString(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, this.height / 2 - 116, 0xFFFFFF);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void onClose() {
		ClientPlayNetworking.send(
			new LampConfigurePayload(
				this.pos, this.mode, this.sensitivity, this.speed, this.color, this.frameless, this.blackback, this.idleLight, this.particles, this.orientation, this.unlink
			)
		);
		super.onClose();
	}

	private abstract static class ValueSlider extends AbstractSliderButton {
		private final Component label;
		private final double min;
		private final double max;

		protected ValueSlider(int x, int y, Component label, double currentValue, double min, double max) {
			super(x, y, 200, 20, label, (currentValue - min) / (max - min));
			this.label = label;
			this.min = min;
			this.max = max;
			this.updateMessage();
		}

		protected void reset(double currentValue, double min, double max) {
			this.value = (currentValue - min) / (max - min);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			double current = Mth.lerp(this.value, this.min, this.max);
			this.setMessage(this.label.copy().append(": ").append(String.format("%.2f", current)));
		}
	}
}
