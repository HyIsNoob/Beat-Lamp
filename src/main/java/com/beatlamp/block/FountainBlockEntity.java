package com.beatlamp.block;

import com.beatlamp.BeatLampBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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

	public static ClientTicker clientTicker = fountain -> {
	};

	private int color = BeatLampBlockEntity.COLOR_OLED;
	private boolean fireworkMode;
	private BlockPos sourcePos;

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

	public void toggleFirework() {
		this.fireworkMode = !this.fireworkMode;
		this.markUpdated();
	}

	public BlockPos getSource() {
		return this.sourcePos;
	}

	public void setSource(BlockPos newSource) {
		this.sourcePos = newSource == null ? null : newSource.immutable();
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

		if (this.sourcePos != null) {
			compoundTag.putLong("source", this.sourcePos.asLong());
		}
	}

	@Override
	protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
		super.loadAdditional(compoundTag, provider);
		this.color = compoundTag.contains("color") ? compoundTag.getInt("color") : BeatLampBlockEntity.COLOR_OLED;
		this.fireworkMode = compoundTag.getBoolean("firework");
		this.sourcePos = compoundTag.contains("source") ? BlockPos.of(compoundTag.getLong("source")) : null;
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
