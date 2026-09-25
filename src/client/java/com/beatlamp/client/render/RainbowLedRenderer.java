package com.beatlamp.client.render;

import com.beatlamp.block.RainbowLedBlockEntity;
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
		float r,
		float g,
		float b,
		float a,
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

		if (drawSouth) quad(consumer, pose, matrix, -inset, -inset, h, inset, -inset, h, inset, inset, h, -inset, inset, h, 0, 0, 1, r, g, b, a, light);
		if (drawNorth) quad(consumer, pose, matrix, inset, -inset, -h, -inset, -inset, -h, -inset, inset, -h, inset, inset, -h, 0, 0, -1, r, g, b, a, light);
		if (drawWest) quad(consumer, pose, matrix, -h, -inset, -inset, -h, -inset, inset, -h, inset, inset, -h, inset, -inset, -1, 0, 0, r, g, b, a, light);
		if (drawEast) quad(consumer, pose, matrix, h, -inset, inset, h, -inset, -inset, h, inset, -inset, h, inset, inset, 1, 0, 0, r, g, b, a, light);
		if (drawUp) quad(consumer, pose, matrix, -inset, h, inset, inset, h, inset, inset, h, -inset, -inset, h, -inset, 0, 1, 0, r, g, b, a, light);
		if (drawDown) quad(consumer, pose, matrix, -inset, -h, -inset, inset, -h, -inset, inset, -h, inset, -inset, -h, inset, 0, -1, 0, r, g, b, a, light);
	}

	private static boolean shouldRenderFace(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos neighborPos) {
		net.minecraft.world.level.block.state.BlockState state = level.getBlockState(neighborPos);
		if (state.getBlock() instanceof com.beatlamp.block.RainbowLedBlock || (com.beatlamp.BeatLampBlocks.RAINBOW_LED_BLOCK != null && state.is(com.beatlamp.BeatLampBlocks.RAINBOW_LED_BLOCK))) {
			return false;
		}
		return !state.isSolidRender(level, neighborPos);
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
		consumer.addVertex(matrix, x0, y0, z0).setColor(r, g, b, a).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
		consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
		consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
		consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
	}
}
