package com.beatlamp.item;

import java.util.List;

import com.beatlamp.BeatLamp;
import com.beatlamp.BeatLampItems;
import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class LampControllerItem extends Item {
	public LampControllerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		BlockPos blockPos = context.getClickedPos();

		if (player == null) {
			return InteractionResult.PASS;
		}

		if (player.isShiftKeyDown()) {
			if (level.getBlockState(blockPos).is(Blocks.JUKEBOX)) {
				if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
					BeatLamp.handleSourceSelect(serverPlayer, blockPos, context.getItemInHand());
				}

				return InteractionResult.SUCCESS;
			}

			if (level.getBlockEntity(blockPos) instanceof BeatEmitterBlockEntity) {
				BlockPos pendingSource = context.getItemInHand().get(BeatLampItems.SOURCE_POS);

				if (pendingSource != null) {
					if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
						BeatLamp.bindEmitterSource(serverPlayer, blockPos, context.getItemInHand(), pendingSource);
					}

					return InteractionResult.SUCCESS;
				}

				if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
					BeatLamp.toggleEmitterMode(serverPlayer, blockPos);
				}

				return InteractionResult.SUCCESS;
			}

			if (level.getBlockEntity(blockPos) instanceof BeatLampBlockEntity beatLamp) {
				ItemStack controller = context.getItemInHand();
				BlockPos pendingSource = controller.get(BeatLampItems.SOURCE_POS);

				if (pendingSource != null) {
					if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
						BeatLamp.bindSource(serverLevel, blockPos, serverPlayer, controller, pendingSource);
					}

					return InteractionResult.SUCCESS;
				}

				if (level.isClientSide) {
					BeatLampBlockEntity.controllerUser.use(beatLamp);
				}

				return InteractionResult.SUCCESS;
			}

			return InteractionResult.PASS;
		}

		if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
			BeatLamp.handleLink(serverLevel, blockPos, serverPlayer, context.getItemInHand());
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
		ItemStack itemStack = player.getItemInHand(interactionHand);

		if (itemStack.get(BeatLampItems.ANCHOR_POS) != null || itemStack.get(BeatLampItems.SOURCE_POS) != null) {
			if (!level.isClientSide) {
				boolean hadAnchor = itemStack.get(BeatLampItems.ANCHOR_POS) != null;
				itemStack.remove(BeatLampItems.ANCHOR_POS);
				itemStack.remove(BeatLampItems.SOURCE_POS);
				player.displayClientMessage(
					Component.translatable(hadAnchor ? "message.beatlamp.link.cancel" : "message.beatlamp.source.cancel").withStyle(ChatFormatting.AQUA), true
				);
			}

			return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
		}

		return InteractionResultHolder.pass(itemStack);
	}

	@Override
	public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		tooltipComponents.add(Component.translatable("item.beatlamp.controller.tooltip").withStyle(ChatFormatting.GRAY));

		BlockPos anchor = itemStack.get(BeatLampItems.ANCHOR_POS);

		if (anchor != null) {
			tooltipComponents.add(
				Component.translatable("item.beatlamp.controller.anchor", anchor.getX(), anchor.getY(), anchor.getZ()).withStyle(ChatFormatting.GOLD)
			);
		}
	}
}
