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
	float masterSpeed,
	java.util.List<BlockPos> mutedGroups
) implements CustomPacketPayload {
	public DmxConsolePayload(BlockPos pos, boolean blackout, boolean strobeAll, float masterDimmer, float masterSpeed) {
		this(pos, blackout, strobeAll, masterDimmer, masterSpeed, java.util.List.of());
	}

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
		buf.writeVarInt(payload.mutedGroups().size());
		for (BlockPos p : payload.mutedGroups()) {
			buf.writeBlockPos(p);
		}
	}

	private static DmxConsolePayload read(FriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		boolean blackout = buf.readBoolean();
		boolean strobeAll = buf.readBoolean();
		float masterDimmer = buf.readFloat();
		float masterSpeed = buf.readFloat();
		int count = buf.readVarInt();
		java.util.List<BlockPos> muted = new java.util.ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			muted.add(buf.readBlockPos());
		}
		return new DmxConsolePayload(pos, blackout, strobeAll, masterDimmer, masterSpeed, muted);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
