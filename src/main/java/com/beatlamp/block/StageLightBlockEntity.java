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

public class StageLightBlockEntity extends BlockEntity {
	public static final int COLOR_BEAT_CYCLE = -2;

	public interface ClientTicker {
		void tick(StageLightBlockEntity light);
	}

	public interface ControllerUser {
		void use(StageLightBlockEntity light);
	}

	public static ClientTicker clientTicker = light -> {
	};

	public static ControllerUser controllerUser = light -> {
	};

	private StageLightMode mode = StageLightMode.SWEEP;
	private int color = BeatLampBlockEntity.COLOR_OLED;
	private float sensitivity = 1.0F;
	private float speed = 1.0F;
	private boolean tempoPulse = true;
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

	public float beamEnergy;
	public float beamBeat;
	public int groupIndex;
	public int groupSize = 1;

	// Smooth angle interpolation for BEAT_STEP mode
	public float currentPan;
	public float targetPan;
	public float currentTilt = 30.0F;
	public float targetTilt = 30.0F;
	public int beatColorIndex;
	public long lastBeatChange;

	public StageLightBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BeatLampBlockEntities.STAGE_LIGHT, blockPos, blockState);
	}

	public StageLightMode getMode() {
		return this.mode;
	}

	public void setMode(StageLightMode newMode) {
		this.mode = newMode == null ? StageLightMode.SWEEP : newMode;
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

	public float getSensitivity() {
		return this.sensitivity;
	}

	public void setSensitivity(float newSensitivity) {
		this.sensitivity = Math.clamp(newSensitivity, 0.25F, 3.0F);
		this.markUpdated();
	}

	public float getSpeed() {
		return this.speed;
	}

	public void setSpeed(float newSpeed) {
		this.speed = Math.clamp(newSpeed, 0.25F, 3.0F);
		this.markUpdated();
	}

	public boolean isTempoPulse() {
		return this.tempoPulse;
	}

	public void setTempoPulse(boolean tempoPulse) {
		this.tempoPulse = tempoPulse;
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
		compoundTag.putInt("color", this.color);
		compoundTag.putFloat("sensitivity", this.sensitivity);
		compoundTag.putFloat("speed", this.speed);
		compoundTag.putBoolean("tempoPulse", this.tempoPulse);
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
		this.mode = compoundTag.contains("mode") ? StageLightMode.byName(compoundTag.getString("mode")) : StageLightMode.SWEEP;
		this.color = compoundTag.contains("color") ? compoundTag.getInt("color") : BeatLampBlockEntity.COLOR_OLED;
		this.sensitivity = compoundTag.contains("sensitivity") ? compoundTag.getFloat("sensitivity") : 1.0F;
		this.speed = compoundTag.contains("speed") ? compoundTag.getFloat("speed") : 1.0F;
		this.tempoPulse = !compoundTag.contains("tempoPulse") || compoundTag.getBoolean("tempoPulse");
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

	public static void clientTick(Level level, BlockPos blockPos, BlockState blockState, StageLightBlockEntity light) {
		clientTicker.tick(light);
	}
}
