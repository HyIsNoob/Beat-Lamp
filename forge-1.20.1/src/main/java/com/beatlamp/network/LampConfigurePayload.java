package com.beatlamp.network;

import com.beatlamp.block.LampMode;
import com.beatlamp.block.LampOrientation;
import com.beatlamp.block.LampParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

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
	boolean tempoPulse,
	boolean dmxEnrolled,
	String customName
) {
	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.pos);
		buf.writeEnum(this.mode);
		buf.writeFloat(this.sensitivity);
		buf.writeFloat(this.speed);
		buf.writeInt(this.color);
		buf.writeBoolean(this.frameless);
		buf.writeBoolean(this.blackback);
		buf.writeBoolean(this.idleLight);
		buf.writeBoolean(this.reverse);
		buf.writeEnum(this.particles);
		buf.writeEnum(this.orientation);
		buf.writeBoolean(this.unlink);
		buf.writeBoolean(this.tempoPulse);
		buf.writeBoolean(this.dmxEnrolled);
		buf.writeUtf(this.customName != null ? this.customName : "", 64);
	}

	public static LampConfigurePayload read(FriendlyByteBuf buf) {
		return new LampConfigurePayload(
			buf.readBlockPos(),
			buf.readEnum(LampMode.class),
			buf.readFloat(),
			buf.readFloat(),
			buf.readInt(),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readEnum(LampParticles.class),
			buf.readEnum(LampOrientation.class),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readUtf(64)
		);
	}
}
