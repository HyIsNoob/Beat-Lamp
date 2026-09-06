package com.beatlamp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class StageJukeboxBlock extends BaseEntityBlock {
	public static final BooleanProperty HAS_RECORD = BlockStateProperties.HAS_RECORD;

	public StageJukeboxBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.getStateDefinition().any().setValue(HAS_RECORD, false));
	}

	@Override
	public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
		if (blockState.getValue(HAS_RECORD)) {
			if (!level.isClientSide) {
				BlockEntity blockEntity = level.getBlockEntity(blockPos);
				if (blockEntity instanceof StageJukeboxBlockEntity jukebox) {
					jukebox.dropRecord();
					level.setBlock(blockPos, blockState.setValue(HAS_RECORD, false), Block.UPDATE_ALL);
				}
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		} else {
			ItemStack itemstack = player.getItemInHand(interactionHand);
			if (itemstack.getItem() instanceof RecordItem) {
				if (!level.isClientSide) {
					BlockEntity blockEntity = level.getBlockEntity(blockPos);
					if (blockEntity instanceof StageJukeboxBlockEntity jukebox) {
						ItemStack toInsert = itemstack.copy();
						toInsert.setCount(1);
						jukebox.setRecord(toInsert);
						jukebox.playRecord();
						level.setBlock(blockPos, blockState.setValue(HAS_RECORD, true), Block.UPDATE_ALL);
						if (!player.getAbilities().instabuild) {
							itemstack.shrink(1);
						}
					}
				}
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
		}

		return InteractionResult.PASS;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof StageJukeboxBlockEntity jukebox) {
				jukebox.dropRecord();
			}
			super.onRemove(state, level, pos, newState, isMoving);
		}
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new StageJukeboxBlockEntity(blockPos, blockState);
	}

	@Override
	public RenderShape getRenderShape(BlockState blockState) {
		return RenderShape.MODEL;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HAS_RECORD);
	}
}
