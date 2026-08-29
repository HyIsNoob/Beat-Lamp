package com.beatlamp.block;

import java.util.ArrayList;
import java.util.List;

import com.beatlamp.BeatLampBlockEntities;
import com.beatlamp.BeatLampBlocks;
import com.beatlamp.JukeboxTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BeatLampBlockEntity extends BlockEntity {
	public static final int COLOR_OLED = 0;

	public interface ClientTicker {
		void tick(BeatLampBlockEntity beatLamp);
	}

	public interface ControllerUser {
		void use(BeatLampBlockEntity beatLamp);
	}

	public static ClientTicker clientTicker = beatLamp -> {
	};

	public static ControllerUser controllerUser = beatLamp -> {
	};

	private int color = COLOR_OLED;
	private LampMode mode = LampMode.PULSE;
	private float sensitivity = 1.0F;
	private float speed = 1.0F;
	private boolean frameless = true;
	private boolean blackback = true;
	private boolean idleLight;
	private boolean reverse;
	private boolean tempoPulse = true;
	private LampParticles particles = LampParticles.NOTE;
	private LampOrientation orientation = LampOrientation.AUTO;
	private List<BlockPos> manualGroup = List.of();
	private BlockPos sourcePos;

	public float pulse;
	public float beatPulse;
	public float smoothLevel;
	public float barValue;
	public float spectrumLevel;
	public float peakLevel;
	public int displayColor = COLOR_OLED;
	public int groupIndex;
	public int groupSize = 1;
	public float groupDistance;
	public int columnIndex;
	public int columnSize = 1;
	public int bandIndex;
	public int bandCount = 1;

	public BeatLampBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BeatLampBlockEntities.BEAT_LAMP, blockPos, blockState);
	}

	public int getColor() {
		return this.color;
	}

	public LampMode getMode() {
		return this.mode;
	}

	public float getSensitivity() {
		return this.sensitivity;
	}

	public float getSpeed() {
		return this.speed;
	}

	public boolean isFrameless() {
		return this.frameless;
	}

	public boolean isBlackback() {
		return this.blackback;
	}

	public boolean isIdleLight() {
		return this.idleLight;
	}

	public boolean isReverse() {
		return this.reverse;
	}

	public LampParticles getParticles() {
		return this.particles;
	}

	public LampOrientation getOrientation() {
		return this.orientation;
	}

	public List<BlockPos> getManualGroup() {
		return this.manualGroup;
	}

	public BlockPos getSource() {
		return this.sourcePos;
	}

	public void setSource(BlockPos newSource) {
		this.sourcePos = newSource == null ? null : newSource.immutable();
		this.markUpdated();
	}

	public boolean setColor(int newColor) {
		if (this.color == newColor) {
			return false;
		}

		this.color = newColor;
		this.markUpdated();
		return true;
	}

	public void cycleMode(Player player) {
		this.applyMode(this.mode.next());
		player.displayClientMessage(Component.translatable("message.beatlamp.mode." + this.mode.getSerializedName()), true);
	}

	public void cycleColor(Player player) {
		net.minecraft.world.item.DyeColor[] values = net.minecraft.world.item.DyeColor.values();
		int index = -1;

		for (int i = 0; i < values.length; i++) {
			if (values[i].getFireworkColor() == this.color) {
				index = i;
				break;
			}
		}

		this.setColor(values[(index + 1) % values.length].getFireworkColor());
		player.displayClientMessage(Component.translatable("message.beatlamp.color"), true);
	}

	public void applyMode(LampMode newMode) {
		this.mode = newMode;
		this.markUpdated();
	}

	public boolean isTempoPulse() {
		return this.tempoPulse;
	}

	public void setTempoPulse(boolean tempoPulse) {
		this.tempoPulse = tempoPulse;
		this.markUpdated();
	}

	private boolean dmxEnrolled = true;

	public boolean isDmxEnrolled() {
		return this.dmxEnrolled;
	}

	public void setDmxEnrolled(boolean dmxEnrolled) {
		this.dmxEnrolled = dmxEnrolled;
		this.markUpdated();
	}

	public void applyConfig(
		LampMode newMode,
		float newSensitivity,
		float newSpeed,
		int newColor,
		boolean newFrameless,
		boolean newBlackback,
		boolean newIdleLight,
		boolean newReverse,
		LampParticles newParticles,
		LampOrientation newOrientation,
		boolean newTempoPulse,
		boolean newDmxEnrolled
	) {
		this.mode = newMode;
		this.sensitivity = newSensitivity;
		this.speed = newSpeed;
		this.color = newColor;
		this.blackback = newBlackback;
		this.idleLight = newIdleLight;
		this.reverse = newReverse;
		this.particles = newParticles;
		this.orientation = newOrientation;
		this.tempoPulse = newTempoPulse;
		this.dmxEnrolled = newDmxEnrolled;
		this.setFrameless(newFrameless);
		this.markUpdated();
	}

	public void setFrameless(boolean newFrameless) {
		if (this.frameless != newFrameless) {
			this.frameless = newFrameless;
			if (this.level != null && this.level.getBlockState(this.worldPosition).is(BeatLampBlocks.BEAT_LAMP)) {
				this.level.setBlock(
					this.worldPosition,
					this.level.getBlockState(this.worldPosition).setValue(BeatLampBlock.FRAMELESS, newFrameless),
					Block.UPDATE_CLIENTS
				);
			}
		}
	}

	public void setManualGroup(List<BlockPos> group) {
		this.manualGroup = List.copyOf(group);
		this.markUpdated();
	}

	public void clearManualGroup() {
		this.manualGroup = List.of();
		this.markUpdated();
	}

	public void removeFromManualGroup(BlockPos removed) {
		if (this.manualGroup.contains(removed)) {
			List<BlockPos> members = new ArrayList<>(this.manualGroup);
			members.remove(removed);
			this.manualGroup = members.size() >= 2 ? List.copyOf(members) : List.of();
			this.markUpdated();
		}
	}

	public void onRemovedFromWorld() {
		if (this.level == null || this.manualGroup.isEmpty()) {
			return;
		}

		for (BlockPos member : this.manualGroup) {
			if (member.equals(this.worldPosition)) {
				continue;
			}

			if (this.level.getBlockEntity(member) instanceof BeatLampBlockEntity other) {
				other.removeFromManualGroup(this.worldPosition);
			}
		}
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
		compoundTag.putString("mode", this.mode.getSerializedName());
		compoundTag.putFloat("sensitivity", this.sensitivity);
		compoundTag.putFloat("speed", this.speed);
		compoundTag.putBoolean("frameless", this.frameless);
		compoundTag.putBoolean("blackback", this.blackback);
		compoundTag.putBoolean("idleLight", this.idleLight);
		compoundTag.putBoolean("reverse", this.reverse);
		compoundTag.putBoolean("tempoPulse", this.tempoPulse);
		compoundTag.putBoolean("dmxEnrolled", this.dmxEnrolled);
		compoundTag.putString("particles", this.particles.getSerializedName());
		compoundTag.putString("orientation", this.orientation.getSerializedName());

		if (!this.manualGroup.isEmpty()) {
			ListTag listTag = new ListTag();

			for (BlockPos member : this.manualGroup) {
				listTag.add(LongTag.valueOf(member.asLong()));
			}

			compoundTag.put("group", listTag);
		}

		if (this.sourcePos != null) {
			compoundTag.putLong("source", this.sourcePos.asLong());
		}
	}

	@Override
	protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
		super.loadAdditional(compoundTag, provider);
		this.color = compoundTag.contains("color") ? compoundTag.getInt("color") : COLOR_OLED;
		this.mode = LampMode.byName(compoundTag.getString("mode"));
		this.sensitivity = compoundTag.contains("sensitivity") ? compoundTag.getFloat("sensitivity") : 1.0F;
		this.speed = compoundTag.contains("speed") ? compoundTag.getFloat("speed") : 1.0F;
		this.frameless = !compoundTag.contains("frameless") || compoundTag.getBoolean("frameless");
		this.blackback = !compoundTag.contains("blackback") || compoundTag.getBoolean("blackback");
		this.idleLight = compoundTag.contains("idleLight") && compoundTag.getBoolean("idleLight");
		this.reverse = compoundTag.getBoolean("reverse");
		this.tempoPulse = !compoundTag.contains("tempoPulse") || compoundTag.getBoolean("tempoPulse");
		this.dmxEnrolled = !compoundTag.contains("dmxEnrolled") || compoundTag.getBoolean("dmxEnrolled");
		this.particles = LampParticles.byName(compoundTag.getString("particles"));
		this.orientation = LampOrientation.byName(compoundTag.getString("orientation"));

		List<BlockPos> members = new ArrayList<>();
		ListTag listTag = compoundTag.getList("group", 4);

		for (int i = 0; i < listTag.size(); i++) {
			members.add(BlockPos.of(((LongTag) listTag.get(i)).getAsLong()));
		}

		this.manualGroup = members.size() >= 2 ? List.copyOf(members) : List.of();
		this.sourcePos = compoundTag.contains("source") ? BlockPos.of(compoundTag.getLong("source")) : null;
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

	public static void clientTick(Level level, BlockPos blockPos, BlockState blockState, BeatLampBlockEntity beatLamp) {
		clientTicker.tick(beatLamp);
	}

	public static void serverTick(Level level, BlockPos blockPos, BlockState blockState, BeatLampBlockEntity beatLamp) {
		long gameTime = level.getGameTime();
		if ((gameTime + Math.abs(blockPos.hashCode())) % 20L != 0L) {
			return;
		}

		boolean desired = beatLamp.idleLight || JukeboxTracker.isPlayingNear(level, blockPos, beatLamp.getSource());

		if (blockState.getValue(BeatLampBlock.LIT) != desired) {
			level.setBlock(blockPos, blockState.setValue(BeatLampBlock.LIT, desired), Block.UPDATE_CLIENTS);
		}
	}
}
