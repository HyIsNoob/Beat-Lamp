package com.beatlamp.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
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
import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.EmitterMode;
import com.beatlamp.network.EmitterConfigurePayload;
import com.beatlamp.network.FogGeneratorConfigurePayload;
import com.beatlamp.network.FountainConfigurePayload;
import com.beatlamp.network.LampConfigurePayload;
import com.beatlamp.network.LaserProjectorConfigurePayload;
import com.beatlamp.network.StageLightConfigurePayload;
import com.beatlamp.client.PlatformNetwork;

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
		LAMP("Beat Lamp"),
		STAGE_LIGHT("Moving Head"),
		LASER("Laser"),
		FOUNTAIN("Fountain"),
		FOG("Fog Generator"),
		EMITTER("Beat Emitter");

		public final String displayName;

		GroupType(String displayName) {
			this.displayName = displayName;
		}
	}

	public static final class StageGroupInfo {
		public final GroupType type;
		public final BlockPos leadPos;
		public final List<BlockPos> members;
		public final BlockEntity leadEntity;
		public String name;
		public boolean muted = false;
		public boolean pinned = false;

		public int modeIndex = 0;
		public int colorIndex = 0;
		public float sensitivity = 1.0F;
		public float speed = 1.0F;

		public StageGroupInfo(GroupType type, BlockPos leadPos, List<BlockPos> members, BlockEntity leadEntity, int index) {
			this.type = type;
			this.leadPos = leadPos;
			this.members = members;
			this.leadEntity = leadEntity;

			String custom = getCustomNameFromEntity(leadEntity);
			if (custom != null && !custom.isBlank()) {
				this.name = custom;
			} else if (members.size() > 1) {
				this.name = type.displayName + " (" + members.size() + "x)";
			} else {
				this.name = type.displayName + " #" + index;
			}

			this.initFromEntity();
		}

		public static String getCustomNameFromEntity(BlockEntity be) {
			if (be instanceof BeatLampBlockEntity lamp) return lamp.getCustomName();
			if (be instanceof StageLightBlockEntity light) return light.getCustomName();
			if (be instanceof LaserProjectorBlockEntity laser) return laser.getCustomName();
			if (be instanceof FountainBlockEntity fountain) return fountain.getCustomName();
			if (be instanceof FogGeneratorBlockEntity fog) return fog.getCustomName();
			if (be instanceof BeatEmitterBlockEntity emitter) return emitter.getCustomName();
			return "";
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
			} else if (this.leadEntity instanceof BeatEmitterBlockEntity emitter) {
				this.modeIndex = emitter.getMode().ordinal();
				this.colorIndex = 1;
				this.sensitivity = emitter.getThreshold();
				this.speed = emitter.isInverted() ? 1.0F : 0.0F;
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
	private final List<StageGroupInfo> rawGroups = new ArrayList<>();
	private final List<StageGroupInfo> visibleGroups = new ArrayList<>();
	private static final Set<BlockPos> PINNED_POSITIONS = new HashSet<>();
	private boolean filterLinkedOnly = true;
	private int selectedGroupIndex = 0;
	private int groupListPage = 0;
	private static final int GROUPS_PER_PAGE = 5;

	// Master Widgets
	private Button blackoutButton;
	private Button strobeButton;
	private ValueSlider dimmerSlider;
	private ValueSlider speedSlider;
	private Button qualityButton;

	// Group Widgets
	private Button groupPinButton;
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

		// Tab Navigation Buttons
		this.addRenderableWidget(
			Button.builder(Component.literal("Master Control"), button -> {
				this.activeTab = 0;
				this.rebuildWidgets();
			}).bounds(centerX - 135, 16, 130, 20).build()
		);

		this.addRenderableWidget(
			Button.builder(Component.literal("Stage Groups (" + this.visibleGroups.size() + ")"), button -> {
				this.activeTab = 1;
				this.rebuildWidgets();
			}).bounds(centerX + 5, 16, 130, 20).build()
		);

		if (this.activeTab == 0) {
			this.initMasterTab(centerX);
		} else {
			this.initGroupsTab(centerX);
		}

		// Done / Close Button
		this.addRenderableWidget(
			Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(centerX - 100, this.height - 26, 200, 20)
				.build()
		);
	}

	private void initMasterTab(int centerX) {
		int y = this.height / 2 - 56;
		int width = 200;

		this.blackoutButton = this.addRenderableWidget(
			Button.builder(this.blackoutLabel(), button -> {
				this.blackout = !this.blackout;
				button.setMessage(this.blackoutLabel());
				this.sendConfig();
			}).bounds(centerX - 100, y, width, 22).build()
		);

		this.strobeButton = this.addRenderableWidget(
			Button.builder(this.strobeLabel(), button -> {
				this.strobeAll = !this.strobeAll;
				button.setMessage(this.strobeLabel());
				this.sendConfig();
			}).bounds(centerX - 100, y + 26, width, 22).build()
		);

		this.dimmerSlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 52, width, 20, Component.translatable("screen.beatlamp.dmx.dimmer"), this.masterDimmer, 0.0, 1.0, "%d%%") {
				@Override
				protected void applyValue() {
					DmxConsoleScreen.this.masterDimmer = (float) this.value;
					DmxConsoleScreen.this.sendConfig();
				}
			}
		);

		this.speedSlider = this.addRenderableWidget(
			new ValueSlider(centerX - 100, y + 76, width, 20, Component.translatable("screen.beatlamp.speed"), this.masterSpeed, 0.2, 3.0, "%.1fx") {
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
			}).bounds(centerX - 100, y + 100, width, 20).build()
		);
	}

	private void initGroupsTab(int centerX) {
		int startY = 44;
		int listX = centerX - 180;
		int listWidth = 140;

		// Filter toggle button above list
		this.addRenderableWidget(
			Button.builder(Component.literal(this.filterLinkedOnly ? "Show: Linked" : "Show: All"), button -> {
				this.filterLinkedOnly = !this.filterLinkedOnly;
				this.rebuildVisibleGroups();
				this.selectedGroupIndex = 0;
				this.groupListPage = 0;
				this.rebuildWidgets();
			}).bounds(listX, startY, listWidth, 18).build()
		);

		int listStartY = startY + 22;
		int totalPages = Math.max(1, (this.visibleGroups.size() + GROUPS_PER_PAGE - 1) / GROUPS_PER_PAGE);
		if (this.groupListPage >= totalPages) this.groupListPage = totalPages - 1;
		if (this.groupListPage < 0) this.groupListPage = 0;

		int startIdx = this.groupListPage * GROUPS_PER_PAGE;
		int endIdx = Math.min(this.visibleGroups.size(), startIdx + GROUPS_PER_PAGE);

		for (int i = startIdx; i < endIdx; i++) {
			final int index = i;
			StageGroupInfo g = this.visibleGroups.get(i);
			boolean isSelected = (i == this.selectedGroupIndex);
			String prefix = (g.pinned ? "* " : "") + (isSelected ? "> " : "");
			Component label = Component.literal(prefix + g.name);

			this.addRenderableWidget(
				Button.builder(label, button -> {
					this.selectedGroupIndex = index;
					this.rebuildWidgets();
				}).bounds(listX, listStartY + (i - startIdx) * 22, listWidth, 20).build()
			);
		}

		// Pagination controls
		if (totalPages > 1) {
			this.addRenderableWidget(
				Button.builder(Component.literal("<"), button -> {
					if (this.groupListPage > 0) {
						this.groupListPage--;
						this.rebuildWidgets();
					}
				}).bounds(listX, listStartY + 114, 38, 18).build()
			);

			this.addRenderableWidget(
				Button.builder(Component.literal(">"), button -> {
					if (this.groupListPage < totalPages - 1) {
						this.groupListPage++;
						this.rebuildWidgets();
					}
				}).bounds(listX + listWidth - 38, listStartY + 114, 38, 18).build()
			);
		}

		if (this.visibleGroups.isEmpty()) {
			return;
		}

		StageGroupInfo g = this.getSelectedGroup();
		if (g == null) return;

		int rightX = centerX - 25;
		int rightWidth = 190;
		int rightY = startY;

		// Pin / Unpin button
		this.groupPinButton = this.addRenderableWidget(
			Button.builder(Component.literal(g.pinned ? "Pinned to Top: Yes" : "Pinned to Top: No"), button -> {
				g.pinned = !g.pinned;
				if (g.pinned) {
					PINNED_POSITIONS.add(g.leadPos);
				} else {
					PINNED_POSITIONS.remove(g.leadPos);
				}
				this.rebuildVisibleGroups();
				this.rebuildWidgets();
			}).bounds(rightX, rightY, rightWidth, 20).build()
		);

		// Mute button
		this.groupMuteButton = this.addRenderableWidget(
			Button.builder(this.groupMuteLabel(g), button -> {
				g.muted = !g.muted;
				button.setMessage(this.groupMuteLabel(g));
				this.dispatchGroupConfig(g);
			}).bounds(rightX, rightY + 24, rightWidth, 20).build()
		);

		// Mode button
		this.groupModeButton = this.addRenderableWidget(
			Button.builder(this.groupModeLabel(g), button -> {
				this.cycleGroupMode(g);
				button.setMessage(this.groupModeLabel(g));
				this.dispatchGroupConfig(g);
			}).bounds(rightX, rightY + 48, rightWidth, 20).build()
		);

		// Color button
		this.groupColorButton = this.addRenderableWidget(
			Button.builder(this.groupColorLabel(g), button -> {
				g.colorIndex = (g.colorIndex + 1) % PALETTE.length;
				button.setMessage(this.groupColorLabel(g));
				this.dispatchGroupConfig(g);
			}).bounds(rightX, rightY + 72, rightWidth, 20).build()
		);

		// Sensitivity slider
		this.groupSensSlider = this.addRenderableWidget(
			new ValueSlider(rightX, rightY + 96, rightWidth, 20, Component.translatable("screen.beatlamp.sensitivity"), g.sensitivity, 0.2, 3.0, "%.1fx") {
				@Override
				protected void applyValue() {
					g.sensitivity = (float) Mth.lerp(this.value, 0.2, 3.0);
					DmxConsoleScreen.this.dispatchGroupConfig(g);
				}
			}
		);

		// Speed slider
		this.groupSpeedSlider = this.addRenderableWidget(
			new ValueSlider(rightX, rightY + 120, rightWidth, 20, Component.translatable("screen.beatlamp.speed"), g.speed, 0.2, 3.0, "%.1fx") {
				@Override
				protected void applyValue() {
					g.speed = (float) Mth.lerp(this.value, 0.2, 3.0);
					DmxConsoleScreen.this.dispatchGroupConfig(g);
				}
			}
		);
	}

	private StageGroupInfo getSelectedGroup() {
		if (this.selectedGroupIndex >= 0 && this.selectedGroupIndex < this.visibleGroups.size()) {
			return this.visibleGroups.get(this.selectedGroupIndex);
		}
		return null;
	}

	private Component groupMuteLabel(StageGroupInfo g) {
		if (g.muted) {
			return Component.literal("Status: Muted").withStyle(ChatFormatting.RED);
		} else {
			return Component.literal("Status: Enabled").withStyle(ChatFormatting.GREEN);
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
			case EMITTER -> modeName = EmitterMode.values()[g.modeIndex % EmitterMode.values().length].name();
		}
		return Component.literal("Mode: " + modeName);
	}

	private void cycleGroupMode(StageGroupInfo g) {
		switch (g.type) {
			case LAMP -> g.modeIndex = (g.modeIndex + 1) % LampMode.values().length;
			case STAGE_LIGHT -> g.modeIndex = (g.modeIndex + 1) % StageLightMode.values().length;
			case LASER -> g.modeIndex = (g.modeIndex + 1) % LaserMode.values().length;
			case FOUNTAIN -> g.modeIndex = (g.modeIndex + 1) % 2;
			case FOG -> g.modeIndex = (g.modeIndex + 1) % FogDensity.values().length;
			case EMITTER -> g.modeIndex = (g.modeIndex + 1) % EmitterMode.values().length;
		}
	}

	private Component groupColorLabel(StageGroupInfo g) {
		String name = PALETTE_NAMES[g.colorIndex % PALETTE_NAMES.length];
		return Component.literal("Color: " + name);
	}

	private void dispatchGroupConfig(StageGroupInfo g) {
		int targetColor = g.muted ? 0x000000 : PALETTE[g.colorIndex % PALETTE.length];
		float targetSens = g.muted ? 0.0F : g.sensitivity;
		String customName = StageGroupInfo.getCustomNameFromEntity(g.leadEntity);

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

				PlatformNetwork.sendToServer(new LampConfigurePayload(
					g.leadPos, mode, targetSens, g.speed, targetColor, frameless, blackback, idleLight, reverse, particles, orientation, false, tempoPulse, true, customName
				));
			}
			case STAGE_LIGHT -> {
				StageLightMode mode = StageLightMode.values()[g.modeIndex % StageLightMode.values().length];
				boolean tempoPulse = true;
				if (g.leadEntity instanceof StageLightBlockEntity light) {
					tempoPulse = light.isTempoPulse();
				}
				PlatformNetwork.sendToServer(new StageLightConfigurePayload(
					g.leadPos, mode, targetSens, g.speed, targetColor, false, tempoPulse, true, customName
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
				PlatformNetwork.sendToServer(new LaserProjectorConfigurePayload(
					g.leadPos, mode, count, spread, g.speed, targetColor, false, true, customName
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
				PlatformNetwork.sendToServer(new FountainConfigurePayload(
					g.leadPos, fw, spray, impact, true, pt, targetColor, false, true, customName
				));
			}
			case FOG -> {
				FogDensity density = FogDensity.values()[g.modeIndex % FogDensity.values().length];
				int radius = (int) (g.sensitivity * 16.0F);
				PlatformNetwork.sendToServer(new FogGeneratorConfigurePayload(
					g.leadPos, density, radius, targetColor, false, true, customName
				));
			}
			case EMITTER -> {
				EmitterMode mode = EmitterMode.values()[g.modeIndex % EmitterMode.values().length];
				boolean inverted = g.speed > 0.5F;
				PlatformNetwork.sendToServer(new EmitterConfigurePayload(
					g.leadPos, mode, targetSens, inverted, false, true, customName
				));
			}
		}
	}

	private void scanStageGroups() {
		this.rawGroups.clear();
		Level level = Minecraft.getInstance().level;
		if (level == null) return;

		int minChunkX = (this.pos.getX() - 64) >> 4;
		int maxChunkX = (this.pos.getX() + 64) >> 4;
		int minChunkZ = (this.pos.getZ() - 64) >> 4;
		int maxChunkZ = (this.pos.getZ() + 64) >> 4;

		Set<BlockPos> processed = new HashSet<>();
		int lampCount = 0, lightCount = 0, laserCount = 0, fountainCount = 0, fogCount = 0, emitterCount = 0;

		for (int cx = minChunkX; cx <= maxChunkX; cx++) {
			for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
				if (!level.hasChunk(cx, cz)) continue;
				LevelChunk chunk = level.getChunk(cx, cz);
				for (BlockPos bePos : chunk.getBlockEntitiesPos()) {
					if (bePos.distSqr(this.pos) > 64 * 64) continue;
					if (processed.contains(bePos)) continue;

					BlockEntity be = chunk.getBlockEntity(bePos);
					if (be instanceof BeatLampBlockEntity lamp) {
						if (!lamp.isDmxEnrolled()) continue;
						List<BlockPos> members = lamp.getManualGroup().size() >= 2 ? lamp.getManualGroup() : BeatLamp.floodFill(level, bePos);
						processed.addAll(members);
						lampCount++;
						StageGroupInfo group = new StageGroupInfo(GroupType.LAMP, bePos, members, lamp, lampCount);
						group.pinned = PINNED_POSITIONS.contains(bePos);
						this.rawGroups.add(group);
					} else if (be instanceof StageLightBlockEntity light) {
						if (!light.isDmxEnrolled()) continue;
						List<BlockPos> members = light.getManualGroup().size() >= 2 ? light.getManualGroup() : List.of(bePos);
						processed.addAll(members);
						lightCount++;
						StageGroupInfo group = new StageGroupInfo(GroupType.STAGE_LIGHT, bePos, members, light, lightCount);
						group.pinned = PINNED_POSITIONS.contains(bePos);
						this.rawGroups.add(group);
					} else if (be instanceof LaserProjectorBlockEntity laser) {
						if (!laser.isDmxEnrolled()) continue;
						List<BlockPos> members = laser.getManualGroup().size() >= 2 ? laser.getManualGroup() : List.of(bePos);
						processed.addAll(members);
						laserCount++;
						StageGroupInfo group = new StageGroupInfo(GroupType.LASER, bePos, members, laser, laserCount);
						group.pinned = PINNED_POSITIONS.contains(bePos);
						this.rawGroups.add(group);
					} else if (be instanceof FountainBlockEntity fountain) {
						if (!fountain.isDmxEnrolled()) continue;
						List<BlockPos> members = fountain.getManualGroup().size() >= 2 ? fountain.getManualGroup() : List.of(bePos);
						processed.addAll(members);
						fountainCount++;
						StageGroupInfo group = new StageGroupInfo(GroupType.FOUNTAIN, bePos, members, fountain, fountainCount);
						group.pinned = PINNED_POSITIONS.contains(bePos);
						this.rawGroups.add(group);
					} else if (be instanceof FogGeneratorBlockEntity fog) {
						if (!fog.isDmxEnrolled()) continue;
						List<BlockPos> members = fog.getManualGroup().size() >= 2 ? fog.getManualGroup() : List.of(bePos);
						processed.addAll(members);
						fogCount++;
						StageGroupInfo group = new StageGroupInfo(GroupType.FOG, bePos, members, fog, fogCount);
						group.pinned = PINNED_POSITIONS.contains(bePos);
						this.rawGroups.add(group);
					} else if (be instanceof BeatEmitterBlockEntity emitter) {
						if (!emitter.isDmxEnrolled()) continue;
						List<BlockPos> members = emitter.getManualGroup().size() >= 2 ? emitter.getManualGroup() : List.of(bePos);
						processed.addAll(members);
						emitterCount++;
						StageGroupInfo group = new StageGroupInfo(GroupType.EMITTER, bePos, members, emitter, emitterCount);
						group.pinned = PINNED_POSITIONS.contains(bePos);
						this.rawGroups.add(group);
					}
				}
			}
		}

		// Sort closest to DMX Console first, with pinned groups prioritized
		this.rawGroups.sort(Comparator
			.comparing((StageGroupInfo g) -> !g.pinned)
			.thenComparingDouble(g -> g.leadPos.distSqr(this.pos))
		);

		this.rebuildVisibleGroups();
	}

	private void rebuildVisibleGroups() {
		this.visibleGroups.clear();
		for (StageGroupInfo g : this.rawGroups) {
			if (this.filterLinkedOnly && g.members.size() <= 1 && !g.pinned) {
				continue;
			}
			this.visibleGroups.add(g);
		}
	}

	private Component blackoutLabel() {
		if (this.blackout) {
			return Component.literal("Blackout: ON").withStyle(ChatFormatting.RED);
		} else {
			return Component.literal("Blackout: OFF");
		}
	}

	private Component strobeLabel() {
		if (this.strobeAll) {
			return Component.literal("Strobe All: ON").withStyle(ChatFormatting.YELLOW);
		} else {
			return Component.literal("Strobe All: OFF");
		}
	}

	private Component qualityLabel() {
		BeatLampClientConfig.AudioQualityProfile p = BeatLampClientConfig.getQualityProfile();
		String key = p == BeatLampClientConfig.AudioQualityProfile.LITE ? "screen.beatlamp.quality.lite" : "screen.beatlamp.quality.studio";
		return Component.translatable("screen.beatlamp.dmx.quality", Component.translatable(key));
	}

	private void sendConfig() {
		this.dmxEntity.setBlackout(this.blackout);
		this.dmxEntity.setStrobeAll(this.strobeAll);
		this.dmxEntity.setMasterDimmer(this.masterDimmer);
		this.dmxEntity.setMasterSpeed(this.masterSpeed);
		DmxMasterTracker.register(this.dmxEntity);
		PlatformNetwork.sendToServer(new DmxConsolePayload(this.pos, this.blackout, this.strobeAll, this.masterDimmer, this.masterSpeed));
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
			int y = this.height / 2 - 56;
			if (this.blackout) {
				graphics.fill(centerX - 104, y - 2, centerX - 102, y + 24, 0xFFFF2020);
				graphics.fill(centerX + 102, y - 2, centerX + 104, y + 24, 0xFFFF2020);
			}

			if (this.strobeAll) {
				graphics.fill(centerX - 104, y + 24, centerX - 102, y + 50, 0xFFFFE020);
				graphics.fill(centerX + 102, y + 24, centerX + 104, y + 50, 0xFFFFE020);
			}
		} else {
			int totalPages = Math.max(1, (this.visibleGroups.size() + GROUPS_PER_PAGE - 1) / GROUPS_PER_PAGE);
			if (totalPages > 1) {
				String pageStr = (this.groupListPage + 1) + " / " + totalPages;
				graphics.drawCenteredString(this.font, pageStr, centerX - 110, 44 + 22 + 119, 0xFF888888);
			}

			if (this.visibleGroups.isEmpty()) {
				graphics.drawCenteredString(this.font, Component.literal("No linked stage groups found").withStyle(ChatFormatting.GRAY), centerX - 110, 100, 0xFF888888);
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
