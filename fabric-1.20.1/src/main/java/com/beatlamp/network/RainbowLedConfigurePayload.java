package com.beatlamp.network;

import com.beatlamp.block.RainbowLedMode;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record RainbowLedConfigurePayload(
	BlockPos pos,
	RainbowLedMode mode,
	float speed,
	int color,
	int brightness,
	boolean frameless,
	boolean unlink
) {
	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.pos);
		buf.writeEnum(this.mode);
		buf.writeFloat(this.speed);
		buf.writeInt(this.color);
		buf.writeInt(this.brightness);
		buf.writeBoolean(this.frameless);
		buf.writeBoolean(this.unlink);
	}

	public static RainbowLedConfigurePayload read(FriendlyByteBuf buf) {
		return new RainbowLedConfigurePayload(
			buf.readBlockPos(),
			buf.readEnum(RainbowLedMode.class),
			buf.readFloat(),
			buf.readInt(),
			buf.readInt(),
			buf.readBoolean(),
			buf.readBoolean()
		);
	}
}
