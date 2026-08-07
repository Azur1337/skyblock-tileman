package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.dungeon.DungeonChatListener;
import com.azur.skyblocktileman.client.tileman.dungeon.DungeonTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class TilemanShopTicker {

    private static long lastTickTime = 0;
    private static long lastDungeonCheckTime = 0;
    
    // Check dungeon API every 2 minutes
    private static final long DUNGEON_CHECK_INTERVAL_MS = 2 * 60 * 1000;

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
            lastDungeonCheckTime = 0;
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (lastTickTime == 0) {
            lastTickTime = currentTime;
            lastDungeonCheckTime = currentTime;
            return;
        }

        long deltaMs = currentTime - lastTickTime;
        lastTickTime = currentTime;

        if (deltaMs > 0 && deltaMs < 1000) {
            TilemanState.getInstance().tickShop(deltaMs);
        }
        
        // Tick dungeon chat listener for delayed warp
        DungeonChatListener.tick();
        
        // Periodic dungeon API check
        if (currentTime - lastDungeonCheckTime >= DUNGEON_CHECK_INTERVAL_MS) {
            lastDungeonCheckTime = currentTime;
            if (HypixelUtil.isConnectedToHypixel(client)) {
                DungeonTracker.getInstance().fetchAndUpdateCompletions();
            }
        }
    }
}
