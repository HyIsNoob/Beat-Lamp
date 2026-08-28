package com.beatlamp.network;

import com.beatlamp.BeatLamp;
import com.beatlamp.block.LampMode;
import com.beatlamp.block.LampOrientation;
import com.beatlamp.block.LampParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LampConfigurePayload(
	BlockPos pos,
	LampMode mode,
	float sensitivity,
	float speed,
	int color,
	boolean frameless,
	boolean blackback,
	boolean idleLight,
	boolean reverse,
	LampParticles particles,
	LampOrientation orientation,
	boolean unlink,
	boolean tempoPulse
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<LampConfigurePayload> ID = new CustomPacketPayload.Type<>(BeatLamp.id("configure_lamp"));

	public static final StreamCodec<FriendlyByteBuf, LampConfigurePayload> CODEC = StreamCodec.of(
		LampConfigurePayload::write, LampConfigurePayload::read
	);

	private static void write(FriendlyByteBuf buf, LampConfigurePayload payload) {
		buf.writeBlockPos(payload.pos());
		buf.writeVarInt(payload.mode().ordinal());
		buf.writeFloat(payload.sensitivity());
		buf.writeFloat(payload.speed());
		buf.writeVarInt(payload.color());
		buf.writeBoolean(payload.frameless());
		buf.writeBoolean(payload.blackback());
		buf.writeBoolean(payload.idleLight());
		buf.writeBoolean(payload.reverse());
		buf.writeVarInt(payload.particles().ordinal());
		buf.writeVarInt(payload.orientation().ordinal());
		buf.writeBoolean(payload.unlink());
		buf.writeBoolean(payload.tempoPulse());
	}

	private static LampConfigurePayload read(FriendlyByteBuf buf) {
		return new LampConfigurePayload(
			buf.readBlockPos(),
			LampMode.values()[buf.readVarInt() % LampMode.values().length],
			buf.readFloat(),
			buf.readFloat(),
			buf.readVarInt(),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readBoolean(),
			LampParticles.values()[buf.readVarInt() % LampParticles.values().length],
			LampOrientation.values()[buf.readVarInt() % LampOrientation.values().length],
			buf.readBoolean(),
			buf.readBoolean()
		);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
