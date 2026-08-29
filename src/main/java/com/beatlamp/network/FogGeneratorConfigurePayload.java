package com.beatlamp.network;

import com.beatlamp.BeatLamp;
import com.beatlamp.block.FogDensity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FogGeneratorConfigurePayload(
	BlockPos pos,
	FogDensity density,
	int radius,
	int color,
	boolean unlink,
	boolean dmxEnrolled,
	String customName
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<FogGeneratorConfigurePayload> ID = new CustomPacketPayload.Type<>(BeatLamp.id("configure_fog"));

	public static final StreamCodec<FriendlyByteBuf, FogGeneratorConfigurePayload> CODEC = StreamCodec.of(
		FogGeneratorConfigurePayload::write, FogGeneratorConfigurePayload::read
	);

	private static void write(FriendlyByteBuf buf, FogGeneratorConfigurePayload payload) {
		buf.writeBlockPos(payload.pos());
		buf.writeVarInt(payload.density().ordinal());
		buf.writeVarInt(payload.radius());
		buf.writeVarInt(payload.color());
		buf.writeBoolean(payload.unlink());
		buf.writeBoolean(payload.dmxEnrolled());
		buf.writeUtf(payload.customName() == null ? "" : payload.customName(), 32);
	}

	private static FogGeneratorConfigurePayload read(FriendlyByteBuf buf) {
		return new FogGeneratorConfigurePayload(
			buf.readBlockPos(),
			FogDensity.values()[buf.readVarInt() % FogDensity.values().length],
			buf.readVarInt(),
			buf.readVarInt(),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readUtf(32)
		);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
