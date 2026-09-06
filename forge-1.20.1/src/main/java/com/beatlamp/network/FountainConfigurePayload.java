package com.beatlamp.network;

import com.beatlamp.block.FountainParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record FountainConfigurePayload(
	BlockPos pos,
	int color,
	boolean fireworkMode,
	float sprayThreshold,
	float impactThreshold,
	boolean smokeEnabled,
	FountainParticles particleType,
	boolean unlink,
	boolean dmxEnrolled,
	String customName
) {
	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.pos);
		buf.writeInt(this.color);
		buf.writeBoolean(this.fireworkMode);
		buf.writeFloat(this.sprayThreshold);
		buf.writeFloat(this.impactThreshold);
		buf.writeBoolean(this.smokeEnabled);
		buf.writeEnum(this.particleType);
		buf.writeBoolean(this.unlink);
		buf.writeBoolean(this.dmxEnrolled);
		buf.writeUtf(this.customName != null ? this.customName : "", 64);
	}

	public static FountainConfigurePayload read(FriendlyByteBuf buf) {
		return new FountainConfigurePayload(
			buf.readBlockPos(),
			buf.readInt(),
			buf.readBoolean(),
			buf.readFloat(),
			buf.readFloat(),
			buf.readBoolean(),
			buf.readEnum(FountainParticles.class),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readUtf(64)
		);
	}
}
