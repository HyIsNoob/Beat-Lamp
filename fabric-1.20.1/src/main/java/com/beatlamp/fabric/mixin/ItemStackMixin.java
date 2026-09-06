package com.beatlamp.fabric.mixin;

import java.util.List;

import com.beatlamp.BeatLampItems;
import com.beatlamp.item.StageBlockItem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
	@Inject(method = "getTooltipLines", at = @At("RETURN"))
	private void beatlamp$addTooltips(Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir) {
		ItemStack stack = (ItemStack) (Object) this;
		String baseKey = null;
		if (stack.getItem() instanceof StageBlockItem stageBlockItem) {
			baseKey = stageBlockItem.getBaseKey();
		} else if (stack.is(BeatLampItems.LINKER)) {
			baseKey = "linker";
		} else if (stack.is(BeatLampItems.CONTROLLER)) {
			baseKey = "controller";
		}

		if (baseKey != null) {
			List<Component> lines = cir.getReturnValue();
			if (Screen.hasShiftDown()) {
				lines.add(Component.empty());
				lines.add(Component.translatable("item.beatlamp.tag.guide_header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
				String raw = Language.getInstance().getOrDefault("item.beatlamp." + baseKey + ".tooltip.details");
				for (String subLine : raw.split("\n")) {
					if (!subLine.trim().isEmpty()) {
						lines.add(Component.literal(subLine.trim()).withStyle(ChatFormatting.AQUA));
					}
				}
			} else {
				lines.add(Component.translatable("item.beatlamp.tag.hold_shift").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
			}
		}
	}
}
