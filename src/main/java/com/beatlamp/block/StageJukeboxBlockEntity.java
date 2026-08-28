package com.beatlamp.block;

import com.beatlamp.BeatLampBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class StageJukeboxBlockEntity extends BlockEntity implements Clearable, Container {
	private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	private final JukeboxSongPlayer songPlayer = new JukeboxSongPlayer(this::onSongChanged, this.getBlockPos());

	public StageJukeboxBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BeatLampBlockEntities.STAGE_JUKEBOX, blockPos, blockState);
	}

	public static void tick(Level level, BlockPos blockPos, BlockState blockState, StageJukeboxBlockEntity jukebox) {
		jukebox.songPlayer.tick(level, blockState);
	}

	public JukeboxSongPlayer getSongPlayer() {
		return this.songPlayer;
	}

	public void playSong(Holder<JukeboxSong> song) {
		if (this.level != null) {
			this.songPlayer.play(this.level, song);
			this.setChanged();
		}
	}

	public void stopSong() {
		if (this.level != null) {
			this.songPlayer.stop(this.level, this.getBlockState());
			this.setChanged();
		}
	}

	public boolean isPlaying() {
		return this.songPlayer.isPlaying();
	}

	public ItemStack getRecord() {
		return this.items.get(0);
	}

	public void setRecord(ItemStack stack) {
		this.items.set(0, stack);
		this.setChanged();
	}

	private void onSongChanged() {
		this.setChanged();
		if (this.level != null) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return this.items.get(0).isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return slot == 0 ? this.items.get(0) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack stack = ContainerHelper.removeItem(this.items, slot, amount);
		if (!stack.isEmpty()) {
			this.stopSong();
		}
		return stack;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack stack = ContainerHelper.takeItem(this.items, slot);
		if (!stack.isEmpty()) {
			this.stopSong();
		}
		return stack;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot == 0) {
			this.items.set(0, stack);
			this.setChanged();
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		this.items.clear();
		this.stopSong();
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.items.clear();
		ContainerHelper.loadAllItems(tag, this.items, registries);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, this.items, registries);
	}
}
