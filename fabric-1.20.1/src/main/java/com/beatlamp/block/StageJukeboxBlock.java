package com.beatlamp.block;

import com.beatlamp.BeatLampBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
					jukebox.ejectRecord(player);
				}
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		ItemStack itemstack = player.getItemInHand(interactionHand);
		boolean isPlayable = itemstack.getItem() instanceof RecordItem;
		boolean isCustomDisc = !isPlayable && isMusicDisc(itemstack);

		if (isPlayable || isCustomDisc) {
			if (!level.isClientSide) {
				BlockEntity blockEntity = level.getBlockEntity(blockPos);
				if (blockEntity instanceof StageJukeboxBlockEntity jukebox) {
					ItemStack toInsert = itemstack.copy();
					toInsert.setCount(1);
					if (!player.getAbilities().instabuild) {
						itemstack.shrink(1);
					}
					jukebox.setRecord(toInsert);
					level.setBlock(blockPos, blockState.setValue(HAS_RECORD, true), Block.UPDATE_ALL);
					jukebox.playRecord();
					level.gameEvent(player, GameEvent.JUKEBOX_PLAY, blockPos);
				}
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		return InteractionResult.PASS;
	}

	private static boolean isMusicDisc(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return false;
		String name = stack.getItem().getClass().getName();
		if (name.contains("MusicDisc") || name.contains("musicdiscmaker")) return true;
		ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return loc != null && ("music_disc_maker".equals(loc.getNamespace()) || loc.getPath().contains("disc"));
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

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
		return createTickerHelper(blockEntityType, BeatLampBlockEntities.STAGE_JUKEBOX, StageJukeboxBlockEntity::tick);
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
