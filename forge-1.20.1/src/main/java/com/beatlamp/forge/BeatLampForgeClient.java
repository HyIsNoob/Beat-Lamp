package com.beatlamp.forge;

import com.beatlamp.BeatLamp;
import com.beatlamp.BeatLampItems;
import com.beatlamp.client.BeatLampClient;
import com.beatlamp.client.DmxMasterTracker;
import com.beatlamp.client.audio.JukeboxAudioTracker;
import com.beatlamp.client.render.BeatLampRenderer;
import com.beatlamp.client.render.LampOutlineRenderer;
import com.beatlamp.client.render.LaserProjectorRenderer;
import com.beatlamp.client.render.StageLightRenderer;
import com.beatlamp.item.StageBlockItem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BeatLamp.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BeatLampForgeClient {

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		BeatLampClient.init();
		MinecraftForge.EVENT_BUS.register(ClientTickEvents.class);
	}

	@SubscribeEvent
	public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(BeatLampForge.BEAT_LAMP_BE.get(), new net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<com.beatlamp.block.BeatLampBlockEntity>() {
			@Override
			public net.minecraft.client.renderer.blockentity.BlockEntityRenderer<com.beatlamp.block.BeatLampBlockEntity> create(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
				return new BeatLampRenderer(context);
			}
		});
		event.registerBlockEntityRenderer(BeatLampForge.STAGE_LIGHT_BE.get(), new net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<com.beatlamp.block.StageLightBlockEntity>() {
			@Override
			public net.minecraft.client.renderer.blockentity.BlockEntityRenderer<com.beatlamp.block.StageLightBlockEntity> create(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
				return new StageLightRenderer(context);
			}
		});
		event.registerBlockEntityRenderer(BeatLampForge.LASER_PROJECTOR_BE.get(), new net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<com.beatlamp.block.LaserProjectorBlockEntity>() {
			@Override
			public net.minecraft.client.renderer.blockentity.BlockEntityRenderer<com.beatlamp.block.LaserProjectorBlockEntity> create(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
				return new LaserProjectorRenderer(context);
			}
		});
	}

	public static class ClientTickEvents {
		@SubscribeEvent
		public static void onClientTick(TickEvent.ClientTickEvent event) {
			if (event.phase == TickEvent.Phase.END) {
				JukeboxAudioTracker.clientTick();
			}
		}

		@SubscribeEvent
		public static void onRenderLevelStage(RenderLevelStageEvent event) {
			if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
				Level level = Minecraft.getInstance().level;
				if (level != null) {
					var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
					LampOutlineRenderer.renderOutlines(level, event.getPoseStack(), bufferSource, Vec3.ZERO);
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
			DmxMasterTracker.clear();
		}

		@SubscribeEvent
		public static void onItemTooltip(ItemTooltipEvent event) {
			var stack = event.getItemStack();
			String baseKey = null;
			if (stack.getItem() instanceof StageBlockItem stageBlockItem) {
				baseKey = stageBlockItem.getBaseKey();
			} else if (stack.is(BeatLampItems.LINKER)) {
				baseKey = "linker";
			} else if (stack.is(BeatLampItems.CONTROLLER)) {
				baseKey = "controller";
			}

			if (baseKey != null) {
				var lines = event.getToolTip();
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
		}
	}
}
