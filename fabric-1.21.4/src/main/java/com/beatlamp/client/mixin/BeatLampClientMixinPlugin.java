package com.beatlamp.client.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class BeatLampClientMixinPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	private static boolean isDreamDisplaysPresent() {
		// 1. NeoForge LoadingModList check (fmlloader - always available at mixin time)
		try {
			Class<?> loadingModListClass = Class.forName("net.neoforged.fml.loading.LoadingModList");
			Object loadingModList = loadingModListClass.getMethod("get").invoke(null);
			if (loadingModList != null) {
				Object modFile = loadingModListClass.getMethod("getModFileById", String.class).invoke(loadingModList, "dreamdisplays");
				if (modFile != null) {
					return true;
				}
			}
		} catch (Throwable ignored) {
		}

		// 2. FabricLoader check
		try {
			Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
			Object fabricLoader = fabricLoaderClass.getMethod("getInstance").invoke(null);
			if (fabricLoader != null) {
				Boolean isLoaded = (Boolean) fabricLoaderClass.getMethod("isModLoaded", String.class).invoke(fabricLoader, "dreamdisplays");
				if (Boolean.TRUE.equals(isLoaded)) {
					return true;
				}
			}
		} catch (Throwable ignored) {
		}

		// 3. Fallback: ClassLoader check
		try {
			Class.forName("com.dreamdisplays.media.player.pipeline.AudioSink", false, Thread.currentThread().getContextClassLoader());
			return true;
		} catch (Throwable ignored) {
		}
		try {
			Class.forName("com.dreamdisplays.media.player.pipeline.AudioSink", false, BeatLampClientMixinPlugin.class.getClassLoader());
			return true;
		} catch (Throwable ignored) {
		}

		return false;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.contains("dreamdisplays")) {
			boolean present = isDreamDisplaysPresent();
			System.out.println("[BeatLamp] shouldApplyMixin: " + mixinClassName + " (target: " + targetClassName + ") -> " + present);
			return present;
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
