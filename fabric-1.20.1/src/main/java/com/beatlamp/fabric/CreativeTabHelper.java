package com.beatlamp.fabric;

import java.lang.reflect.Method;

import net.minecraft.world.item.CreativeModeTab;

public final class CreativeTabHelper {
	private CreativeTabHelper() {
	}

	public static CreativeModeTab.Builder builder() {
		try {
			Method m = CreativeModeTab.class.getMethod("builder");
			return (CreativeModeTab.Builder) m.invoke(null);
		} catch (Throwable t1) {
			try {
				Method m = CreativeModeTab.class.getMethod("builder", CreativeModeTab.Row.class, int.class);
				return (CreativeModeTab.Builder) m.invoke(null, CreativeModeTab.Row.TOP, 0);
			} catch (Throwable t2) {
				throw new RuntimeException("Failed to create CreativeModeTab.Builder", t2);
			}
		}
	}
}
