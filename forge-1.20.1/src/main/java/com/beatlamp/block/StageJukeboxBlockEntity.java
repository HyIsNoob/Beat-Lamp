package com.beatlamp.block;

import com.beatlamp.BeatLampBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class StageJukeboxBlockEntity extends BlockEntity implements Clearable {
	private ItemStack record = ItemStack.EMPTY;

	public StageJukeboxBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BeatLampBlockEntities.STAGE_JUKEBOX, blockPos, blockState);
	}

	public ItemStack getRecord() {
		return this.record;
	}

	public void setRecord(ItemStack stack) {
		this.record = stack;
		this.setChanged();
	}

	public void playRecord() {
		if (!this.record.isEmpty() && this.level != null && !this.level.isClientSide) {
			this.level.levelEvent(null, 1010, this.worldPosition, Item.getId(this.record.getItem()));
		}
	}

	public void stopRecord() {
		if (this.level != null && !this.level.isClientSide) {
			this.level.levelEvent(null, 1011, this.worldPosition, 0);
		}
	}

	public void dropRecord() {
		if (this.level != null && !this.level.isClientSide && !this.record.isEmpty()) {
			this.stopRecord();
			ItemStack drop = this.record.copy();
			this.record = ItemStack.EMPTY;
			this.setChanged();

			double d0 = (double) (this.level.random.nextFloat() * 0.7F) + (double) 0.15F;
			double d1 = (double) (this.level.random.nextFloat() * 0.7F) + (double) 0.06F + 0.6D;
			double d2 = (double) (this.level.random.nextFloat() * 0.7F) + (double) 0.15F;
			ItemStack itemstack1 = drop.copy();
			ItemEntity itementity = new ItemEntity(this.level, (double) this.worldPosition.getX() + d0, (double) this.worldPosition.getY() + d1, (double) this.worldPosition.getZ() + d2, itemstack1);
			itementity.setDefaultPickUpDelay();
			this.level.addFreshEntity(itementity);
		}
	}

	@Override
	public void clearContent() {
		this.setRecord(ItemStack.EMPTY);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		if (tag.contains("RecordItem", 10)) {
			this.setRecord(ItemStack.of(tag.getCompound("RecordItem")));
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (!this.getRecord().isEmpty()) {
			tag.put("RecordItem", this.getRecord().save(new CompoundTag()));
		}
	}
}
