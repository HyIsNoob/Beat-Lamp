package com.beatlamp.block;

import java.util.Collections;
import java.util.List;

import com.beatlamp.BeatLampBlockEntities;
import com.beatlamp.BeatLampBlocks;

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

	private int currentSignal = 0;
	private long lastPulseTick = 0;

	private final List<BlockPos> manualGroup = new java.util.ArrayList<>();

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
		this.threshold = net.minecraft.util.Mth.clamp(threshold, 0.05F, 1.00F);
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

	public void applyConfig(EmitterMode mode, float threshold, boolean inverted, boolean dmxEnrolled, String customName) {
		this.mode = mode == null ? EmitterMode.PULSE : mode;
		this.threshold = net.minecraft.util.Mth.clamp(threshold, 0.05F, 1.00F);
		this.inverted = inverted;
		this.dmxEnrolled = dmxEnrolled;
		this.customName = customName == null ? "" : customName.trim();
		this.markUpdated();
	}

	public int getSignal() {
		return this.currentSignal;
	}

	public void setSignal(int signal, long gameTime) {
		int clamped = net.minecraft.util.Mth.clamp(signal, 0, 15);
		if (this.currentSignal != clamped) {
			this.currentSignal = clamped;
			this.lastPulseTick = gameTime;
			this.markUpdated();

			Level level = this.level;
			if (level != null && !level.isClientSide) {
				BlockState state = this.getBlockState();
				level.setBlock(this.worldPosition, state.setValue(BeatEmitterBlock.POWERED, clamped > 0), Block.UPDATE_ALL);
				level.updateNeighborsAt(this.worldPosition, state.getBlock());
			}
		}
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
		this.mode = EmitterMode.values()[Math.floorMod(tag.getInt("Mode"), EmitterMode.values().length)];
		this.threshold = tag.contains("Threshold") ? tag.getFloat("Threshold") : 0.50F;
		this.inverted = tag.getBoolean("Inverted");
		this.dmxEnrolled = tag.getBoolean("DmxEnrolled");
		this.customName = tag.getString("CustomName");
		this.currentSignal = tag.getInt("Signal");

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
		tag.putFloat("Threshold", this.threshold);
		tag.putBoolean("Inverted", this.inverted);
		tag.putBoolean("DmxEnrolled", this.dmxEnrolled);
		tag.putString("CustomName", this.customName);
		tag.putInt("Signal", this.currentSignal);

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
