package com.beatlamp.network;

import com.beatlamp.BeatLamp;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StageJukeboxConfigurePayload(
	BlockPos pos,
	int volume,
	int range,
	boolean loop,
	int action,
	int seekSeconds
) implements CustomPacketPayload {
	public static final int ACTION_UPDATE_SETTINGS = 0;
	public static final int ACTION_TOGGLE_PAUSE = 1;
	public static final int ACTION_EJECT = 2;
	public static final int ACTION_SEEK = 3;

	public static final CustomPacketPayload.Type<StageJukeboxConfigurePayload> ID = new CustomPacketPayload.Type<>(BeatLamp.id("configure_stage_jukebox"));

	public static final StreamCodec<FriendlyByteBuf, StageJukeboxConfigurePayload> CODEC = StreamCodec.of(
		StageJukeboxConfigurePayload::write, StageJukeboxConfigurePayload::read
	);

	private static void write(FriendlyByteBuf buf, StageJukeboxConfigurePayload payload) {
		buf.writeBlockPos(payload.pos());
		buf.writeVarInt(payload.volume());
		buf.writeVarInt(payload.range());
		buf.writeBoolean(payload.loop());
		buf.writeVarInt(payload.action());
		buf.writeVarInt(payload.seekSeconds());
	}

	private static StageJukeboxConfigurePayload read(FriendlyByteBuf buf) {
		return new StageJukeboxConfigurePayload(
			buf.readBlockPos(),
			buf.readVarInt(),
			buf.readVarInt(),
			buf.readBoolean(),
			buf.readVarInt(),
			buf.readVarInt()
		);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
