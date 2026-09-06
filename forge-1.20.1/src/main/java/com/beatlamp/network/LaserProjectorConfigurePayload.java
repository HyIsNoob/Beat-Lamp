package com.beatlamp.network;

import com.beatlamp.block.LaserMode;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record LaserProjectorConfigurePayload(
	BlockPos pos,
	LaserMode mode,
	int beamCount,
	float spread,
	float speed,
	int color,
	boolean unlink,
	boolean dmxEnrolled,
	String customName
) {
	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.pos);
		buf.writeEnum(this.mode);
		buf.writeInt(this.beamCount);
		buf.writeFloat(this.spread);
		buf.writeFloat(this.speed);
		buf.writeInt(this.color);
		buf.writeBoolean(this.unlink);
		buf.writeBoolean(this.dmxEnrolled);
		buf.writeUtf(this.customName != null ? this.customName : "", 64);
	}

	public static LaserProjectorConfigurePayload read(FriendlyByteBuf buf) {
		return new LaserProjectorConfigurePayload(
			buf.readBlockPos(),
			buf.readEnum(LaserMode.class),
			buf.readInt(),
			buf.readFloat(),
			buf.readFloat(),
			buf.readInt(),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readUtf(64)
		);
	}
}
