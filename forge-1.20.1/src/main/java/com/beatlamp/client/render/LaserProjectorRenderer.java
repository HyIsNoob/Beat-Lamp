package com.beatlamp.client.render;

import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.LaserMode;
import com.beatlamp.block.LaserProjectorBlock;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.client.audio.JukeboxAudioTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class LaserProjectorRenderer implements BlockEntityRenderer<LaserProjectorBlockEntity> {
	private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation("minecraft", "textures/entity/beacon_beam.png");

	public LaserProjectorRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public int getViewDistance() {
		return com.beatlamp.client.config.BeatLampClientConfig.beamRenderDistance;
	}

	@Override
	public void render(
		LaserProjectorBlockEntity laser,
		float partialTick,
		PoseStack poseStack,
		MultiBufferSource multiBufferSource,
		int packedLight,
		int packedOverlay
	) {
		if (!com.beatlamp.client.config.BeatLampClientConfig.enableStageEffects || !com.beatlamp.client.config.BeatLampClientConfig.enableLaserBeams) {
			return;
		}

		float intensity = laser.beamIntensity;
		if (intensity <= 0.02F) {
			return;
		}

		int color = resolveColor(laser);
		float red = ((color >> 16) & 0xFF) / 255.0F;
		float green = ((color >> 8) & 0xFF) / 255.0F;
		float blue = (color & 0xFF) / 255.0F;

		poseStack.pushPose();
		poseStack.translate(0.5F, 0.5F, 0.5F);

		poseStack.mulPose(getFacingRotation(laser));

		drawLaserDiode(poseStack, multiBufferSource, red, green, blue, 0.16F);

		int count = laser.getBeamCount();
		float spread = laser.getSpread();
		float speed = laser.getSpeed();
		float time = JukeboxAudioTracker.getEffectTime() * speed;
		LaserMode mode = laser.getMode();

		float length = 28.0F + intensity * 6.0F;

		for (int i = 0; i < count; i++) {
			poseStack.pushPose();

			switch (mode) {
				case FAN_SWEEP -> {
					float baseAngle = count > 1 ? (-spread * 0.5F + (spread / (count - 1)) * i) : 0.0F;
					float sweepOffset = Mth.sin(time * 0.06F) * 28.0F;
					poseStack.mulPose(Axis.YP.rotationDegrees(sweepOffset * 0.5F));
					poseStack.mulPose(Axis.XP.rotationDegrees(baseAngle + sweepOffset));
				}
				case CONE_SPIN -> {
					float circleAngle = time * 2.5F + (360.0F / count) * i;
					float coneTilt = spread * 0.45F;
					poseStack.mulPose(Axis.YP.rotationDegrees(circleAngle));
					poseStack.mulPose(Axis.XP.rotationDegrees(coneTilt));
				}
				case STATIC_FAN -> {
					float baseAngle = count > 1 ? (-spread * 0.5F + (spread / (count - 1)) * i) : 0.0F;
					poseStack.mulPose(Axis.XP.rotationDegrees(baseAngle));
				}
				case BEAT_BURST -> {
					float baseAngle = count > 1 ? (-spread * 0.5F + (spread / (count - 1)) * i) : 0.0F;
					poseStack.mulPose(Axis.XP.rotationDegrees(baseAngle));
				}
			}

			drawLaserCylinder(poseStack, multiBufferSource, 0.022F, length, 1.0F, 1.0F, 1.0F, 0.95F * intensity);
			drawLaserCylinder(poseStack, multiBufferSource, 0.085F, length, red, green, blue, 0.45F * intensity);

			poseStack.popPose();
		}

		poseStack.popPose();
	}

	private static Quaternionf getFacingRotation(LaserProjectorBlockEntity laser) {
		Direction facing = Direction.UP;
		if (laser.getBlockState().hasProperty(LaserProjectorBlock.FACING)) {
			facing = laser.getBlockState().getValue(LaserProjectorBlock.FACING);
		}

		return switch (facing) {
			case DOWN -> Axis.XP.rotationDegrees(180.0F);
			case UP -> new Quaternionf();
			case NORTH -> Axis.XP.rotationDegrees(-90.0F);
			case SOUTH -> Axis.XP.rotationDegrees(90.0F);
			case WEST -> Axis.ZP.rotationDegrees(90.0F);
			case EAST -> Axis.ZP.rotationDegrees(-90.0F);
		};
	}

	private static int resolveColor(LaserProjectorBlockEntity laser) {
		int col = laser.getColor();
		if (col == BeatLampBlockEntity.COLOR_OLED) {
			float hue = (JukeboxAudioTracker.getEffectTime() * 2.5F % 360.0F) / 360.0F;
			return java.awt.Color.HSBtoRGB(hue, 0.85F, 1.0F);
		}
		return col;
	}

	private static void drawLaserDiode(PoseStack poseStack, MultiBufferSource buffers, float r, float g, float b, float size) {
		VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
		Matrix4f matrix = poseStack.last().pose();

		consumer.vertex(matrix, -size, 0.0F, -size).color(r, g, b, 1.0F).endVertex();
		consumer.vertex(matrix, size, 0.0F, -size).color(r, g, b, 1.0F).endVertex();
		consumer.vertex(matrix, size, 0.0F, size).color(r, g, b, 1.0F).endVertex();
		consumer.vertex(matrix, -size, 0.0F, size).color(r, g, b, 1.0F).endVertex();
	}

	private static void drawLaserCylinder(
		PoseStack poseStack,
		MultiBufferSource buffers,
		float radius,
		float length,
		float r,
		float g,
		float b,
		float a
	) {
		VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(BEAM_TEXTURE));
		Matrix4f matrix = poseStack.last().pose();

		int segments = 8;
		for (int i = 0; i < segments; i++) {
			double a1 = 2.0 * Math.PI * i / segments;
			double a2 = 2.0 * Math.PI * (i + 1) / segments;

			float x1 = (float) (Math.cos(a1) * radius);
			float z1 = (float) (Math.sin(a1) * radius);
			float x2 = (float) (Math.cos(a2) * radius);
			float z2 = (float) (Math.sin(a2) * radius);

			consumer.vertex(matrix, x1, 0.0F, z1).color(r, g, b, a).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 1, 0).endVertex();
			consumer.vertex(matrix, x2, 0.0F, z2).color(r, g, b, a).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 1, 0).endVertex();
			consumer.vertex(matrix, x2, length, z2).color(r, g, b, a * 0.15F).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 1, 0).endVertex();
			consumer.vertex(matrix, x1, length, z1).color(r, g, b, a * 0.15F).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(0, 1, 0).endVertex();
		}
	}
}
