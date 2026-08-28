package com.beatlamp.network;

import com.beatlamp.BeatLamp;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DmxConsolePayload(
	BlockPos pos,
	boolean blackout,
	boolean strobeAll,
	float masterDimmer,
	float masterSpeed
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<DmxConsolePayload> ID = new CustomPacketPayload.Type<>(BeatLamp.id("configure_dmx_console"));

	public static final StreamCodec<FriendlyByteBuf, DmxConsolePayload> CODEC = StreamCodec.of(
		DmxConsolePayload::write, DmxConsolePayload::read
	);

	private static void write(FriendlyByteBuf buf, DmxConsolePayload payload) {
		buf.writeBlockPos(payload.pos());
		buf.writeBoolean(payload.blackout());
		buf.writeBoolean(payload.strobeAll());
		buf.writeFloat(payload.masterDimmer());
		buf.writeFloat(payload.masterSpeed());
	}

	private static DmxConsolePayload read(FriendlyByteBuf buf) {
		return new DmxConsolePayload(
			buf.readBlockPos(),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readFloat(),
			buf.readFloat()
		);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
