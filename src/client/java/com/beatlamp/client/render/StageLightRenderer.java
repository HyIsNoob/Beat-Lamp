package com.beatlamp.client.render;

import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;
import com.beatlamp.block.StageLightMode;
import com.beatlamp.client.audio.JukeboxAudioTracker;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

import org.joml.Matrix4f;

public class StageLightRenderer implements BlockEntityRenderer<StageLightBlockEntity> {
	private static final ResourceLocation BEAM_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
	private static final DyeColor[] DYES = DyeColor.values();

	public StageLightRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public int getViewDistance() {
		return com.beatlamp.client.config.BeatLampClientConfig.beamRenderDistance;
	}

	@Override
	public void render(
		StageLightBlockEntity light,
		float partialTick,
		PoseStack poseStack,
		MultiBufferSource multiBufferSource,
		int packedLight,
		int packedOverlay
	) {
		float energy = Mth.clamp(light.beamEnergy * light.getSensitivity(), 0.0F, 1.0F);
		float beat = Mth.clamp(light.beamBeat, 0.0F, 1.0F);

		if (energy <= 0.02F) {
			return;
		}

		StageLightMode mode = light.getMode();

		// Strobe mode check
		if (mode == StageLightMode.STROBE) {
			float strobeTime = JukeboxAudioTracker.getEffectTime() * light.getSpeed();
			if ((int) (strobeTime * 0.8F) % 2 == 1 && beat < 0.7F) {
				return;
			}
		}

		int color = resolveColor(light, energy);
		float red = ((color >> 16) & 0xFF) / 255.0F;
		float green = ((color >> 8) & 0xFF) / 255.0F;
		float blue = (color & 0xFF) / 255.0F;

		float brightness = 0.6F + energy * 1.0F;
		float coreRed = Math.min(red * brightness, 1.0F);
		float coreGreen = Math.min(green * brightness, 1.0F);
		float coreBlue = Math.min(blue * brightness, 1.0F);

		poseStack.pushPose();
		poseStack.translate(0.5F, 0.5F, 0.5F);
		drawCore(poseStack, multiBufferSource, coreRed, coreGreen, coreBlue, 0.32F + beat * 0.12F);

		poseStack.mulPose(alignment(light));
		float effectTime = JukeboxAudioTracker.getEffectTime() * light.getSpeed();

		switch (mode) {
			case SWEEP -> {
				float pan = Mth.sin(effectTime * 0.05F) * 48.0F;
				float tilt = 28.0F + Mth.sin(effectTime * 0.075F + 0.8F) * 22.0F;
				poseStack.mulPose(Axis.YP.rotationDegrees(pan));
				poseStack.mulPose(Axis.XP.rotationDegrees(tilt));
			}
			case BEAT_STEP -> {
				light.currentPan = Mth.lerp(0.28F, light.currentPan, light.targetPan);
				light.currentTilt = Mth.lerp(0.28F, light.currentTilt, light.targetTilt);
				poseStack.mulPose(Axis.YP.rotationDegrees(light.currentPan));
				poseStack.mulPose(Axis.XP.rotationDegrees(light.currentTilt));
			}
			case CHASE -> {
				float phase = effectTime * 0.12F + light.groupIndex * 0.5F;
				float pan = Mth.sin(phase) * 52.0F;
				float tilt = 30.0F + Mth.cos(phase) * 22.0F;
				poseStack.mulPose(Axis.YP.rotationDegrees(pan));
				poseStack.mulPose(Axis.XP.rotationDegrees(tilt));
			}
			case STATIC, STROBE -> {
				// Fixed straight beam
			}
		}

		VertexConsumer consumer = multiBufferSource.getBuffer(RenderType.beaconBeam(BEAM_TEXTURE, true));
		Matrix4f matrix = poseStack.last().pose();

		// Outer wide atmospheric glow cone
		float outerLength = 14.0F + beat * 18.0F;
		float outerAlpha = 0.22F + energy * 0.35F;
		drawBeam(consumer, matrix, outerLength, outerAlpha, 0.0F, 0.18F, 1.25F, coreRed, coreGreen, coreBlue);

		// Inner intense core beam
		float innerLength = 12.0F + beat * 16.0F;
		float innerAlpha = 0.55F + energy * 0.4F;
		drawBeam(consumer, matrix, innerLength, innerAlpha, 0.05F, 0.09F, 0.45F, coreRed, coreGreen, coreBlue);

		poseStack.popPose();
	}

	private static org.joml.Quaternionf alignment(StageLightBlockEntity light) {
		Direction facing = light.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);

		return switch (facing) {
			case DOWN -> Axis.XP.rotationDegrees(180.0F);
			case NORTH -> Axis.XN.rotationDegrees(90.0F);
			case SOUTH -> Axis.XP.rotationDegrees(90.0F);
			case WEST -> Axis.ZP.rotationDegrees(90.0F);
			case EAST -> Axis.ZN.rotationDegrees(90.0F);
			default -> Axis.YP.rotationDegrees(0.0F);
		};
	}

	private static void drawCore(PoseStack poseStack, MultiBufferSource multiBufferSource, float red, float green, float blue, float half) {
		VertexConsumer consumer = multiBufferSource.getBuffer(RenderType.beaconBeam(BEAM_TEXTURE, true));
		PoseStack.Pose pose = poseStack.last();
		Matrix4f matrix = pose.pose();
		float a = 0.95F;

		float[][][] faces = {
			{{-half, -half, half}, {half, -half, half}, {half, half, half}, {-half, half, half}},
			{{half, -half, -half}, {-half, -half, -half}, {-half, half, -half}, {half, half, -half}},
			{{half, -half, half}, {half, -half, -half}, {half, half, -half}, {half, half, half}},
			{{-half, -half, -half}, {-half, -half, half}, {-half, half, half}, {-half, half, -half}},
			{{-half, half, half}, {half, half, half}, {half, half, -half}, {-half, half, -half}},
			{{-half, -half, -half}, {half, -half, -half}, {half, -half, half}, {-half, -half, half}}
		};

		for (float[][] face : faces) {
			for (float[] vertex : face) {
				consumer.addVertex(matrix, vertex[0], vertex[1], vertex[2])
					.setColor(red, green, blue, a)
					.setUv(0.5F, 0.5F)
					.setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
					.setLight(0xF000F0)
					.setNormal(pose, 0.0F, 1.0F, 0.0F);
			}
		}
	}

	private static void drawBeam(
		VertexConsumer consumer,
		Matrix4f matrix,
		float length,
		float alphaStart,
		float alphaEnd,
		float baseHalf,
		float endHalf,
		float red,
		float green,
		float blue
	) {
		float[][] cornersBase = {
			{-baseHalf, 0.0F, -baseHalf},
			{baseHalf, 0.0F, -baseHalf},
			{baseHalf, 0.0F, baseHalf},
			{-baseHalf, 0.0F, baseHalf}
		};
		float[][] cornersEnd = {
			{-endHalf, length, -endHalf},
			{endHalf, length, -endHalf},
			{endHalf, length, endHalf},
			{-endHalf, length, endHalf}
		};

		for (int i = 0; i < 4; i++) {
			int next = (i + 1) % 4;
			float[] b0 = cornersBase[i];
			float[] b1 = cornersBase[next];
			float[] e1 = cornersEnd[next];
			float[] e0 = cornersEnd[i];

			quad(consumer, matrix, b0, b1, e1, e0, alphaStart, alphaEnd, red, green, blue);
			quad(consumer, matrix, b1, b0, e0, e1, alphaStart, alphaEnd, red, green, blue);
		}
	}

	private static void quad(
		VertexConsumer consumer,
		Matrix4f matrix,
		float[] v0,
		float[] v1,
		float[] v2,
		float[] v3,
		float alphaBottom,
		float alphaTop,
		float red,
		float green,
		float blue
	) {
		vertex(consumer, matrix, v0, red, green, blue, alphaBottom, 0.0F, 0.0F);
		vertex(consumer, matrix, v1, red, green, blue, alphaBottom, 1.0F, 0.0F);
		vertex(consumer, matrix, v2, red, green, blue, alphaTop, 1.0F, 1.0F);
		vertex(consumer, matrix, v3, red, green, blue, alphaTop, 0.0F, 1.0F);
	}

	private static void vertex(
		VertexConsumer consumer,
		Matrix4f matrix,
		float[] position,
		float red,
		float green,
		float blue,
		float alpha,
		float u,
		float v
	) {
		consumer.addVertex(matrix, position[0], position[1], position[2])
			.setColor(red, green, blue, alpha)
			.setUv(u, v)
			.setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
			.setLight(0xF000F0)
			.setNormal(0.0F, 1.0F, 0.0F);
	}

	private static int resolveColor(StageLightBlockEntity light, float energy) {
		if (energy < 0.03F) {
			return 0;
		}

		int color = light.getColor();

		if (color == BeatLampBlockEntity.COLOR_OLED) {
			float hue = (JukeboxAudioTracker.getEffectTime() * 2.0F % 360.0F) / 360.0F;
			return java.awt.Color.HSBtoRGB(hue, 0.85F, 1.0F);
		}

		if (color == StageLightBlockEntity.COLOR_BEAT_CYCLE) {
			int dyeIndex = Math.abs(light.beatColorIndex) % DYES.length;
			return DYES[dyeIndex].getFireworkColor();
		}

		return color;
	}
}
