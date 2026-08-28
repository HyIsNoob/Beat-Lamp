package com.beatlamp.neoforge;

import com.beatlamp.BeatLamp;
import com.beatlamp.BeatLampBlockEntities;
import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;
import com.beatlamp.client.audio.JukeboxAudioTracker;
import com.beatlamp.client.gui.FogGeneratorConfigScreen;
import com.beatlamp.client.gui.FountainConfigScreen;
import com.beatlamp.client.gui.LampConfigScreen;
import com.beatlamp.client.gui.LaserProjectorConfigScreen;
import com.beatlamp.client.gui.StageLightConfigScreen;
import com.beatlamp.client.render.BeatLampRenderer;
import com.beatlamp.client.render.LaserProjectorRenderer;
import com.beatlamp.client.render.StageLightRenderer;

import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = BeatLamp.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BeatLampNeoForgeClient {

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(BeatLampBlockEntities.BEAT_LAMP, BeatLampRenderer::new);
		event.registerBlockEntityRenderer(BeatLampBlockEntities.STAGE_LIGHT, StageLightRenderer::new);
		event.registerBlockEntityRenderer(BeatLampBlockEntities.LASER_PROJECTOR, LaserProjectorRenderer::new);
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		BeatLampBlockEntity.controllerUser = beatLamp -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.execute(() -> {
				if (minecraft.screen == null && minecraft.player != null) {
					minecraft.setScreen(new LampConfigScreen(beatLamp));
				}
			});
		};

		StageLightBlockEntity.controllerUser = light -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.execute(() -> {
				if (minecraft.screen == null && minecraft.player != null) {
					minecraft.setScreen(new StageLightConfigScreen(light));
				}
			});
		};

		FountainBlockEntity.controllerUser = fountain -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.execute(() -> {
				if (minecraft.screen == null && minecraft.player != null) {
					minecraft.setScreen(new FountainConfigScreen(fountain));
				}
			});
		};

		LaserProjectorBlockEntity.controllerUser = laser -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.execute(() -> {
				if (minecraft.screen == null && minecraft.player != null) {
					minecraft.setScreen(new LaserProjectorConfigScreen(laser));
				}
			});
		};

		FogGeneratorBlockEntity.controllerUser = fog -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.execute(() -> {
				if (minecraft.screen == null && minecraft.player != null) {
					minecraft.setScreen(new FogGeneratorConfigScreen(fog));
				}
			});
		};
	}

	@EventBusSubscriber(modid = BeatLamp.MOD_ID, value = Dist.CLIENT)
	public static class ClientForgeEvents {
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			JukeboxAudioTracker.clientTick();
		}
	}
}
