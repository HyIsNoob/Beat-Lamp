package com.beatlamp.fabric;

import com.beatlamp.BeatLampBlocks;
import com.beatlamp.BeatLampBlockEntities;
import com.beatlamp.BeatLampItems;
import com.beatlamp.block.BeatEmitterBlockEntity;
import com.beatlamp.block.BeatLampBlockEntity;
import com.beatlamp.block.DmxConsoleBlockEntity;
import com.beatlamp.block.FogGeneratorBlockEntity;
import com.beatlamp.block.FountainBlockEntity;
import com.beatlamp.block.LaserProjectorBlockEntity;
import com.beatlamp.block.StageLightBlockEntity;
import com.beatlamp.client.BeatLampClient;
import com.beatlamp.client.PlatformNetwork;
import com.beatlamp.client.audio.JukeboxAudioTracker;
import com.beatlamp.client.config.BeatLampClientConfig;
import com.beatlamp.client.gui.DmxConsoleScreen;
import com.beatlamp.client.gui.EmitterConfigScreen;
import com.beatlamp.client.gui.FogGeneratorConfigScreen;
import com.beatlamp.client.gui.FountainConfigScreen;
import com.beatlamp.client.gui.LampConfigScreen;
import com.beatlamp.client.gui.LaserProjectorConfigScreen;
import com.beatlamp.client.gui.StageLightConfigScreen;
import com.beatlamp.client.render.BeatLampRenderer;
import com.beatlamp.client.render.LampOutlineRenderer;
import com.beatlamp.client.render.LaserProjectorRenderer;
import com.beatlamp.client.render.StageLightRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

public class BeatLampFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PlatformNetwork.setSender(ClientPlayNetworking::send);
		BeatLampClientConfig.load();

		BlockRenderLayerMap.INSTANCE.putBlock(BeatLampBlocks.BEAT_LAMP, RenderType.cutout());
		BlockEntityRenderers.register(BeatLampBlockEntities.BEAT_LAMP, BeatLampRenderer::new);
		BlockEntityRenderers.register(BeatLampBlockEntities.STAGE_LIGHT, StageLightRenderer::new);
		BlockEntityRenderers.register(BeatLampBlockEntities.LASER_PROJECTOR, LaserProjectorRenderer::new);

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

		WorldRenderEvents.AFTER_TRANSLUCENT.register(LampOutlineRenderer::render);

		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			String baseKey = null;
			if (stack.getItem() instanceof com.beatlamp.item.StageBlockItem stageBlockItem) {
				baseKey = stageBlockItem.getBaseKey();
			} else if (stack.is(BeatLampItems.LINKER)) {
				baseKey = "linker";
			} else if (stack.is(BeatLampItems.CONTROLLER)) {
				baseKey = "controller";
			}

			if (baseKey != null) {
				if (Screen.hasShiftDown()) {
					lines.add(Component.empty());
					lines.add(Component.translatable("item.beatlamp.tag.guide_header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
					String raw = Language.getInstance().getOrDefault("item.beatlamp." + baseKey + ".tooltip.details");
					for (String subLine : raw.split("\n")) {
						if (!subLine.trim().isEmpty()) {
							lines.add(Component.literal(subLine.trim()).withStyle(ChatFormatting.AQUA));
						}
					}
				} else {
					lines.add(Component.translatable("item.beatlamp.tag.hold_shift").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
				}
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> JukeboxAudioTracker.clientTick());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> JukeboxAudioTracker.clear());
	}
}
