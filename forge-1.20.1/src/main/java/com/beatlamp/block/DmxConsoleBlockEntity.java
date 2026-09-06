package com.beatlamp.block;

import com.beatlamp.BeatLampBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DmxConsoleBlockEntity extends BlockEntity {
	public interface ControllerUser {
		void use(DmxConsoleBlockEntity dmx);
	}

	public static ControllerUser controllerUser = dmx -> {
	};

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
		this.markUpdated();
	}

	public boolean isStrobeAll() {
		return this.strobeAll;
	}

	public void setStrobeAll(boolean strobeAll) {
		this.strobeAll = strobeAll;
		this.markUpdated();
	}

	public float getMasterDimmer() {
		return this.masterDimmer;
	}

	public void setMasterDimmer(float dimmer) {
		this.masterDimmer = net.minecraft.util.Mth.clamp(dimmer, 0.0F, 1.0F);
		this.markUpdated();
	}

	public float getMasterSpeed() {
		return this.masterSpeed;
	}

	public void setMasterSpeed(float speed) {
		this.masterSpeed = net.minecraft.util.Mth.clamp(speed, 0.25F, 4.0F);
		this.markUpdated();
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
		this.blackout = tag.getBoolean("Blackout");
		this.strobeAll = tag.getBoolean("StrobeAll");
		this.masterDimmer = tag.contains("MasterDimmer") ? tag.getFloat("MasterDimmer") : 1.0F;
		this.masterSpeed = tag.contains("MasterSpeed") ? tag.getFloat("MasterSpeed") : 1.0F;
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putBoolean("Blackout", this.blackout);
		tag.putBoolean("StrobeAll", this.strobeAll);
		tag.putFloat("MasterDimmer", this.masterDimmer);
		tag.putFloat("MasterSpeed", this.masterSpeed);
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
