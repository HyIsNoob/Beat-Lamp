package com.beatlamp.client.gui;

import com.beatlamp.block.DmxConsoleBlockEntity;
import com.beatlamp.client.DmxMasterTracker;
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
		int y = this.height / 2 - 70;

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
			}).bounds(centerX - 100, y + 30, 200, 24).build()
		);

		this.dimmerSlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 60, Component.translatable("screen.beatlamp.dmx.dimmer"), this.masterDimmer, 0.0, 1.0, "%d%%") {
				@Override
				protected void applyValue() {
					DmxConsoleScreen.this.masterDimmer = (float) this.value;
					DmxConsoleScreen.this.sendConfig();
				}
			}
		);

		this.speedSlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 86, Component.translatable("screen.beatlamp.speed"), this.masterSpeed, 0.2, 3.0, "%.1fx") {
				@Override
				protected void applyValue() {
					DmxConsoleScreen.this.masterSpeed = (float) Mth.lerp(this.value, 0.2, 3.0);
					DmxConsoleScreen.this.sendConfig();
				}
			}
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(centerX - 100, y + 120, 200, 20)
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
		int y = this.height / 2 - 70;

		graphics.drawCenteredString(this.font, this.title, centerX, y - 28, 0xFFE0E0E0);
		graphics.drawCenteredString(this.font, Component.literal("Range: 64 Blocks Radius").withStyle(ChatFormatting.DARK_GRAY), centerX, y - 14, 0xFF888888);

		if (this.blackout) {
			graphics.fill(centerX - 104, y - 2, centerX + 104, y + 26, 0x40FF0000);
		}
		if (this.strobeAll) {
			graphics.fill(centerX - 104, y + 28, centerX + 104, y + 56, 0x40FFFF00);
		}
	}

	private abstract static class ValueSlider extends AbstractSliderButton {
		private final Component prefix;
		private final double min;
		private final double max;
		private final String format;

		public ValueSlider(int x, int y, Component prefix, double current, double min, double max, String format) {
			super(x, y, 200, 20, Component.empty(), (current - min) / (max - min));
			this.prefix = prefix;
			this.min = min;
			this.max = max;
			this.format = format;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			double val = Mth.lerp(this.value, this.min, this.max);
			String formatted = this.format.contains("%d%%") ? String.format(this.format, (int) (val * 100.0)) : String.format(this.format, val);
			this.setMessage(Component.empty().append(this.prefix).append(": ").append(formatted));
		}
	}
}
