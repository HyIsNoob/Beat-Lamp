package com.beatlamp.item;

import java.util.List;

import com.beatlamp.BeatLampItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class LampControllerItem extends Item {
	public LampControllerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
		ItemStack itemStack = player.getItemInHand(interactionHand);

		if (itemStack.get(BeatLampItems.ANCHOR_POS) != null) {
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
		tooltipComponents.add(Component.translatable("item.beatlamp.controller.tooltip").withStyle(ChatFormatting.GRAY));

		BlockPos anchor = itemStack.get(BeatLampItems.ANCHOR_POS);

		if (anchor != null) {
			tooltipComponents.add(
				Component.translatable("item.beatlamp.controller.anchor", anchor.getX(), anchor.getY(), anchor.getZ()).withStyle(ChatFormatting.GOLD)
			);
		}
	}
}
