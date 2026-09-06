package com.beatlamp.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.beatlamp.BeatLamp;
import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LampControllerItem extends Item {
	public LampControllerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos clickedPos = context.getClickedPos();
		Player player = context.getPlayer();
		ItemStack held = context.getItemInHand();

		var blockState = level.getBlockState(clickedPos);
		boolean isJukebox = blockState.is(Blocks.JUKEBOX) || blockState.getBlock() instanceof com.beatlamp.block.StageJukeboxBlock;

		if (player != null && player.isShiftKeyDown() && isJukebox) {
			if (player instanceof ServerPlayer serverPlayer) {
				BeatLamp.handleSourceSelect(serverPlayer, clickedPos, held);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		BlockEntity be = level.getBlockEntity(clickedPos);
		boolean isDevice = be instanceof BeatLampBlockEntity || be instanceof StageLightBlockEntity
			|| be instanceof FountainBlockEntity || be instanceof LaserProjectorBlockEntity
			|| be instanceof FogGeneratorBlockEntity || be instanceof BeatEmitterBlockEntity;

		if (held.hasTag() && held.getTag().contains("SourcePos") && isDevice) {
			BlockPos source = BlockPos.of(held.getTag().getLong("SourcePos"));
			if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
				BeatLamp.bindSource(serverLevel, clickedPos, source, serverPlayer, held);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		if (isDevice) {
			if (level.isClientSide) {
				if (be instanceof BeatLampBlockEntity beatLamp && BeatLampBlockEntity.controllerUser != null) {
					BeatLampBlockEntity.controllerUser.use(beatLamp);
				} else if (be instanceof StageLightBlockEntity light && StageLightBlockEntity.controllerUser != null) {
					StageLightBlockEntity.controllerUser.use(light);
				} else if (be instanceof FountainBlockEntity fountain && FountainBlockEntity.controllerUser != null) {
					FountainBlockEntity.controllerUser.use(fountain);
				} else if (be instanceof LaserProjectorBlockEntity laser && LaserProjectorBlockEntity.controllerUser != null) {
					LaserProjectorBlockEntity.controllerUser.use(laser);
				} else if (be instanceof FogGeneratorBlockEntity fog && FogGeneratorBlockEntity.controllerUser != null) {
					FogGeneratorBlockEntity.controllerUser.use(fog);
				} else if (be instanceof BeatEmitterBlockEntity emitter && BeatEmitterBlockEntity.controllerUser != null) {
					BeatEmitterBlockEntity.controllerUser.use(emitter);
				}
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.beatlamp.controller.summary").withStyle(ChatFormatting.GRAY));

		if (stack.hasTag() && stack.getTag().contains("SourcePos")) {
			BlockPos source = BlockPos.of(stack.getTag().getLong("SourcePos"));
			tooltip.add(Component.translatable("tooltip.beatlamp.controller.source", source.toShortString()).withStyle(ChatFormatting.GOLD));
		}
	}
}
