package com.beatlamp.client.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.beatlamp.BeatLamp;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.DmxConsoleBlockEntity;
import com.beatlamp.block.FogDensity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.FountainParticles;
import com.beatlamp.block.LampMode;
import com.beatlamp.block.LampOrientation;
import com.beatlamp.block.LampParticles;
import com.beatlamp.block.LaserMode;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;
import com.beatlamp.block.StageLightMode;
import com.beatlamp.client.DmxMasterTracker;
import com.beatlamp.client.config.BeatLampClientConfig;
import com.beatlamp.network.DmxConsolePayload;
import com.beatlamp.network.FogGeneratorConfigurePayload;
import com.beatlamp.network.FountainConfigurePayload;
import com.beatlamp.network.LampConfigurePayload;
import com.beatlamp.network.LaserProjectorConfigurePayload;
import com.beatlamp.network.StageLightConfigurePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public class DmxConsoleScreen extends Screen {
	private static final int[] PALETTE = new int[] {
		BeatLampBlockEntity.COLOR_OLED,
		0xFF2020, 0xFF7A00, 0xFFE020, 0x55FF20, 0x20FF80,
		0x00E5FF, 0x2080FF, 0x6020FF, 0xDD20FF, 0xFF2080, 0xFFFFFF
	};

	private static final String[] PALETTE_NAMES = new String[] {
		"OLED Rainbow", "Red", "Orange", "Yellow", "Lime", "Green",
		"Cyan", "Blue", "Purple", "Magenta", "Pink", "White"
	};

	public enum GroupType {
		LAMP("Beat Lamp", "💡", 0x33CCFF),
		STAGE_LIGHT("Moving Head", "🔦", 0xFFCC00),
		LASER("Laser Projector", "⚡", 0xFF0055),
		FOUNTAIN("Pyro Fountain", "🎆", 0xFF8800),
		FOG("Fog Generator", "💨", 0xCCCCCC);

		public final String displayName;
		public final String icon;
		public final int color;

		GroupType(String displayName, String icon, int color) {
			this.displayName = displayName;
			this.icon = icon;
			this.color = color;
		}
	}

	public static final class StageGroupInfo {
		public final GroupType type;
		public final BlockPos leadPos;
		public final List<BlockPos> members;
		public final BlockEntity leadEntity;
		public String name;
		public boolean muted = false;

		// Config state
		public int modeIndex = 0;
		public int colorIndex = 0;
		public float sensitivity = 1.0F;
		public float speed = 1.0F;

		public StageGroupInfo(GroupType type, BlockPos leadPos, List<BlockPos> members, BlockEntity leadEntity, int index) {
			this.type = type;
			this.leadPos = leadPos;
			this.members = members;
			this.leadEntity = leadEntity;

			if (members.size() > 1) {
				this.name = type.displayName + " Group (" + members.size() + "x)";
			} else {
				this.name = type.displayName + " #" + index;
			}

			this.initFromEntity();
		}

		private void initFromEntity() {
			if (this.leadEntity instanceof BeatLampBlockEntity lamp) {
				this.modeIndex = lamp.getMode().ordinal();
				this.colorIndex = findPaletteIndex(lamp.getColor());
				this.sensitivity = lamp.getSensitivity();
				this.speed = lamp.getSpeed();
			} else if (this.leadEntity instanceof StageLightBlockEntity light) {
				this.modeIndex = light.getMode().ordinal();
				this.colorIndex = findPaletteIndex(light.getColor());
				this.sensitivity = light.getSensitivity();
				this.speed = light.getSpeed();
			} else if (this.leadEntity instanceof LaserProjectorBlockEntity laser) {
				this.modeIndex = laser.getMode().ordinal();
				this.colorIndex = findPaletteIndex(laser.getColor());
				this.sensitivity = laser.getSpread() / 60.0F;
				this.speed = laser.getSpeed();
			} else if (this.leadEntity instanceof FountainBlockEntity fountain) {
				this.modeIndex = fountain.isFireworkMode() ? 1 : 0;
				this.colorIndex = findPaletteIndex(fountain.getColor());
				this.sensitivity = fountain.getSprayThreshold();
				this.speed = fountain.getImpactThreshold();
			} else if (this.leadEntity instanceof FogGeneratorBlockEntity fog) {
				this.modeIndex = fog.getDensity().ordinal();
				this.colorIndex = findPaletteIndex(fog.getColor());
				this.sensitivity = fog.getRadius() / 16.0F;
			}
		}

		private static int findPaletteIndex(int col) {
			for (int i = 0; i < PALETTE.length; i++) {
				if (PALETTE[i] == col) return i;
			}
			return 0;
		}
	}

	private final BlockPos pos;
	private final DmxConsoleBlockEntity dmxEntity;
	private boolean blackout;
	private boolean strobeAll;
	private float masterDimmer;
	private float masterSpeed;

	private int activeTab = 0; // 0 = Master, 1 = Stage Groups
	private final List<StageGroupInfo> stageGroups = new ArrayList<>();
	private int selectedGroupIndex = 0;

	// Master Widgets
	private Button blackoutButton;
	private Button strobeButton;
	private ValueSlider dimmerSlider;
	private ValueSlider speedSlider;
	private Button qualityButton;

	// Group Widgets
	private Button groupMuteButton;
	private Button groupModeButton;
	private Button groupColorButton;
	private ValueSlider groupSensSlider;
	private ValueSlider groupSpeedSlider;

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
		this.scanStageGroups();
		int centerX = this.width / 2;

		// Tab Buttons
		this.addRenderableWidget(
			Button.builder(Component.literal("🎛️ Master Control"), button -> {
				this.activeTab = 0;
				this.rebuildWidgets();
			}).bounds(centerX - 135, 18, 130, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.literal("📋 Stage Groups (" + this.stageGroups.size() + ")"), button -> {
				this.activeTab = 1;
				this.rebuildWidgets();
			}).bounds(centerX + 5, 18, 130, 20).build()
		);

		if (this.activeTab == 0) {
			this.initMasterTab(centerX);
		} else {
			this.initGroupsTab(centerX);
		}

		// Done / Close Button
		this.addRenderableWidget(
			Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(centerX - 100, this.height - 28, 200, 20)
				.build()
		);
	}

	private void initMasterTab(int centerX) {
		int y = this.height / 2 - 60;

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
	}

	private void initGroupsTab(int centerX) {
		int startY = 48;

		// Left list buttons (up to 6 groups displayed on screen)
		int listX = centerX - 180;
		int listWidth = 140;

		for (int i = 0; i < Math.min(6, this.stageGroups.size()); i++) {
			final int index = i;
			StageGroupInfo g = this.stageGroups.get(i);
			Component label = Component.literal((i == this.selectedGroupIndex ? "▶ " : "") + g.type.icon + " " + g.name);
			this.addRenderableWidget(
				Button.builder(label, button -> {
					this.selectedGroupIndex = index;
					this.rebuildWidgets();
				}).bounds(listX, startY + i * 22, listWidth, 20).build()
			);
		}

		if (this.stageGroups.isEmpty()) {
			return;
		}

		StageGroupInfo g = this.getSelectedGroup();
		if (g == null) return;

		int rightX = centerX - 30;
		int rightWidth = 190;
		int rightY = startY + 16;

		// Mute button
		this.groupMuteButton = this.addRenderableWidget(
			Button.builder(this.groupMuteLabel(g), button -> {
				g.muted = !g.muted;
				button.setMessage(this.groupMuteLabel(g));
				this.dispatchGroupConfig(g);
			}).bounds(rightX, rightY, rightWidth, 20).build()
		);

		// Mode button
		this.groupModeButton = this.addRenderableWidget(
			Button.builder(this.groupModeLabel(g), button -> {
				this.cycleGroupMode(g);
				button.setMessage(this.groupModeLabel(g));
				this.dispatchGroupConfig(g);
			}).bounds(rightX, rightY + 24, rightWidth, 20).build()
		);

		// Color button
		this.groupColorButton = this.addRenderableWidget(
			Button.builder(this.groupColorLabel(g), button -> {
				g.colorIndex = (g.colorIndex + 1) % PALETTE.length;
				button.setMessage(this.groupColorLabel(g));
				this.dispatchGroupConfig(g);
			}).bounds(rightX, rightY + 48, rightWidth, 20).build()
		);

		// Sensitivity slider
		this.groupSensSlider = this.addRenderableWidget(
			new ValueSlider(rightX, rightY + 72, Component.translatable("screen.beatlamp.sensitivity"), g.sensitivity, 0.2, 3.0, "%.1fx") {
				@Override
				protected void applyValue() {
					g.sensitivity = (float) Mth.lerp(this.value, 0.2, 3.0);
					DmxConsoleScreen.this.dispatchGroupConfig(g);
				}
			}
		);

		// Speed slider
		this.groupSpeedSlider = this.addRenderableWidget(
			new ValueSlider(rightX, rightY + 96, Component.translatable("screen.beatlamp.speed"), g.speed, 0.2, 3.0, "%.1fx") {
				@Override
				protected void applyValue() {
					g.speed = (float) Mth.lerp(this.value, 0.2, 3.0);
					DmxConsoleScreen.this.dispatchGroupConfig(g);
				}
			}
		);
	}

	private StageGroupInfo getSelectedGroup() {
		if (this.selectedGroupIndex >= 0 && this.selectedGroupIndex < this.stageGroups.size()) {
			return this.stageGroups.get(this.selectedGroupIndex);
		}
		return null;
	}

	private Component groupMuteLabel(StageGroupInfo g) {
		if (g.muted) {
			return Component.literal("🔇 STATUS: MUTED (OFF)").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
		} else {
			return Component.literal("🔊 STATUS: ACTIVE").withStyle(ChatFormatting.GREEN);
		}
	}

	private Component groupModeLabel(StageGroupInfo g) {
		String modeName = "Mode";
		switch (g.type) {
			case LAMP -> modeName = LampMode.values()[g.modeIndex % LampMode.values().length].getSerializedName().toUpperCase();
			case STAGE_LIGHT -> modeName = StageLightMode.values()[g.modeIndex % StageLightMode.values().length].name();
			case LASER -> modeName = LaserMode.values()[g.modeIndex % LaserMode.values().length].name();
			case FOUNTAIN -> modeName = g.modeIndex == 1 ? "FIREWORK" : "FOUNTAIN";
			case FOG -> modeName = FogDensity.values()[g.modeIndex % FogDensity.values().length].name();
		}
		return Component.literal("Mode: " + modeName).withStyle(ChatFormatting.AQUA);
	}

	private void cycleGroupMode(StageGroupInfo g) {
		switch (g.type) {
			case LAMP -> g.modeIndex = (g.modeIndex + 1) % LampMode.values().length;
			case STAGE_LIGHT -> g.modeIndex = (g.modeIndex + 1) % StageLightMode.values().length;
			case LASER -> g.modeIndex = (g.modeIndex + 1) % LaserMode.values().length;
			case FOUNTAIN -> g.modeIndex = (g.modeIndex + 1) % 2;
			case FOG -> g.modeIndex = (g.modeIndex + 1) % FogDensity.values().length;
		}
	}

	private Component groupColorLabel(StageGroupInfo g) {
		String name = PALETTE_NAMES[g.colorIndex % PALETTE_NAMES.length];
		return Component.literal("Color: " + name).withStyle(ChatFormatting.GOLD);
	}

	private void dispatchGroupConfig(StageGroupInfo g) {
		int targetColor = g.muted ? 0x000000 : PALETTE[g.colorIndex % PALETTE.length];
		float targetSens = g.muted ? 0.0F : g.sensitivity;

		switch (g.type) {
			case LAMP -> {
				LampMode mode = LampMode.values()[g.modeIndex % LampMode.values().length];
				boolean frameless = true;
				boolean blackback = true;
				boolean idleLight = false;
				boolean reverse = false;
				LampParticles particles = LampParticles.NOTE;
				LampOrientation orientation = LampOrientation.AUTO;
				boolean tempoPulse = true;

				if (g.leadEntity instanceof BeatLampBlockEntity lamp) {
					frameless = lamp.isFrameless();
					blackback = lamp.isBlackback();
					idleLight = lamp.isIdleLight();
					reverse = lamp.isReverse();
					particles = lamp.getParticles();
					orientation = lamp.getOrientation();
					tempoPulse = lamp.isTempoPulse();
				}

				ClientPlayNetworking.send(new LampConfigurePayload(
					g.leadPos, mode, targetSens, g.speed, targetColor, frameless, blackback, idleLight, reverse, particles, orientation, false, tempoPulse
				));
			}
			case STAGE_LIGHT -> {
				StageLightMode mode = StageLightMode.values()[g.modeIndex % StageLightMode.values().length];
				ClientPlayNetworking.send(new StageLightConfigurePayload(
					g.leadPos, mode, targetSens, g.speed, targetColor, false
				));
			}
			case LASER -> {
				LaserMode mode = LaserMode.values()[g.modeIndex % LaserMode.values().length];
				int count = 4;
				float spread = 45.0F;
				if (g.leadEntity instanceof LaserProjectorBlockEntity laser) {
					count = laser.getBeamCount();
					spread = laser.getSpread();
				}
				ClientPlayNetworking.send(new LaserProjectorConfigurePayload(
					g.leadPos, mode, count, spread, g.speed, targetColor, false
				));
			}
			case FOUNTAIN -> {
				boolean fw = g.modeIndex == 1;
				float spray = targetSens;
				float impact = g.speed;
				FountainParticles pt = FountainParticles.FLAME;
				if (g.leadEntity instanceof FountainBlockEntity fountain) {
					pt = fountain.getParticleType();
				}
				ClientPlayNetworking.send(new FountainConfigurePayload(
					g.leadPos, fw, spray, impact, true, pt, targetColor, false
				));
			}
			case FOG -> {
				FogDensity density = FogDensity.values()[g.modeIndex % FogDensity.values().length];
				int radius = (int) (g.sensitivity * 16.0F);
				ClientPlayNetworking.send(new FogGeneratorConfigurePayload(
					g.leadPos, density, radius, targetColor, false
				));
			}
		}
	}

	private void scanStageGroups() {
		this.stageGroups.clear();
		Level level = Minecraft.getInstance().level;
		if (level == null) return;

		int minChunkX = (this.pos.getX() - 64) >> 4;
		int maxChunkX = (this.pos.getX() + 64) >> 4;
		int minChunkZ = (this.pos.getZ() - 64) >> 4;
		int maxChunkZ = (this.pos.getZ() + 64) >> 4;

		Set<BlockPos> processed = new HashSet<>();
		int lampCount = 0, lightCount = 0, laserCount = 0, fountainCount = 0, fogCount = 0;

		for (int cx = minChunkX; cx <= maxChunkX; cx++) {
			for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
				if (!level.hasChunk(cx, cz)) continue;
				LevelChunk chunk = level.getChunk(cx, cz);
				for (BlockPos bePos : chunk.getBlockEntitiesPos()) {
					if (bePos.distSqr(this.pos) > 64 * 64) continue;
					if (processed.contains(bePos)) continue;

					BlockEntity be = chunk.getBlockEntity(bePos);
					if (be instanceof BeatLampBlockEntity lamp) {
						List<BlockPos> members = lamp.getManualGroup().size() >= 2 ? lamp.getManualGroup() : BeatLamp.floodFill(level, bePos);
						processed.addAll(members);
						lampCount++;
						this.stageGroups.add(new StageGroupInfo(GroupType.LAMP, bePos, members, lamp, lampCount));
					} else if (be instanceof StageLightBlockEntity light) {
						List<BlockPos> members = light.getManualGroup().size() >= 2 ? light.getManualGroup() : List.of(bePos);
						processed.addAll(members);
						lightCount++;
						this.stageGroups.add(new StageGroupInfo(GroupType.STAGE_LIGHT, bePos, members, light, lightCount));
					} else if (be instanceof LaserProjectorBlockEntity laser) {
						List<BlockPos> members = laser.getManualGroup().size() >= 2 ? laser.getManualGroup() : List.of(bePos);
						processed.addAll(members);
						laserCount++;
						this.stageGroups.add(new StageGroupInfo(GroupType.LASER, bePos, members, laser, laserCount));
					} else if (be instanceof FountainBlockEntity fountain) {
						List<BlockPos> members = fountain.getManualGroup().size() >= 2 ? fountain.getManualGroup() : List.of(bePos);
						processed.addAll(members);
						fountainCount++;
						this.stageGroups.add(new StageGroupInfo(GroupType.FOUNTAIN, bePos, members, fountain, fountainCount));
					} else if (be instanceof FogGeneratorBlockEntity fog) {
						List<BlockPos> members = fog.getManualGroup().size() >= 2 ? fog.getManualGroup() : List.of(bePos);
						processed.addAll(members);
						fogCount++;
						this.stageGroups.add(new StageGroupInfo(GroupType.FOG, bePos, members, fog, fogCount));
					}
				}
			}
		}
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

		if (this.activeTab == 0) {
			int y = this.height / 2 - 60;
			graphics.drawCenteredString(this.font, Component.literal("Master Stage Lighting Controls").withStyle(ChatFormatting.GRAY), centerX, y - 14, 0xFF888888);

			if (this.blackout) {
				graphics.fill(centerX - 104, y - 4, centerX - 101, y + 28, 0xFFFF2020);
				graphics.fill(centerX + 101, y - 4, centerX + 104, y + 28, 0xFFFF2020);
			}

			if (this.strobeAll) {
				graphics.fill(centerX - 104, y + 24, centerX - 101, y + 56, 0xFFFFE020);
				graphics.fill(centerX + 101, y + 24, centerX + 104, y + 56, 0xFFFFE020);
			}
		} else {
			// Groups Tab Header & Realtime status
			int rightX = centerX - 30;
			StageGroupInfo g = this.getSelectedGroup();
			if (g != null) {
				graphics.drawString(this.font, Component.literal("⚙ " + g.name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), rightX, 48, 0xFFFFFFFF);
				graphics.drawString(this.font, Component.literal("● REALTIME SYNC (Changes apply instantly)").withStyle(ChatFormatting.GREEN), rightX, 172, 0xFF88FF88);
			} else if (this.stageGroups.isEmpty()) {
				graphics.drawCenteredString(this.font, Component.literal("No stage devices found within 64 blocks").withStyle(ChatFormatting.RED), centerX, 100, 0xFFFF6666);
			}
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
			super(x, y, 190, 20, Component.empty(), (initial - min) / (max - min));
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
