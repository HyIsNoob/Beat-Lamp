package com.beatlamp.client.gui;

import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.FogDensity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.client.PlatformNetwork;
import com.beatlamp.network.FogGeneratorConfigurePayload;
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

public class FogGeneratorConfigScreen extends Screen {
	private static final int[] PALETTE = buildPalette();

	private final BlockPos pos;
	private FogDensity density;
	private float radius;
	private int color;
	private boolean dmxEnrolled;
	private String customName;
	private boolean unlink;
	private BlockPos sourcePos;

	private EditBox nameBox;
	private Button densityButton;
	private ValueSlider radiusSlider;
	private Button colorButton;
	private Button dmxButton;
	private Button sourceButton;

	public FogGeneratorConfigScreen(FogGeneratorBlockEntity fog) {
		super(Component.translatable("screen.beatlamp.fog.config"));
		this.pos = fog.getBlockPos().immutable();
		this.density = fog.getDensity();
		this.radius = fog.getRadius();
		this.color = fog.getColor();
		this.dmxEnrolled = fog.isDmxEnrolled();
		this.customName = fog.getCustomName();
		this.sourcePos = fog.getSource() == null ? null : fog.getSource().immutable();
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
		int y = this.height / 2 - 80;

		this.nameBox = new EditBox(this.font, centerX - 100, y, 200, 18, Component.literal("Group Name"));
		this.nameBox.setValue(this.customName);
		this.nameBox.setHint(Component.translatable("screen.beatlamp.name_hint"));
		this.nameBox.setMaxLength(32);
		this.addRenderableWidget(this.nameBox);

		this.densityButton = this.addRenderableWidget(
			Button.builder(this.densityLabel(), button -> {
				this.density = this.density.next();
				button.setMessage(this.densityLabel());
			}).bounds(centerX - 100, y + 24, 200, 20).build()
		);

		this.radiusSlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 48, Component.translatable("screen.beatlamp.fog.radius"), this.radius, 4.0, 16.0, "%.1fm") {
				@Override
				protected void applyValue() {
					FogGeneratorConfigScreen.this.radius = (float) Mth.lerp(this.value, 4.0, 16.0);
				}
			}
		);

		this.colorButton = this.addRenderableWidget(
			Button.builder(this.colorLabel(), button -> {
				int nextIdx = (this.colorIndex() + 1) % PALETTE.length;
				this.color = PALETTE[nextIdx];
				button.setMessage(this.colorLabel());
			}).bounds(centerX + 2, y + 48, 98, 20).build()
		);

		this.dmxButton = this.addRenderableWidget(
			Button.builder(this.dmxLabel(), button -> {
				this.dmxEnrolled = !this.dmxEnrolled;
				button.setMessage(this.dmxLabel());
			}).bounds(centerX - 100, y + 72, 200, 20).build()
		);

		this.sourceButton = this.addRenderableWidget(
			Button.builder(this.sourceLabel(), button -> {
				if (this.sourcePos != null) {
					this.sourcePos = null;
					button.setMessage(this.sourceLabel());
					PlatformNetwork.sendToServer(new LampSourcePayload(this.pos, null));
				}
			}).bounds(centerX - 100, y + 96, 200, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("screen.beatlamp.unlink"), button -> {
				this.unlink = true;
				this.onClose();
			}).bounds(centerX - 100, y + 120, 98, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(centerX + 2, y + 120, 98, 20).build()
		);
	}

	private Component densityLabel() {
		return Component.translatable("screen.beatlamp.fog.density", Component.translatable(this.density.getTranslationKey()));
	}

	private Component colorLabel() {
		int idx = this.colorIndex();
		if (idx == 0) {
			return Component.translatable("screen.beatlamp.color", Component.translatable("screen.beatlamp.color.oled"));
		}
		DyeColor dye = DyeColor.values()[idx - 1];
		return Component.translatable("screen.beatlamp.color", Component.translatable("color.minecraft." + dye.getName()));
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
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 94, 0xFFFFFF);
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
			new FogGeneratorConfigurePayload(
				this.pos, this.density, this.radius, this.color, this.unlink, this.dmxEnrolled, finalName
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
