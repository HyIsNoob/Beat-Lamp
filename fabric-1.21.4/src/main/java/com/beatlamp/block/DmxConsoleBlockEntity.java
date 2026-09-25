package com.beatlamp.block;

import java.util.function.Consumer;

import com.beatlamp.BeatLampBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.blackout = tag.getBoolean("Blackout");
		this.strobeAll = tag.getBoolean("StrobeAll");
		this.masterDimmer = tag.contains("MasterDimmer") ? tag.getFloat("MasterDimmer") : 1.0F;
		this.masterSpeed = tag.contains("MasterSpeed") ? tag.getFloat("MasterSpeed") : 1.0F;
		this.mutedGroups.clear();
		if (tag.contains("MutedGroups", net.minecraft.nbt.Tag.TAG_LIST)) {
			net.minecraft.nbt.ListTag list = tag.getList("MutedGroups", net.minecraft.nbt.Tag.TAG_LONG);
			for (int i = 0; i < list.size(); i++) {
				this.mutedGroups.add(BlockPos.of(((net.minecraft.nbt.LongTag) list.get(i)).getAsLong()));
			}
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putBoolean("Blackout", this.blackout);
		tag.putBoolean("StrobeAll", this.strobeAll);
		tag.putFloat("MasterDimmer", this.masterDimmer);
		tag.putFloat("MasterSpeed", this.masterSpeed);
		net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
		for (BlockPos p : this.mutedGroups) {
			list.add(net.minecraft.nbt.LongTag.valueOf(p.asLong()));
		}
		tag.put("MutedGroups", list);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		this.saveAdditional(tag, registries);
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
