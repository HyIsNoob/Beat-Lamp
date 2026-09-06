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
	private float radius = 8.0F;
	private int color = BeatLampBlockEntity.COLOR_OLED;
	private boolean dmxEnrolled = false;
	private String customName = "";
	private BlockPos sourcePos;

	private final List<BlockPos> manualGroup = new java.util.ArrayList<>();

	public float fogAlpha;
	public float pulse;
	public int groupIndex;
	public int groupSize = 1;

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

	public float getRadius() {
		return this.radius;
	}

	public void setRadius(float radius) {
		this.radius = net.minecraft.util.Mth.clamp(radius, 4.0F, 16.0F);
		this.markUpdated();
	}

	public int getColor() {
		return this.color;
	}

	public void setColor(int color) {
		this.color = color;
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
		this.density = FogDensity.values()[Math.floorMod(tag.getInt("Density"), FogDensity.values().length)];
		this.radius = tag.contains("Radius") ? tag.getFloat("Radius") : 8.0F;
		this.color = tag.contains("Color") ? tag.getInt("Color") : BeatLampBlockEntity.COLOR_OLED;
		this.dmxEnrolled = tag.getBoolean("DmxEnrolled");
		this.customName = tag.getString("CustomName");

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
		tag.putInt("Density", this.density.ordinal());
		tag.putFloat("Radius", this.radius);
		tag.putInt("Color", this.color);
		tag.putBoolean("DmxEnrolled", this.dmxEnrolled);
		tag.putString("CustomName", this.customName);

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
