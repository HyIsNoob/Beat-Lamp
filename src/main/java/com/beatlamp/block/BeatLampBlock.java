package com.beatlamp.block;

import com.beatlamp.BeatLamp;
import com.beatlamp.BeatLampBlockEntities;
import com.beatlamp.BeatLampBlocks;
import com.beatlamp.BeatLampItems;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

public class BeatLampBlock extends BaseEntityBlock {
	public static final BooleanProperty FRAMELESS = BooleanProperty.create("frameless");
	public static final BooleanProperty LIT = BooleanProperty.create("lit");
	public static final MapCodec<BeatLampBlock> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(propertiesCodec()).apply(instance, BeatLampBlock::new)
	);

	public BeatLampBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FRAMELESS, true).setValue(LIT, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FRAMELESS);
		builder.add(LIT);
	}

	@Override
	protected RenderShape getRenderShape(BlockState blockState) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new BeatLampBlockEntity(blockPos, blockState);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
		return level.isClientSide
			? createTickerHelper(blockEntityType, BeatLampBlockEntities.BEAT_LAMP, BeatLampBlockEntity::clientTick)
			: createTickerHelper(blockEntityType, BeatLampBlockEntities.BEAT_LAMP, BeatLampBlockEntity::serverTick);
	}

	@Override
	protected void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState newState, boolean movedByPiston) {
		if (!blockState.is(newState.getBlock())) {
			if (level.getBlockEntity(blockPos) instanceof BeatLampBlockEntity beatLamp) {
				beatLamp.onRemovedFromWorld();
			}
		}

		super.onRemove(blockState, level, blockPos, newState, movedByPiston);
	}

	@Override
	protected ItemInteractionResult useItemOn(
		ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
	) {
		if (itemStack.is(BeatLampItems.CONTROLLER)) {
			if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
				BeatLamp.handleLink((ServerLevel) level, blockPos, serverPlayer, itemStack);
			}

			return ItemInteractionResult.SUCCESS;
		}

		if (itemStack.getItem() instanceof DyeItem dyeItem) {
			if (!level.isClientSide
				&& level.getBlockEntity(blockPos) instanceof BeatLampBlockEntity beatLamp
				&& beatLamp.setColor(dyeItem.getDyeColor().getFireworkColor())) {
				if (!player.isCreative()) {
					itemStack.shrink(1);
				}

				return ItemInteractionResult.CONSUME;
			}

			return ItemInteractionResult.CONSUME;
		}

		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}

		if (level.getBlockEntity(blockPos) instanceof BeatLampBlockEntity beatLamp) {
			if (player.isShiftKeyDown()) {
				beatLamp.cycleColor(player);
			} else {
				beatLamp.cycleMode(player);
			}

			return InteractionResult.CONSUME;
		}

		return InteractionResult.PASS;
	}
}
