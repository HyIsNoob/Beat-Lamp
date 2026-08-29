package com.beatlamp.network;

import com.beatlamp.BeatLamp;
import com.beatlamp.block.LaserMode;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LaserProjectorConfigurePayload(
	BlockPos pos,
	LaserMode mode,
	int beamCount,
	float spread,
	float speed,
	int color,
	boolean unlink,
	boolean dmxEnrolled
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<LaserProjectorConfigurePayload> ID = new CustomPacketPayload.Type<>(BeatLamp.id("configure_laser"));

	public static final StreamCodec<FriendlyByteBuf, LaserProjectorConfigurePayload> CODEC = StreamCodec.of(
		LaserProjectorConfigurePayload::write, LaserProjectorConfigurePayload::read
	);

	private static void write(FriendlyByteBuf buf, LaserProjectorConfigurePayload payload) {
		buf.writeBlockPos(payload.pos());
		buf.writeVarInt(payload.mode().ordinal());
		buf.writeVarInt(payload.beamCount());
		buf.writeFloat(payload.spread());
		buf.writeFloat(payload.speed());
		buf.writeVarInt(payload.color());
		buf.writeBoolean(payload.unlink());
		buf.writeBoolean(payload.dmxEnrolled());
	}

	private static LaserProjectorConfigurePayload read(FriendlyByteBuf buf) {
		return new LaserProjectorConfigurePayload(
			buf.readBlockPos(),
			LaserMode.values()[buf.readVarInt() % LaserMode.values().length],
			buf.readVarInt(),
			buf.readFloat(),
			buf.readFloat(),
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
