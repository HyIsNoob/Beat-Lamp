package com.beatlamp.client.gui;

import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.EmitterMode;
import com.beatlamp.network.EmitterConfigurePayload;
import com.beatlamp.network.LampSourcePayload;
import com.beatlamp.client.PlatformNetwork;

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
	private boolean unlink;
	private boolean dmxEnrolled;
	private String customName;
	private BlockPos sourcePos;

	private EditBox nameBox;
	private Button modeButton;
	private ValueSlider thresholdSlider;
	private Button invertButton;
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
		int y = this.height / 2 - 88;

		this.nameBox = new EditBox(this.font, centerX - 100, y, 200, 18, Component.literal("Group Name"));
		this.nameBox.setValue(this.customName);
		this.nameBox.setHint(Component.translatable("screen.beatlamp.name_hint"));
		this.nameBox.setMaxLength(32);
		this.addRenderableWidget(this.nameBox);

		this.modeButton = this.addRenderableWidget(
			Button.builder(this.modeLabel(), button -> {
				this.mode = this.mode.next();
				button.setMessage(this.modeLabel());
			}).bounds(centerX - 100, y + 22, 200, 20).build()
		);

		this.thresholdSlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 44, 98, 20, Component.translatable("screen.beatlamp.emitter.threshold"), this.threshold, 0.10, 0.95) {
				@Override
				protected void applyValue() {
					EmitterConfigScreen.this.threshold = (float) Mth.lerp(this.value, 0.10, 0.95);
				}
			}
		);

		this.invertButton = this.addRenderableWidget(
			Button.builder(this.invertLabel(), button -> {
				this.inverted = !this.inverted;
				button.setMessage(this.invertLabel());
			}).bounds(centerX + 2, y + 44, 98, 20).build()
		);

		this.dmxButton = this.addRenderableWidget(
			Button.builder(this.dmxLabel(), button -> {
				this.dmxEnrolled = !this.dmxEnrolled;
				button.setMessage(this.dmxLabel());
			}).bounds(centerX - 100, y + 66, 200, 20).build()
		);

		this.sourceButton = this.addRenderableWidget(
			Button.builder(this.sourceLabel(), button -> {
				if (this.sourcePos != null) {
					PlatformNetwork.sendToServer(new LampSourcePayload(this.pos));
					this.sourcePos = null;
					button.setMessage(this.sourceLabel());
				}
			}).bounds(centerX - 100, y + 88, 200, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("screen.beatlamp.reset"), button -> {
				this.mode = EmitterMode.PULSE;
				this.threshold = 0.50F;
				this.inverted = false;
				this.modeButton.setMessage(this.modeLabel());
				this.thresholdSlider.setValue(0.50F);
				this.invertButton.setMessage(this.invertLabel());
			}).bounds(centerX - 100, y + 110, 64, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("screen.beatlamp.unlink"), button -> {
				this.unlink = true;
				this.onClose();
			}).bounds(centerX - 32, y + 110, 64, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(centerX + 36, y + 110, 64, 20)
				.build()
		);
	}

	private Component modeLabel() {
		return Component.translatable("screen.beatlamp.emitter.mode", this.mode.getTranslation());
	}

	private Component invertLabel() {
		return Component.translatable("screen.beatlamp.emitter.invert",
			Component.translatable(this.inverted ? "screen.beatlamp.enabled" : "screen.beatlamp.disabled")
		);
	}

	private Component dmxLabel() {
		return Component.translatable("screen.beatlamp.dmx_link",
			Component.translatable(this.dmxEnrolled ? "screen.beatlamp.enabled" : "screen.beatlamp.disabled")
		);
	}

	private Component sourceLabel() {
		if (this.sourcePos == null) {
			return Component.translatable("screen.beatlamp.source.none");
		}
		return Component.translatable("screen.beatlamp.source.bound", this.sourcePos.getX(), this.sourcePos.getY(), this.sourcePos.getZ());
	}

	@Override
	public void onClose() {
		String finalName = this.nameBox != null ? this.nameBox.getValue().trim() : this.customName;
		PlatformNetwork.sendToServer(new EmitterConfigurePayload(
			this.pos,
			this.mode,
			this.threshold,
			this.inverted,
			this.unlink,
			this.dmxEnrolled,
			finalName
		));
		super.onClose();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 104, 0xFFFFFF);
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

		ValueSlider(int x, int y, int width, int height, Component prefix, float initial, double min, double max) {
			super(x, y, width, height, Component.empty(), (initial - min) / (max - min));
			this.prefix = prefix;
			this.min = min;
			this.max = max;
			this.updateMessage();
		}

		void setValue(float target) {
			this.value = Mth.clamp((target - this.min) / (this.max - this.min), 0.0, 1.0);
			this.updateMessage();
			this.applyValue();
		}

		@Override
		protected void updateMessage() {
			double val = Mth.lerp(this.value, this.min, this.max);
			this.setMessage(Component.translatable("screen.beatlamp.slider.value", this.prefix, String.format("%.2fx", val)));
		}
	}
}
