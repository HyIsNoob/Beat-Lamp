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

public class FogGeneratorBlockEntity extends BlockEntity {
	public interface ClientTicker {
		void tick(FogGeneratorBlockEntity fog);
	}

	public interface ControllerUser {
		void use(FogGeneratorBlockEntity fog);
	}

	public static ClientTicker clientTicker = fog -> {
	};

	public static ControllerUser controllerUser = fog -> {
	};

	private FogDensity density = FogDensity.MEDIUM;
	private int radius = 8; // 4, 8, 16 blocks
	private int color = BeatLampBlockEntity.COLOR_OLED;
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

	public FogGeneratorBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BeatLampBlockEntities.FOG_GENERATOR, blockPos, blockState);
	}

	public FogDensity getDensity() {
		return this.density;
	}

	public void setDensity(FogDensity density) {
		this.density = density == null ? FogDensity.MEDIUM : density;
		this.markUpdated();
	}

	public int getRadius() {
		return this.radius;
	}

	public void setRadius(int radius) {
		this.radius = Math.clamp(radius, 4, 16);
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
		compoundTag.putString("density", this.density.name());
		compoundTag.putInt("radius", this.radius);
		compoundTag.putInt("color", this.color);
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
		this.density = compoundTag.contains("density") ? FogDensity.byName(compoundTag.getString("density")) : FogDensity.MEDIUM;
		this.radius = compoundTag.contains("radius") ? compoundTag.getInt("radius") : 8;
		this.color = compoundTag.contains("color") ? compoundTag.getInt("color") : BeatLampBlockEntity.COLOR_OLED;
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

	public static void clientTick(Level level, BlockPos blockPos, BlockState blockState, FogGeneratorBlockEntity fog) {
		clientTicker.tick(fog);
	}
}
