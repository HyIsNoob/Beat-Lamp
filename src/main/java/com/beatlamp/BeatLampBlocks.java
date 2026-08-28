package com.beatlamp;

import com.beatlamp.block.BeatEmitterBlock;
import com.beatlamp.block.BeatLampBlock;
import com.beatlamp.block.FogGeneratorBlock;
import com.beatlamp.block.FountainBlock;
import com.beatlamp.block.LaserProjectorBlock;
import com.beatlamp.block.StageLightBlock;

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

	public static final StageLightBlock STAGE_LIGHT = new StageLightBlock(
		BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.METAL)
	);

	public static final FountainBlock FOUNTAIN = new FountainBlock(
		BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.STONE)
	);

	public static final LaserProjectorBlock LASER_PROJECTOR = new LaserProjectorBlock(
		BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.METAL)
	);

	public static final FogGeneratorBlock FOG_GENERATOR = new FogGeneratorBlock(
		BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.STONE)
	);

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "beat_lamp"), BEAT_LAMP);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "beat_emitter"), BEAT_EMITTER);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "stage_light"), STAGE_LIGHT);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "fountain"), FOUNTAIN);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "laser_projector"), LASER_PROJECTOR);
		Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "fog_generator"), FOG_GENERATOR);
	}
}
