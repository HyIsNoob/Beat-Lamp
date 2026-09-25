package com.beatlamp.block;

import java.util.List;

import com.beatlamp.item.LampControllerItem;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class RainbowLedBlock extends BaseEntityBlock {
	public static final BooleanProperty FRAMELESS = BooleanProperty.create("frameless");
	public static final MapCodec<RainbowLedBlock> CODEC = simpleCodec(RainbowLedBlock::new);

	public RainbowLedBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FRAMELESS, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FRAMELESS);
	}

	@Override
	protected RenderShape getRenderShape(BlockState blockState) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new RainbowLedBlockEntity(blockPos, blockState);
	}

	@Override
	protected InteractionResult useItemOn(
		ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
	) {
		if (itemStack.getItem() instanceof LampControllerItem) {
			if (player.isShiftKeyDown()) {
				toggleGroupFrameless(level, blockPos, blockState);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		if (player.isShiftKeyDown()) {
			toggleGroupFrameless(level, blockPos, blockState);
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult) {
		if (player.isShiftKeyDown()) {
			toggleGroupFrameless(level, blockPos, blockState);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	private void toggleGroupFrameless(Level level, BlockPos blockPos, BlockState blockState) {
		if (level.isClientSide) return;
		boolean newFrameless = !blockState.getValue(FRAMELESS);
		List<BlockPos> members = null;
		if (level.getBlockEntity(blockPos) instanceof RainbowLedBlockEntity led && led.getManualGroup().size() >= 2) {
			members = led.getManualGroup();
		} else {
			members = com.beatlamp.BeatLamp.floodFillRainbowLed(level, blockPos);
		}
		for (BlockPos member : members) {
			BlockState state = level.getBlockState(member);
			if (state.hasProperty(FRAMELESS)) {
				level.setBlock(member, state.setValue(FRAMELESS, newFrameless), 3);
			}
			if (level.getBlockEntity(member) instanceof RainbowLedBlockEntity l) {
				l.setFrameless(newFrameless);
			}
		}
	}
}
