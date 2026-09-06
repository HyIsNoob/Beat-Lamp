package com.beatlamp.client;

import com.beatlamp.fabric.network.FabricNetwork;

public final class PlatformNetwork {
	private PlatformNetwork() {
	}

	public static void sendToServer(Object message) {
		FabricNetwork.sendToServer(message);
	}
}
