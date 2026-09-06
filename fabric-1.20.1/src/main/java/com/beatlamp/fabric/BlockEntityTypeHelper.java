package com.beatlamp.fabric;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockEntityTypeHelper {
	@FunctionalInterface
	public interface Factory<T extends BlockEntity> {
		T create(BlockPos pos, BlockState state);
	}

	@SuppressWarnings("unchecked")
	public static <T extends BlockEntity> BlockEntityType<T> create(Factory<T> factory, Block... blocks) {
		try {
			Class<?> supplierClass = Class.forName("net.minecraft.world.level.block.entity.BlockEntityType$BlockEntitySupplier");
			Object supplierProxy = Proxy.newProxyInstance(
				supplierClass.getClassLoader(),
				new Class<?>[] { supplierClass },
				(proxy, method, args) -> {
					if (args != null && args.length == 2 && args[0] instanceof BlockPos && args[1] instanceof BlockState) {
						return factory.create((BlockPos) args[0], (BlockState) args[1]);
					}
					if ("toString".equals(method.getName())) {
						return "BlockEntitySupplierProxy@" + Integer.toHexString(System.identityHashCode(proxy));
					}
					if ("equals".equals(method.getName())) {
						return proxy == (args != null && args.length == 1 ? args[0] : null);
					}
					if ("hashCode".equals(method.getName())) {
						return System.identityHashCode(proxy);
					}
					return null;
				}
			);

			Constructor<?> ctor = BlockEntityType.class.getDeclaredConstructor(supplierClass, Set.class, com.mojang.datafixers.types.Type.class);
			ctor.setAccessible(true);
			return (BlockEntityType<T>) ctor.newInstance(supplierProxy, Set.of(blocks), null);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create BlockEntityType for Fabric 1.20.1", e);
		}
	}
}
