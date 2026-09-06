package com.beatlamp.client.gui;

import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.LampMode;
import com.beatlamp.block.LampOrientation;
import com.beatlamp.block.LampParticles;
import com.beatlamp.client.PlatformNetwork;
import com.beatlamp.network.LampConfigurePayload;
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
	private boolean reverse;
	private boolean tempoPulse;
	private boolean dmxEnrolled;
	private String customName;
	private LampParticles particles;
	private LampOrientation orientation;
	private boolean unlink;
	private BlockPos sourcePos;

	private EditBox nameBox;
	private Button modeButton;
	private ValueSlider sensitivitySlider;
	private ValueSlider speedSlider;
	private Button colorButton;
	private Button particlesButton;
	private Button framelessButton;
	private Button blackbackButton;
	private Button orientationButton;
	private Button tempoPulseButton;
	private Button idleLightButton;
	private Button dmxButton;
	private Button sourceButton;

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
		this.reverse = beatLamp.isReverse();
		this.tempoPulse = beatLamp.isTempoPulse();
		this.dmxEnrolled = beatLamp.isDmxEnrolled();
		this.customName = beatLamp.getCustomName();
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
		int y = this.height / 2 - 110;

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
			new ValueSlider(centerX - 100, y + 46, Component.translatable("screen.beatlamp.sensitivity"), this.sensitivity, 0.25, 4.0, "%.2fx") {
				@Override
				protected void applyValue() {
					LampConfigScreen.this.sensitivity = (float) Mth.lerp(this.value, 0.25, 4.0);
				}
			}
		);

		this.speedSlider = this.addRenderableWidget(
			new ValueSlider(centerX + 2, y + 46, Component.translatable("screen.beatlamp.speed"), this.speed, 0.25, 4.0, "%.2fx") {
				@Override
				protected void applyValue() {
					LampConfigScreen.this.speed = (float) Mth.lerp(this.value, 0.25, 4.0);
				}
			}
		);

		this.colorButton = this.addRenderableWidget(
			Button.builder(this.colorLabel(), button -> {
				int nextIdx = (this.colorIndex() + 1) % PALETTE.length;
				this.color = PALETTE[nextIdx];
				button.setMessage(this.colorLabel());
			}).bounds(centerX - 100, y + 70, 98, 20).build()
		);

		this.particlesButton = this.addRenderableWidget(
			Button.builder(this.particlesLabel(), button -> {
				this.particles = this.particles.next();
				button.setMessage(this.particlesLabel());
			}).bounds(centerX + 2, y + 70, 98, 20).build()
		);

		this.framelessButton = this.addRenderableWidget(
			Button.builder(this.framelessLabel(), button -> {
				this.frameless = !this.frameless;
				button.setMessage(this.framelessLabel());
			}).bounds(centerX - 100, y + 94, 98, 20).build()
		);

		this.blackbackButton = this.addRenderableWidget(
			Button.builder(this.blackbackLabel(), button -> {
				this.blackback = !this.blackback;
				button.setMessage(this.blackbackLabel());
			}).bounds(centerX + 2, y + 94, 98, 20).build()
		);

		this.orientationButton = this.addRenderableWidget(
			Button.builder(this.orientationLabel(), button -> {
				this.orientation = this.orientation.next();
				button.setMessage(this.orientationLabel());
			}).bounds(centerX - 100, y + 118, 98, 20).build()
		);

		this.tempoPulseButton = this.addRenderableWidget(
			Button.builder(this.tempoPulseLabel(), button -> {
				this.tempoPulse = !this.tempoPulse;
				button.setMessage(this.tempoPulseLabel());
			}).bounds(centerX + 2, y + 118, 98, 20).build()
		);

		this.idleLightButton = this.addRenderableWidget(
			Button.builder(this.idleLightLabel(), button -> {
				this.idleLight = !this.idleLight;
				button.setMessage(this.idleLightLabel());
			}).bounds(centerX - 100, y + 142, 98, 20).build()
		);

		this.dmxButton = this.addRenderableWidget(
			Button.builder(this.dmxLabel(), button -> {
				this.dmxEnrolled = !this.dmxEnrolled;
				button.setMessage(this.dmxLabel());
			}).bounds(centerX + 2, y + 142, 98, 20).build()
		);

		this.sourceButton = this.addRenderableWidget(
			Button.builder(this.sourceLabel(), button -> {
				if (this.sourcePos != null) {
					this.sourcePos = null;
					button.setMessage(this.sourceLabel());
					PlatformNetwork.sendToServer(new LampSourcePayload(this.pos, null));
				}
			}).bounds(centerX - 100, y + 166, 200, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("screen.beatlamp.unlink"), button -> {
				this.unlink = true;
				this.onClose();
			}).bounds(centerX - 100, y + 190, 98, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(centerX + 2, y + 190, 98, 20).build()
		);
	}

	private Component modeLabel() {
		return Component.translatable("screen.beatlamp.mode", Component.translatable(this.mode.getTranslationKey()));
	}

	private Component colorLabel() {
		int idx = this.colorIndex();
		if (idx == 0) {
			return Component.translatable("screen.beatlamp.color", Component.translatable("screen.beatlamp.color.oled"));
		}
		DyeColor dye = DyeColor.values()[idx - 1];
		return Component.translatable("screen.beatlamp.color", Component.translatable("color.minecraft." + dye.getName()));
	}

	private Component particlesLabel() {
		return Component.translatable("screen.beatlamp.particles", Component.translatable(this.particles.getTranslationKey()));
	}

	private Component framelessLabel() {
		return Component.translatable("screen.beatlamp.frameless", Component.translatable(this.frameless ? "screen.beatlamp.toggle.on" : "screen.beatlamp.toggle.off"));
	}

	private Component blackbackLabel() {
		return Component.translatable("screen.beatlamp.blackback", Component.translatable(this.blackback ? "screen.beatlamp.toggle.on" : "screen.beatlamp.toggle.off"));
	}

	private Component orientationLabel() {
		return Component.translatable("screen.beatlamp.orientation", Component.translatable(this.orientation.getTranslationKey()));
	}

	private Component tempoPulseLabel() {
		return Component.translatable("screen.beatlamp.tempo_pulse", Component.translatable(this.tempoPulse ? "screen.beatlamp.toggle.on" : "screen.beatlamp.toggle.off"));
	}

	private Component idleLightLabel() {
		return Component.translatable("screen.beatlamp.idle_light", Component.translatable(this.idleLight ? "screen.beatlamp.toggle.on" : "screen.beatlamp.toggle.off"));
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
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 124, 0xFFFFFF);
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
			new LampConfigurePayload(
				this.pos, this.mode, this.sensitivity, this.speed, this.color, this.frameless, this.blackback, this.idleLight, this.reverse, this.particles, this.orientation, this.unlink, this.tempoPulse, this.dmxEnrolled, finalName
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

		public void updateVal(float initial) {
			this.value = (initial - this.min) / (this.max - this.min);
			this.updateMessage();
			this.applyValue();
		}
	}
}
