package com.beatlamp.network;

import com.beatlamp.block.StageLightMode;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record StageLightConfigurePayload(
	BlockPos pos,
	StageLightMode mode,
	float sensitivity,
	float speed,
	int color,
	boolean unlink,
	boolean dmxEnrolled,
	String customName
) {
	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.pos);
		buf.writeEnum(this.mode);
		buf.writeFloat(this.sensitivity);
		buf.writeFloat(this.speed);
		buf.writeInt(this.color);
		buf.writeBoolean(this.unlink);
		buf.writeBoolean(this.dmxEnrolled);
		buf.writeUtf(this.customName != null ? this.customName : "", 64);
	}

	public static StageLightConfigurePayload read(FriendlyByteBuf buf) {
		return new StageLightConfigurePayload(
			buf.readBlockPos(),
			buf.readEnum(StageLightMode.class),
			buf.readFloat(),
			buf.readFloat(),
			buf.readInt(),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readUtf(64)
		);
	}
}
