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

	private static boolean isModOrClassPresent(String modId, String... classNames) {
		// 1. NeoForge LoadingModList check (fmlloader - always available at mixin time)
		try {
			Class<?> loadingModListClass = Class.forName("net.neoforged.fml.loading.LoadingModList");
			Object loadingModList = loadingModListClass.getMethod("get").invoke(null);
			if (loadingModList != null) {
				Object modFile = loadingModListClass.getMethod("getModFileById", String.class).invoke(loadingModList, modId);
				if (modFile != null) {
					return true;
				}
			}
		} catch (Throwable ignored) {
		}

		// 2. Forge FMLLoader / LoadingModList check (always available at mixin time)
		try {
			Class<?> fmlLoaderClass = Class.forName("net.minecraftforge.fml.loading.FMLLoader");
			Object loadingModList = fmlLoaderClass.getMethod("getLoadingModList").invoke(null);
			if (loadingModList != null) {
				Object modFile = loadingModList.getClass().getMethod("getModFileById", String.class).invoke(loadingModList, modId);
				if (modFile != null) {
					return true;
				}
			}
		} catch (Throwable ignored) {
		}
		try {
			Class<?> forgeLoadingModListClass = Class.forName("net.minecraftforge.fml.loading.LoadingModList");
			Object loadingModList = forgeLoadingModListClass.getMethod("get").invoke(null);
			if (loadingModList != null) {
				Object modFile = forgeLoadingModListClass.getMethod("getModFileById", String.class).invoke(loadingModList, modId);
				if (modFile != null) {
					return true;
				}
			}
		} catch (Throwable ignored) {
		}

		// 3. Forge ModList check (runtime fallback)
		try {
			Class<?> modListClass = Class.forName("net.minecraftforge.fml.ModList");
			Object modList = modListClass.getMethod("get").invoke(null);
			if (modList != null) {
				Boolean isLoaded = (Boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
				if (Boolean.TRUE.equals(isLoaded)) {
					return true;
				}
			}
		} catch (Throwable ignored) {
		}

		// 4. FabricLoader check
		try {
			Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
			Object fabricLoader = fabricLoaderClass.getMethod("getInstance").invoke(null);
			if (fabricLoader != null) {
				Boolean isLoaded = (Boolean) fabricLoaderClass.getMethod("isModLoaded", String.class).invoke(fabricLoader, modId);
				if (Boolean.TRUE.equals(isLoaded)) {
					return true;
				}
			}
		} catch (Throwable ignored) {
		}

		// 4. Fallback: ClassLoader check
		if (classNames != null) {
			for (String className : classNames) {
				try {
					Class.forName(className, false, Thread.currentThread().getContextClassLoader());
					return true;
				} catch (Throwable ignored) {
				}
				try {
					Class.forName(className, false, BeatLampClientMixinPlugin.class.getClassLoader());
					return true;
				} catch (Throwable ignored) {
				}
			}
		}

		return false;
	}

	private boolean isClassPresent(String className) {
		try {
			Class.forName(className, false, Thread.currentThread().getContextClassLoader());
			return true;
		} catch (Throwable ignored) {
		}
		try {
			Class.forName(className, false, BeatLampClientMixinPlugin.class.getClassLoader());
			return true;
		} catch (Throwable ignored) {
		}
		return false;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.contains("dreamdisplays")) {
			boolean present = isModOrClassPresent("dreamdisplays", "com.dreamdisplays.media.player.pipeline.AudioSink");
			System.out.println("[BeatLamp] shouldApplyMixin: " + mixinClassName + " -> " + present);
			return present;
		}
		if (mixinClassName.contains("musicdiscmaker")) {
			boolean present = isModOrClassPresent("music_disc_maker", "com.kuronami.musicdiscmaker.client.audio.LavaPlayerAudioStream");
			System.out.println("[BeatLamp] shouldApplyMixin: " + mixinClassName + " -> " + present);
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
