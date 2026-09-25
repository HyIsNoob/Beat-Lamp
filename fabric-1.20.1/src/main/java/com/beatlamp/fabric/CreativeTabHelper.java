package com.beatlamp.fabric;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.world.item.CreativeModeTab;

public final class CreativeTabHelper {
	private CreativeTabHelper() {
	}

	public static CreativeModeTab.Builder builder() {
		return FabricItemGroup.builder();
	}
}

