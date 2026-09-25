package com.beatlamp.block;

import com.beatlamp.BeatLampBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import org.jetbrains.annotations.Nullable;

public class StageJukeboxBlockEntity extends BlockEntity implements Clearable, Container {
	public interface ControllerUser {
		void use(StageJukeboxBlockEntity jukebox);
	}

	public static ControllerUser controllerUser = jukebox -> {};

	private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	private final JukeboxSongPlayer songPlayer = new JukeboxSongPlayer(this::onSongChanged, this.getBlockPos());

	private int volume = 100;
	private int range = 64;
	private boolean loop = false;
	private boolean paused = false;
	private int elapsedTicks = 0;
	private int totalDurationTicks = 0;

	public StageJukeboxBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BeatLampBlockEntities.STAGE_JUKEBOX, blockPos, blockState);
	}

	public static void tick(Level level, BlockPos blockPos, BlockState blockState, StageJukeboxBlockEntity jukebox) {
		if (!jukebox.paused) {
			jukebox.songPlayer.tick(level, blockState);
		}
		if (blockState.getValue(StageJukeboxBlock.HAS_RECORD) && !jukebox.paused && !jukebox.isEmpty()) {
			jukebox.elapsedTicks++;
			if (jukebox.totalDurationTicks > 0 && jukebox.elapsedTicks >= jukebox.totalDurationTicks) {
				if (jukebox.loop) {
					jukebox.elapsedTicks = 0;
					if (!level.isClientSide) {
						ItemStack record = jukebox.getRecord();
						if (record.has(DataComponents.JUKEBOX_PLAYABLE)) {
							JukeboxPlayable playable = record.get(DataComponents.JUKEBOX_PLAYABLE);
							if (playable != null) {
								playable.song().unwrap(level.registryAccess()).ifPresent(jukebox::playSong);
							}
						} else {
							jukebox.playCustomDisc();
						}
					}
				} else {
					jukebox.elapsedTicks = jukebox.totalDurationTicks;
				}
			}
		}
	}

	public static void clientTick(Level level, BlockPos blockPos, BlockState blockState, StageJukeboxBlockEntity jukebox) {
		if (blockState.getValue(StageJukeboxBlock.HAS_RECORD) && !jukebox.paused && !jukebox.isEmpty()) {
			jukebox.elapsedTicks++;
			if (jukebox.totalDurationTicks > 0 && jukebox.elapsedTicks >= jukebox.totalDurationTicks) {
				if (jukebox.loop) {
					jukebox.elapsedTicks = 0;
				} else {
					jukebox.elapsedTicks = jukebox.totalDurationTicks;
				}
			}
		}
	}

	public JukeboxSongPlayer getSongPlayer() {
		return this.songPlayer;
	}

	public int getVolume() {
		return this.volume;
	}

	public void setVolume(int volume) {
		this.volume = Mth.clamp(volume, 0, 100);
		this.setChanged();
	}

	public int getRange() {
		return this.range;
	}

	public void setRange(int range) {
		this.range = Mth.clamp(range, 16, 128);
		this.setChanged();
	}

	public boolean isLoop() {
		return this.loop;
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
		this.setChanged();
	}

	public boolean isPaused() {
		return this.paused;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
		this.setChanged();
	}

	public int getElapsedTicks() {
		return this.elapsedTicks;
	}

	public void setElapsedTicks(int elapsedTicks) {
		this.elapsedTicks = elapsedTicks;
		this.setChanged();
	}

	public int getTotalDurationTicks() {
		return this.totalDurationTicks;
	}

	public void setTotalDurationTicks(int totalDurationTicks) {
		this.totalDurationTicks = totalDurationTicks;
		this.setChanged();
	}

	public void applyConfiguration(int volume, int range, boolean loop) {
		this.volume = Mth.clamp(volume, 0, 100);
		this.range = Mth.clamp(range, 16, 128);
		this.loop = loop;
		this.setChanged();
		if (this.level != null) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	public void togglePause() {
		this.paused = !this.paused;
		this.setChanged();
		if (this.level != null) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	public void seekTo(int seekSeconds) {
		this.elapsedTicks = Math.max(0, seekSeconds * 20);
		this.setChanged();
		if (this.level != null) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
		}
	}

	public void ejectRecord(@Nullable Player player) {
		if (this.level != null && !this.level.isClientSide) {
			ItemStack record = this.getRecord();
			if (!record.isEmpty()) {
				this.stopSong();
				this.setRecord(ItemStack.EMPTY);
				this.level.setBlock(this.worldPosition, this.getBlockState().setValue(StageJukeboxBlock.HAS_RECORD, false), 3);
				this.level.gameEvent(null, GameEvent.JUKEBOX_STOP_PLAY, this.worldPosition);
				this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);

				if (player != null && !player.getAbilities().instabuild) {
					if (!player.getInventory().add(record)) {
						Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.9, this.worldPosition.getZ() + 0.5, record);
					}
				} else if (player == null) {
					Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.9, this.worldPosition.getZ() + 0.5, record);
				}
			}
		}
	}

	public void playSong(Holder<JukeboxSong> song) {
		if (this.level != null) {
			this.songPlayer.play(this.level, song);
			this.totalDurationTicks = (int) (song.value().lengthInSeconds() * 20.0F);
			this.elapsedTicks = 0;
			this.paused = false;
			this.setChanged();
			MusicDiscMakerBridge.onInserted(this.level, this.worldPosition, this.getRecord());
		}
	}

	public void playCustomDisc() {
		if (this.level != null && !this.level.isClientSide) {
			this.totalDurationTicks = resolveCustomDurationTicks(this.getRecord());
			this.elapsedTicks = 0;
			this.paused = false;
			this.setChanged();
			this.onSongChanged();
			MusicDiscMakerBridge.onInserted(this.level, this.worldPosition, this.getRecord());
		}
	}

	private static int resolveCustomDurationTicks(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return 3600;
		try {
			for (var comp : stack.getComponents()) {
				if (comp.type().toString().contains("custom_track")) {
					Object val = comp.value();
					for (java.lang.reflect.Method m : val.getClass().getMethods()) {
						if ("durationMs".equals(m.getName()) && m.getParameterCount() == 0) {
							long ms = ((Number) m.invoke(val)).longValue();
							if (ms > 0) return (int) (ms / 50L);
						}
					}
				}
			}
		} catch (Throwable ignored) {
		}
		return 3600;
	}

	public void stopSong() {
		if (this.level != null) {
			this.songPlayer.stop(this.level, this.getBlockState());
			this.paused = false;
			this.elapsedTicks = 0;
			this.setChanged();
			MusicDiscMakerBridge.onStopped(this.level, this.worldPosition);
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
		if (tag.contains("Volume")) this.volume = tag.getInt("Volume");
		if (tag.contains("Range")) this.range = tag.getInt("Range");
		if (tag.contains("Loop")) this.loop = tag.getBoolean("Loop");
		if (tag.contains("Paused")) this.paused = tag.getBoolean("Paused");
		if (tag.contains("ElapsedTicks")) this.elapsedTicks = tag.getInt("ElapsedTicks");
		if (tag.contains("TotalDurationTicks")) this.totalDurationTicks = tag.getInt("TotalDurationTicks");
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, this.items, registries);
		tag.putInt("Volume", this.volume);
		tag.putInt("Range", this.range);
		tag.putBoolean("Loop", this.loop);
		tag.putBoolean("Paused", this.paused);
		tag.putInt("ElapsedTicks", this.elapsedTicks);
		tag.putInt("TotalDurationTicks", this.totalDurationTicks);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
		CompoundTag compoundTag = new CompoundTag();
		this.saveAdditional(compoundTag, provider);
		return compoundTag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	private static final class MusicDiscMakerBridge {
		private static java.lang.reflect.Method ON_CONTENT_CHANGED;
		private static boolean INITIALIZED = false;

		private static void init() {
			if (INITIALIZED) return;
			INITIALIZED = true;
			try {
				Class<?> clazz = Class.forName("com.kuronami.musicdiscmaker.event.JukeboxDiscController");
				ON_CONTENT_CHANGED = clazz.getMethod("onContentChanged", Level.class, BlockPos.class, ItemStack.class);
			} catch (Throwable ignored) {
			}
		}

		public static void onInserted(Level level, BlockPos pos, ItemStack stack) {
			if (level == null || level.isClientSide || pos == null || stack == null || stack.isEmpty()) return;
			init();
			if (ON_CONTENT_CHANGED != null) {
				try {
					ON_CONTENT_CHANGED.invoke(null, level, pos, stack);
				} catch (Throwable ignored) {
				}
			}
		}

		public static void onStopped(Level level, BlockPos pos) {
			if (level == null || level.isClientSide || pos == null) return;
			init();
			if (ON_CONTENT_CHANGED != null) {
				try {
					ON_CONTENT_CHANGED.invoke(null, level, pos, ItemStack.EMPTY);
				} catch (Throwable ignored) {
				}
			}
			sendStopDiscPayload(level, pos);
		}

		private static void sendStopDiscPayload(Level level, BlockPos pos) {
			if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
			try {
				Class<?> payloadClass = Class.forName("com.kuronami.musicdiscmaker.network.StopDiscPayload");
				java.lang.reflect.Constructor<?> ctor = payloadClass.getConstructor(BlockPos.class);
				Object payload = ctor.newInstance(pos);

				Class<?> servicesClass = Class.forName("com.kuronami.musicdiscmaker.platform.Services");
				Object network = servicesClass.getField("NETWORK").get(null);
				net.minecraft.world.level.ChunkPos chunkPos = new net.minecraft.world.level.ChunkPos(pos);

				for (java.lang.reflect.Method m : network.getClass().getMethods()) {
					if ("sendToPlayersTrackingChunk".equals(m.getName()) && m.getParameterCount() == 3) {
						m.invoke(network, serverLevel, chunkPos, payload);
						break;
					}
				}
			} catch (Throwable ignored) {
			}
		}
	}
}
