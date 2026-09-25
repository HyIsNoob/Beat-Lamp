package com.beatlamp.client.gui;

import com.beatlamp.block.StageJukeboxBlockEntity;
import com.beatlamp.network.StageJukeboxConfigurePayload;
import com.beatlamp.client.PlatformNetwork;
import com.beatlamp.client.audio.MusicDiscMakerAudioBridge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;

public class StageJukeboxConfigScreen extends Screen {
	private final BlockPos pos;
	private int volume;
	private int range;
	private boolean loop;
	private boolean paused;
	private ItemStack record;

	private Button playPauseButton;
	private Button loopButton;
	private Button ejectButton;
	private ValueSlider volumeSlider;
	private ValueSlider rangeSlider;

	public StageJukeboxConfigScreen(StageJukeboxBlockEntity jukebox) {
		super(Component.translatable("screen.beatlamp.stage_jukebox.title"));
		this.pos = jukebox.getBlockPos().immutable();
		this.volume = jukebox.getVolume();
		this.range = jukebox.getRange();
		this.loop = jukebox.isLoop();
		this.paused = jukebox.isPaused();
		this.record = jukebox.getRecord().copy();
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int y = this.height / 2 - 65;

		// 1. Play / Pause & Loop buttons
		this.playPauseButton = this.addRenderableWidget(
			Button.builder(Component.literal(this.paused ? "Play" : "Pause"), b -> {
				this.paused = !this.paused;
				b.setMessage(Component.literal(this.paused ? "Play" : "Pause"));
				PlatformNetwork.sendToServer(new StageJukeboxConfigurePayload(
					this.pos, this.volume, this.range, this.loop, StageJukeboxConfigurePayload.ACTION_TOGGLE_PAUSE, 0
				));
			}).bounds(centerX - 100, y + 26, 98, 20).build()
		);

		this.loopButton = this.addRenderableWidget(
			Button.builder(Component.literal(this.loop ? "Loop: ON" : "Loop: OFF"), b -> {
				this.loop = !this.loop;
				b.setMessage(Component.literal(this.loop ? "Loop: ON" : "Loop: OFF"));
				sendSettings();
			}).bounds(centerX + 2, y + 26, 98, 20).build()
		);

		// 2. Eject button
		this.ejectButton = this.addRenderableWidget(
			Button.builder(Component.literal("Eject"), b -> {
				PlatformNetwork.sendToServer(new StageJukeboxConfigurePayload(
					this.pos, this.volume, this.range, this.loop, StageJukeboxConfigurePayload.ACTION_EJECT, 0
				));
				this.onClose();
			}).bounds(centerX - 100, y + 50, 200, 20).build()
		);

		// 3. Volume slider
		this.volumeSlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 74, 200, 20, Component.translatable("screen.beatlamp.stage_jukebox.volume"), this.volume, 0, 100, "%d%%") {
				@Override
				protected void applyValue() {
					StageJukeboxConfigScreen.this.volume = (int) Math.round(Mth.lerp(this.value, this.min, this.max));
					sendSettings();
				}
			}
		);

		// 4. Range slider
		this.rangeSlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 98, 200, 20, Component.translatable("screen.beatlamp.stage_jukebox.range"), this.range, 16, 128, "%dm") {
				@Override
				protected void applyValue() {
					StageJukeboxConfigScreen.this.range = (int) Math.round(Mth.lerp(this.value, this.min, this.max));
					sendSettings();
				}
			}
		);

		// 5. Done button
		this.addRenderableWidget(
			Button.builder(Component.translatable("gui.done"), b -> this.onClose())
				.bounds(centerX - 100, y + 124, 200, 20).build()
		);
	}

	private void sendSettings() {
		MusicDiscMakerAudioBridge.updateDiscSettings(this.pos, this.volume, this.range);
		PlatformNetwork.sendToServer(new StageJukeboxConfigurePayload(
			this.pos, this.volume, this.range, this.loop, StageJukeboxConfigurePayload.ACTION_UPDATE_SETTINGS, 0
		));
	}

	@Override
	public void tick() {
		super.tick();
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.level.getBlockEntity(this.pos) instanceof StageJukeboxBlockEntity jb) {
			this.record = jb.getRecord();
			this.paused = jb.isPaused();
			this.loop = jb.isLoop();
			if (this.playPauseButton != null) {
				this.playPauseButton.setMessage(Component.literal(this.paused ? "Play" : "Pause"));
			}
			if (this.loopButton != null) {
				this.loopButton.setMessage(Component.literal(this.loop ? "Loop: ON" : "Loop: OFF"));
			}
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		int centerX = this.width / 2;
		int y = this.height / 2 - 65;

		guiGraphics.drawCenteredString(this.font, this.title, centerX, y, 0xFFFFFF);

		String discInfo = resolveDiscTitle(this.record);
		if (!discInfo.isEmpty()) {
			String status = (this.record != null && !this.record.isEmpty()) ?
				(this.paused ? " (" + Component.translatable("screen.beatlamp.stage_jukebox.paused").getString() + ")" :
				" (" + Component.translatable("screen.beatlamp.stage_jukebox.playing").getString() + ")") : "";
			String text = this.font.plainSubstrByWidth(discInfo + status, 240);
			guiGraphics.drawCenteredString(this.font, text, centerX, y + 12, 0xAAAAAA);
		}

		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private String resolveDiscTitle(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return Component.translatable("screen.beatlamp.stage_jukebox.no_disc").getString();
		}
		if (stack.has(DataComponents.JUKEBOX_PLAYABLE)) {
			JukeboxPlayable playable = stack.get(DataComponents.JUKEBOX_PLAYABLE);
			if (playable != null) {
				Minecraft mc = Minecraft.getInstance();
				if (mc.level != null) {
					var holder = playable.song().unwrap(mc.level.registryAccess());
					if (holder.isPresent()) {
						return holder.get().value().description().getString();
					}
				}
			}
		}
		try {
			for (var comp : stack.getComponents()) {
				if (comp.type().toString().contains("custom_track")) {
					Object val = comp.value();
					String title = null;
					String artist = null;
					for (java.lang.reflect.Method m : val.getClass().getMethods()) {
						if ("title".equals(m.getName()) && m.getParameterCount() == 0) {
							Object res = m.invoke(val);
							if (res != null) title = res.toString();
						} else if (("artist".equals(m.getName()) || "author".equals(m.getName())) && m.getParameterCount() == 0) {
							Object res = m.invoke(val);
							if (res != null) artist = res.toString();
						}
					}
					if (title != null && !title.isEmpty()) {
						if (artist != null && !artist.isEmpty()) {
							return artist + " - " + title;
						}
						return title;
					}
				}
			}
		} catch (Throwable ignored) {
		}
		return stack.getHoverName().getString();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private abstract static class ValueSlider extends AbstractSliderButton {
		protected final Component prefix;
		protected final double min;
		protected final double max;
		protected final String format;

		ValueSlider(int x, int y, int width, int height, Component prefix, float initial, double min, double max, String format) {
			super(x, y, width, height, Component.empty(), (initial - min) / (max - min));
			this.prefix = prefix;
			this.min = min;
			this.max = max;
			this.format = format;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int val = (int) Math.round(Mth.lerp(this.value, this.min, this.max));
			this.setMessage(Component.translatable("screen.beatlamp.slider.value", this.prefix, String.format(this.format, val)));
		}
	}
}
