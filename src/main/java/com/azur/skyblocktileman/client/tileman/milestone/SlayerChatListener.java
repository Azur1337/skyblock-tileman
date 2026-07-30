package com.azur.skyblocktileman.client.tileman.milestone;

import com.azur.skyblocktileman.client.tileman.TilemanConfig;
import com.azur.skyblocktileman.client.tileman.TilemanLog;
import com.azur.skyblocktileman.client.tileman.DebugCategory;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

public final class SlayerChatListener {

    private SlayerChatListener() {}

    public static void register() {
        ClientReceiveMessageEvents.GAME.register(SlayerChatListener::onGameMessage);
    }

    private static void onGameMessage(Component message, boolean overlay) {
        if (overlay) return;
        if (!TilemanConfig.getInstance().isEnabled()) return;

        String text = message.getString();
        
        if (text.contains("SLAYER QUEST STARTED!")) {
            TilemanLog.debug(DebugCategory.ALL, "Detected slayer quest start");
            MilestoneTracker.getInstance().onSlayerQuestStart();
        } else if (text.contains("SLAYER QUEST COMPLETE!")) {
            TilemanLog.debug(DebugCategory.ALL, "Detected slayer quest complete");
            MilestoneTracker.getInstance().onSlayerQuestComplete();
        } else if (text.contains("SLAYER QUEST FAILED!")) {
            TilemanLog.debug(DebugCategory.ALL, "Detected slayer quest failed");
            MilestoneTracker.getInstance().onSlayerQuestFailed();
        }
    }
}
