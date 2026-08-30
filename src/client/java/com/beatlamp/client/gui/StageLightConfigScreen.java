package com.beatlamp.client.gui;

import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;
import com.beatlamp.block.StageLightMode;
import com.beatlamp.network.LampSourcePayload;
import com.beatlamp.network.StageLightConfigurePayload;
import com.beatlamp.client.PlatformNetwork;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

public class StageLightConfigScreen extends Screen {
	private static final int[] PALETTE = buildPalette();

	private final BlockPos pos;
	private StageLightMode mode;
	private float sensitivity;
	private float speed;
	private int color;
	private boolean unlink;
	private boolean dmxEnrolled;
	private String customName;
	private BlockPos sourcePos;

	private EditBox nameBox;
	private Button modeButton;
	private Button colorButton;
	private Button dmxButton;
	private Button sourceButton;
	private ValueSlider sensitivitySlider;
	private ValueSlider speedSlider;

	public StageLightConfigScreen(StageLightBlockEntity light) {
		super(Component.translatable("screen.beatlamp.stagelight.config"));
		this.pos = light.getBlockPos().immutable();
		this.mode = light.getMode();
		this.sensitivity = light.getSensitivity();
		this.speed = light.getSpeed();
		this.color = light.getColor();
		this.dmxEnrolled = light.isDmxEnrolled();
		this.customName = light.getCustomName();
		this.sourcePos = light.getSource() == null ? null : light.getSource().immutable();
	}

	private static int[] buildPalette() {
		DyeColor[] dyes = DyeColor.values();
		int[] palette = new int[dyes.length + 2];
		palette[0] = BeatLampBlockEntity.COLOR_OLED;
		palette[1] = StageLightBlockEntity.COLOR_BEAT_CYCLE;

		for (int i = 0; i < dyes.length; i++) {
			palette[i + 2] = dyes[i].getFireworkColor();
		}

		return palette;
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

		this.sensitivitySlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 44, 98, 20, Component.translatable("screen.beatlamp.sensitivity"), this.sensitivity, 0.25, 3.0) {
				@Override
				protected void applyValue() {
					StageLightConfigScreen.this.sensitivity = (float) Mth.lerp(this.value, 0.25, 3.0);
				}
			}
		);

		this.speedSlider = this.addRenderableWidget(
			new ValueSlider(centerX + 2, y + 44, 98, 20, Component.translatable("screen.beatlamp.speed"), this.speed, 0.25, 3.0) {
				@Override
				protected void applyValue() {
					StageLightConfigScreen.this.speed = (float) Mth.lerp(this.value, 0.25, 3.0);
				}
			}
		);

		this.colorButton = this.addRenderableWidget(
			Button.builder(this.colorLabel(), button -> {
				int index = this.colorIndex();
				this.color = PALETTE[(index + 1) % PALETTE.length];
				button.setMessage(this.colorLabel());
			}).bounds(centerX - 100, y + 66, 98, 20).build()
		);

		this.dmxButton = this.addRenderableWidget(
			Button.builder(this.dmxLabel(), button -> {
				this.dmxEnrolled = !this.dmxEnrolled;
				button.setMessage(this.dmxLabel());
			}).bounds(centerX + 2, y + 66, 98, 20).build()
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
				this.mode = StageLightMode.SWEEP;
				this.sensitivity = 1.0F;
				this.speed = 1.0F;
				this.color = BeatLampBlockEntity.COLOR_OLED;
				this.modeButton.setMessage(this.modeLabel());
				this.colorButton.setMessage(this.colorLabel());
				this.sensitivitySlider.updateVal(1.0F);
				this.speedSlider.updateVal(1.0F);
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
		return Component.translatable("screen.beatlamp.stagelight.mode",
			Component.translatable("screen.beatlamp.stagelight.mode." + this.mode.name().toLowerCase())
		);
	}

	private Component colorLabel() {
		if (this.color == BeatLampBlockEntity.COLOR_OLED) {
			return Component.translatable("screen.beatlamp.color", Component.translatable("screen.beatlamp.color.oled"));
		}
		if (this.color == StageLightBlockEntity.COLOR_BEAT_CYCLE) {
			return Component.translatable("screen.beatlamp.color", Component.translatable("screen.beatlamp.color.beat_cycle"));
		}

		for (DyeColor dye : DyeColor.values()) {
			if (dye.getFireworkColor() == this.color) {
				return Component.translatable("screen.beatlamp.color", Component.translatable("color.minecraft." + dye.getName()));
			}
		}

		return Component.translatable("screen.beatlamp.color", Component.literal(String.format("#%06X", this.color)));
	}

	private Component dmxLabel() {
		return Component.translatable("screen.beatlamp.dmx_link", Component.translatable(this.dmxEnrolled ? "screen.beatlamp.enabled" : "screen.beatlamp.disabled"));
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
		String finalName = this.nameBox != null ? this.nameBox.getValue().trim() : this.customName;
		PlatformNetwork.sendToServer(new StageLightConfigurePayload(
			this.pos,
			this.mode,
			this.sensitivity,
			this.speed,
			this.color,
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

		@Override
		protected void updateMessage() {
			double val = Mth.lerp(this.value, this.min, this.max);
			this.setMessage(Component.translatable("screen.beatlamp.slider.value", this.prefix, String.format("%.2fx", val)));
		}

		public void updateVal(float initial) {
			this.value = (initial - this.min) / (this.max - this.min);
			this.updateMessage();
			this.applyValue();
		}
	}
}
