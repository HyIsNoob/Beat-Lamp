package com.beatlamp.item;

import java.util.List;

import com.beatlamp.BeatLamp;
import com.beatlamp.BeatLampItems;
import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;

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

		ItemStack controller = context.getItemInHand();
		BlockPos pendingSource = controller.get(BeatLampItems.SOURCE_POS);

		// 1. Sneak interaction: Jukebox Source Select or Binding
		if (player.isShiftKeyDown()) {
			if (level.getBlockState(blockPos).is(Blocks.JUKEBOX)) {
				if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
					BeatLamp.handleSourceSelect(serverPlayer, blockPos, controller);
				}
				return InteractionResult.SUCCESS;
			}

			if (pendingSource != null) {
				if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
					if (level.getBlockEntity(blockPos) instanceof BeatLampBlockEntity) {
						BeatLamp.bindSource(serverLevel, blockPos, serverPlayer, controller, pendingSource);
					} else if (level.getBlockEntity(blockPos) instanceof StageLightBlockEntity
						|| level.getBlockEntity(blockPos) instanceof FountainBlockEntity
						|| level.getBlockEntity(blockPos) instanceof BeatEmitterBlockEntity) {
						BeatLamp.bindTarget(serverLevel, blockPos, serverPlayer, controller, pendingSource);
					}
				}
				return InteractionResult.SUCCESS;
			}

			if (level.getBlockEntity(blockPos) instanceof BeatEmitterBlockEntity) {
				if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
					BeatLamp.toggleTarget(serverPlayer, blockPos);
				}
				return InteractionResult.SUCCESS;
			}
		}

		// 2. Normal Right-Click: Open Config GUI directly for the clicked block/group
		if (pendingSource != null) {
			// If holding pending source, right click also binds
			if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
				if (level.getBlockEntity(blockPos) instanceof BeatLampBlockEntity) {
					BeatLamp.bindSource(serverLevel, blockPos, serverPlayer, controller, pendingSource);
				} else if (level.getBlockEntity(blockPos) instanceof StageLightBlockEntity
					|| level.getBlockEntity(blockPos) instanceof FountainBlockEntity
					|| level.getBlockEntity(blockPos) instanceof BeatEmitterBlockEntity) {
					BeatLamp.bindTarget(serverLevel, blockPos, serverPlayer, controller, pendingSource);
				}
			}
			return InteractionResult.SUCCESS;
		}

		if (level.isClientSide) {
			if (level.getBlockEntity(blockPos) instanceof BeatLampBlockEntity beatLamp) {
				BeatLampBlockEntity.controllerUser.use(beatLamp);
				return InteractionResult.SUCCESS;
			} else if (level.getBlockEntity(blockPos) instanceof StageLightBlockEntity stageLight) {
				StageLightBlockEntity.controllerUser.use(stageLight);
				return InteractionResult.SUCCESS;
			} else if (level.getBlockEntity(blockPos) instanceof FountainBlockEntity fountain) {
				FountainBlockEntity.controllerUser.use(fountain);
				return InteractionResult.SUCCESS;
			}
		}

		if (level.getBlockEntity(blockPos) instanceof BeatEmitterBlockEntity) {
			if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
				BeatLamp.toggleTarget(serverPlayer, blockPos);
			}
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
		ItemStack itemStack = player.getItemInHand(interactionHand);

		if (itemStack.get(BeatLampItems.SOURCE_POS) != null) {
			if (!level.isClientSide) {
				itemStack.remove(BeatLampItems.SOURCE_POS);
				player.displayClientMessage(
					Component.translatable("message.beatlamp.source.cancel").withStyle(ChatFormatting.AQUA), true
				);
			}

			return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
		}

		return InteractionResultHolder.pass(itemStack);
	}

	@Override
	public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		tooltipComponents.add(Component.translatable("item.beatlamp.controller.tooltip").withStyle(ChatFormatting.GRAY));

		BlockPos source = itemStack.get(BeatLampItems.SOURCE_POS);
		if (source != null) {
			tooltipComponents.add(
				Component.translatable("item.beatlamp.controller.source", source.getX(), source.getY(), source.getZ()).withStyle(ChatFormatting.AQUA)
			);
		}
	}
}
