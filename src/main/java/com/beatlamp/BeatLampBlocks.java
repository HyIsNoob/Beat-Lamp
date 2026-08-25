package com.beatlamp;

import com.beatlamp.block.BeatEmitterBlock;
import com.beatlamp.block.BeatLampBlock;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BeatLampBlocks {
	public static final BeatLampBlock BEAT_LAMP = new BeatLampBlock(
		BlockBehaviour.Properties.of()
			.strength(0.3F)
			.sound(SoundType.GLASS)
			.lightLevel(state -> state.getValue(BeatLampBlock.LIT) ? 15 : 0)
			.noOcclusion()
	);

	public static final BeatEmitterBlock BEAT_EMITTER = new BeatEmitterBlock(BeatEmitterBlock.emitterProperties());

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "beat_lamp"), BEAT_LAMP);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "beat_emitter"), BEAT_EMITTER);
	}
}
