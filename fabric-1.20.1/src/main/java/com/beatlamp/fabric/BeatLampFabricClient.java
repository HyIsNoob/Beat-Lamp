package com.beatlamp.fabric;

import com.beatlamp.BeatLampBlockEntities;
import com.beatlamp.BeatLampBlocks;
import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.DmxConsoleBlockEntity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;
import com.beatlamp.client.BeatLampClient;
import com.beatlamp.client.config.BeatLampClientConfig;
import com.beatlamp.client.gui.DmxConsoleScreen;
import com.beatlamp.client.gui.EmitterConfigScreen;
import com.beatlamp.client.gui.FogGeneratorConfigScreen;
import com.beatlamp.client.gui.FountainConfigScreen;
import com.beatlamp.client.gui.LampConfigScreen;
import com.beatlamp.client.gui.LaserProjectorConfigScreen;
import com.beatlamp.client.gui.StageLightConfigScreen;
import com.beatlamp.client.render.BeatLampRenderer;
import com.beatlamp.client.render.LaserProjectorRenderer;
import com.beatlamp.client.render.StageLightRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;

public class BeatLampFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BeatLampClientConfig.load();

		BlockRenderLayerHelper.setRenderLayer(BeatLampBlocks.BEAT_LAMP, RenderType.cutout());
		BlockEntityRendererHelper.register(BeatLampBlockEntities.BEAT_LAMP, BeatLampRenderer::new);
		BlockEntityRendererHelper.register(BeatLampBlockEntities.STAGE_LIGHT, StageLightRenderer::new);
		BlockEntityRendererHelper.register(BeatLampBlockEntities.LASER_PROJECTOR, LaserProjectorRenderer::new);

		BeatLampBlockEntity.clientTicker = BeatLampClient::tickLamp;
		BeatEmitterBlockEntity.clientTicker = BeatLampClient::tickEmitter;
		StageLightBlockEntity.clientTicker = BeatLampClient::tickStageLight;
		FountainBlockEntity.clientTicker = BeatLampClient::tickFountain;
		LaserProjectorBlockEntity.clientTicker = BeatLampClient::tickLaserProjector;
		FogGeneratorBlockEntity.clientTicker = BeatLampClient::tickFogGenerator;

		BeatLampBlockEntity.controllerUser = beatLamp -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.execute(() -> {
				if (minecraft.screen == null && minecraft.player != null) {
					minecraft.setScreen(new LampConfigScreen(beatLamp));
				}
			});
		};
		BeatEmitterBlockEntity.controllerUser = emitter -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.execute(() -> {
				if (minecraft.screen == null && minecraft.player != null) {
					minecraft.setScreen(new EmitterConfigScreen(emitter));
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
		DmxConsoleBlockEntity.controllerUser = dmx -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.execute(() -> {
				if (minecraft.screen == null && minecraft.player != null) {
					minecraft.setScreen(new DmxConsoleScreen(dmx));
				}
			});
		};
	}
}
