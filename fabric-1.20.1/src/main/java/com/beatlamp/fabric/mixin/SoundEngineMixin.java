package com.beatlamp.fabric.mixin;

import com.beatlamp.client.audio.JukeboxAudioTracker;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
	@Inject(method = "play", at = @At("TAIL"))
	private void beatlamp$onPlay(SoundInstance soundInstance, CallbackInfo ci) {
		JukeboxAudioTracker.onSoundPlayed(soundInstance);
	}

	@Inject(method = "stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"))
	private void beatlamp$onStop(SoundInstance soundInstance, CallbackInfo ci) {
		JukeboxAudioTracker.onSoundStopped(soundInstance);
	}
}
