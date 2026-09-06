package com.beatlamp.client.mixin.dreamdisplays;

import com.beatlamp.client.audio.DreamDisplaysAudioBridge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.dreamdisplays.media.player.pipeline.AudioSink", remap = false)
public abstract class DreamDisplaysAudioSinkMixin {
	@Shadow(remap = false)
	private String debugLabel;

	@Inject(method = "ringPush", at = @At("HEAD"), remap = false)
	private void beatlamp$onRingPush(byte[] chunk, int len, CallbackInfo ci) {
		DreamDisplaysAudioBridge.onAudioChunk(this.debugLabel, chunk, len);
	}

	@Inject(method = "stop", at = @At("HEAD"), remap = false)
	private void beatlamp$onStop(CallbackInfo ci) {
		DreamDisplaysAudioBridge.onAudioStopped(this.debugLabel);
	}

	@Inject(method = "pauseForPark", at = @At("HEAD"), remap = false)
	private void beatlamp$onPauseForPark(CallbackInfo ci) {
		DreamDisplaysAudioBridge.onAudioPaused(this.debugLabel);
	}

	@Inject(method = "resumeFromPark", at = @At("HEAD"), remap = false)
	private void beatlamp$onResumeFromPark(CallbackInfo ci) {
		DreamDisplaysAudioBridge.onAudioResumed(this.debugLabel);
	}
}
