package com.beatlamp.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record DmxConsolePayload(
	BlockPos pos,
	boolean blackout,
	boolean strobeAll,
	float masterDimmer,
	float masterSpeed
) {
	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.pos);
		buf.writeBoolean(this.blackout);
		buf.writeBoolean(this.strobeAll);
		buf.writeFloat(this.masterDimmer);
		buf.writeFloat(this.masterSpeed);
	}

	public static DmxConsolePayload read(FriendlyByteBuf buf) {
		return new DmxConsolePayload(
			buf.readBlockPos(),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readFloat(),
			buf.readFloat()
		);
	}
}
