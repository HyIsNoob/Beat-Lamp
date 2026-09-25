package com.beatlamp.network;

import com.beatlamp.BeatLamp;
import com.beatlamp.block.RainbowLedMode;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RainbowLedConfigurePayload(
	BlockPos pos,
	RainbowLedMode mode,
	float speed,
	int color,
	int brightness,
	boolean frameless,
	boolean unlink
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RainbowLedConfigurePayload> ID = new CustomPacketPayload.Type<>(BeatLamp.id("configure_rainbow_led"));

	public static final StreamCodec<FriendlyByteBuf, RainbowLedConfigurePayload> CODEC = StreamCodec.of(
		RainbowLedConfigurePayload::write, RainbowLedConfigurePayload::read
	);

	private static void write(FriendlyByteBuf buf, RainbowLedConfigurePayload payload) {
		buf.writeBlockPos(payload.pos());
		buf.writeVarInt(payload.mode().ordinal());
		buf.writeFloat(payload.speed());
		buf.writeVarInt(payload.color());
		buf.writeVarInt(payload.brightness());
		buf.writeBoolean(payload.frameless());
		buf.writeBoolean(payload.unlink());
	}

	private static RainbowLedConfigurePayload read(FriendlyByteBuf buf) {
		return new RainbowLedConfigurePayload(
			buf.readBlockPos(),
			RainbowLedMode.values()[buf.readVarInt() % RainbowLedMode.values().length],
			buf.readFloat(),
			buf.readVarInt(),
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
