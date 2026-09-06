package com.beatlamp.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class StageBlockItem extends BlockItem {
	private final String baseKey;
	private final boolean isDecoration;

	public StageBlockItem(Block block, Properties properties, String baseKey, boolean isDecoration) {
		super(block, properties);
		this.baseKey = baseKey;
		this.isDecoration = isDecoration;
	}

	public String getBaseKey() {
		return this.baseKey;
	}

	public boolean isDecoration() {
		return this.isDecoration;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		if (this.isDecoration) {
			tooltip.add(Component.translatable("item.beatlamp.tag.decoration").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
		}
		tooltip.add(Component.translatable("item.beatlamp." + this.baseKey + ".tooltip.summary").withStyle(ChatFormatting.GRAY));
	}
}
