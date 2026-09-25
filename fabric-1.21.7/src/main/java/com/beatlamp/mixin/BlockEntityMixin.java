package com.beatlamp.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
	@Inject(method = "getUpdatePacket", at = @At("HEAD"), cancellable = true)
	private void beatlamp$getJukeboxUpdatePacket(CallbackInfoReturnable<Packet<ClientGamePacketListener>> cir) {
		if ((Object) this instanceof JukeboxBlockEntity jukebox) {
			cir.setReturnValue(ClientboundBlockEntityDataPacket.create(jukebox));
		}
	}

	@Inject(method = "getUpdateTag", at = @At("HEAD"), cancellable = true)
	private void beatlamp$getJukeboxUpdateTag(HolderLookup.Provider provider, CallbackInfoReturnable<CompoundTag> cir) {
		if ((Object) this instanceof JukeboxBlockEntity jukebox) {
			cir.setReturnValue(jukebox.saveCustomOnly(provider));
		}
	}
}
