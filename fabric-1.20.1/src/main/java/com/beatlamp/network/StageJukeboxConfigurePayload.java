package com.beatlamp.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record StageJukeboxConfigurePayload(
	BlockPos pos,
	int volume,
	int range,
	boolean loop,
	int action,
	int seekSeconds
) {
	public static final int ACTION_UPDATE_SETTINGS = 0;
	public static final int ACTION_TOGGLE_PAUSE = 1;
	public static final int ACTION_EJECT = 2;
	public static final int ACTION_SEEK = 3;

	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.pos);
		buf.writeVarInt(this.volume);
		buf.writeVarInt(this.range);
		buf.writeBoolean(this.loop);
		buf.writeVarInt(this.action);
		buf.writeVarInt(this.seekSeconds);
	}

	public static StageJukeboxConfigurePayload read(FriendlyByteBuf buf) {
		return new StageJukeboxConfigurePayload(
			buf.readBlockPos(),
			buf.readVarInt(),
			buf.readVarInt(),
			buf.readBoolean(),
			buf.readVarInt(),
			buf.readVarInt()
		);
	}
}
