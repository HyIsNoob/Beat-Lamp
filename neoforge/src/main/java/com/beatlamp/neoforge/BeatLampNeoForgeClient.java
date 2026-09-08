package com.beatlamp.neoforge;

import com.beatlamp.BeatLamp;
import com.beatlamp.BeatLampBlockEntities;
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

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import com.beatlamp.client.gui.BeatLampClientSettingsScreen;

@EventBusSubscriber(modid = BeatLamp.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BeatLampNeoForgeClient {
	public static final KeyMapping OPEN_SETTINGS_KEY = new KeyMapping(
		"key.beatlamp.open_settings",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_O,
		"key.categories.beatlamp"
	);

	@SubscribeEvent
	public static void registerKeys(RegisterKeyMappingsEvent event) {
		event.register(OPEN_SETTINGS_KEY);
	}

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(BeatLampNeoForge.BEAT_LAMP_BE.get(), BeatLampRenderer::new);
		event.registerBlockEntityRenderer(BeatLampNeoForge.STAGE_LIGHT_BE.get(), StageLightRenderer::new);
		event.registerBlockEntityRenderer(BeatLampNeoForge.LASER_PROJECTOR_BE.get(), LaserProjectorRenderer::new);
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		com.beatlamp.client.config.BeatLampClientConfig.load();
		PlatformNetwork.setSender(PacketDistributor::sendToServer);

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

	@EventBusSubscriber(modid = BeatLamp.MOD_ID, value = Dist.CLIENT)
	public static class ClientForgeEvents {
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			JukeboxAudioTracker.clientTick();
			while (OPEN_SETTINGS_KEY.consumeClick()) {
				Minecraft mc = Minecraft.getInstance();
				if (mc.screen == null) {
					mc.setScreen(new BeatLampClientSettingsScreen());
				}
			}
		}

		@SubscribeEvent
		public static void onPlaySound(PlaySoundEvent event) {
			if (event.getSound() != null) {
				JukeboxAudioTracker.onSoundPlayed(event.getSound());
			}
		}

		@SubscribeEvent
		public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
			JukeboxAudioTracker.clear();
		}

		@SubscribeEvent
		public static void onRenderLevelStage(RenderLevelStageEvent event) {
			if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
				Minecraft mc = Minecraft.getInstance();
				Level level = mc.level;
				if (level != null) {
					LampOutlineRenderer.renderOutlines(level, event.getPoseStack(), mc.renderBuffers().bufferSource(), event.getCamera().getPosition());
				}
			}
		}
	}
}
