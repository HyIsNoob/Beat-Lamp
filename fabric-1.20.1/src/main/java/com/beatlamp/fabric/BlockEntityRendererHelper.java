package com.beatlamp.fabric;

import java.lang.reflect.Method;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class BlockEntityRendererHelper {
	private BlockEntityRendererHelper() {
	}

	public static <T extends BlockEntity> void register(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> provider) {
		try {
			Method m = BlockEntityRenderers.class.getDeclaredMethod("register", BlockEntityType.class, BlockEntityRendererProvider.class);
			m.setAccessible(true);
			m.invoke(null, type, provider);
		} catch (Exception e) {
			throw new RuntimeException("Failed to register BlockEntityRenderer", e);
		}
	}
}
