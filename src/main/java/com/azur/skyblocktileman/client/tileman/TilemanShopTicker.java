package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.dungeon.DungeonTracker;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class TilemanShopTicker {

    private static long lastTickTime = 0;
    private static int marathonTickCounter = 0;

    private TilemanShopTicker() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TilemanShopTicker::onEndTick);
    }

    private static void onEndTick(Minecraft client) {
        if (!TilemanState.getInstance().isTilemanActive()) {
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

        // Marathon milestone needs time-based evaluation; once per second is plenty
        if (++marathonTickCounter >= 20) {
            marathonTickCounter = 0;
            MilestoneTracker.getInstance().onTick();
        }

        DungeonTracker.getInstance().tickAutoSync(client);
    }
}
