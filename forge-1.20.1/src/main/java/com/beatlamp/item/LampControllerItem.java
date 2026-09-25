package com.beatlamp.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.beatlamp.BeatLamp;
import com.beatlamp.BeatLampTags;
import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.entity.BlockEntity;

public class LampControllerItem extends Item {
	public LampControllerItem(Properties properties) {
		super(properties);
	}

	public static boolean isSelectableAudioSource(Level level, BlockPos pos) {
		if (level == null || pos == null) {
			return false;
		}
		// BeatLamp stage fixtures are targets, not external music sources
		if (level.getBlockEntity(pos) instanceof com.beatlamp.block.BeatLampBlockEntity
			|| level.getBlockEntity(pos) instanceof com.beatlamp.block.StageLightBlockEntity
			|| level.getBlockEntity(pos) instanceof com.beatlamp.block.FountainBlockEntity
			|| level.getBlockEntity(pos) instanceof com.beatlamp.block.LaserProjectorBlockEntity
			|| level.getBlockEntity(pos) instanceof com.beatlamp.block.FogGeneratorBlockEntity
			|| level.getBlockEntity(pos) instanceof com.beatlamp.block.BeatEmitterBlockEntity
			|| level.getBlockEntity(pos) instanceof com.beatlamp.block.RainbowLedBlockEntity) {
			return false;
		}

		var state = level.getBlockState(pos);
		if (state.isAir()) {
			return false;
		}

		// Jukeboxes, Music Disc Maker, and any external media/speaker block
		return true;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos clickedPos = context.getClickedPos();
		Player player = context.getPlayer();
		ItemStack held = context.getItemInHand();

		if (player == null) {
			return InteractionResult.PASS;
		}

		// 1. Sneak interaction: Jukebox/TV Source Select, Binding or Unbinding
		if (player.isShiftKeyDown()) {
			if (isSelectableAudioSource(level, clickedPos)) {
				if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
					BeatLamp.handleSourceSelect(serverPlayer, clickedPos, held);
				}
				return InteractionResult.sidedSuccess(level.isClientSide);
			}

			if (held.hasTag() && held.getTag().contains("SourcePos")) {
				BlockPos source = BlockPos.of(held.getTag().getLong("SourcePos"));
				if (level.getBlockEntity(clickedPos) instanceof BlockEntity) {
					if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
						BeatLamp.bindSource(serverLevel, clickedPos, source, serverPlayer, held);
					}
					return InteractionResult.sidedSuccess(level.isClientSide);
				}
			}

			// If controller has no pending source: Shift+Right-Click on bound device to unbind it
			BlockEntity be = level.getBlockEntity(clickedPos);
			boolean hasSource = false;
			if (be instanceof BeatLampBlockEntity lamp && lamp.getSource() != null) hasSource = true;
			else if (be instanceof StageLightBlockEntity light && light.getSource() != null) hasSource = true;
			else if (be instanceof FountainBlockEntity fountain && fountain.getSource() != null) hasSource = true;
			else if (be instanceof LaserProjectorBlockEntity laser && laser.getSource() != null) hasSource = true;
			else if (be instanceof FogGeneratorBlockEntity fog && fog.getSource() != null) hasSource = true;
			else if (be instanceof BeatEmitterBlockEntity emitter && emitter.getSource() != null) hasSource = true;

			if (hasSource) {
				if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
					BeatLamp.unbindSource(serverLevel, clickedPos, serverPlayer);
				}
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
		}

		BlockEntity be = level.getBlockEntity(clickedPos);
		boolean isDevice = be instanceof BeatLampBlockEntity || be instanceof StageLightBlockEntity
			|| be instanceof FountainBlockEntity || be instanceof LaserProjectorBlockEntity
			|| be instanceof FogGeneratorBlockEntity || be instanceof BeatEmitterBlockEntity
			|| be instanceof com.beatlamp.block.RainbowLedBlockEntity
			|| be instanceof com.beatlamp.block.StageJukeboxBlockEntity;

		// 2. Normal Right-Click with pending source: also binds
		if (held.hasTag() && held.getTag().contains("SourcePos") && isDevice) {
			BlockPos source = BlockPos.of(held.getTag().getLong("SourcePos"));
			if (!level.isClientSide && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
				BeatLamp.bindSource(serverLevel, clickedPos, source, serverPlayer, held);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		// 3. Normal Right-Click without pending source: open Config GUI
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
				} else if (be instanceof com.beatlamp.block.RainbowLedBlockEntity rainbowLed && com.beatlamp.block.RainbowLedBlockEntity.controllerUser != null) {
					com.beatlamp.block.RainbowLedBlockEntity.controllerUser.use(rainbowLed);
				} else if (be instanceof com.beatlamp.block.StageJukeboxBlockEntity stageJukebox && com.beatlamp.block.StageJukeboxBlockEntity.controllerUser != null) {
					com.beatlamp.block.StageJukeboxBlockEntity.controllerUser.use(stageJukebox);
				}
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		return InteractionResult.PASS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack itemStack = player.getItemInHand(hand);
		if (itemStack.hasTag() && itemStack.getTag().contains("SourcePos")) {
			if (!level.isClientSide) {
				itemStack.getTag().remove("SourcePos");
				player.displayClientMessage(
					Component.translatable("message.beatlamp.source.cancel").withStyle(ChatFormatting.AQUA), true
				);
			}
			return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
		}
		return InteractionResultHolder.pass(itemStack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.beatlamp.controller.tooltip.summary").withStyle(ChatFormatting.GRAY));

		if (stack.hasTag() && stack.getTag().contains("SourcePos")) {
			BlockPos source = BlockPos.of(stack.getTag().getLong("SourcePos"));
			tooltip.add(Component.translatable("item.beatlamp.controller.source", source.getX(), source.getY(), source.getZ()).withStyle(ChatFormatting.GOLD));
		}
	}
}
