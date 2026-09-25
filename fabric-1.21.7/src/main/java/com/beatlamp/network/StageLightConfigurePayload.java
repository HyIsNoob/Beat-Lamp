package com.beatlamp.network;

import com.beatlamp.BeatLamp;
import com.beatlamp.block.StageLightMode;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StageLightConfigurePayload(
	BlockPos pos,
	StageLightMode mode,
	float sensitivity,
	float speed,
	int color,
	boolean unlink,
	boolean tempoPulse,
	boolean dmxEnrolled,
	String customName
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<StageLightConfigurePayload> ID = new CustomPacketPayload.Type<>(BeatLamp.id("configure_stage_light"));

	public static final StreamCodec<FriendlyByteBuf, StageLightConfigurePayload> CODEC = StreamCodec.of(
		StageLightConfigurePayload::write, StageLightConfigurePayload::read
	);

	private static void write(FriendlyByteBuf buf, StageLightConfigurePayload payload) {
		buf.writeBlockPos(payload.pos());
		buf.writeVarInt(payload.mode().ordinal());
		buf.writeFloat(payload.sensitivity());
		buf.writeFloat(payload.speed());
		buf.writeVarInt(payload.color());
		buf.writeBoolean(payload.unlink());
		buf.writeBoolean(payload.tempoPulse());
		buf.writeBoolean(payload.dmxEnrolled());
		buf.writeUtf(payload.customName() == null ? "" : payload.customName(), 32);
	}

	private static StageLightConfigurePayload read(FriendlyByteBuf buf) {
		return new StageLightConfigurePayload(
			buf.readBlockPos(),
			StageLightMode.values()[buf.readVarInt() % StageLightMode.values().length],
			buf.readFloat(),
			buf.readFloat(),
			buf.readVarInt(),
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
