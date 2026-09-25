package com.beatlamp.network;

import com.beatlamp.BeatLamp;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LampSourcePayload(BlockPos pos) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<LampSourcePayload> ID = new CustomPacketPayload.Type<>(BeatLamp.id("clear_lamp_source"));

	public static final StreamCodec<FriendlyByteBuf, LampSourcePayload> CODEC = StreamCodec.of(
		LampSourcePayload::write, LampSourcePayload::read
	);

	private static void write(FriendlyByteBuf buf, LampSourcePayload payload) {
		buf.writeBlockPos(payload.pos());
	}

	private static LampSourcePayload read(FriendlyByteBuf buf) {
		return new LampSourcePayload(buf.readBlockPos());
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
