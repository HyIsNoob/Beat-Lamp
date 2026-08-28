package com.beatlamp.forge;

import com.beatlamp.BeatLamp;
import com.beatlamp.BeatLampBlockEntities;
import com.beatlamp.BeatLampBlocks;
import com.beatlamp.BeatLampItems;
import com.beatlamp.forge.network.ForgeNetwork;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(BeatLamp.MOD_ID)
public class BeatLampForge {
	public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BeatLamp.MOD_ID);

	public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
		.title(Component.translatable("itemGroup.beatlamp"))
		.icon(() -> new ItemStack(BeatLampItems.CONTROLLER))
		.displayItems((parameters, output) -> {
			output.accept(BeatLampItems.BEAT_LAMP);
			output.accept(BeatLampItems.STAGE_LIGHT);
			output.accept(BeatLampItems.LASER_PROJECTOR);
			output.accept(BeatLampItems.FOUNTAIN);
			output.accept(BeatLampItems.FOG_GENERATOR);
			output.accept(BeatLampItems.BEAT_EMITTER);
			output.accept(BeatLampItems.LINKER);
			output.accept(BeatLampItems.CONTROLLER);
		})
		.build()
	);

	public BeatLampForge() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		BeatLamp.LOGGER.info("Beat Lamp Forge initializing (1.20.1)");

		BeatLampBlocks.register();
		BeatLampBlockEntities.register();
		BeatLampItems.register();
		CREATIVE_TABS.register(modEventBus);

		ForgeNetwork.register();
		MinecraftForge.EVENT_BUS.register(this);
	}
}
