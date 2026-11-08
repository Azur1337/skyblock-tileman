package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.dungeon.DungeonTracker;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class TilemanShopTicker {

    private static final int TICKS_PER_SECOND = 20;

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
            return;
        }

        // frenzy expiry is an absolute timestamp now, so a plain per-tick check suffices
        TilemanState.getInstance().tickShop();

        // Marathon milestone needs time-based evaluation; once per second is plenty
        if (++marathonTickCounter >= TICKS_PER_SECOND) {
            marathonTickCounter = 0;
            MilestoneTracker.getInstance().onTick();
        }

        DungeonTracker.getInstance().tickAutoSync(client);
    }
}
