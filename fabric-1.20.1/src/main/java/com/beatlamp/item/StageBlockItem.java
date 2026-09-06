package com.beatlamp.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class StageBlockItem extends BlockItem {
	private final String baseKey;
	private final boolean decorationOnly;

	public StageBlockItem(Block block, Properties properties, String baseKey, boolean decorationOnly) {
		super(block, properties);
		this.baseKey = baseKey;
		this.decorationOnly = decorationOnly;
	}

	public String getBaseKey() {
		return this.baseKey;
	}

	public boolean isDecorationOnly() {
		return this.decorationOnly;
	}

	@Override
	public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag) {
		if (this.decorationOnly) {
			list.add(Component.translatable("item.beatlamp.tag.decoration").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
		}
		list.add(Component.translatable("item.beatlamp." + this.baseKey + ".tooltip.summary").withStyle(ChatFormatting.GRAY));
	}
}
