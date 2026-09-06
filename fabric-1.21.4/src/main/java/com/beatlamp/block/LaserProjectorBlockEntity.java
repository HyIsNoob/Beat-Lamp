package com.beatlamp.block;

import java.util.Collections;
import java.util.List;

import com.beatlamp.BeatLampBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class LaserProjectorBlockEntity extends BlockEntity {
	public interface ClientTicker {
		void tick(LaserProjectorBlockEntity laser);
	}

	public interface ControllerUser {
		void use(LaserProjectorBlockEntity laser);
	}

	public static ClientTicker clientTicker = laser -> {
	};

	public static ControllerUser controllerUser = laser -> {
	};

	private LaserMode mode = LaserMode.FAN_SWEEP;
	private int beamCount = 4;
	private float spread = 45.0F; // Fan spread angle: 10 - 120 deg
	private float speed = 1.0F; // 0.1 - 3.0
	private int color = BeatLampBlockEntity.COLOR_OLED;
	private boolean dmxEnrolled = false;
	private String customName = "";
	private BlockPos sourcePos;

	public boolean isDmxEnrolled() {
		return this.dmxEnrolled;
	}

	public void setDmxEnrolled(boolean dmxEnrolled) {
		this.dmxEnrolled = dmxEnrolled;
		this.markUpdated();
	}

	public String getCustomName() {
		return this.customName;
	}

	public void setCustomName(String customName) {
		this.customName = customName == null ? "" : customName.trim();
		this.markUpdated();
	}
	private final List<BlockPos> manualGroup = new java.util.ArrayList<>();

	// Dynamic client-side animation state
	public float currentAngle;
	public float burstExpansion = 1.0F;
	public float targetBurst = 1.0F;
	public float activeIntensity = 0.0F;

	public LaserProjectorBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BeatLampBlockEntities.LASER_PROJECTOR, blockPos, blockState);
	}

	public LaserMode getMode() {
		return this.mode;
	}

	public void setMode(LaserMode mode) {
		this.mode = mode == null ? LaserMode.FAN_SWEEP : mode;
		this.markUpdated();
	}

	public int getBeamCount() {
		return this.beamCount;
	}

	public void setBeamCount(int beamCount) {
		this.beamCount = Math.clamp(beamCount, 1, 8);
		this.markUpdated();
	}

	public float getSpread() {
		return this.spread;
	}

	public void setSpread(float spread) {
		this.spread = Math.clamp(spread, 10.0F, 120.0F);
		this.markUpdated();
	}

	public float getSpeed() {
		return this.speed;
	}

	public void setSpeed(float speed) {
		this.speed = Math.clamp(speed, 0.1F, 3.0F);
		this.markUpdated();
	}

	public int getColor() {
		return this.color;
	}

	public boolean setColor(int newColor) {
		if (this.color == newColor) {
			return false;
		}
		this.color = newColor;
		this.markUpdated();
		return true;
	}

	public BlockPos getSource() {
		return this.sourcePos;
	}

	public void setSource(BlockPos newSource) {
		this.sourcePos = newSource == null ? null : newSource.immutable();
		this.markUpdated();
	}

	public List<BlockPos> getManualGroup() {
		return Collections.unmodifiableList(this.manualGroup);
	}

	public void setManualGroup(List<BlockPos> group) {
		this.manualGroup.clear();
		if (group != null) {
			for (BlockPos pos : group) {
				this.manualGroup.add(pos.immutable());
			}
		}
		this.markUpdated();
	}

	public void clearManualGroup() {
		this.manualGroup.clear();
		this.markUpdated();
	}

	public void markUpdated() {
		this.setChanged();

		if (this.level instanceof ServerLevel serverLevel) {
			ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this);
			Vec3 center = Vec3.atCenterOf(this.worldPosition);

			for (ServerPlayer player : serverLevel.players()) {
				if (player.distanceToSqr(center) < 4096.0) {
					player.connection.send(packet);
				}
			}
		}
	}

	@Override
	protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
		super.saveAdditional(compoundTag, provider);
		compoundTag.putString("mode", this.mode.name());
		compoundTag.putInt("beamCount", this.beamCount);
		compoundTag.putFloat("spread", this.spread);
		compoundTag.putFloat("speed", this.speed);
		compoundTag.putInt("color", this.color);
		compoundTag.putBoolean("dmxEnrolled", this.dmxEnrolled);
		if (!this.customName.isEmpty()) {
			compoundTag.putString("customName", this.customName);
		}

		if (this.sourcePos != null) {
			compoundTag.putLong("source", this.sourcePos.asLong());
		}

		if (!this.manualGroup.isEmpty()) {
			ListTag list = new ListTag();
			for (BlockPos pos : this.manualGroup) {
				list.add(LongTag.valueOf(pos.asLong()));
			}
			compoundTag.put("group", list);
		}
	}

	@Override
	protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
		super.loadAdditional(compoundTag, provider);
		this.mode = compoundTag.contains("mode") ? LaserMode.byName(compoundTag.getString("mode")) : LaserMode.FAN_SWEEP;
		this.beamCount = compoundTag.contains("beamCount") ? compoundTag.getInt("beamCount") : 4;
		this.spread = compoundTag.contains("spread") ? compoundTag.getFloat("spread") : 45.0F;
		this.speed = compoundTag.contains("speed") ? compoundTag.getFloat("speed") : 1.0F;
		this.color = compoundTag.contains("color") ? compoundTag.getInt("color") : BeatLampBlockEntity.COLOR_OLED;
		this.dmxEnrolled = compoundTag.contains("dmxEnrolled") && compoundTag.getBoolean("dmxEnrolled");
		this.customName = compoundTag.contains("customName") ? compoundTag.getString("customName") : "";
		this.sourcePos = compoundTag.contains("source") ? BlockPos.of(compoundTag.getLong("source")) : null;

		this.manualGroup.clear();
		if (compoundTag.contains("group", Tag.TAG_LIST)) {
			ListTag list = compoundTag.getList("group", Tag.TAG_LONG);
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) instanceof LongTag longTag) {
					this.manualGroup.add(BlockPos.of(longTag.getAsLong()));
				}
			}
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
		CompoundTag compoundTag = new CompoundTag();
		this.saveAdditional(compoundTag, provider);
		return compoundTag;
	}

	public static void clientTick(Level level, BlockPos blockPos, BlockState blockState, LaserProjectorBlockEntity laser) {
		clientTicker.tick(laser);
	}
}
