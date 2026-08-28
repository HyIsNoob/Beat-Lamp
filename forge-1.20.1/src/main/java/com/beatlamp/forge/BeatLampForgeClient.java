package com.beatlamp.forge;

import com.beatlamp.BeatLamp;
import com.beatlamp.BeatLampBlockEntities;
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

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BeatLamp.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BeatLampForgeClient {

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

	@Mod.EventBusSubscriber(modid = BeatLamp.MOD_ID, value = Dist.CLIENT)
	public static class ClientTickEvents {
		@SubscribeEvent
		public static void onClientTick(TickEvent.ClientTickEvent event) {
			if (event.phase == TickEvent.Phase.END) {
				JukeboxAudioTracker.clientTick();
			}
		}
	}
}
