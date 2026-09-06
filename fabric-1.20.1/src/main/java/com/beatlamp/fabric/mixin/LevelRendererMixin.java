package com.beatlamp.fabric.mixin;

import com.beatlamp.client.render.LampOutlineRenderer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@Shadow
	@Final
	private RenderBuffers renderBuffers;

	@Inject(method = "renderLevel", at = @At("TAIL"))
	private void beatlamp$renderOutlines(
		PoseStack poseStack,
		float partialTick,
		long finishNanoTime,
		boolean renderBlockOutline,
		Camera camera,
		GameRenderer gameRenderer,
		LightTexture lightTexture,
		Matrix4f projectionMatrix,
		CallbackInfo ci
	) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
			Vec3 camPos = camera.getPosition();
			LampOutlineRenderer.renderOutlines(mc.level, poseStack, bufferSource, camPos);
			bufferSource.endBatch();
		}
	}
}
