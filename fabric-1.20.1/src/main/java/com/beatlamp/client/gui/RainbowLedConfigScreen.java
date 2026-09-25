package com.beatlamp.client.gui;

import com.beatlamp.block.RainbowLedBlockEntity;
import com.beatlamp.block.RainbowLedMode;
import com.beatlamp.client.PlatformNetwork;
import com.beatlamp.network.RainbowLedConfigurePayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

public class RainbowLedConfigScreen extends Screen {
	private static final int[] PALETTE = buildPalette();

	private final BlockPos pos;
	private final RainbowLedBlockEntity blockEntity;
	private RainbowLedMode mode;
	private float speed;
	private int color;
	private int brightness;
	private boolean frameless;
	private boolean unlink = false;

	private Button modeButton;
	private Button colorButton;
	private Button framelessButton;
	private ValueSlider speedSlider;
	private IntSlider brightnessSlider;

	public RainbowLedConfigScreen(RainbowLedBlockEntity led) {
		super(Component.translatable("screen.beatlamp.rainbow_led.config"));
		this.pos = led.getBlockPos().immutable();
		this.blockEntity = led;
		this.mode = led.getMode();
		this.speed = led.getSpeed();
		this.color = led.getColor();
		this.brightness = led.getBrightness();
		this.frameless = led.isFrameless();
	}

	private static int[] buildPalette() {
		DyeColor[] dyes = DyeColor.values();
		int[] palette = new int[dyes.length + 1];
		palette[0] = RainbowLedBlockEntity.COLOR_RAINBOW;

		for (int i = 0; i < dyes.length; i++) {
			palette[i + 1] = dyes[i].getFireworkColor();
		}

		return palette;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int y = this.height / 2 - 75;

		// 1. Mode Button (Cycles through all modes)
		this.modeButton = this.addRenderableWidget(
			Button.builder(this.modeLabel(), button -> {
				this.mode = this.mode.next();
				if (this.mode == RainbowLedMode.RAINBOW) {
					this.color = RainbowLedBlockEntity.COLOR_RAINBOW;
					if (this.colorButton != null) {
						this.colorButton.setMessage(this.colorLabel());
					}
				}
				button.setMessage(this.modeLabel());
				this.updatePreview();
			}).bounds(centerX - 100, y, 200, 20).build()
		);
		y += 24;

		// 2. Color Button (Cycles palette)
		this.colorButton = this.addRenderableWidget(
			Button.builder(this.colorLabel(), button -> {
				int index = this.colorIndex();
				this.color = PALETTE[(index + 1) % PALETTE.length];
				if (this.color != RainbowLedBlockEntity.COLOR_RAINBOW && this.mode == RainbowLedMode.RAINBOW) {
					this.mode = RainbowLedMode.STATIC;
					if (this.modeButton != null) {
						this.modeButton.setMessage(this.modeLabel());
					}
				}
				button.setMessage(this.colorLabel());
				this.updatePreview();
			}).bounds(centerX - 100, y, 98, 20).build()
		);

		// 3. Frameless Toggle Button
		this.framelessButton = this.addRenderableWidget(
			Button.builder(this.framelessLabel(), button -> {
				this.frameless = !this.frameless;
				button.setMessage(this.framelessLabel());
				this.updatePreview();
			}).bounds(centerX + 2, y, 98, 20).build()
		);
		y += 24;

		// 4. Speed Slider (0.25x - 3.00x)
		this.speedSlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y, 200, 20, Component.translatable("screen.beatlamp.rainbow_led.speed"), this.speed, 0.25, 3.0) {
				@Override
				protected void applyValue() {
					RainbowLedConfigScreen.this.speed = (float) Mth.lerp(this.value, this.min, this.max);
					RainbowLedConfigScreen.this.updatePreview();
				}
			}
		);
		y += 24;

		// 5. Brightness Slider (1 - 15)
		this.brightnessSlider = this.addRenderableWidget(
			new IntSlider(centerX - 100, y, 200, 20, Component.translatable("screen.beatlamp.rainbow_led.brightness"), this.brightness, 1, 15) {
				@Override
				protected void applyValue() {
					RainbowLedConfigScreen.this.brightness = (int) Math.round(Mth.lerp(this.value, this.min, this.max));
					RainbowLedConfigScreen.this.updatePreview();
				}
			}
		);
		y += 26;

		// Reset, Unlink & Done buttons
		this.addRenderableWidget(
			Button.builder(Component.translatable("screen.beatlamp.reset"), button -> {
				this.mode = RainbowLedMode.RAINBOW;
				this.speed = 1.0F;
				this.color = RainbowLedBlockEntity.COLOR_RAINBOW;
				this.brightness = 15;
				this.frameless = false;

				this.modeButton.setMessage(this.modeLabel());
				this.colorButton.setMessage(this.colorLabel());
				this.framelessButton.setMessage(this.framelessLabel());
				this.speedSlider.updateVal(1.0F);
				this.brightnessSlider.updateVal(15);
				this.updatePreview();
			}).bounds(centerX - 100, y, 64, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("screen.beatlamp.unlink"), button -> {
				this.unlink = true;
				this.onClose();
			}).bounds(centerX - 32, y, 64, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(centerX + 36, y, 64, 20)
				.build()
		);
	}

	private void updatePreview() {
		if (this.blockEntity != null) {
			this.blockEntity.setMode(this.mode);
			this.blockEntity.setSpeed(this.speed);
			this.blockEntity.setColor(this.color);
			this.blockEntity.setBrightness(this.brightness);
			this.blockEntity.setFrameless(this.frameless);
		}
	}

	private Component modeLabel() {
		String key = switch (this.mode) {
			case RAINBOW -> "screen.beatlamp.rainbow_led.mode.rainbow";
			case BREATHING -> "screen.beatlamp.rainbow_led.mode.breathing";
			case STROBE -> "screen.beatlamp.rainbow_led.mode.strobe";
			case STATIC -> "screen.beatlamp.rainbow_led.mode.static";
			case WAVE -> "screen.beatlamp.rainbow_led.mode.wave";
		};
		return Component.translatable("screen.beatlamp.rainbow_led.mode", Component.translatable(key));
	}

	private Component colorLabel() {
		if (this.color == RainbowLedBlockEntity.COLOR_RAINBOW) {
			return Component.translatable("screen.beatlamp.color", Component.translatable("screen.beatlamp.rainbow_led.color.rainbow"));
		}

		for (DyeColor dye : DyeColor.values()) {
			if (dye.getFireworkColor() == this.color) {
				return Component.translatable("screen.beatlamp.color", Component.translatable("color.minecraft." + dye.getName()));
			}
		}

		return Component.translatable("screen.beatlamp.color", Component.literal(String.format("#%06X", this.color)));
	}

	private Component framelessLabel() {
		return Component.translatable("screen.beatlamp.rainbow_led.frame",
			Component.translatable(this.frameless ? "screen.beatlamp.rainbow_led.frame.frameless" : "screen.beatlamp.rainbow_led.frame.framed")
		);
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
		PlatformNetwork.sendToServer(new RainbowLedConfigurePayload(
			this.pos,
			this.mode,
			this.speed,
			this.color,
			this.brightness,
			this.frameless,
			this.unlink
		));
		super.onClose();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 92, 0xFFFFFF);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private abstract static class ValueSlider extends AbstractSliderButton {
		private final Component prefix;
		protected final double min;
		protected final double max;

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

	private abstract static class IntSlider extends AbstractSliderButton {
		private final Component prefix;
		protected final int min;
		protected final int max;

		IntSlider(int x, int y, int width, int height, Component prefix, int initial, int min, int max) {
			super(x, y, width, height, Component.empty(), (double) (initial - min) / (max - min));
			this.prefix = prefix;
			this.min = min;
			this.max = max;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int val = (int) Math.round(Mth.lerp(this.value, this.min, this.max));
			this.setMessage(Component.literal(this.prefix.getString() + ": " + val + " / " + this.max));
		}

		public void updateVal(int initial) {
			this.value = (double) (initial - this.min) / (this.max - this.min);
			this.updateMessage();
			this.applyValue();
		}
	}
}
