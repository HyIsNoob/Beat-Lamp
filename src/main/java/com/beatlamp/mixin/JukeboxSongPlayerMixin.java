package com.beatlamp.mixin;

import com.beatlamp.JukeboxTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JukeboxSongPlayer.class)
public abstract class JukeboxSongPlayerMixin {
	@Shadow
	@Final
	private BlockPos blockPos;

	@Inject(method = "play", at = @At("TAIL"))
	private void beatlamp$onPlay(LevelAccessor levelAccessor, Holder<?> holder, CallbackInfo ci) {
		if (levelAccessor instanceof Level level) {
			JukeboxTracker.setPlaying(level, this.blockPos, true);
		}
	}

	@Inject(method = "stop", at = @At("TAIL"))
	private void beatlamp$onStop(LevelAccessor levelAccessor, BlockState blockState, CallbackInfo ci) {
		if (levelAccessor instanceof Level level) {
			JukeboxTracker.setPlaying(level, this.blockPos, false);
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void beatlamp$onTick(LevelAccessor levelAccessor, BlockState blockState, CallbackInfo ci) {
		if (levelAccessor instanceof Level level) {
			JukeboxTracker.setPlaying(level, this.blockPos, ((JukeboxSongPlayer) (Object) this).isPlaying());
		}
	}
}
