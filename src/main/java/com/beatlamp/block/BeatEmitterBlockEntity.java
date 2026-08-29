package com.beatlamp.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.beatlamp.BeatLampBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BeatEmitterBlockEntity extends BlockEntity {
	public interface ClientTicker {
		void tick(BeatEmitterBlockEntity emitter);
	}

	public interface ControllerUser {
		void use(BeatEmitterBlockEntity emitter);
	}

	public static ClientTicker clientTicker = emitter -> {
	};

	public static ControllerUser controllerUser = emitter -> {
	};

	private EmitterMode mode = EmitterMode.PULSE;
	private float threshold = 0.50F;
	private boolean inverted = false;
	private boolean dmxEnrolled = false;
	private String customName = "";
	private BlockPos sourcePos;
	private final List<BlockPos> manualGroup = new ArrayList<>();

	private int signalLevel;
	private long lastServerUpdate;

	private int lastSentSignal = -1;
	private long lastSendTime;

	public BeatEmitterBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BeatLampBlockEntities.BEAT_EMITTER, blockPos, blockState);
	}

	public EmitterMode getMode() {
		return this.mode;
	}

	public void setMode(EmitterMode mode) {
		this.mode = mode == null ? EmitterMode.PULSE : mode;
		this.markUpdated();
	}

	public float getThreshold() {
		return this.threshold;
	}

	public void setThreshold(float threshold) {
		this.threshold = Math.clamp(threshold, 0.10F, 0.95F);
		this.markUpdated();
	}

	public boolean isInverted() {
		return this.inverted;
	}

	public void setInverted(boolean inverted) {
		this.inverted = inverted;
		this.markUpdated();
	}

	public boolean isDmxEnrolled() {
		return this.dmxEnrolled;
	}

	public void setDmxEnrolled(boolean enrolled) {
		this.dmxEnrolled = enrolled;
		this.markUpdated();
	}

	public String getCustomName() {
		return this.customName;
	}

	public void setCustomName(String name) {
		this.customName = name == null ? "" : name.trim();
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

	public void setManualGroup(List<BlockPos> positions) {
		this.manualGroup.clear();
		for (BlockPos pos : positions) {
			this.manualGroup.add(pos.immutable());
		}
		this.markUpdated();
	}

	public void clearManualGroup() {
		this.manualGroup.clear();
		this.markUpdated();
	}

	public void applyConfig(EmitterMode mode, float threshold, boolean inverted, boolean dmxEnrolled, String customName) {
		this.mode = mode == null ? EmitterMode.PULSE : mode;
		this.threshold = Math.clamp(threshold, 0.10F, 0.95F);
		this.inverted = inverted;
		this.dmxEnrolled = dmxEnrolled;
		this.customName = customName == null ? "" : customName.trim();
		this.markUpdated();
	}

	public int getSignalLevel() {
		return this.signalLevel;
	}

	public void setSignal(int signal, long gameTime) {
		if (this.signalLevel != signal) {
			this.signalLevel = signal;
			this.lastServerUpdate = gameTime;
			if (this.level != null && !this.level.isClientSide) {
				BlockState state = this.getBlockState();
				if (state.hasProperty(BeatEmitterBlock.POWER)) {
					this.level.setBlock(this.worldPosition, state.setValue(BeatEmitterBlock.POWER, signal), Block.UPDATE_ALL);
				}
				this.level.updateNeighborsAt(this.worldPosition, state.getBlock());
				for (Direction dir : Direction.values()) {
					this.level.updateNeighborsAt(this.worldPosition.relative(dir), state.getBlock());
				}
			}
		} else {
			this.lastServerUpdate = gameTime;
		}
	}

	public void tickServerTimeout(Level level) {
		if (this.signalLevel != 0 && level.getGameTime() - this.lastServerUpdate > 40L) {
			this.signalLevel = 0;
			BlockState state = this.getBlockState();
			if (state.hasProperty(BeatEmitterBlock.POWER)) {
				level.setBlock(this.worldPosition, state.setValue(BeatEmitterBlock.POWER, 0), Block.UPDATE_ALL);
			}
			level.updateNeighborsAt(this.worldPosition, state.getBlock());
			for (Direction dir : Direction.values()) {
				level.updateNeighborsAt(this.worldPosition.relative(dir), state.getBlock());
			}
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
		compoundTag.putFloat("threshold", this.threshold);
		compoundTag.putBoolean("inverted", this.inverted);
		compoundTag.putBoolean("dmxEnrolled", this.dmxEnrolled);
		if (this.customName != null && !this.customName.isEmpty()) {
			compoundTag.putString("customName", this.customName);
		}

		if (this.sourcePos != null) {
			compoundTag.putLong("source", this.sourcePos.asLong());
		}

		if (!this.manualGroup.isEmpty()) {
			ListTag list = new ListTag();
			for (BlockPos memberPos : this.manualGroup) {
				list.add(LongTag.valueOf(memberPos.asLong()));
			}
			compoundTag.put("manualGroup", list);
		}
	}

	@Override
	protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
		super.loadAdditional(compoundTag, provider);
		if (compoundTag.contains("mode")) {
			try {
				this.mode = EmitterMode.valueOf(compoundTag.getString("mode"));
			} catch (IllegalArgumentException ignored) {
				this.mode = EmitterMode.PULSE;
			}
		} else if (compoundTag.contains("pulseMode")) {
			this.mode = compoundTag.getBoolean("pulseMode") ? EmitterMode.PULSE : EmitterMode.ENERGY;
		}

		this.threshold = compoundTag.contains("threshold") ? compoundTag.getFloat("threshold") : 0.50F;
		this.inverted = compoundTag.getBoolean("inverted");
		this.dmxEnrolled = compoundTag.contains("dmxEnrolled") && compoundTag.getBoolean("dmxEnrolled");
		this.customName = compoundTag.contains("customName") ? compoundTag.getString("customName") : "";
		this.sourcePos = compoundTag.contains("source") ? BlockPos.of(compoundTag.getLong("source")) : null;

		this.manualGroup.clear();
		if (compoundTag.contains("manualGroup", Tag.TAG_LIST)) {
			ListTag list = compoundTag.getList("manualGroup", Tag.TAG_LONG);
			for (int i = 0; i < list.size(); i++) {
				this.manualGroup.add(BlockPos.of(((LongTag) list.get(i)).getAsLong()));
			}
		}
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

