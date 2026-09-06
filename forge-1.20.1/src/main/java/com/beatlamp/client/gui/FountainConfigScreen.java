package com.beatlamp.client.gui;

import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.FountainParticles;
import com.beatlamp.client.PlatformNetwork;
import com.beatlamp.network.FountainConfigurePayload;
import com.beatlamp.network.LampSourcePayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

public class FountainConfigScreen extends Screen {
	private static final int[] PALETTE = buildPalette();

	private final BlockPos pos;
	private int color;
	private boolean fireworkMode;
	private float sprayThreshold;
	private float impactThreshold;
	private boolean smokeEnabled;
	private FountainParticles particleType;
	private boolean dmxEnrolled;
	private String customName;
	private boolean unlink;
	private BlockPos sourcePos;

	private EditBox nameBox;
	private Button colorButton;
	private Button fireworkButton;
	private ValueSlider spraySlider;
	private ValueSlider impactSlider;
	private Button smokeButton;
	private Button particleButton;
	private Button dmxButton;
	private Button sourceButton;

	public FountainConfigScreen(FountainBlockEntity fountain) {
		super(Component.translatable("screen.beatlamp.fountain.config"));
		this.pos = fountain.getBlockPos().immutable();
		this.color = fountain.getColor();
		this.fireworkMode = fountain.isFireworkMode();
		this.sprayThreshold = fountain.getSprayThreshold();
		this.impactThreshold = fountain.getImpactThreshold();
		this.smokeEnabled = fountain.isSmokeEnabled();
		this.particleType = fountain.getParticleType();
		this.dmxEnrolled = fountain.isDmxEnrolled();
		this.customName = fountain.getCustomName();
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
		int y = this.height / 2 - 92;

		this.nameBox = new EditBox(this.font, centerX - 100, y, 200, 18, Component.literal("Group Name"));
		this.nameBox.setValue(this.customName);
		this.nameBox.setHint(Component.translatable("screen.beatlamp.name_hint"));
		this.nameBox.setMaxLength(32);
		this.addRenderableWidget(this.nameBox);

		this.colorButton = this.addRenderableWidget(
			Button.builder(this.colorLabel(), button -> {
				int nextIdx = (this.colorIndex() + 1) % PALETTE.length;
				this.color = PALETTE[nextIdx];
				button.setMessage(this.colorLabel());
			}).bounds(centerX - 100, y + 24, 98, 20).build()
		);

		this.fireworkButton = this.addRenderableWidget(
			Button.builder(this.fireworkLabel(), button -> {
				this.fireworkMode = !this.fireworkMode;
				button.setMessage(this.fireworkLabel());
			}).bounds(centerX + 2, y + 24, 98, 20).build()
		);

		this.spraySlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 48, Component.translatable("screen.beatlamp.fountain.spray"), this.sprayThreshold, 0.00, 0.80, "%.2f") {
				@Override
				protected void applyValue() {
					FountainConfigScreen.this.sprayThreshold = (float) Mth.lerp(this.value, 0.00, 0.80);
				}
			}
		);

		this.impactSlider = this.addRenderableWidget(
			new ValueSlider(centerX + 2, y + 48, Component.translatable("screen.beatlamp.fountain.impact"), this.impactThreshold, 0.10, 1.00, "%.2f") {
				@Override
				protected void applyValue() {
					FountainConfigScreen.this.impactThreshold = (float) Mth.lerp(this.value, 0.10, 1.00);
				}
			}
		);

		this.smokeButton = this.addRenderableWidget(
			Button.builder(this.smokeLabel(), button -> {
				this.smokeEnabled = !this.smokeEnabled;
				button.setMessage(this.smokeLabel());
			}).bounds(centerX - 100, y + 72, 98, 20).build()
		);

		this.particleButton = this.addRenderableWidget(
			Button.builder(this.particleLabel(), button -> {
				this.particleType = this.particleType.next();
				button.setMessage(this.particleLabel());
			}).bounds(centerX + 2, y + 72, 98, 20).build()
		);

		this.dmxButton = this.addRenderableWidget(
			Button.builder(this.dmxLabel(), button -> {
				this.dmxEnrolled = !this.dmxEnrolled;
				button.setMessage(this.dmxLabel());
			}).bounds(centerX - 100, y + 96, 200, 20).build()
		);

		this.sourceButton = this.addRenderableWidget(
			Button.builder(this.sourceLabel(), button -> {
				if (this.sourcePos != null) {
					this.sourcePos = null;
					button.setMessage(this.sourceLabel());
					PlatformNetwork.sendToServer(new LampSourcePayload(this.pos, null));
				}
			}).bounds(centerX - 100, y + 120, 200, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("screen.beatlamp.unlink"), button -> {
				this.unlink = true;
				this.onClose();
			}).bounds(centerX - 100, y + 144, 98, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(centerX + 2, y + 144, 98, 20).build()
		);
	}

	private Component colorLabel() {
		int idx = this.colorIndex();
		if (idx == 0) {
			return Component.translatable("screen.beatlamp.color", Component.translatable("screen.beatlamp.color.oled"));
		}
		DyeColor dye = DyeColor.values()[idx - 1];
		return Component.translatable("screen.beatlamp.color", Component.translatable("color.minecraft." + dye.getName()));
	}

	private Component fireworkLabel() {
		return Component.translatable("screen.beatlamp.fountain.firework", Component.translatable(this.fireworkMode ? "screen.beatlamp.toggle.on" : "screen.beatlamp.toggle.off"));
	}

	private Component smokeLabel() {
		return Component.translatable("screen.beatlamp.fountain.smoke", Component.translatable(this.smokeEnabled ? "screen.beatlamp.toggle.on" : "screen.beatlamp.toggle.off"));
	}

	private Component particleLabel() {
		return Component.translatable("screen.beatlamp.fountain.particle", Component.translatable(this.particleType.getTranslationKey()));
	}

	private Component dmxLabel() {
		return Component.translatable("screen.beatlamp.dmx_link", Component.translatable(this.dmxEnrolled ? "screen.beatlamp.toggle.on" : "screen.beatlamp.toggle.off"));
	}

	private Component sourceLabel() {
		if (this.sourcePos == null) {
			return Component.translatable("screen.beatlamp.source", Component.translatable("screen.beatlamp.source.auto"));
		}
		return Component.translatable("screen.beatlamp.source.bound_click_clear", this.sourcePos.toShortString());
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
		this.renderBackground(guiGraphics);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 106, 0xFFFFFF);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		String finalName = this.nameBox != null ? this.nameBox.getValue().trim() : this.customName;
		PlatformNetwork.sendToServer(
			new FountainConfigurePayload(
				this.pos, this.color, this.fireworkMode, this.sprayThreshold, this.impactThreshold,
				this.smokeEnabled, this.particleType, this.unlink, this.dmxEnrolled, finalName
			)
		);
		super.onClose();
	}

	private abstract static class ValueSlider extends AbstractSliderButton {
		private final Component prefix;
		private final double min;
		private final double max;
		private final String format;

		ValueSlider(int x, int y, Component prefix, float initial, double min, double max, String format) {
			super(x, y, 98, 20, Component.empty(), (initial - min) / (max - min));
			this.prefix = prefix;
			this.min = min;
			this.max = max;
			this.format = format;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			double val = Mth.lerp(this.value, this.min, this.max);
			this.setMessage(Component.translatable("screen.beatlamp.slider.value", this.prefix, String.format(this.format, val)));
		}
	}
}
