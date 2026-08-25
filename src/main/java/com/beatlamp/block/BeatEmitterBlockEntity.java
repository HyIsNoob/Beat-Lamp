package com.beatlamp.block;

import com.beatlamp.BeatLampBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BeatEmitterBlockEntity extends BlockEntity {
	public interface ClientTicker {
		void tick(BeatEmitterBlockEntity emitter);
	}

	public static ClientTicker clientTicker = emitter -> {
	};

	private BlockPos sourcePos;
	private int signalLevel;
	private long lastServerUpdate;

	private int lastSentSignal = -1;
	private long lastSendTime;

	public BeatEmitterBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BeatLampBlockEntities.BEAT_EMITTER, blockPos, blockState);
	}

	public BlockPos getSource() {
		return this.sourcePos;
	}

	public void setSource(BlockPos newSource) {
		this.sourcePos = newSource == null ? null : newSource.immutable();
		this.setChanged();
		this.markUpdated();
	}

	public int getSignalLevel() {
		return this.signalLevel;
	}

	public void setSignal(int signal, long gameTime) {
		this.signalLevel = signal;
		this.lastServerUpdate = gameTime;
	}

	public void tickServerTimeout(Level level) {
		if (this.signalLevel != 0 && level.getGameTime() - this.lastServerUpdate > 40L) {
			this.signalLevel = 0;
			level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
		}
	}

	public boolean shouldSendSignal(long gameTime, int signal) {
		if (gameTime - this.lastSendTime < 2L) {
			return false;
		}

		return signal != this.lastSentSignal || gameTime - this.lastSendTime >= 40L;
	}

	public void markSent(long gameTime, int signal) {
		this.lastSendTime = gameTime;
		this.lastSentSignal = signal;
	}

	public void markUpdated() {
		this.setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
		super.saveAdditional(compoundTag, provider);

		if (this.sourcePos != null) {
			compoundTag.putLong("source", this.sourcePos.asLong());
		}
	}

	@Override
	protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
		super.loadAdditional(compoundTag, provider);
		this.sourcePos = compoundTag.contains("source") ? BlockPos.of(compoundTag.getLong("source")) : null;
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
		CompoundTag compoundTag = new CompoundTag();
		this.saveAdditional(compoundTag, provider);
		return compoundTag;
	}

	public static void clientTick(Level level, BlockPos blockPos, BlockState blockState, BeatEmitterBlockEntity emitter) {
		clientTicker.tick(emitter);
	}

	public static void serverTick(Level level, BlockPos blockPos, BlockState blockState, BeatEmitterBlockEntity emitter) {
		emitter.tickServerTimeout(level);
	}
}
