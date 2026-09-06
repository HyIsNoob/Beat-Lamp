package com.beatlamp.client.gui;

import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.EmitterMode;
import com.beatlamp.client.PlatformNetwork;
import com.beatlamp.network.EmitterSignalPayload;
import com.beatlamp.network.LampSourcePayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class EmitterConfigScreen extends Screen {
	private final BlockPos pos;
	private EmitterMode mode;
	private float threshold;
	private boolean inverted;
	private boolean dmxEnrolled;
	private String customName;
	private boolean unlink;
	private BlockPos sourcePos;

	private EditBox nameBox;
	private Button modeButton;
	private ValueSlider thresholdSlider;
	private Button invertedButton;
	private Button dmxButton;
	private Button sourceButton;

	public EmitterConfigScreen(BeatEmitterBlockEntity emitter) {
		super(Component.translatable("screen.beatlamp.emitter.config"));
		this.pos = emitter.getBlockPos().immutable();
		this.mode = emitter.getMode();
		this.threshold = emitter.getThreshold();
		this.inverted = emitter.isInverted();
		this.dmxEnrolled = emitter.isDmxEnrolled();
		this.customName = emitter.getCustomName();
		this.sourcePos = emitter.getSource() == null ? null : emitter.getSource().immutable();
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

		this.modeButton = this.addRenderableWidget(
			Button.builder(this.modeLabel(), button -> {
				this.mode = this.mode.next();
				button.setMessage(this.modeLabel());
			}).bounds(centerX - 100, y + 24, 200, 20).build()
		);

		this.thresholdSlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 48, Component.translatable("screen.beatlamp.emitter.threshold"), this.threshold, 0.05, 1.00, "%.2f") {
				@Override
				protected void applyValue() {
					EmitterConfigScreen.this.threshold = (float) Mth.lerp(this.value, 0.05, 1.00);
				}
			}
		);

		this.invertedButton = this.addRenderableWidget(
			Button.builder(this.invertedLabel(), button -> {
				this.inverted = !this.inverted;
				button.setMessage(this.invertedLabel());
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

	private Component modeLabel() {
		return Component.translatable("screen.beatlamp.mode", Component.translatable(this.mode.getTranslationKey()));
	}

	private Component invertedLabel() {
		return Component.translatable("screen.beatlamp.emitter.invert", Component.translatable(this.inverted ? "screen.beatlamp.toggle.on" : "screen.beatlamp.toggle.off"));
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
			new EmitterSignalPayload(
				this.pos, this.mode, this.threshold, this.inverted, this.unlink, this.dmxEnrolled, finalName
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
