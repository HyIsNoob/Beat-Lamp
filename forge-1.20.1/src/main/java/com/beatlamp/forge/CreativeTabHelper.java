package com.beatlamp.forge;

import net.minecraft.world.item.CreativeModeTab;

public final class CreativeTabHelper {
	private CreativeTabHelper() {
	}

	public static CreativeModeTab.Builder builder() {
		return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0);
	}
}

