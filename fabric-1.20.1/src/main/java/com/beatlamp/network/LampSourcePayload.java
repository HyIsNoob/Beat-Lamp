package com.beatlamp.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record LampSourcePayload(
	BlockPos targetPos,
	BlockPos sourcePos
) {
	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.targetPos);
		buf.writeBoolean(this.sourcePos != null);
		if (this.sourcePos != null) {
			buf.writeBlockPos(this.sourcePos);
		}
	}

	public static LampSourcePayload read(FriendlyByteBuf buf) {
		BlockPos target = buf.readBlockPos();
		BlockPos source = buf.readBoolean() ? buf.readBlockPos() : null;
		return new LampSourcePayload(target, source);
	}
}
