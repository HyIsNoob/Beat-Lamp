package com.beatlamp.fabric.mixin;

import com.beatlamp.client.audio.JukeboxAudioTracker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "tick", at = @At("END"))
	private void beatlamp$onTick(CallbackInfo ci) {
		JukeboxAudioTracker.clientTick();
		if (com.beatlamp.fabric.BeatLampFabricClient.OPEN_SETTINGS_KEY != null) {
			while (com.beatlamp.fabric.BeatLampFabricClient.OPEN_SETTINGS_KEY.consumeClick()) {
				Minecraft mc = (Minecraft)(Object)this;
				if (mc.screen == null) {
					mc.setScreen(new com.beatlamp.client.gui.BeatLampClientSettingsScreen());
				}
			}
		}
	}

	@Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
	private void beatlamp$onDisconnect(Screen screen, CallbackInfo ci) {
		JukeboxAudioTracker.clear();
	}
}
