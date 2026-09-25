package com.beatlamp.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

public enum LampOrientation implements StringRepresentable {
	AUTO("auto"),
	EAST_WEST("east_west"),
	NORTH_SOUTH("north_south"),
	VERTICAL("vertical");

	private final String name;

	LampOrientation(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	public LampOrientation next() {
		return values()[(this.ordinal() + 1) % values().length];
	}

	public Direction.Axis getAxis() {
		return switch (this) {
			case EAST_WEST -> Direction.Axis.X;
			case VERTICAL -> Direction.Axis.Y;
			case NORTH_SOUTH -> Direction.Axis.Z;
			default -> null;
		};
	}

	public static LampOrientation byName(String name) {
		for (LampOrientation value : values()) {
			if (value.name.equals(name)) {
				return value;
			}
		}

		return AUTO;
	}
}
