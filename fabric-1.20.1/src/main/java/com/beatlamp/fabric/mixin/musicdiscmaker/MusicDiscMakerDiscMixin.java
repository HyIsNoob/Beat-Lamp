package com.beatlamp.fabric.mixin.musicdiscmaker;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.beatlamp.client.audio.MusicDiscMakerAudioBridge;
import net.minecraft.client.resources.sounds.SoundInstance;

@Mixin(targets = "com.kuronami.musicdiscmaker.client.audio.DiscSoundInstance", remap = false)
public abstract class MusicDiscMakerDiscMixin {

	@Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 0)
	private void beatlamp$onInit(CallbackInfo ci) {
		if ((Object) this instanceof SoundInstance si) {
			MusicDiscMakerAudioBridge.registerSoundInstance(si);
		}
	}

	@Inject(method = "getCustomStream", at = @At("RETURN"), remap = false)
	private void beatlamp$onGetCustomStream(CallbackInfoReturnable<CompletableFuture<?>> cir) {
		CompletableFuture<?> future = cir.getReturnValue();
		if (future != null) {
			future.thenAccept(stream -> {
				if (stream != null) {
					MusicDiscMakerAudioBridge.registerStream(stream, this);
				}
			});
		}
	}

	@Inject(method = {"tick", "m_7788_"}, at = @At("HEAD"), remap = false, require = 0)
	private void beatlamp$onTick(CallbackInfo ci) {
		try {
			java.lang.reflect.Field f = this.getClass().getDeclaredField("stream");
			f.setAccessible(true);
			Object stream = f.get(this);
			if (stream != null) {
				MusicDiscMakerAudioBridge.registerStream(stream, this);
			}
		} catch (Throwable ignored) {
		}
	}

	@Inject(method = "requestStop", at = @At("HEAD"), remap = false)
	private void beatlamp$onRequestStop(CallbackInfo ci) {
		MusicDiscMakerAudioBridge.onDiscStopped(this);
	}

	@Inject(method = "stopAndRelease", at = @At("HEAD"), remap = false)
	private void beatlamp$onStopAndRelease(CallbackInfo ci) {
		MusicDiscMakerAudioBridge.onDiscStopped(this);
	}
}
