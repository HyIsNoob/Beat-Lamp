package com.beatlamp.fabric.mixin.musicdiscmaker;

import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.beatlamp.client.audio.MusicDiscMakerAudioBridge;

@Mixin(targets = "com.kuronami.musicdiscmaker.client.audio.LavaPlayerAudioStream", remap = false)
public abstract class MusicDiscMakerStreamMixin {
	@Shadow(remap = false)
	private byte[] scratch;

	@Shadow(remap = false)
	private AudioFormat format;

	@Inject(method = "emit", at = @At("HEAD"), remap = false)
	private void beatlamp$onEmit(ByteBuffer buffer, int len, CallbackInfo ci) {
		MusicDiscMakerAudioBridge.onAudioChunk(this, this.scratch, len, this.format);
	}

	@Inject(method = "endOfStream", at = @At("HEAD"), remap = false)
	private void beatlamp$onEndOfStream(CallbackInfo ci) {
		MusicDiscMakerAudioBridge.onStreamEnded(this);
	}
}
