package com.azur.skyblocktileman.client;

import com.azur.skyblocktileman.client.tileman.KeybindSync;
import com.azur.skyblocktileman.client.tileman.MilestonePopup;
import com.azur.skyblocktileman.client.tileman.TilemanActionBarListener;
import com.azur.skyblocktileman.client.tileman.TilemanCommands;
import com.azur.skyblocktileman.client.tileman.TilemanCompassRenderer;
import com.azur.skyblocktileman.client.tileman.TilemanConfig;
import com.azur.skyblocktileman.client.tileman.TilemanFirstBlockMode;
import com.azur.skyblocktileman.client.tileman.TilemanHealthHandler;
import com.azur.skyblocktileman.client.tileman.TilemanHudOverlay;
import com.azur.skyblocktileman.client.tileman.TilemanIslandHandler;
import com.azur.skyblocktileman.client.tileman.TilemanLoginHandler;
import com.azur.skyblocktileman.client.tileman.TilemanModApiBridge;
import com.azur.skyblocktileman.client.tileman.TilemanProfileDetector;
import com.azur.skyblocktileman.client.tileman.TilemanPunishmentHandler;
import com.azur.skyblocktileman.client.tileman.TilemanRenderer;
import com.azur.skyblocktileman.client.tileman.TilemanSelectionMode;
import com.azur.skyblocktileman.client.tileman.TilemanShopTicker;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import com.azur.skyblocktileman.client.tileman.XpReconciler;
import com.azur.skyblocktileman.client.tileman.dungeon.DungeonTooltipHandler;
import com.azur.skyblocktileman.client.tileman.slayer.SlayerFeature;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class SkyblockTilemanClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TilemanState.getInstance();
        TilemanConfig.getInstance();

        TilemanLoginHandler.register();
        TilemanProfileDetector.register();
        TilemanIslandHandler.register();
        TilemanModApiBridge.register();
        TilemanActionBarListener.register();
        TilemanCommands.register();
        TilemanRenderer.register();
        TilemanSelectionMode.register();
        KeybindSync.register();
        TilemanFirstBlockMode.register();
        TilemanPunishmentHandler.register();
        TilemanHealthHandler.register();
        TilemanHudOverlay.register();
        MilestonePopup.register();
        TilemanShopTicker.register();
        TilemanCompassRenderer.register();
        DungeonTooltipHandler.register();
        SlayerFeature.register();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            TilemanState.getInstance().save();
            XpReconciler.getInstance().shutdown();
        });
    }
}
