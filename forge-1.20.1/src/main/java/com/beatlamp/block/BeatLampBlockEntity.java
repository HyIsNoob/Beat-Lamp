package com.beatlamp.block;

import java.util.ArrayList;
import java.util.List;

import com.beatlamp.BeatLampBlockEntities;
import com.beatlamp.BeatLampBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
	private boolean dmxEnrolled = false;
	private String customName = "";
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

	public boolean isTempoPulse() {
		return this.tempoPulse;
	}

	public boolean isDmxEnrolled() {
		return this.dmxEnrolled;
	}

	public void setDmxEnrolled(boolean enrolled) {
		this.dmxEnrolled = enrolled;
		this.markUpdated();
	}

	public String getCustomName() {
		return this.customName;
	}

	public void setCustomName(String name) {
		this.customName = name == null ? "" : name.trim();
		this.markUpdated();
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

	public void applyConfig(LampMode mode, float sensitivity, float speed, int color,
		boolean frameless, boolean blackback, boolean idleLight, boolean reverse,
		LampParticles particles, LampOrientation orientation, boolean tempoPulse,
		boolean dmxEnrolled, String customName) {
		this.mode = mode == null ? LampMode.PULSE : mode;
		this.sensitivity = net.minecraft.util.Mth.clamp(sensitivity, 0.25F, 4.0F);
		this.speed = net.minecraft.util.Mth.clamp(speed, 0.25F, 4.0F);
		this.color = color;
		this.frameless = frameless;
		this.blackback = blackback;
		this.idleLight = idleLight;
		this.reverse = reverse;
		this.particles = particles == null ? LampParticles.NOTE : particles;
		this.orientation = orientation == null ? LampOrientation.AUTO : orientation;
		this.tempoPulse = tempoPulse;
		this.dmxEnrolled = dmxEnrolled;
		this.customName = customName == null ? "" : customName.trim();
		if (this.level != null && !this.level.isClientSide) {
			BlockState current = this.level.getBlockState(this.worldPosition);
			if (current.is(BeatLampBlocks.BEAT_LAMP) && current.hasProperty(BeatLampBlock.FRAMELESS) && current.getValue(BeatLampBlock.FRAMELESS) != frameless) {
				this.level.setBlock(this.worldPosition, current.setValue(BeatLampBlock.FRAMELESS, frameless), Block.UPDATE_ALL);
			}
		}
		this.markUpdated();
	}

	public void applyMode(LampMode newMode) {
		if (this.mode == newMode) {
			return;
		}

		this.mode = newMode == null ? LampMode.PULSE : newMode;
		this.markUpdated();
	}

	public void setManualGroup(List<BlockPos> group) {
		this.manualGroup = group == null ? List.of() : List.copyOf(group);
		this.markUpdated();
	}

	public void clearManualGroup() {
		if (this.manualGroup.isEmpty()) {
			return;
		}

		this.manualGroup = List.of();
		this.markUpdated();
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
		this.color = tag.getInt("Color");
		this.mode = LampMode.values()[Math.floorMod(tag.getInt("Mode"), LampMode.values().length)];
		this.sensitivity = tag.contains("Sensitivity") ? tag.getFloat("Sensitivity") : 1.0F;
		this.speed = tag.contains("Speed") ? tag.getFloat("Speed") : 1.0F;
		this.frameless = !tag.contains("Frameless") || tag.getBoolean("Frameless");
		this.blackback = !tag.contains("Blackback") || tag.getBoolean("Blackback");
		this.idleLight = tag.getBoolean("IdleLight");
		this.reverse = tag.getBoolean("Reverse");
		this.tempoPulse = !tag.contains("TempoPulse") || tag.getBoolean("TempoPulse");
		this.dmxEnrolled = tag.getBoolean("DmxEnrolled");
		this.customName = tag.getString("CustomName");
		this.particles = LampParticles.values()[Math.floorMod(tag.getInt("Particles"), LampParticles.values().length)];
		this.orientation = LampOrientation.values()[Math.floorMod(tag.getInt("Orientation"), LampOrientation.values().length)];

		if (tag.contains("SourcePos")) {
			this.sourcePos = BlockPos.of(tag.getLong("SourcePos"));
		} else {
			this.sourcePos = null;
		}

		if (tag.contains("ManualGroup", Tag.TAG_LIST)) {
			ListTag list = tag.getList("ManualGroup", Tag.TAG_LONG);
			List<BlockPos> group = new ArrayList<>(list.size());

			for (int i = 0; i < list.size(); i++) {
				group.add(BlockPos.of(((LongTag) list.get(i)).getAsLong()));
			}

			this.manualGroup = List.copyOf(group);
		} else {
			this.manualGroup = List.of();
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("Color", this.color);
		tag.putInt("Mode", this.mode.ordinal());
		tag.putFloat("Sensitivity", this.sensitivity);
		tag.putFloat("Speed", this.speed);
		tag.putBoolean("Frameless", this.frameless);
		tag.putBoolean("Blackback", this.blackback);
		tag.putBoolean("IdleLight", this.idleLight);
		tag.putBoolean("Reverse", this.reverse);
		tag.putBoolean("TempoPulse", this.tempoPulse);
		tag.putBoolean("DmxEnrolled", this.dmxEnrolled);
		tag.putString("CustomName", this.customName);
		tag.putInt("Particles", this.particles.ordinal());
		tag.putInt("Orientation", this.orientation.ordinal());

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
