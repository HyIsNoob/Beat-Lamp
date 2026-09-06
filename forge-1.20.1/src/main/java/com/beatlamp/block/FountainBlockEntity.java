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

public class FountainBlockEntity extends BlockEntity {
	public interface ClientTicker {
		void tick(FountainBlockEntity fountain);
	}

	public interface ControllerUser {
		void use(FountainBlockEntity fountain);
	}

	public static ClientTicker clientTicker = fountain -> {
	};

	public static ControllerUser controllerUser = fountain -> {
	};

	private int color = BeatLampBlockEntity.COLOR_OLED;
	private boolean fireworkMode;
	private float sprayThreshold = 0.12F;
	private float impactThreshold = 0.75F;
	private boolean smokeEnabled = true;
	private FountainParticles particleType = FountainParticles.FLAME;
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

	public float fountainEnergy;
	public float fountainImpact;

	public long lastFireSend;
	public long lastServerFire;

	public FountainBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BeatLampBlockEntities.FOUNTAIN, blockPos, blockState);
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

	public boolean isFireworkMode() {
		return this.fireworkMode;
	}

	public void setFireworkMode(boolean fireworkMode) {
		this.fireworkMode = fireworkMode;
		this.markUpdated();
	}

	public void toggleFirework() {
		this.fireworkMode = !this.fireworkMode;
		this.markUpdated();
	}

	public float getSprayThreshold() {
		return this.sprayThreshold;
	}

	public void setSprayThreshold(float threshold) {
		this.sprayThreshold = net.minecraft.util.Mth.clamp(threshold, 0.00F, 0.80F);
		this.markUpdated();
	}

	public float getImpactThreshold() {
		return this.impactThreshold;
	}

	public void setImpactThreshold(float threshold) {
		this.impactThreshold = net.minecraft.util.Mth.clamp(threshold, 0.10F, 1.00F);
		this.markUpdated();
	}

	public boolean isSmokeEnabled() {
		return this.smokeEnabled;
	}

	public void setSmokeEnabled(boolean smokeEnabled) {
		this.smokeEnabled = smokeEnabled;
		this.markUpdated();
	}

	public FountainParticles getParticleType() {
		return this.particleType;
	}

	public void setParticleType(FountainParticles particleType) {
		this.particleType = particleType == null ? FountainParticles.FLAME : particleType;
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

	public boolean canFire(long gameTime) {
		return gameTime - this.lastServerFire >= 10L;
	}

	public void markFired(long gameTime) {
		this.lastServerFire = gameTime;
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
		this.color = tag.contains("Color") ? tag.getInt("Color") : BeatLampBlockEntity.COLOR_OLED;
		this.fireworkMode = tag.getBoolean("FireworkMode");
		this.sprayThreshold = tag.contains("SprayThreshold") ? tag.getFloat("SprayThreshold") : 0.12F;
		this.impactThreshold = tag.contains("ImpactThreshold") ? tag.getFloat("ImpactThreshold") : 0.75F;
		this.smokeEnabled = !tag.contains("SmokeEnabled") || tag.getBoolean("SmokeEnabled");
		this.dmxEnrolled = tag.getBoolean("DmxEnrolled");
		this.customName = tag.getString("CustomName");
		this.particleType = FountainParticles.values()[Math.floorMod(tag.getInt("ParticleType"), FountainParticles.values().length)];

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
		tag.putInt("Color", this.color);
		tag.putBoolean("FireworkMode", this.fireworkMode);
		tag.putFloat("SprayThreshold", this.sprayThreshold);
		tag.putFloat("ImpactThreshold", this.impactThreshold);
		tag.putBoolean("SmokeEnabled", this.smokeEnabled);
		tag.putBoolean("DmxEnrolled", this.dmxEnrolled);
		tag.putString("CustomName", this.customName);
		tag.putInt("ParticleType", this.particleType.ordinal());

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
