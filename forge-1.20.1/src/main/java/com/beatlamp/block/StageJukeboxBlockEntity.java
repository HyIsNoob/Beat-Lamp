package com.beatlamp.block;

import com.beatlamp.BeatLampBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import org.jetbrains.annotations.Nullable;

public class StageJukeboxBlockEntity extends BlockEntity implements Clearable {
	public interface ControllerUser {
		void use(StageJukeboxBlockEntity jukebox);
	}

	public static ControllerUser controllerUser = jukebox -> {};

	private ItemStack record = ItemStack.EMPTY;
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
		if (blockState.getValue(StageJukeboxBlock.HAS_RECORD) && !jukebox.paused && !jukebox.getRecord().isEmpty()) {
			jukebox.elapsedTicks++;
			if (jukebox.totalDurationTicks > 0 && jukebox.elapsedTicks >= jukebox.totalDurationTicks) {
				if (jukebox.loop) {
					jukebox.elapsedTicks = 0;
					if (!level.isClientSide) {
						jukebox.playRecord();
					}
				} else {
					jukebox.elapsedTicks = jukebox.totalDurationTicks;
				}
			}
		}
	}

	public ItemStack getRecord() {
		return this.record;
	}

	public void setRecord(ItemStack stack) {
		this.record = stack;
		this.setChanged();
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

	public void playRecord() {
		if (!this.record.isEmpty() && this.level != null && !this.level.isClientSide) {
			if (this.record.getItem() instanceof RecordItem recordItem) {
				this.totalDurationTicks = recordItem.getLengthInTicks();
				this.level.levelEvent(null, 1010, this.worldPosition, Item.getId(this.record.getItem()));
			} else {
				this.totalDurationTicks = resolveCustomDurationTicks(this.record);
			}
			this.elapsedTicks = 0;
			this.paused = false;
			this.setChanged();
			MusicDiscMakerBridge.onInserted(this.level, this.worldPosition, this.record);
		}
	}

	private static int resolveCustomDurationTicks(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return 3600;
		CompoundTag tag = stack.getTag();
		if (tag != null && tag.contains("custom_track")) {
			CompoundTag ct = tag.getCompound("custom_track");
			if (ct.contains("durationMs")) {
				long ms = ct.getLong("durationMs");
				if (ms > 0) return (int) (ms / 50L);
			}
		}
		return 3600;
	}

	public void stopRecord() {
		if (this.level != null && !this.level.isClientSide) {
			this.level.levelEvent(null, 1011, this.worldPosition, 0);
			this.paused = false;
			this.elapsedTicks = 0;
			this.setChanged();
			MusicDiscMakerBridge.onStopped(this.level, this.worldPosition);
		}
	}

	public void dropRecord() {
		this.ejectRecord(null);
	}

	public void ejectRecord(@Nullable Player player) {
		if (this.level != null && !this.level.isClientSide) {
			if (!this.record.isEmpty()) {
				ItemStack drop = this.record.copy();
				this.stopRecord();
				this.setRecord(ItemStack.EMPTY);
				this.level.setBlock(this.worldPosition, this.getBlockState().setValue(StageJukeboxBlock.HAS_RECORD, false), 3);
				this.level.gameEvent(null, GameEvent.JUKEBOX_STOP_PLAY, this.worldPosition);
				this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);

				if (player != null && !player.getAbilities().instabuild) {
					if (!player.getInventory().add(drop)) {
						Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.9, this.worldPosition.getZ() + 0.5, drop);
					}
				} else if (player == null) {
					Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.9, this.worldPosition.getZ() + 0.5, drop);
				}
			}
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
		if (tag.contains("Volume")) this.volume = tag.getInt("Volume");
		if (tag.contains("Range")) this.range = tag.getInt("Range");
		if (tag.contains("Loop")) this.loop = tag.getBoolean("Loop");
		if (tag.contains("Paused")) this.paused = tag.getBoolean("Paused");
		if (tag.contains("ElapsedTicks")) this.elapsedTicks = tag.getInt("ElapsedTicks");
		if (tag.contains("TotalDurationTicks")) this.totalDurationTicks = tag.getInt("TotalDurationTicks");
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (!this.getRecord().isEmpty()) {
			tag.put("RecordItem", this.getRecord().save(new CompoundTag()));
		}
		tag.putInt("Volume", this.volume);
		tag.putInt("Range", this.range);
		tag.putBoolean("Loop", this.loop);
		tag.putBoolean("Paused", this.paused);
		tag.putInt("ElapsedTicks", this.elapsedTicks);
		tag.putInt("TotalDurationTicks", this.totalDurationTicks);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag() {
		return this.saveWithoutMetadata();
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
