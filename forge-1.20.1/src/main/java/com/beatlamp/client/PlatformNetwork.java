package com.beatlamp.client;

import com.beatlamp.forge.network.ForgeNetwork;

public final class PlatformNetwork {
	private PlatformNetwork() {
	}

	public static void sendToServer(Object message) {
		ForgeNetwork.sendToServer(message);
	}
}
