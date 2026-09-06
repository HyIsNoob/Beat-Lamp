package com.beatlamp.client.render;

import java.util.List;

import com.beatlamp.BeatLamp;
import com.beatlamp.BeatLampItems;
import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class LampOutlineRenderer {
	private static BlockPos cachedTarget;
	private static List<BlockPos> cachedMembers = List.of();
	private static long cacheTime;

	private LampOutlineRenderer() {
	}

	public static void renderOutlines(Level level, PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camera) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;

		if (player == null || bufferSource == null || level == null) {
			return;
		}

		ItemStack heldItem = getHeldTool(player);
		if (heldItem == null) {
			cachedTarget = null;
			return;
		}

		BlockPos anchor = heldItem.hasTag() && heldItem.getTag().contains("AnchorPos") ? BlockPos.of(heldItem.getTag().getLong("AnchorPos")) : null;

		poseStack.pushPose();
		if (camera != null && !camera.equals(Vec3.ZERO)) {
			poseStack.translate(-camera.x, -camera.y, -camera.z);
		}

		if (anchor == null) {
			renderGroupPreview(level, bufferSource, poseStack, player);
		} else {
			renderAreaPreview(level, bufferSource, poseStack, player, anchor);
		}

		poseStack.popPose();
	}

	private static void renderGroupPreview(Level level, MultiBufferSource bufferSource, PoseStack poseStack, Player player) {
		BlockPos target = findLookedAtDevice(level, player);
		if (target == null) {
			cachedTarget = null;
			return;
		}

		long now = level.getGameTime();
		if (!target.equals(cachedTarget) || now - cacheTime >= 5) {
			cachedTarget = target;
			cachedMembers = resolveMembers(level, target);
			cacheTime = now;
		}

		VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

		for (BlockPos member : cachedMembers) {
			AABB aabb = new AABB(member).inflate(0.002);
			LevelRenderer.renderLineBox(poseStack, consumer, aabb, 0.2F, 0.9F, 1.0F, 0.9F);
		}
	}

	private static void renderAreaPreview(Level level, MultiBufferSource bufferSource, PoseStack poseStack, Player player, BlockPos anchor) {
		VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());

		AABB anchorAabb = new AABB(anchor).inflate(0.003);
		LevelRenderer.renderLineBox(poseStack, lines, anchorAabb, 1.0F, 0.85F, 0.2F, 1.0F);

		HitResult hit = player.pick(5.0D, 0.0F, false);
		if (hit instanceof BlockHitResult blockHit) {
			BlockPos target = blockHit.getBlockPos();
			if (!target.equals(anchor)) {
				int minX = Math.min(anchor.getX(), target.getX());
				int minY = Math.min(anchor.getY(), target.getY());
				int minZ = Math.min(anchor.getZ(), target.getZ());
				int maxX = Math.max(anchor.getX(), target.getX()) + 1;
				int maxY = Math.max(anchor.getY(), target.getY()) + 1;
				int maxZ = Math.max(anchor.getZ(), target.getZ()) + 1;

				AABB areaAabb = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
				LevelRenderer.renderLineBox(poseStack, lines, areaAabb, 0.2F, 1.0F, 0.4F, 0.8F);
			}
		}
	}

	private static ItemStack getHeldTool(Player player) {
		ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
		if (isLinkTool(main)) return main;
		ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
		if (isLinkTool(off)) return off;
		return null;
	}

	private static boolean isLinkTool(ItemStack stack) {
		return stack.getItem() instanceof com.beatlamp.item.GroupLinkerItem || stack.getItem() instanceof com.beatlamp.item.LampControllerItem;
	}

	private static BlockPos findLookedAtDevice(Level level, Player player) {
		HitResult hit = player.pick(5.0D, 0.0F, false);
		if (hit instanceof BlockHitResult blockHit) {
			BlockPos pos = blockHit.getBlockPos();
			var be = level.getBlockEntity(pos);
			if (be instanceof BeatLampBlockEntity || be instanceof StageLightBlockEntity
				|| be instanceof FountainBlockEntity || be instanceof LaserProjectorBlockEntity
				|| be instanceof FogGeneratorBlockEntity || be instanceof BeatEmitterBlockEntity) {
				return pos;
			}
		}
		return null;
	}

	private static List<BlockPos> resolveMembers(Level level, BlockPos target) {
		var be = level.getBlockEntity(target);
		if (be instanceof BeatLampBlockEntity lamp) {
			return lamp.getManualGroup().isEmpty() ? BeatLamp.floodFill(level, target) : lamp.getManualGroup();
		} else if (be instanceof StageLightBlockEntity light) {
			return light.getManualGroup().isEmpty() ? List.of(target) : light.getManualGroup();
		} else if (be instanceof FountainBlockEntity fountain) {
			return fountain.getManualGroup().isEmpty() ? List.of(target) : fountain.getManualGroup();
		} else if (be instanceof LaserProjectorBlockEntity laser) {
			return laser.getManualGroup().isEmpty() ? List.of(target) : laser.getManualGroup();
		} else if (be instanceof FogGeneratorBlockEntity fog) {
			return fog.getManualGroup().isEmpty() ? List.of(target) : fog.getManualGroup();
		} else if (be instanceof BeatEmitterBlockEntity emitter) {
			return emitter.getManualGroup().isEmpty() ? List.of(target) : emitter.getManualGroup();
		}
		return List.of();
	}
}
