package com.beatlamp.network;

import com.beatlamp.BeatLamp;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FountainFirePayload(BlockPos pos) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<FountainFirePayload> ID = new CustomPacketPayload.Type<>(BeatLamp.id("fountain_fire"));

	public static final StreamCodec<FriendlyByteBuf, FountainFirePayload> CODEC = StreamCodec.of(
		FountainFirePayload::write, FountainFirePayload::read
	);

	private static void write(FriendlyByteBuf buf, FountainFirePayload payload) {
		buf.writeBlockPos(payload.pos());
	}

	private static FountainFirePayload read(FriendlyByteBuf buf) {
		return new FountainFirePayload(buf.readBlockPos());
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
