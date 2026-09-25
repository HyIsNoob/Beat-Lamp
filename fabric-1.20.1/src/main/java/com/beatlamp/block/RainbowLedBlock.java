package com.beatlamp.block;

import com.beatlamp.item.LampControllerItem;

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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class RainbowLedBlock extends BaseEntityBlock {
	public static final BooleanProperty FRAMELESS = BooleanProperty.create("frameless");

	public RainbowLedBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.getStateDefinition().any().setValue(FRAMELESS, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FRAMELESS);
	}

	@Override
	public RenderShape getRenderShape(BlockState blockState) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new RainbowLedBlockEntity(blockPos, blockState);
	}

	@Override
	public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
		ItemStack itemStack = player.getItemInHand(interactionHand);
		if (itemStack.getItem() instanceof LampControllerItem) {
			if (player.isShiftKeyDown()) {
				toggleGroupFrameless(level, blockPos, blockState);
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
			return InteractionResult.PASS;
		}

		if (player.isShiftKeyDown() && interactionHand == InteractionHand.MAIN_HAND) {
			toggleGroupFrameless(level, blockPos, blockState);
			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		return InteractionResult.PASS;
	}

	private void toggleGroupFrameless(Level level, BlockPos blockPos, BlockState blockState) {
		if (level.isClientSide) return;
		boolean newFrameless = !blockState.getValue(FRAMELESS);
		java.util.List<BlockPos> members = null;
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
