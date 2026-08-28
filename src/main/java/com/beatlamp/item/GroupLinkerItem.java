package com.beatlamp.item;

import java.util.List;

import com.beatlamp.BeatLamp;
import com.beatlamp.BeatLampItems;

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

public class GroupLinkerItem extends Item {
	public GroupLinkerItem(Properties properties) {
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

		ItemStack itemStack = context.getItemInHand();

		// Sneak + Right Click to cancel active selection
		if (player.isShiftKeyDown()) {
			if (itemStack.get(BeatLampItems.ANCHOR_POS) != null) {
				if (!level.isClientSide) {
					itemStack.remove(BeatLampItems.ANCHOR_POS);
					player.displayClientMessage(
						Component.translatable("message.beatlamp.link.cancel").withStyle(ChatFormatting.AQUA), true
					);
				}
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
			return InteractionResult.PASS;
		}

		if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
			BeatLamp.handleLink(serverLevel, blockPos, serverPlayer, itemStack);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
		ItemStack itemStack = player.getItemInHand(interactionHand);

		// Sneak + Right Click in air to cancel active selection
		if (player.isShiftKeyDown() && itemStack.get(BeatLampItems.ANCHOR_POS) != null) {
			if (!level.isClientSide) {
				itemStack.remove(BeatLampItems.ANCHOR_POS);
				player.displayClientMessage(
					Component.translatable("message.beatlamp.link.cancel").withStyle(ChatFormatting.AQUA), true
				);
			}

			return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
		}

		return InteractionResultHolder.pass(itemStack);
	}

	@Override
	public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		tooltipComponents.add(Component.translatable("item.beatlamp.linker.tooltip").withStyle(ChatFormatting.GRAY));

		BlockPos anchor = itemStack.get(BeatLampItems.ANCHOR_POS);
		if (anchor != null) {
			tooltipComponents.add(
				Component.translatable("item.beatlamp.linker.anchor", anchor.getX(), anchor.getY(), anchor.getZ()).withStyle(ChatFormatting.GOLD)
			);
		}
	}
}
