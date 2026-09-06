package com.beatlamp.neoforge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class NeoForgeBlockEntityTypeHelper {
	@FunctionalInterface
	public interface BlockEntitySupplier<T extends BlockEntity> {
		T create(BlockPos pos, BlockState state);
	}

	private static final Constructor<?> CONSTRUCTOR;
	private static final Class<?> SUPPLIER_CLASS;

	static {
		try {
			Constructor<?> found = null;
			for (Constructor<?> c : BlockEntityType.class.getDeclaredConstructors()) {
				if (c.getParameterCount() == 2 && Set.class.isAssignableFrom(c.getParameterTypes()[1])) {
					found = c;
					break;
				}
			}
			if (found == null) {
				throw new IllegalStateException("Could not find BlockEntityType(supplier, set) constructor");
			}
			found.setAccessible(true);
			CONSTRUCTOR = found;
			SUPPLIER_CLASS = found.getParameterTypes()[0];
		} catch (Exception e) {
			throw new RuntimeException("Failed to inspect BlockEntityType constructor", e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends BlockEntity> BlockEntityType<T> create(
		BlockEntitySupplier<T> factory,
		Block... blocks
	) {
		try {
			Object proxy = Proxy.newProxyInstance(
				SUPPLIER_CLASS.getClassLoader(),
				new Class<?>[]{ SUPPLIER_CLASS },
				(p, method, args) -> {
					if (args != null && args.length == 2 && args[0] instanceof BlockPos pos && args[1] instanceof BlockState state) {
						return factory.create(pos, state);
					}
					if ("toString".equals(method.getName())) {
						return "BlockEntitySupplierProxy";
					}
					if ("hashCode".equals(method.getName())) {
						return System.identityHashCode(p);
					}
					if ("equals".equals(method.getName())) {
						return p == args[0];
					}
					return null;
				}
			);
			return (BlockEntityType<T>) CONSTRUCTOR.newInstance(proxy, Set.of(blocks));
		} catch (Exception e) {
			throw new RuntimeException("Failed to create BlockEntityType", e);
		}
	}
}
