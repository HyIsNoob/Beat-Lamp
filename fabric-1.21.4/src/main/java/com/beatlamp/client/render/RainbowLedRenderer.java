package com.beatlamp.client.render;

import com.beatlamp.block.RainbowLedBlockEntity;
import com.beatlamp.block.RainbowLedMode;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import org.joml.Matrix4f;

public class RainbowLedRenderer implements BlockEntityRenderer<RainbowLedBlockEntity> {
	private static final ResourceLocation CORE_TEXTURE = ResourceLocation.fromNamespaceAndPath("beatlamp", "textures/block/beat_lamp_core.png");
	private static final float HALF = 0.501F;

	public RainbowLedRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public boolean shouldRenderOffScreen(RainbowLedBlockEntity blockEntity) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

	@Override
	public void render(
		RainbowLedBlockEntity led,
		float partialTick,
		PoseStack poseStack,
		MultiBufferSource multiBufferSource,
		int packedLight,
		int packedOverlay
	) {
		int color = led.getCalculatedColor(partialTick);
		float red = ((color >> 16) & 0xFF) / 255.0F;
		float green = ((color >> 8) & 0xFF) / 255.0F;
		float blue = (color & 0xFF) / 255.0F;

		float masterDimmer = com.beatlamp.client.DmxMasterTracker.getMasterDimmerNear(led.getBlockPos());
		float brightNorm = Math.max(0.1F, (float) led.getBrightness() / 15.0F) * masterDimmer;
		float r = red * brightNorm;
		float g = green * brightNorm;
		float b = blue * brightNorm;

		poseStack.pushPose();
		poseStack.translate(0.5F, 0.5F, 0.5F);
		PoseStack.Pose pose = poseStack.last();
		Matrix4f matrix = pose.pose();

		float inset = led.isFrameless() ? 0.501F : 0.375F;
		float h = 0.501F;

		VertexConsumer consumer = multiBufferSource.getBuffer(RenderType.entityCutout(CORE_TEXTURE));
		renderFaces(led, consumer, pose, matrix, inset, h, r, g, b, 1.0F, 0xF000F0);

		poseStack.popPose();
	}

	private static void renderFaces(
		RainbowLedBlockEntity led,
		VertexConsumer consumer,
		PoseStack.Pose pose,
		Matrix4f matrix,
		float inset,
		float h,
		float red,
		float green,
		float blue,
		float alpha,
		int light
	) {
		net.minecraft.world.level.Level level = led.getLevel();
		net.minecraft.core.BlockPos pos = led.getBlockPos();
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
			vertex(consumer, matrix, -inset, -inset, h, red, green, blue, alpha, 0.0F, 0.0F, light, pose);
			vertex(consumer, matrix, inset, -inset, h, red, green, blue, alpha, 1.0F, 0.0F, light, pose);
			vertex(consumer, matrix, inset, inset, h, red, green, blue, alpha, 1.0F, 1.0F, light, pose);
			vertex(consumer, matrix, -inset, inset, h, red, green, blue, alpha, 0.0F, 1.0F, light, pose);
		}

		// North (-Z)
		if (drawNorth) {
			vertex(consumer, matrix, inset, -inset, -h, red, green, blue, alpha, 0.0F, 0.0F, light, pose);
			vertex(consumer, matrix, -inset, -inset, -h, red, green, blue, alpha, 1.0F, 0.0F, light, pose);
			vertex(consumer, matrix, -inset, inset, -h, red, green, blue, alpha, 1.0F, 1.0F, light, pose);
			vertex(consumer, matrix, inset, inset, -h, red, green, blue, alpha, 0.0F, 1.0F, light, pose);
		}

		// East (+X)
		if (drawEast) {
			vertex(consumer, matrix, h, -inset, inset, red, green, blue, alpha, 0.0F, 0.0F, light, pose);
			vertex(consumer, matrix, h, -inset, -inset, red, green, blue, alpha, 1.0F, 0.0F, light, pose);
			vertex(consumer, matrix, h, inset, -inset, red, green, blue, alpha, 1.0F, 1.0F, light, pose);
			vertex(consumer, matrix, h, inset, inset, red, green, blue, alpha, 0.0F, 1.0F, light, pose);
		}

		// West (-X)
		if (drawWest) {
			vertex(consumer, matrix, -h, -inset, -inset, red, green, blue, alpha, 0.0F, 0.0F, light, pose);
			vertex(consumer, matrix, -h, -inset, inset, red, green, blue, alpha, 1.0F, 0.0F, light, pose);
			vertex(consumer, matrix, -h, inset, inset, red, green, blue, alpha, 1.0F, 1.0F, light, pose);
			vertex(consumer, matrix, -h, inset, -inset, red, green, blue, alpha, 0.0F, 1.0F, light, pose);
		}

		// Up (+Y)
		if (drawUp) {
			vertex(consumer, matrix, -inset, h, inset, red, green, blue, alpha, 0.0F, 0.0F, light, pose);
			vertex(consumer, matrix, inset, h, inset, red, green, blue, alpha, 1.0F, 0.0F, light, pose);
			vertex(consumer, matrix, inset, h, -inset, red, green, blue, alpha, 1.0F, 1.0F, light, pose);
			vertex(consumer, matrix, -inset, h, -inset, red, green, blue, alpha, 0.0F, 1.0F, light, pose);
		}

		// Down (-Y)
		if (drawDown) {
			vertex(consumer, matrix, -inset, -h, -inset, red, green, blue, alpha, 0.0F, 0.0F, light, pose);
			vertex(consumer, matrix, inset, -h, -inset, red, green, blue, alpha, 1.0F, 0.0F, light, pose);
			vertex(consumer, matrix, inset, -h, inset, red, green, blue, alpha, 1.0F, 1.0F, light, pose);
			vertex(consumer, matrix, -inset, -h, inset, red, green, blue, alpha, 0.0F, 1.0F, light, pose);
		}
	}

	private static boolean shouldRenderFace(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos neighborPos) {
		net.minecraft.world.level.block.state.BlockState state = level.getBlockState(neighborPos);
		if (state.getBlock() instanceof com.beatlamp.block.RainbowLedBlock || (com.beatlamp.BeatLampBlocks.RAINBOW_LED_BLOCK != null && state.is(com.beatlamp.BeatLampBlocks.RAINBOW_LED_BLOCK))) {
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
