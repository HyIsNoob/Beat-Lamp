package com.beatlamp.network;

import com.beatlamp.BeatLamp;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EmitterSignalPayload(BlockPos pos, int signal) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<EmitterSignalPayload> ID = new CustomPacketPayload.Type<>(BeatLamp.id("emitter_signal"));

	public static final StreamCodec<FriendlyByteBuf, EmitterSignalPayload> CODEC = StreamCodec.of(
		EmitterSignalPayload::write, EmitterSignalPayload::read
	);

	private static void write(FriendlyByteBuf buf, EmitterSignalPayload payload) {
		buf.writeBlockPos(payload.pos());
		buf.writeByte(payload.signal());
	}

	private static EmitterSignalPayload read(FriendlyByteBuf buf) {
		return new EmitterSignalPayload(buf.readBlockPos(), buf.readByte());
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
