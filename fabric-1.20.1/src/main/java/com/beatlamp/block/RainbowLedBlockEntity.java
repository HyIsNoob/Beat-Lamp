package com.beatlamp.block;

import java.awt.Color;

import com.beatlamp.BeatLampBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RainbowLedBlockEntity extends BlockEntity {
	public static final int COLOR_RAINBOW = 0;

	public interface ControllerUser {
		void use(RainbowLedBlockEntity led);
	}

	public static ControllerUser controllerUser = led -> {};

	private RainbowLedMode mode = RainbowLedMode.RAINBOW;
	private float speed = 1.0F;
	private int color = COLOR_RAINBOW;
	private int brightness = 15;
	private boolean frameless = false;
	private final java.util.List<BlockPos> manualGroup = new java.util.ArrayList<>();

	public RainbowLedBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BeatLampBlockEntities.RAINBOW_LED, blockPos, blockState);
		if (blockState.hasProperty(RainbowLedBlock.FRAMELESS)) {
			this.frameless = blockState.getValue(RainbowLedBlock.FRAMELESS);
		}
	}

	public RainbowLedMode getMode() {
		return this.mode;
	}

	public void setMode(RainbowLedMode mode) {
		this.mode = mode == null ? RainbowLedMode.RAINBOW : mode;
		this.markUpdated();
	}

	public float getSpeed() {
		return this.speed;
	}

	public void setSpeed(float speed) {
		this.speed = Mth.clamp(speed, 0.25F, 3.0F);
		this.markUpdated();
	}

	public int getColor() {
		return this.color;
	}

	public void setColor(int color) {
		this.color = color;
		this.markUpdated();
	}

	public int getBrightness() {
		return this.brightness;
	}

	public void setBrightness(int brightness) {
		this.brightness = Mth.clamp(brightness, 1, 15);
		this.markUpdated();
	}

	public boolean isFrameless() {
		return this.frameless;
	}

	public void setFrameless(boolean frameless) {
		this.frameless = frameless;
		this.markUpdated();
	}

	public java.util.List<BlockPos> getManualGroup() {
		return java.util.Collections.unmodifiableList(this.manualGroup);
	}

	public void setManualGroup(java.util.List<BlockPos> group) {
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

	public void applyConfig(RainbowLedMode mode, float speed, int color, int brightness, boolean frameless) {
		this.mode = mode == null ? RainbowLedMode.RAINBOW : mode;
		this.speed = Mth.clamp(speed, 0.25F, 3.0F);
		this.color = color;
		this.brightness = Mth.clamp(brightness, 1, 15);
		this.frameless = frameless;
		if (this.level != null) {
			BlockState state = this.getBlockState();
			if (state.hasProperty(RainbowLedBlock.FRAMELESS) && state.getValue(RainbowLedBlock.FRAMELESS) != frameless) {
				this.level.setBlock(this.worldPosition, state.setValue(RainbowLedBlock.FRAMELESS, frameless), 3);
			}
		}
		this.markUpdated();
	}

	public int getCalculatedColor(float partialTick) {
		float time = ((float) (System.currentTimeMillis() % 86400000L) / 50.0F) + partialTick;
		float brightNorm = (float) this.brightness / 15.0F;

		boolean isRainbowColor = (this.color == COLOR_RAINBOW || this.mode == RainbowLedMode.RAINBOW);
		int baseRgb;
		if (isRainbowColor) {
			float hue = (time * this.speed * 0.025F) % 1.0F;
			if (hue < 0) hue += 1.0F;
			baseRgb = Color.HSBtoRGB(hue, 0.9F, 1.0F);
		} else {
			baseRgb = this.color;
		}

		float r = ((baseRgb >> 16) & 0xFF) / 255.0F;
		float g = ((baseRgb >> 8) & 0xFF) / 255.0F;
		float b = (baseRgb & 0xFF) / 255.0F;

		float factor = 1.0F;
		switch (this.mode) {
			case RAINBOW -> {
				factor = 1.0F;
			}
			case BREATHING -> {
				float breath = 0.5F + 0.5F * (float) Math.sin(time * this.speed * 0.12F);
				factor = 0.15F + 0.85F * breath;
			}
			case STROBE -> {
				int phase = (int) (time * this.speed * 0.5F);
				factor = (phase % 2 == 0) ? 1.0F : 0.05F;
			}
			case STATIC -> {
				factor = 1.0F;
			}
			case WAVE -> {
				float phase = (this.worldPosition.getX() * 0.25F + this.worldPosition.getY() * 0.25F + this.worldPosition.getZ() * 0.25F);
				if (isRainbowColor) {
					float hue = (time * this.speed * 0.03F + phase) % 1.0F;
					if (hue < 0) hue += 1.0F;
					baseRgb = Color.HSBtoRGB(hue, 0.9F, 1.0F);
					r = ((baseRgb >> 16) & 0xFF) / 255.0F;
					g = ((baseRgb >> 8) & 0xFF) / 255.0F;
					b = (baseRgb & 0xFF) / 255.0F;
					factor = 1.0F;
				} else {
					float wave = 0.5F + 0.5F * (float) Math.sin(time * this.speed * 0.15F + phase);
					factor = 0.2F + 0.8F * wave;
				}
			}
		}

		factor *= brightNorm;
		int finalR = Mth.clamp((int) (r * factor * 255.0F), 0, 255);
		int finalG = Mth.clamp((int) (g * factor * 255.0F), 0, 255);
		int finalB = Mth.clamp((int) (b * factor * 255.0F), 0, 255);

		return (finalR << 16) | (finalG << 8) | finalB;
	}

	public void markUpdated() {
		this.setChanged();
		Level lvl = this.level;
		if (lvl != null && !lvl.isClientSide) {
			lvl.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag compoundTag) {
		super.saveAdditional(compoundTag);
		compoundTag.putString("mode", this.mode.name());
		compoundTag.putFloat("speed", this.speed);
		compoundTag.putInt("color", this.color);
		compoundTag.putInt("brightness", this.brightness);
		compoundTag.putBoolean("frameless", this.frameless);
		if (!this.manualGroup.isEmpty()) {
			net.minecraft.nbt.ListTag groupTag = new net.minecraft.nbt.ListTag();
			for (BlockPos pos : this.manualGroup) {
				groupTag.add(net.minecraft.nbt.LongTag.valueOf(pos.asLong()));
			}
			compoundTag.put("ManualGroup", groupTag);
		}
	}

	@Override
	public void load(CompoundTag compoundTag) {
		super.load(compoundTag);
		if (compoundTag.contains("mode")) {
			try {
				this.mode = RainbowLedMode.valueOf(compoundTag.getString("mode"));
			} catch (IllegalArgumentException e) {
				this.mode = RainbowLedMode.RAINBOW;
			}
		}
		if (compoundTag.contains("speed")) {
			this.speed = compoundTag.getFloat("speed");
		}
		if (compoundTag.contains("color")) {
			this.color = compoundTag.getInt("color");
		}
		if (compoundTag.contains("brightness")) {
			this.brightness = compoundTag.getInt("brightness");
		}
		if (compoundTag.contains("frameless")) {
			this.frameless = compoundTag.getBoolean("frameless");
		}
		this.manualGroup.clear();
		if (compoundTag.contains("ManualGroup", net.minecraft.nbt.Tag.TAG_LIST)) {
			net.minecraft.nbt.ListTag groupTag = compoundTag.getList("ManualGroup", net.minecraft.nbt.Tag.TAG_LONG);
			for (net.minecraft.nbt.Tag tag : groupTag) {
				if (tag instanceof net.minecraft.nbt.LongTag longTag) {
					this.manualGroup.add(BlockPos.of(longTag.getAsLong()));
				}
			}
		}
	}

	@Override
	public CompoundTag getUpdateTag() {
		return this.saveWithoutMetadata();
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
