package com.beatlamp.fabric;

import java.lang.reflect.Field;
import java.util.Map;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

public final class BlockRenderLayerHelper {
	private BlockRenderLayerHelper() {
	}

	@SuppressWarnings("unchecked")
	public static void setRenderLayer(Block block, RenderType renderType) {
		try {
			Field field = ItemBlockRenderTypes.class.getDeclaredField("TYPE_BY_BLOCK");
			field.setAccessible(true);
			Map<Block, RenderType> map = (Map<Block, RenderType>) field.get(null);
			map.put(block, renderType);
		} catch (Exception ignored) {
		}
	}
}
