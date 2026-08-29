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
		this.sprayThreshold = Math.clamp(threshold, 0.00F, 0.80F);
		this.markUpdated();
	}

	public float getImpactThreshold() {
		return this.impactThreshold;
	}

	public void setImpactThreshold(float threshold) {
		this.impactThreshold = Math.clamp(threshold, 0.20F, 0.95F);
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
		this.manualGroup.clear();
		this.markUpdated();
	}

	public void markFired(long gameTime) {
		this.lastServerFire = gameTime;
		this.setChanged();
	}

	public boolean canFire(long gameTime) {
		return gameTime - this.lastServerFire >= 20L;
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
		compoundTag.putInt("color", this.color);
		compoundTag.putBoolean("firework", this.fireworkMode);
		compoundTag.putFloat("sprayThreshold", this.sprayThreshold);
		compoundTag.putFloat("impactThreshold", this.impactThreshold);
		compoundTag.putBoolean("smokeEnabled", this.smokeEnabled);
		compoundTag.putBoolean("dmxEnrolled", this.dmxEnrolled);
		if (!this.customName.isEmpty()) {
			compoundTag.putString("customName", this.customName);
		}
		compoundTag.putString("particleType", this.particleType.name());

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
		this.color = compoundTag.contains("color") ? compoundTag.getInt("color") : BeatLampBlockEntity.COLOR_OLED;
		this.fireworkMode = compoundTag.getBoolean("firework");
		this.sprayThreshold = compoundTag.contains("sprayThreshold") ? compoundTag.getFloat("sprayThreshold") : 0.12F;
		this.impactThreshold = compoundTag.contains("impactThreshold") ? compoundTag.getFloat("impactThreshold") : 0.75F;
		this.smokeEnabled = !compoundTag.contains("smokeEnabled") || compoundTag.getBoolean("smokeEnabled");
		this.dmxEnrolled = compoundTag.contains("dmxEnrolled") && compoundTag.getBoolean("dmxEnrolled");
		this.customName = compoundTag.contains("customName") ? compoundTag.getString("customName") : "";
		this.particleType = compoundTag.contains("particleType") ? FountainParticles.byName(compoundTag.getString("particleType")) : FountainParticles.FLAME;
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

	public static void clientTick(Level level, BlockPos blockPos, BlockState blockState, FountainBlockEntity fountain) {
		clientTicker.tick(fountain);
	}
}
