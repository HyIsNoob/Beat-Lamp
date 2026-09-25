package com.beatlamp.forge.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class BeatLampForgeMixinPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.contains("musicdiscmaker")) {
			try {
				Class<?> fmlLoaderClass = Class.forName("net.minecraftforge.fml.loading.FMLLoader");
				Object loadingModList = fmlLoaderClass.getMethod("getLoadingModList").invoke(null);
				if (loadingModList != null) {
					Object modFile = loadingModList.getClass().getMethod("getModFileById", String.class).invoke(loadingModList, "music_disc_maker");
					if (modFile != null) return true;
					Object modFile2 = loadingModList.getClass().getMethod("getModFileById", String.class).invoke(loadingModList, "musicdiscmaker");
					if (modFile2 != null) return true;
				}
			} catch (Throwable ignored) {
			}
			try {
				Class<?> loadingModListClass = Class.forName("net.minecraftforge.fml.loading.LoadingModList");
				Object loadingModList = loadingModListClass.getMethod("get").invoke(null);
				if (loadingModList != null) {
					Object modFile = loadingModListClass.getMethod("getModFileById", String.class).invoke(loadingModList, "music_disc_maker");
					if (modFile != null) return true;
					Object modFile2 = loadingModListClass.getMethod("getModFileById", String.class).invoke(loadingModList, "musicdiscmaker");
					if (modFile2 != null) return true;
				}
			} catch (Throwable ignored) {
			}
			try {
				Class.forName("com.kuronami.musicdiscmaker.client.audio.LavaPlayerAudioStream", false, Thread.currentThread().getContextClassLoader());
				return true;
			} catch (Throwable ignored) {
				try {
					Class.forName("com.kuronami.musicdiscmaker.client.audio.LavaPlayerAudioStream", false, this.getClass().getClassLoader());
					return true;
				} catch (Throwable ignored2) {
					return false;
				}
			}
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
