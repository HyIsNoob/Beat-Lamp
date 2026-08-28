package com.beatlamp.forge.network;

import com.beatlamp.BeatLamp;
import com.beatlamp.network.EmitterSignalPayload;
import com.beatlamp.network.FogGeneratorConfigurePayload;
import com.beatlamp.network.FountainConfigurePayload;
import com.beatlamp.network.FountainFirePayload;
import com.beatlamp.network.LampConfigurePayload;
import com.beatlamp.network.LampSourcePayload;
import com.beatlamp.network.LaserProjectorConfigurePayload;
import com.beatlamp.network.StageLightConfigurePayload;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ForgeNetwork {
	private static final String PROTOCOL_VERSION = "1";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		ResourceLocation.fromNamespaceAndPath(BeatLamp.MOD_ID, "main"),
		() -> PROTOCOL_VERSION,
		PROTOCOL_VERSION::equals,
		PROTOCOL_VERSION::equals
	);

	private static int packetId = 0;

	public static void register() {
		CHANNEL.registerMessage(
			packetId++,
			LampConfigurePayload.class,
			(msg, buf) -> LampConfigurePayload.CODEC.encode(buf, msg),
			buf -> LampConfigurePayload.CODEC.decode(buf),
			(msg, ctxSupplier) -> {
				var ctx = ctxSupplier.get();
				ctx.enqueueWork(() -> {
					var player = ctx.getSender();
					if (player != null) {
						// Process on server
					}
				});
				ctx.setPacketHandled(true);
			}
		);
	}

	public static void sendToServer(Object message) {
		CHANNEL.sendToServer(message);
	}
}
