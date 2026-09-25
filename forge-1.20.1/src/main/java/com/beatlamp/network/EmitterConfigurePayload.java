package com.beatlamp.network;

import com.beatlamp.block.EmitterMode;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record EmitterConfigurePayload(
	BlockPos pos,
	EmitterMode mode,
	float threshold,
	boolean inverted,
	boolean unlink,
	boolean dmxEnrolled,
	String customName
) {
	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.pos);
		buf.writeEnum(this.mode);
		buf.writeFloat(this.threshold);
		buf.writeBoolean(this.inverted);
		buf.writeBoolean(this.unlink);
		buf.writeBoolean(this.dmxEnrolled);
		buf.writeUtf(this.customName != null ? this.customName : "", 64);
	}

	public static EmitterConfigurePayload read(FriendlyByteBuf buf) {
		return new EmitterConfigurePayload(
			buf.readBlockPos(),
			buf.readEnum(EmitterMode.class),
			buf.readFloat(),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readUtf(64)
		);
	}
}
