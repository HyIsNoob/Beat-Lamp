package com.beatlamp.block;

import java.util.function.Consumer;

import com.beatlamp.BeatLampBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DmxConsoleBlockEntity extends BlockEntity {
	public static Consumer<DmxConsoleBlockEntity> controllerUser;

	private boolean blackout = false;
	private boolean strobeAll = false;
	private float masterDimmer = 1.0F;
	private float masterSpeed = 1.0F;

	public DmxConsoleBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BeatLampBlockEntities.DMX_CONSOLE, blockPos, blockState);
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
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putBoolean("Blackout", this.blackout);
		tag.putBoolean("StrobeAll", this.strobeAll);
		tag.putFloat("MasterDimmer", this.masterDimmer);
		tag.putFloat("MasterSpeed", this.masterSpeed);
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
