package com.azur.skyblocktileman.client.tileman;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class TilemanShopTicker {

    private static long lastTickTime = 0;

    private TilemanShopTicker() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TilemanShopTicker::onEndTick);
    }

    private static void onEndTick(Minecraft client) {
        if (!TilemanConfig.getInstance().isEnabled()) {
            return;
        }

        if (client.player == null || client.level == null) {
            lastTickTime = 0;
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (lastTickTime == 0) {
            lastTickTime = currentTime;
            return;
        }

        long deltaMs = currentTime - lastTickTime;
        lastTickTime = currentTime;

        if (deltaMs > 0 && deltaMs < 1000) {
            TilemanState.getInstance().tickShop(deltaMs);
        }
    }
}
