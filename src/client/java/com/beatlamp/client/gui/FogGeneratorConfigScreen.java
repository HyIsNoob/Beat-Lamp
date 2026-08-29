package com.beatlamp.client.gui;

import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.FogDensity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.network.FogGeneratorConfigurePayload;
import com.beatlamp.network.LampSourcePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

public class FogGeneratorConfigScreen extends Screen {
	private static final int[] PALETTE = buildPalette();

	private final BlockPos pos;
	private FogDensity density;
	private int radius;
	private int color;
	private boolean unlink;
	private boolean dmxEnrolled;
	private String customName;
	private BlockPos sourcePos;

	private EditBox nameBox;
	private Button densityButton;
	private Button radiusButton;
	private Button dmxButton;
	private Button colorButton;
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
		int y = this.height / 2 - 88;

		this.nameBox = new EditBox(this.font, centerX - 100, y, 200, 18, Component.literal("Group Name"));
		this.nameBox.setValue(this.customName);
		this.nameBox.setHint(Component.translatable("screen.beatlamp.name_hint"));
		this.nameBox.setMaxLength(32);
		this.addRenderableWidget(this.nameBox);

		this.densityButton = this.addRenderableWidget(
			Button.builder(this.densityLabel(), button -> {
				this.density = this.density.next();
				button.setMessage(this.densityLabel());
			}).bounds(centerX - 100, y + 22, 200, 20).build()
		);

		this.radiusButton = this.addRenderableWidget(
			Button.builder(this.radiusLabel(), button -> {
				int[] radii = {4, 8, 12, 16};
				int idx = 0;
				for (int i = 0; i < radii.length; i++) {
					if (radii[i] == this.radius) {
						idx = (i + 1) % radii.length;
						break;
					}
				}
				this.radius = radii[idx];
				button.setMessage(this.radiusLabel());
			}).bounds(centerX - 100, y + 44, 98, 20).build()
		);

		this.dmxButton = this.addRenderableWidget(
			Button.builder(this.dmxLabel(), button -> {
				this.dmxEnrolled = !this.dmxEnrolled;
				button.setMessage(this.dmxLabel());
			}).bounds(centerX + 2, y + 44, 98, 20).build()
		);

		this.colorButton = this.addRenderableWidget(
			Button.builder(this.colorLabel(), button -> {
				int index = this.colorIndex();
				this.color = PALETTE[(index + 1) % PALETTE.length];
				button.setMessage(this.colorLabel());
			}).bounds(centerX - 100, y + 66, 200, 20).build()
		);

		this.sourceButton = this.addRenderableWidget(
			Button.builder(this.sourceLabel(), button -> {
				if (this.sourcePos != null) {
					ClientPlayNetworking.send(new LampSourcePayload(this.pos));
					this.sourcePos = null;
					button.setMessage(this.sourceLabel());
				}
			}).bounds(centerX - 100, y + 88, 200, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.translatable("screen.beatlamp.reset"), button -> {
				this.density = FogDensity.MEDIUM;
				this.radius = 8;
				this.color = BeatLampBlockEntity.COLOR_OLED;
				this.densityButton.setMessage(this.densityLabel());
				this.radiusButton.setMessage(this.radiusLabel());
				this.colorButton.setMessage(this.colorLabel());
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

	private Component densityLabel() {
		return Component.translatable("screen.beatlamp.fog.density", this.density.getDisplayName());
	}

	private Component radiusLabel() {
		return Component.translatable("screen.beatlamp.fog.radius", this.radius);
	}

	private Component colorLabel() {
		if (this.color == BeatLampBlockEntity.COLOR_OLED) {
			return Component.translatable("screen.beatlamp.color", Component.translatable("screen.beatlamp.fog.color.white"));
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
		ClientPlayNetworking.send(new FogGeneratorConfigurePayload(
			this.pos,
			this.density,
			this.radius,
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
}
