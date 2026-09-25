package com.beatlamp.block;

import java.util.function.Consumer;

import com.beatlamp.BeatLampBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DmxConsoleBlockEntity extends BlockEntity {
	public static Consumer<DmxConsoleBlockEntity> controllerUser;

	private boolean blackout = false;
	private boolean strobeAll = false;
	private float masterDimmer = 1.0F;
	private float masterSpeed = 1.0F;
	private final java.util.Set<BlockPos> mutedGroups = new java.util.HashSet<>();

	public DmxConsoleBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BeatLampBlockEntities.DMX_CONSOLE, blockPos, blockState);
	}

	public boolean isGroupMuted(BlockPos pos) {
		return this.mutedGroups.contains(pos);
	}

	public void setGroupMuted(BlockPos pos, boolean muted) {
		if (pos == null) return;
		if (muted) {
			this.mutedGroups.add(pos.immutable());
		} else {
			this.mutedGroups.remove(pos);
		}
		this.markUpdated();
	}

	public java.util.Set<BlockPos> getMutedGroups() {
		return this.mutedGroups;
	}

	public void markUpdated() {
		this.setChanged();
		Level level = this.level;
		if (level != null && !level.isClientSide) {
			level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		}
	}

	public boolean isBlackout() {
		return this.blackout;
	}

	public void setBlackout(boolean blackout) {
		this.blackout = blackout;
		this.setChanged();
	}

	public boolean isStrobeAll() {
		return this.strobeAll;
	}

	public void setStrobeAll(boolean strobeAll) {
		this.strobeAll = strobeAll;
		this.setChanged();
	}

	public float getMasterDimmer() {
		return this.masterDimmer;
	}

	public void setMasterDimmer(float masterDimmer) {
		this.masterDimmer = masterDimmer;
		this.setChanged();
	}

	public float getMasterSpeed() {
		return this.masterSpeed;
	}

	public void setMasterSpeed(float masterSpeed) {
		this.masterSpeed = masterSpeed;
		this.setChanged();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.blackout = input.getBooleanOr("Blackout", false);
		this.strobeAll = input.getBooleanOr("StrobeAll", false);
		this.masterDimmer = input.getFloatOr("MasterDimmer", 1.0F);
		this.masterSpeed = input.getFloatOr("MasterSpeed", 1.0F);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("Blackout", this.blackout);
		output.putBoolean("StrobeAll", this.strobeAll);
		output.putFloat("MasterDimmer", this.masterDimmer);
		output.putFloat("MasterSpeed", this.masterSpeed);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveCustomOnly(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
