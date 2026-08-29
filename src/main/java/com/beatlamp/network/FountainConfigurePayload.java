package com.beatlamp.network;

import com.beatlamp.BeatLamp;
import com.beatlamp.block.FountainParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FountainConfigurePayload(
	BlockPos pos,
	boolean fireworkMode,
	float sprayThreshold,
	float impactThreshold,
	boolean smokeEnabled,
	FountainParticles particleType,
	int color,
	boolean unlink,
	boolean dmxEnrolled
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<FountainConfigurePayload> ID = new CustomPacketPayload.Type<>(BeatLamp.id("configure_fountain"));

	public static final StreamCodec<FriendlyByteBuf, FountainConfigurePayload> CODEC = StreamCodec.of(
		FountainConfigurePayload::write, FountainConfigurePayload::read
	);

	private static void write(FriendlyByteBuf buf, FountainConfigurePayload payload) {
		buf.writeBlockPos(payload.pos());
		buf.writeBoolean(payload.fireworkMode());
		buf.writeFloat(payload.sprayThreshold());
		buf.writeFloat(payload.impactThreshold());
		buf.writeBoolean(payload.smokeEnabled());
		buf.writeVarInt(payload.particleType().ordinal());
		buf.writeVarInt(payload.color());
		buf.writeBoolean(payload.unlink());
		buf.writeBoolean(payload.dmxEnrolled());
	}

	private static FountainConfigurePayload read(FriendlyByteBuf buf) {
		return new FountainConfigurePayload(
			buf.readBlockPos(),
			buf.readBoolean(),
			buf.readFloat(),
			buf.readFloat(),
			buf.readBoolean(),
			FountainParticles.values()[buf.readVarInt() % FountainParticles.values().length],
			buf.readVarInt(),
			buf.readBoolean(),
			buf.readBoolean()
		);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
