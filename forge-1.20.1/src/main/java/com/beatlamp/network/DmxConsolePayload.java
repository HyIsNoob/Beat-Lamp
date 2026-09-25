package com.beatlamp.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record DmxConsolePayload(
	BlockPos pos,
	boolean blackout,
	boolean strobeAll,
	float masterDimmer,
	float masterSpeed,
	java.util.List<BlockPos> mutedGroups
) {
	public DmxConsolePayload(BlockPos pos, boolean blackout, boolean strobeAll, float masterDimmer, float masterSpeed) {
		this(pos, blackout, strobeAll, masterDimmer, masterSpeed, java.util.List.of());
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.pos);
		buf.writeBoolean(this.blackout);
		buf.writeBoolean(this.strobeAll);
		buf.writeFloat(this.masterDimmer);
		buf.writeFloat(this.masterSpeed);
		buf.writeVarInt(this.mutedGroups.size());
		for (BlockPos p : this.mutedGroups) {
			buf.writeBlockPos(p);
		}
	}

	public static DmxConsolePayload read(FriendlyByteBuf buf) {
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
}
