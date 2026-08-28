package com.beatlamp.client.gui;

import com.beatlamp.block.DmxConsoleBlockEntity;
import com.beatlamp.client.DmxMasterTracker;
import com.beatlamp.client.config.BeatLampClientConfig;
import com.beatlamp.network.DmxConsolePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class DmxConsoleScreen extends Screen {
	private final BlockPos pos;
	private final DmxConsoleBlockEntity dmxEntity;
	private boolean blackout;
	private boolean strobeAll;
	private float masterDimmer;
	private float masterSpeed;

	private Button blackoutButton;
	private Button strobeButton;
	private ValueSlider dimmerSlider;
	private ValueSlider speedSlider;
	private Button qualityButton;

	public DmxConsoleScreen(DmxConsoleBlockEntity dmx) {
		super(Component.translatable("screen.beatlamp.dmx.title"));
		this.dmxEntity = dmx;
		this.pos = dmx.getBlockPos().immutable();
		this.blackout = dmx.isBlackout();
		this.strobeAll = dmx.isStrobeAll();
		this.masterDimmer = dmx.getMasterDimmer();
		this.masterSpeed = dmx.getMasterSpeed();
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int y = this.height / 2 - 80;

		this.blackoutButton = this.addRenderableWidget(
			Button.builder(this.blackoutLabel(), button -> {
				this.blackout = !this.blackout;
				button.setMessage(this.blackoutLabel());
				this.sendConfig();
			}).bounds(centerX - 100, y, 200, 24).build()
		);

		this.strobeButton = this.addRenderableWidget(
			Button.builder(this.strobeLabel(), button -> {
				this.strobeAll = !this.strobeAll;
				button.setMessage(this.strobeLabel());
				this.sendConfig();
			}).bounds(centerX - 100, y + 28, 200, 24).build()
		);

		this.dimmerSlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 56, Component.translatable("screen.beatlamp.dmx.dimmer"), this.masterDimmer, 0.0, 1.0, "%d%%") {
				@Override
				protected void applyValue() {
					DmxConsoleScreen.this.masterDimmer = (float) this.value;
					DmxConsoleScreen.this.sendConfig();
				}
			}
		);

		this.speedSlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 80, Component.translatable("screen.beatlamp.speed"), this.masterSpeed, 0.2, 3.0, "%.1fx") {
				@Override
				protected void applyValue() {
					DmxConsoleScreen.this.masterSpeed = (float) Mth.lerp(this.value, 0.2, 3.0);
					DmxConsoleScreen.this.sendConfig();
				}
			}
		);

		this.qualityButton = this.addRenderableWidget(
			Button.builder(this.qualityLabel(), button -> {
				BeatLampClientConfig.setQualityProfile(BeatLampClientConfig.getQualityProfile().next());
				button.setMessage(this.qualityLabel());
			}).bounds(centerX - 100, y + 104, 200, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(centerX - 100, y + 128, 200, 20)
				.build()
		);
	}

	private Component blackoutLabel() {
		if (this.blackout) {
			return Component.literal("⯈ BLACKOUT: ACTIVE ⯇").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
		} else {
			return Component.literal("BLACKOUT: OFF").withStyle(ChatFormatting.GRAY);
		}
	}

	private Component strobeLabel() {
		if (this.strobeAll) {
			return Component.literal("⚡ STROBE ALL: ACTIVE ⚡").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
		} else {
			return Component.literal("STROBE ALL: OFF").withStyle(ChatFormatting.GRAY);
		}
	}

	private Component qualityLabel() {
		BeatLampClientConfig.AudioQualityProfile p = BeatLampClientConfig.getQualityProfile();
		String key = p == BeatLampClientConfig.AudioQualityProfile.LITE ? "screen.beatlamp.quality.lite" : "screen.beatlamp.quality.studio";
		ChatFormatting color = p == BeatLampClientConfig.AudioQualityProfile.LITE ? ChatFormatting.GREEN : ChatFormatting.LIGHT_PURPLE;
		return Component.translatable("screen.beatlamp.dmx.quality", Component.translatable(key).withStyle(color, ChatFormatting.BOLD));
	}

	private void sendConfig() {
		this.dmxEntity.setBlackout(this.blackout);
		this.dmxEntity.setStrobeAll(this.strobeAll);
		this.dmxEntity.setMasterDimmer(this.masterDimmer);
		this.dmxEntity.setMasterSpeed(this.masterSpeed);
		DmxMasterTracker.register(this.dmxEntity);
		ClientPlayNetworking.send(new DmxConsolePayload(this.pos, this.blackout, this.strobeAll, this.masterDimmer, this.masterSpeed));
	}

	@Override
	public void onClose() {
		this.sendConfig();
		super.onClose();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);

		int centerX = this.width / 2;
		int y = this.height / 2 - 80;

		graphics.drawCenteredString(this.font, this.title, centerX, y - 28, 0xFFE0E0E0);
		graphics.drawCenteredString(this.font, Component.literal("Range: 64 Blocks Radius").withStyle(ChatFormatting.DARK_GRAY), centerX, y - 14, 0xFF888888);

		if (this.blackout) {
			graphics.fill(centerX - 104, y - 4, centerX - 101, y + 28, 0xFFFF2020);
			graphics.fill(centerX + 101, y - 4, centerX + 104, y + 28, 0xFFFF2020);
		}

		if (this.strobeAll) {
			graphics.fill(centerX - 104, y + 24, centerX - 101, y + 56, 0xFFFFE020);
			graphics.fill(centerX + 101, y + 24, centerX + 104, y + 56, 0xFFFFE020);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private abstract static class ValueSlider extends AbstractSliderButton {
		private final Component prefix;
		private final double min;
		private final double max;
		private final String format;

		ValueSlider(int x, int y, Component prefix, float initial, double min, double max, String format) {
			super(x, y, 200, 20, Component.empty(), (initial - min) / (max - min));
			this.prefix = prefix;
			this.min = min;
			this.max = max;
			this.format = format;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			double val = Mth.lerp(this.value, this.min, this.max);
			if (this.format.contains("%d")) {
				int percent = (int) Math.round(val * 100.0);
				this.setMessage(Component.translatable("screen.beatlamp.slider.value", this.prefix, percent + "%"));
			} else {
				this.setMessage(Component.translatable("screen.beatlamp.slider.value", this.prefix, String.format(this.format, val)));
			}
		}

		public void updateVal(float initial) {
			this.value = (initial - this.min) / (this.max - this.min);
			this.updateMessage();
			this.applyValue();
		}
	}
}
