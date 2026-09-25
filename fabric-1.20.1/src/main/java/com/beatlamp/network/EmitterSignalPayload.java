package com.beatlamp.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record EmitterSignalPayload(BlockPos pos, int signal) {
	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.pos);
		buf.writeByte(this.signal);
	}

	public static EmitterSignalPayload read(FriendlyByteBuf buf) {
		return new EmitterSignalPayload(buf.readBlockPos(), buf.readByte());
	}
}
