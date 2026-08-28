package com.beatlamp.client.gui;

import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.FountainParticles;
import com.beatlamp.network.FountainConfigurePayload;
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

public class FountainConfigScreen extends Screen {
	private static final int[] PALETTE = buildPalette();

	private final BlockPos pos;
	private boolean fireworkMode;
	private float sprayThreshold;
	private float impactThreshold;
	private boolean smokeEnabled;
	private FountainParticles particleType;
	private int color;
	private boolean unlink;
	private BlockPos sourcePos;

	private Button fireworkButton;
	private Button smokeButton;
	private Button particleButton;
	private Button colorButton;
	private Button sourceButton;
	private ValueSlider spraySlider;
	private ValueSlider impactSlider;

	public FountainConfigScreen(FountainBlockEntity fountain) {
		super(Component.translatable("screen.beatlamp.fountain.config"));
		this.pos = fountain.getBlockPos().immutable();
		this.fireworkMode = fountain.isFireworkMode();
		this.sprayThreshold = fountain.getSprayThreshold();
		this.impactThreshold = fountain.getImpactThreshold();
		this.smokeEnabled = fountain.isSmokeEnabled();
		this.particleType = fountain.getParticleType();
		this.color = fountain.getColor();
		this.sourcePos = fountain.getSource() == null ? null : fountain.getSource().immutable();
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
		int y = this.height / 2 - 102;

		this.fireworkButton = this.addRenderableWidget(
			Button.builder(this.fireworkLabel(), button -> {
				this.fireworkMode = !this.fireworkMode;
				button.setMessage(this.fireworkLabel());
			}).bounds(centerX - 100, y, 200, 20).build()
		);

		this.spraySlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 24, Component.translatable("screen.beatlamp.fountain.spray_threshold"), this.sprayThreshold, 0.00, 0.80) {
				@Override
				protected void applyValue() {
					FountainConfigScreen.this.sprayThreshold = (float) Mth.lerp(this.value, 0.00, 0.80);
				}
			}
		);

		this.impactSlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 48, Component.translatable("screen.beatlamp.fountain.threshold"), this.impactThreshold, 0.50, 0.95) {
				@Override
				protected void applyValue() {
					FountainConfigScreen.this.impactThreshold = (float) Mth.lerp(this.value, 0.50, 0.95);
				}
			}
		);

		this.smokeButton = this.addRenderableWidget(
			Button.builder(this.smokeLabel(), button -> {
				this.smokeEnabled = !this.smokeEnabled;
				button.setMessage(this.smokeLabel());
			}).bounds(centerX - 100, y + 72, 200, 20).build()
		);

		this.particleButton = this.addRenderableWidget(
			Button.builder(this.particleLabel(), button -> {
				this.particleType = this.particleType.next();
				button.setMessage(this.particleLabel());
			}).bounds(centerX - 100, y + 96, 200, 20).build()
		);

		this.colorButton = this.addRenderableWidget(
			Button.builder(this.colorLabel(), button -> {
				int index = this.colorIndex();
				this.color = PALETTE[(index + 1) % PALETTE.length];
				button.setMessage(this.colorLabel());
			}).bounds(centerX - 100, y + 120, 200, 20).build()
		);

		this.sourceButton = this.addRenderableWidget(
			Button.builder(this.sourceLabel(), button -> {
				if (this.sourcePos != null) {
					ClientPlayNetworking.send(new LampSourcePayload(this.pos));
					this.sourcePos = null;
					button.setMessage(this.sourceLabel());
				}
			}).bounds(centerX - 100, y + 144, 200, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("screen.beatlamp.reset"), button -> {
				this.fireworkMode = false;
				this.sprayThreshold = 0.12F;
				this.impactThreshold = 0.75F;
				this.smokeEnabled = true;
				this.particleType = FountainParticles.FLAME;
				this.color = BeatLampBlockEntity.COLOR_OLED;
				this.fireworkButton.setMessage(this.fireworkLabel());
				this.smokeButton.setMessage(this.smokeLabel());
				this.particleButton.setMessage(this.particleLabel());
				this.colorButton.setMessage(this.colorLabel());
				this.spraySlider.updateVal(0.12F);
				this.impactSlider.updateVal(0.75F);
			}).bounds(centerX - 100, y + 170, 98, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("screen.beatlamp.unlink"), button -> {
				this.unlink = true;
				this.onClose();
			}).bounds(centerX + 2, y + 170, 98, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(centerX - 100, y + 194, 200, 20)
				.build()
		);
	}

	private Component fireworkLabel() {
		return Component.translatable("screen.beatlamp.fountain.firework",
			Component.translatable(this.fireworkMode ? "screen.beatlamp.enabled" : "screen.beatlamp.disabled")
		);
	}

	private Component smokeLabel() {
		return Component.translatable("screen.beatlamp.fountain.smoke",
			Component.translatable(this.smokeEnabled ? "screen.beatlamp.enabled" : "screen.beatlamp.disabled")
		);
	}

	private Component particleLabel() {
		return Component.translatable("screen.beatlamp.fountain.particle",
			Component.translatable("screen.beatlamp.fountain.particle." + this.particleType.name().toLowerCase())
		);
	}

	private Component colorLabel() {
		if (this.color == BeatLampBlockEntity.COLOR_OLED) {
			return Component.translatable("screen.beatlamp.color", Component.translatable("screen.beatlamp.color.oled"));
		}

		for (DyeColor dye : DyeColor.values()) {
			if (dye.getFireworkColor() == this.color) {
				return Component.translatable("screen.beatlamp.color", Component.translatable("color.minecraft." + dye.getName()));
			}
		}

		return Component.translatable("screen.beatlamp.color", Component.literal(String.format("#%06X", this.color)));
	}

	private Component sourceLabel() {
		if (this.sourcePos == null) {
			return Component.translatable("screen.beatlamp.source.none");
		}
		return Component.translatable("screen.beatlamp.source.bound", this.sourcePos.getX(), this.sourcePos.getY(), this.sourcePos.getZ());
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
	public void onClose() {
		ClientPlayNetworking.send(new FountainConfigurePayload(
			this.pos,
			this.fireworkMode,
			this.sprayThreshold,
			this.impactThreshold,
			this.smokeEnabled,
			this.particleType,
			this.color,
			this.unlink
		));
		super.onClose();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 116, 0xFFFFFF);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private abstract static class ValueSlider extends AbstractSliderButton {
		private final Component prefix;
		private final double min;
		private final double max;

		ValueSlider(int x, int y, Component prefix, float initial, double min, double max) {
			super(x, y, 200, 20, Component.empty(), (initial - min) / (max - min));
			this.prefix = prefix;
			this.min = min;
			this.max = max;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			double val = Mth.lerp(this.value, this.min, this.max);
			this.setMessage(Component.translatable("screen.beatlamp.slider.value", this.prefix, String.format("%.2f", val)));
		}

		public void updateVal(float initial) {
			this.value = (initial - this.min) / (this.max - this.min);
			this.updateMessage();
			this.applyValue();
		}
	}
}
