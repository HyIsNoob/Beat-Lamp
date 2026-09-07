package com.beatlamp.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.beatlamp.BeatLamp;

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

		if (player == null) {
			return InteractionResult.PASS;
		}

		ItemStack stack = context.getItemInHand();

		// Sneak + Right Click to cancel active selection
		if (player.isShiftKeyDown()) {
			if (stack.hasTag() && stack.getTag().contains("AnchorPos")) {
				if (!level.isClientSide) {
					stack.getTag().remove("AnchorPos");
					player.displayClientMessage(
						Component.translatable("message.beatlamp.link.cancel").withStyle(ChatFormatting.AQUA), true
					);
				}
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
			return InteractionResult.PASS;
		}

		if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
			BeatLamp.handleLink(serverLevel, context.getClickedPos(), serverPlayer, stack);
		}

		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) {
			if (stack.hasTag() && stack.getTag().contains("AnchorPos")) {
				if (!level.isClientSide) {
					stack.getTag().remove("AnchorPos");
					player.displayClientMessage(Component.translatable("message.beatlamp.link.cancel").withStyle(ChatFormatting.AQUA), true);
				}
				return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
			}
		}
		return InteractionResultHolder.pass(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.beatlamp.linker.summary").withStyle(ChatFormatting.GRAY));

		if (stack.hasTag() && stack.getTag().contains("AnchorPos")) {
			BlockPos anchor = BlockPos.of(stack.getTag().getLong("AnchorPos"));
			tooltip.add(Component.translatable("tooltip.beatlamp.linker.anchor", anchor.toShortString()).withStyle(ChatFormatting.GOLD));
		}
	}
}
