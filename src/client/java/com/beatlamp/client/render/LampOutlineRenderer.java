package com.beatlamp.client.render;

import java.util.List;

import com.beatlamp.BeatLamp;
import com.beatlamp.BeatLampBlocks;
import com.beatlamp.BeatLampItems;
import com.beatlamp.block.BeatLampBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
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

	public static void render(WorldRenderContext context) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;

		if (player == null || context.consumers() == null || context.world() == null) {
			return;
		}

		ItemStack controller = getHeldController(player);

		if (controller == null) {
			cachedTarget = null;
			return;
		}

		debugLookedAtLamp(context.world(), player);

		BlockPos anchor = controller.get(BeatLampItems.ANCHOR_POS);

		PoseStack poseStack = context.matrixStack();
		Vec3 camera = context.camera().getPosition();

		poseStack.pushPose();
		poseStack.translate(-camera.x, -camera.y, -camera.z);

		if (anchor == null) {
			renderGroupPreview(context, poseStack, player);
		} else {
			renderAreaPreview(context, poseStack, player, anchor);
		}

		poseStack.popPose();
	}

	private static void renderGroupPreview(WorldRenderContext context, PoseStack poseStack, Player player) {
		BlockPos target = findLookedAtLamp(context.world(), player);

		if (target == null) {
			cachedTarget = null;
			return;
		}

		long now = context.world().getGameTime();

		if (!target.equals(cachedTarget) || now - cacheTime >= 5) {
			cachedTarget = target;
			cachedMembers = resolveMembers(context.world(), target);
			cacheTime = now;
		}

		VertexConsumer consumer = context.consumers().getBuffer(RenderType.lines());

		for (BlockPos member : cachedMembers) {
			AABB aabb = new AABB(member).inflate(0.002);
			LevelRenderer.renderLineBox(poseStack, consumer, aabb, 0.2F, 0.9F, 1.0F, 0.9F);
		}
	}

	private static void renderAreaPreview(WorldRenderContext context, PoseStack poseStack, Player player, BlockPos anchor) {
		VertexConsumer lines = context.consumers().getBuffer(RenderType.lines());

		LevelRenderer.renderLineBox(poseStack, lines, new AABB(anchor).inflate(0.004), 1.0F, 0.85F, 0.2F, 1.0F);

		HitResult hitResult = player.pick(8.0D, 0.0F, false);

		if (!(hitResult instanceof BlockHitResult blockHitResult) || hitResult.getType() == HitResult.Type.MISS) {
			return;
		}

		BlockPos target = blockHitResult.getBlockPos();

		if (target.equals(anchor)) {
			return;
		}

		int minX = Math.min(anchor.getX(), target.getX());
		int minY = Math.min(anchor.getY(), target.getY());
		int minZ = Math.min(anchor.getZ(), target.getZ());
		int maxX = Math.max(anchor.getX(), target.getX());
		int maxY = Math.max(anchor.getY(), target.getY());
		int maxZ = Math.max(anchor.getZ(), target.getZ());

		AABB area = new AABB(minX - 0.02, minY - 0.02, minZ - 0.02, maxX + 1.02, maxY + 1.02, maxZ + 1.02);

		VertexConsumer fill = context.consumers().getBuffer(RenderType.debugQuads());
		fillBox(fill, poseStack,
			(float) area.minX, (float) area.maxX,
			(float) area.minY, (float) area.maxY,
			(float) area.minZ, (float) area.maxZ,
			1.0F, 0.85F, 0.25F, 0.28F
		);

		VertexConsumer linesAfterFill = context.consumers().getBuffer(RenderType.lines());
		LevelRenderer.renderLineBox(poseStack, linesAfterFill, area, 1.0F, 0.95F, 0.45F, 0.9F);
	}

	private static void fillBox(
		VertexConsumer consumer,
		PoseStack poseStack,
		float minX, float maxX,
		float minY, float maxY,
		float minZ, float maxZ,
		float red, float green, float blue, float alpha
	) {
		face(consumer, poseStack, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, red, green, blue, alpha);
		face(consumer, poseStack, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
		face(consumer, poseStack, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, alpha);
		face(consumer, poseStack, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, red, green, blue, alpha);
		face(consumer, poseStack, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
		face(consumer, poseStack, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ, red, green, blue, alpha);
	}

	private static void face(
		VertexConsumer consumer,
		PoseStack poseStack,
		float x0, float y0, float z0,
		float x1, float y1, float z1,
		float x2, float y2, float z2,
		float x3, float y3, float z3,
		float red, float green, float blue, float alpha
	) {
		consumer.addVertex(poseStack.last(), x0, y0, z0).setColor(red, green, blue, alpha);
		consumer.addVertex(poseStack.last(), x1, y1, z1).setColor(red, green, blue, alpha);
		consumer.addVertex(poseStack.last(), x2, y2, z2).setColor(red, green, blue, alpha);
		consumer.addVertex(poseStack.last(), x3, y3, z3).setColor(red, green, blue, alpha);
	}

	private static ItemStack getHeldController(Player player) {
		for (InteractionHand interactionHand : InteractionHand.values()) {
			ItemStack itemStack = player.getItemInHand(interactionHand);

			if (itemStack.is(BeatLampItems.CONTROLLER)) {
				return itemStack;
			}
		}

		return null;
	}

	private static List<BlockPos> resolveMembers(Level level, BlockPos target) {
		if (level.getBlockEntity(target) instanceof BeatLampBlockEntity beatLamp && beatLamp.getManualGroup().size() >= 2) {
			List<BlockPos> members = new java.util.ArrayList<>();

			for (BlockPos member : beatLamp.getManualGroup()) {
				if (level.getBlockState(member).is(BeatLampBlocks.BEAT_LAMP)) {
					members.add(member);
				}
			}

			return members;
		}

		return BeatLamp.floodFill(level, target);
	}

	private static BlockPos findLookedAtLamp(Level level, Player player) {
		var hitResult = player.pick(8.0, 0.0F, false);

		if (hitResult instanceof BlockHitResult blockHitResult
			&& level.getBlockState(blockHitResult.getBlockPos()).is(BeatLampBlocks.BEAT_LAMP)) {
			return blockHitResult.getBlockPos();
		}

		return null;
	}

	private static void debugLookedAtLamp(Level level, Player player) {
		if (level.getGameTime() % 10L != 0L) {
			return;
		}

		BlockPos target = findLookedAtLamp(level, player);

		if (target == null || !(level.getBlockEntity(target) instanceof BeatLampBlockEntity beatLamp)) {
			return;
		}

		String text = String.format(
			"mode=%s col=%s bb=%d il=%d lit=%d p=%.2f beat=%.2f bar=%.2f dc=%s",
			beatLamp.getMode().getSerializedName(),
			beatLamp.getColor() == BeatLampBlockEntity.COLOR_OLED ? "OLED" : "#" + Integer.toHexString(beatLamp.getColor()).toUpperCase(),
			beatLamp.isBlackback() ? 1 : 0,
			beatLamp.isIdleLight() ? 1 : 0,
			level.getBlockState(target).getValue(com.beatlamp.block.BeatLampBlock.LIT) ? 1 : 0,
			beatLamp.pulse,
			beatLamp.beatPulse,
			beatLamp.barValue,
			"#" + Integer.toHexString(beatLamp.displayColor).toUpperCase()
		);

		player.displayClientMessage(net.minecraft.network.chat.Component.literal(text), true);
	}
}
