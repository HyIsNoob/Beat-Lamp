package com.beatlamp.client;

import java.util.function.Consumer;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class PlatformNetwork {
	private static Consumer<CustomPacketPayload> SENDER = null;

	private PlatformNetwork() {}

	public static void setSender(Consumer<CustomPacketPayload> sender) {
		SENDER = sender;
	}

	public static void sendToServer(CustomPacketPayload payload) {
		if (SENDER != null) {
			SENDER.accept(payload);
		}
	}
}
