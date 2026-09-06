package com.beatlamp.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record FountainFirePayload(
	BlockPos pos
) {
	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.pos);
	}

	public static FountainFirePayload read(FriendlyByteBuf buf) {
		return new FountainFirePayload(buf.readBlockPos());
	}
}
