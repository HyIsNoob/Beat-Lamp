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

import org.joml.Matrix4f;

public class BeatLampRenderer implements BlockEntityRenderer<BeatLampBlockEntity> {
	private static final ResourceLocation CORE_TEXTURE = new ResourceLocation("beatlamp", "textures/block/beat_lamp_core.png");
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
		int packedOverlay
	) {
		LampMode mode = beatLamp.getMode();
		boolean blackback = beatLamp.isBlackback();

		if (!com.beatlamp.client.config.BeatLampClientConfig.enableStageEffects) {
			if (!blackback) {
				return;
			}
		}

		float pulse = Mth.clamp(beatLamp.pulse + beatLamp.beatPulse * 0.65F, 0.0F, 1.0F);
		float bar = Mth.clamp(beatLamp.barValue, 0.0F, 1.0F);
		float beat = Mth.clamp(beatLamp.beatPulse, 0.0F, 1.0F);
		float intensity = com.beatlamp.client.config.BeatLampClientConfig.enableStageEffects ? Math.max(Math.max(pulse, bar), beat) : 0.0F;

		if (intensity <= 0.02F || beatLamp.displayColor == 0) {
			if (!blackback) {
				return;
			}

			poseStack.pushPose();
			poseStack.translate(0.5F, 0.5F, 0.5F);
			PoseStack.Pose pose = poseStack.last();
			Matrix4f matrix = pose.pose();
			VertexConsumer black = multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(CORE_TEXTURE));
			drawCube(black, pose, matrix, FULL_HALF, FULL_HALF, FULL_HALF, 0.0F, 0.0F, 0.0F, 1.0F, 0xF000F0);
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
				half = blackback ? FULL_HALF : 0.3F + pulse * 0.16F;
				brightness = pulse * 1.5F;
			}
		}

		brightness = Math.min(brightness, 1.5F);
		float coreRed = Math.min(red * brightness, 1.0F);
		float coreGreen = Math.min(green * brightness, 1.0F);
		float coreBlue = Math.min(blue * brightness, 1.0F);

		RenderType coreType = blackback ? RenderType.entityCutoutNoCull(CORE_TEXTURE) : RenderType.entityTranslucentEmissive(CORE_TEXTURE);
		VertexConsumer core = multiBufferSource.getBuffer(coreType);
		drawCube(core, pose, matrix, half, half, half, coreRed, coreGreen, coreBlue, 1.0F, 0xF000F0);

		poseStack.popPose();
	}

	private static void drawCube(
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
		quad(consumer, pose, matrix, -hx, -hy, hz, hx, -hy, hz, hx, hy, hz, -hx, hy, hz, 0, 0, 1, r, g, b, a, light);
		quad(consumer, pose, matrix, hx, -hy, -hz, -hx, -hy, -hz, -hx, hy, -hz, hx, hy, -hz, 0, 0, -1, r, g, b, a, light);
		quad(consumer, pose, matrix, -hx, -hy, -hz, -hx, -hy, hz, -hx, hy, hz, -hx, hy, -hz, -1, 0, 0, r, g, b, a, light);
		quad(consumer, pose, matrix, hx, -hy, hz, hx, -hy, -hz, hx, hy, -hz, hx, hy, hz, 1, 0, 0, r, g, b, a, light);
		quad(consumer, pose, matrix, -hx, hy, hz, hx, hy, hz, hx, hy, -hz, -hx, hy, -hz, 0, 1, 0, r, g, b, a, light);
		quad(consumer, pose, matrix, -hx, -hy, -hz, hx, -hy, -hz, hx, -hy, hz, -hx, -hy, hz, 0, -1, 0, r, g, b, a, light);
	}

	private static void quad(
		VertexConsumer consumer,
		PoseStack.Pose pose,
		Matrix4f matrix,
		float x0, float y0, float z0,
		float x1, float y1, float z1,
		float x2, float y2, float z2,
		float x3, float y3, float z3,
		float nx, float ny, float nz,
		float r, float g, float b, float a,
		int light
	) {
		consumer.vertex(matrix, x0, y0, z0).color(r, g, b, a).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
		consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
		consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
		consumer.vertex(matrix, x3, y3, z3).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
	}
}
