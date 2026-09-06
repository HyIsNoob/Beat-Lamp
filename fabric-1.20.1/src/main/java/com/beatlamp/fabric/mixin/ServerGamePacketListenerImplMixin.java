package com.beatlamp.fabric.mixin;

import com.beatlamp.fabric.network.FabricNetwork;

import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;

	@Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
	private void beatlamp$handleCustomPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
		if (FabricNetwork.handleServerPayload(this.player, packet.getIdentifier(), packet.getData())) {
			ci.cancel();
		}
	}
}
