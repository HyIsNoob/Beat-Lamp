package com.beatlamp.block;

import com.beatlamp.BeatLampBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

public class StageJukeboxBlock extends BaseEntityBlock {
	public static final BooleanProperty HAS_RECORD = BlockStateProperties.HAS_RECORD;
	public static final MapCodec<StageJukeboxBlock> CODEC = simpleCodec(StageJukeboxBlock::new);

	public StageJukeboxBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(HAS_RECORD, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
		if (blockState.getValue(HAS_RECORD)) {
			this.dropRecording(level, blockPos, player);
			return InteractionResult.SUCCESS;
		}

		if (itemStack.has(DataComponents.JUKEBOX_PLAYABLE)) {
			JukeboxPlayable playable = itemStack.get(DataComponents.JUKEBOX_PLAYABLE);
			if (playable != null) {
				if (!level.isClientSide) {
					BlockEntity blockEntity = level.getBlockEntity(blockPos);
					if (blockEntity instanceof StageJukeboxBlockEntity jukebox) {
						ItemStack singleDisc = itemStack.copyWithCount(1);
						if (!player.getAbilities().instabuild) {
							itemStack.shrink(1);
						}
						jukebox.setRecord(singleDisc);
						level.setBlock(blockPos, blockState.setValue(HAS_RECORD, true), 3);

						playable.song().unwrap(level.registryAccess()).ifPresent(jukebox::playSong);
						level.gameEvent(player, GameEvent.JUKEBOX_PLAY, blockPos);
					}
				}
				return InteractionResult.SUCCESS;
			}
		}

		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult) {
		if (blockState.getValue(HAS_RECORD)) {
			this.dropRecording(level, blockPos, player);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	private void dropRecording(Level level, BlockPos blockPos, @Nullable Player player) {
		if (!level.isClientSide) {
			BlockEntity blockEntity = level.getBlockEntity(blockPos);
			if (blockEntity instanceof StageJukeboxBlockEntity jukebox) {
				ItemStack record = jukebox.getRecord();
				if (!record.isEmpty()) {
					jukebox.stopSong();
					jukebox.setRecord(ItemStack.EMPTY);
					level.setBlock(blockPos, level.getBlockState(blockPos).setValue(HAS_RECORD, false), 3);
					level.gameEvent(null, GameEvent.JUKEBOX_STOP_PLAY, blockPos);

					if (player != null && !player.getAbilities().instabuild) {
						if (!player.getInventory().add(record)) {
							Containers.dropItemStack(level, blockPos.getX() + 0.5, blockPos.getY() + 0.9, blockPos.getZ() + 0.5, record);
						}
					} else if (player == null) {
						Containers.dropItemStack(level, blockPos.getX() + 0.5, blockPos.getY() + 0.9, blockPos.getZ() + 0.5, record);
					}
				}
			}
		}
	}

	@Override
	protected void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState newState, boolean isMoving) {
		if (!blockState.is(newState.getBlock())) {
			this.dropRecording(level, blockPos, null);
			super.onRemove(blockState, level, blockPos, newState, isMoving);
		}
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new StageJukeboxBlockEntity(blockPos, blockState);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
		return level.isClientSide ? null : createTickerHelper(blockEntityType, BeatLampBlockEntities.STAGE_JUKEBOX, StageJukeboxBlockEntity::tick);
	}

	@Override
	protected RenderShape getRenderShape(BlockState blockState) {
		return RenderShape.MODEL;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HAS_RECORD);
	}
}
