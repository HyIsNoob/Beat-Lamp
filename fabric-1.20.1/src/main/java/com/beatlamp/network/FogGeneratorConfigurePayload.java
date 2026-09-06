package com.beatlamp.network;

import com.beatlamp.block.FogDensity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record FogGeneratorConfigurePayload(
	BlockPos pos,
	FogDensity density,
	float radius,
	int color,
	boolean unlink,
	boolean dmxEnrolled,
	String customName
) {
	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.pos);
		buf.writeEnum(this.density);
		buf.writeFloat(this.radius);
		buf.writeInt(this.color);
		buf.writeBoolean(this.unlink);
		buf.writeBoolean(this.dmxEnrolled);
		buf.writeUtf(this.customName != null ? this.customName : "", 64);
	}

	public static FogGeneratorConfigurePayload read(FriendlyByteBuf buf) {
		return new FogGeneratorConfigurePayload(
			buf.readBlockPos(),
			buf.readEnum(FogDensity.class),
			buf.readFloat(),
			buf.readInt(),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readUtf(64)
		);
	}
}
