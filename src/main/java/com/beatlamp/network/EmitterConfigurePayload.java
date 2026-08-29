package com.beatlamp.network;

import com.beatlamp.BeatLamp;
import com.beatlamp.block.EmitterMode;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EmitterConfigurePayload(
	BlockPos pos,
	EmitterMode mode,
	float threshold,
	boolean inverted,
	boolean unlink,
	boolean dmxEnrolled,
	String customName
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<EmitterConfigurePayload> ID = new CustomPacketPayload.Type<>(BeatLamp.id("configure_emitter"));

	public static final StreamCodec<FriendlyByteBuf, EmitterConfigurePayload> CODEC = StreamCodec.of(
		EmitterConfigurePayload::write, EmitterConfigurePayload::read
	);

	private static void write(FriendlyByteBuf buf, EmitterConfigurePayload payload) {
		buf.writeBlockPos(payload.pos());
		buf.writeVarInt(payload.mode().ordinal());
		buf.writeFloat(payload.threshold());
		buf.writeBoolean(payload.inverted());
		buf.writeBoolean(payload.unlink());
		buf.writeBoolean(payload.dmxEnrolled());
		buf.writeUtf(payload.customName() == null ? "" : payload.customName(), 32);
	}

	private static EmitterConfigurePayload read(FriendlyByteBuf buf) {
		return new EmitterConfigurePayload(
			buf.readBlockPos(),
			EmitterMode.values()[buf.readVarInt() % EmitterMode.values().length],
			buf.readFloat(),
			buf.readBoolean(),
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
