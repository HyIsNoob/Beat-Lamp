package com.beatlamp.client.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class BeatLampClientMixinPlugin implements IMixinConfigPlugin {
	private static final boolean DREAM_DISPLAYS_PRESENT;

	static {
		boolean present = false;
		try {
			Class.forName("com.dreamdisplays.media.player.pipeline.AudioSink", false, Thread.currentThread().getContextClassLoader());
			present = true;
		} catch (Throwable t1) {
			try {
				Class.forName("com.dreamdisplays.media.player.pipeline.AudioSink", false, BeatLampClientMixinPlugin.class.getClassLoader());
				present = true;
			} catch (Throwable t2) {
				present = false;
			}
		}
		DREAM_DISPLAYS_PRESENT = present;
	}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.contains("dreamdisplays")) {
			return DREAM_DISPLAYS_PRESENT;
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
