package com.beatlamp.client.render;

import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.LampMode;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

public class BeatLampRenderer implements BlockEntityRenderer<BeatLampBlockEntity> {
	private static final ResourceLocation CORE_TEXTURE = ResourceLocation.fromNamespaceAndPath("beatlamp", "textures/block/beat_lamp_core.png");
	private static final float FULL_HALF = 0.503F;

	public BeatLampRenderer(BlockEntityRendererProvider.Context context) {
	}


	@Override
	public int getViewDistance() {
		return 192;
	}

	@Override
	public void render(
		BeatLampBlockEntity beatLamp,
		float partialTick,
		PoseStack poseStack,
		MultiBufferSource multiBufferSource,
		int packedLight,
		int packedOverlay,
		Vec3 cameraPos
	) {
		LampMode mode = beatLamp.getMode();
		boolean blackback = beatLamp.isBlackback();

		if (!com.beatlamp.client.config.BeatLampClientConfig.enableStageEffects) {
			if (beatLamp.isFrameless()) {
				poseStack.pushPose();
				poseStack.translate(0.5F, 0.5F, 0.5F);
				PoseStack.Pose pose = poseStack.last();
				Matrix4f matrix = pose.pose();
				VertexConsumer buffer = multiBufferSource.getBuffer(RenderType.entityTranslucent(CORE_TEXTURE));
				float standbyAlpha = blackback ? 1.0F : 0.45F;
				renderFaces(beatLamp, buffer, pose, matrix, FULL_HALF, FULL_HALF, FULL_HALF, 0.04F, 0.04F, 0.06F, standbyAlpha, 0xF000F0);
				poseStack.popPose();
				return;
			}
			if (!blackback) {
				return;
			}
		}

		float pulse = Mth.clamp(beatLamp.pulse + beatLamp.beatPulse * 0.65F, 0.0F, 1.0F);
		float bar = Mth.clamp(beatLamp.barValue, 0.0F, 1.0F);
		float beat = Mth.clamp(beatLamp.beatPulse, 0.0F, 1.0F);
		float masterDimmer = com.beatlamp.client.DmxMasterTracker.getMasterDimmerNear(beatLamp.getBlockPos());
		float intensity = com.beatlamp.client.config.BeatLampClientConfig.enableStageEffects ? Math.max(Math.max(pulse, bar), beat) * masterDimmer : 0.0F;

		if (intensity <= 0.02F || beatLamp.displayColor == 0) {
			if (beatLamp.isFrameless()) {
				poseStack.pushPose();
				poseStack.translate(0.5F, 0.5F, 0.5F);
				PoseStack.Pose pose = poseStack.last();
				Matrix4f matrix = pose.pose();
				VertexConsumer buffer = multiBufferSource.getBuffer(RenderType.entityTranslucent(CORE_TEXTURE));
				float standbyAlpha = blackback ? 1.0F : 0.45F;
				renderFaces(beatLamp, buffer, pose, matrix, FULL_HALF, FULL_HALF, FULL_HALF, 0.04F, 0.04F, 0.06F, standbyAlpha, 0xF000F0);
				poseStack.popPose();
				return;
			}

			if (!blackback) {
				return;
			}

			poseStack.pushPose();
			poseStack.translate(0.5F, 0.5F, 0.5F);
			PoseStack.Pose pose = poseStack.last();
			Matrix4f matrix = pose.pose();
			VertexConsumer black = multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(CORE_TEXTURE));
			renderFaces(beatLamp, black, pose, matrix, FULL_HALF, FULL_HALF, FULL_HALF, 0.0F, 0.0F, 0.0F, 1.0F, 0xF000F0);
			poseStack.popPose();
			return;
		}

		poseStack.pushPose();
		poseStack.translate(0.5F, 0.5F, 0.5F);
		PoseStack.Pose pose = poseStack.last();
		Matrix4f matrix = pose.pose();

		int color = beatLamp.displayColor;
		float red = ((color >> 16) & 0xFF) / 255.0F;
		float green = ((color >> 8) & 0xFF) / 255.0F;
		float blue = (color & 0xFF) / 255.0F;

		float half;
		float brightness;

		switch (mode) {
			case SPECTRUM, VU_METER, OSCILLOSCOPE, MATRIX_RAIN, RIPPLE, WAVE, SCAN -> {
				half = FULL_HALF;
				brightness = bar * 1.5F;
			}
			case RGB -> {
				half = FULL_HALF;
				brightness = pulse * 1.5F;
			}
			default -> {
				half = FULL_HALF;
				brightness = pulse * 1.5F;
			}
		}

		brightness = Math.min(brightness * masterDimmer, 1.5F);
		float coreRed = Math.min(red * brightness, 1.0F);
		float coreGreen = Math.min(green * brightness, 1.0F);
		float coreBlue = Math.min(blue * brightness, 1.0F);

		RenderType coreType = blackback ? RenderType.entityCutoutNoCull(CORE_TEXTURE) : RenderType.entityTranslucentEmissive(CORE_TEXTURE);
		VertexConsumer core = multiBufferSource.getBuffer(coreType);
		renderFaces(beatLamp, core, pose, matrix, half, half, half, coreRed, coreGreen, coreBlue, 1.0F, 0xF000F0);

		poseStack.popPose();
	}

	private static void renderFaces(
		BeatLampBlockEntity beatLamp,
		VertexConsumer consumer,
		PoseStack.Pose pose,
		Matrix4f matrix,
		float hx,
		float hy,
		float hz,
		float r,
		float g,
		float b,
		float a,
		int light
	) {
		net.minecraft.world.level.Level level = beatLamp.getLevel();
		net.minecraft.core.BlockPos pos = beatLamp.getBlockPos();
		boolean drawSouth = true, drawNorth = true, drawWest = true, drawEast = true, drawUp = true, drawDown = true;
		if (level != null && pos != null) {
			drawSouth = shouldRenderFace(level, pos.south());
			drawNorth = shouldRenderFace(level, pos.north());
			drawWest = shouldRenderFace(level, pos.west());
			drawEast = shouldRenderFace(level, pos.east());
			drawUp = shouldRenderFace(level, pos.above());
			drawDown = shouldRenderFace(level, pos.below());
		}

		// South (+Z)
		if (drawSouth) {
			vertex(consumer, matrix, -hx, -hy, hz, r, g, b, a, 0.0F, 0.0F, light, pose);
			vertex(consumer, matrix, hx, -hy, hz, r, g, b, a, 1.0F, 0.0F, light, pose);
			vertex(consumer, matrix, hx, hy, hz, r, g, b, a, 1.0F, 1.0F, light, pose);
			vertex(consumer, matrix, -hx, hy, hz, r, g, b, a, 0.0F, 1.0F, light, pose);
		}

		// North (-Z)
		if (drawNorth) {
			vertex(consumer, matrix, hx, -hy, -hz, r, g, b, a, 0.0F, 0.0F, light, pose);
			vertex(consumer, matrix, -hx, -hy, -hz, r, g, b, a, 1.0F, 0.0F, light, pose);
			vertex(consumer, matrix, -hx, hy, -hz, r, g, b, a, 1.0F, 1.0F, light, pose);
			vertex(consumer, matrix, hx, hy, -hz, r, g, b, a, 0.0F, 1.0F, light, pose);
		}

		// East (+X)
		if (drawEast) {
			vertex(consumer, matrix, hx, -hy, hz, r, g, b, a, 0.0F, 0.0F, light, pose);
			vertex(consumer, matrix, hx, -hy, -hz, r, g, b, a, 1.0F, 0.0F, light, pose);
			vertex(consumer, matrix, hx, hy, -hz, r, g, b, a, 1.0F, 1.0F, light, pose);
			vertex(consumer, matrix, hx, hy, hz, r, g, b, a, 0.0F, 1.0F, light, pose);
		}

		// West (-X)
		if (drawWest) {
			vertex(consumer, matrix, -hx, -hy, -hz, r, g, b, a, 0.0F, 0.0F, light, pose);
			vertex(consumer, matrix, -hx, -hy, hz, r, g, b, a, 1.0F, 0.0F, light, pose);
			vertex(consumer, matrix, -hx, hy, hz, r, g, b, a, 1.0F, 1.0F, light, pose);
			vertex(consumer, matrix, -hx, hy, -hz, r, g, b, a, 0.0F, 1.0F, light, pose);
		}

		// Up (+Y)
		if (drawUp) {
			vertex(consumer, matrix, -hx, hy, hz, r, g, b, a, 0.0F, 0.0F, light, pose);
			vertex(consumer, matrix, hx, hy, hz, r, g, b, a, 1.0F, 0.0F, light, pose);
			vertex(consumer, matrix, hx, hy, -hz, r, g, b, a, 1.0F, 1.0F, light, pose);
			vertex(consumer, matrix, -hx, hy, -hz, r, g, b, a, 0.0F, 1.0F, light, pose);
		}

		// Down (-Y)
		if (drawDown) {
			vertex(consumer, matrix, -hx, -hy, -hz, r, g, b, a, 0.0F, 0.0F, light, pose);
			vertex(consumer, matrix, hx, -hy, -hz, r, g, b, a, 1.0F, 0.0F, light, pose);
			vertex(consumer, matrix, hx, -hy, hz, r, g, b, a, 1.0F, 1.0F, light, pose);
			vertex(consumer, matrix, -hx, -hy, hz, r, g, b, a, 0.0F, 1.0F, light, pose);
		}
	}

	private static boolean shouldRenderFace(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos neighborPos) {
		net.minecraft.world.level.block.state.BlockState state = level.getBlockState(neighborPos);
		if (state.getBlock() instanceof com.beatlamp.block.BeatLampBlock) {
			return false;
		}
		return !state.isSolidRender();
	}

	private static void vertex(
		VertexConsumer consumer,
		Matrix4f matrix,
		float x,
		float y,
		float z,
		float red,
		float green,
		float blue,
		float alpha,
		float u,
		float v,
		int light,
		PoseStack.Pose pose
	) {
		consumer.addVertex(matrix, x, y, z)
			.setColor(red, green, blue, alpha)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}
}
