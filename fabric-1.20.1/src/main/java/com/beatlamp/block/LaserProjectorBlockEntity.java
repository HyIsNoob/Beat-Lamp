package com.beatlamp.block;

import java.util.Collections;
import java.util.List;

import com.beatlamp.BeatLampBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
	private int beamCount = 5;
	private float spread = 45.0F;
	private float speed = 1.0F;
	private int color = BeatLampBlockEntity.COLOR_OLED;
	private boolean dmxEnrolled = false;
	private String customName = "";
	private BlockPos sourcePos;

	private final List<BlockPos> manualGroup = new java.util.ArrayList<>();

	public float currentAngle;
	public float currentSpread;
	public float beamIntensity;
	public float rotationSpeed;
	public int groupIndex;
	public int groupSize = 1;

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

	public void setBeamCount(int count) {
		this.beamCount = net.minecraft.util.Mth.clamp(count, 1, 9);
		this.markUpdated();
	}

	public float getSpread() {
		return this.spread;
	}

	public void setSpread(float spread) {
		this.spread = net.minecraft.util.Mth.clamp(spread, 10.0F, 90.0F);
		this.markUpdated();
	}

	public float getSpeed() {
		return this.speed;
	}

	public void setSpeed(float speed) {
		this.speed = net.minecraft.util.Mth.clamp(speed, 0.25F, 4.0F);
		this.markUpdated();
	}

	public int getColor() {
		return this.color;
	}

	public void setColor(int color) {
		this.color = color;
		this.markUpdated();
	}

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
		if (!this.manualGroup.isEmpty()) {
			this.manualGroup.clear();
			this.markUpdated();
		}
	}

	public void markUpdated() {
		this.setChanged();
		Level level = this.level;
		if (level != null && !level.isClientSide) {
			level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		}
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		this.mode = LaserMode.values()[Math.floorMod(tag.getInt("Mode"), LaserMode.values().length)];
		this.beamCount = tag.contains("BeamCount") ? tag.getInt("BeamCount") : 5;
		this.spread = tag.contains("Spread") ? tag.getFloat("Spread") : 45.0F;
		this.speed = tag.contains("Speed") ? tag.getFloat("Speed") : 1.0F;
		this.color = tag.contains("Color") ? tag.getInt("Color") : BeatLampBlockEntity.COLOR_OLED;
		this.dmxEnrolled = tag.getBoolean("DmxEnrolled");
		this.customName = tag.getString("CustomName");

		if (tag.contains("SourcePos")) {
			this.sourcePos = BlockPos.of(tag.getLong("SourcePos"));
		} else {
			this.sourcePos = null;
		}

		this.manualGroup.clear();
		if (tag.contains("ManualGroup", Tag.TAG_LIST)) {
			ListTag list = tag.getList("ManualGroup", Tag.TAG_LONG);
			for (int i = 0; i < list.size(); i++) {
				this.manualGroup.add(BlockPos.of(((LongTag) list.get(i)).getAsLong()));
			}
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("Mode", this.mode.ordinal());
		tag.putInt("BeamCount", this.beamCount);
		tag.putFloat("Spread", this.spread);
		tag.putFloat("Speed", this.speed);
		tag.putInt("Color", this.color);
		tag.putBoolean("DmxEnrolled", this.dmxEnrolled);
		tag.putString("CustomName", this.customName);

		if (this.sourcePos != null) {
			tag.putLong("SourcePos", this.sourcePos.asLong());
		}

		if (!this.manualGroup.isEmpty()) {
			ListTag list = new ListTag();
			for (BlockPos pos : this.manualGroup) {
				list.add(LongTag.valueOf(pos.asLong()));
			}
			tag.put("ManualGroup", list);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag() {
		return this.saveWithoutMetadata();
	}
}
