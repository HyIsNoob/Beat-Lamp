package com.beatlamp.fabric;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

public final class BlockRenderLayerHelper {
	private BlockRenderLayerHelper() {
	}

	public static void setRenderLayer(Block block, RenderType renderType) {
		BlockRenderLayerMap.INSTANCE.putBlock(block, renderType);
	}
}

