package com.beatlamp.forge;

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

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = BeatLamp.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BeatLampForgeClient {

	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(BeatLampForge.BEAT_LAMP_BE.get(), BeatLampRenderer::new);
		event.registerBlockEntityRenderer(BeatLampForge.STAGE_LIGHT_BE.get(), StageLightRenderer::new);
		event.registerBlockEntityRenderer(BeatLampForge.LASER_PROJECTOR_BE.get(), LaserProjectorRenderer::new);
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		BeatLampClientConfig.load();
		PlatformNetwork.setSender(payload -> BeatLampForge.CHANNEL.send(payload, PacketDistributor.SERVER.noArg()));

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

	@Mod.EventBusSubscriber(modid = BeatLamp.MOD_ID, value = Dist.CLIENT)
	public static class ClientForgeEvents {
		@SubscribeEvent
		public static void onClientTick(TickEvent.ClientTickEvent event) {
			if (event.phase == TickEvent.Phase.END) {
				JukeboxAudioTracker.clientTick();
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
					com.mojang.blaze3d.vertex.PoseStack poseStack = new com.mojang.blaze3d.vertex.PoseStack();
					poseStack.last().pose().set(event.getPoseStack());
					LampOutlineRenderer.renderOutlines(level, poseStack, mc.renderBuffers().bufferSource(), net.minecraft.world.phys.Vec3.ZERO);
				}
			}
		}
	}
}
