package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.milestone.MilestoneTracker;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;

public final class TilemanIslandHandler {

    private static final int RECHECK_INTERVAL_TICKS = 100;

    private static int tickCounter = 0;

    private TilemanIslandHandler() {}

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register(TilemanIslandHandler::onJoin);
        ClientTickEvents.END_CLIENT_TICK.register(
            TilemanIslandHandler::onEndTick
        );
        ClientReceiveMessageEvents.ALLOW_GAME.register(
            TilemanIslandHandler::onGameMessage
        );
    }

    private static void onJoin(
        ClientPacketListener handler,
        PacketSender sender,
        Minecraft client
    ) {
        if (
            TilemanConfig.getInstance().isEnabled() &&
            HypixelUtil.isConnectedToHypixel(client)
        ) {
            requestLocation(handler);
        }
    }

    private static void onEndTick(Minecraft client) {
        if (
            !TilemanConfig.getInstance().isEnabled() ||
            client.player == null ||
            !HypixelUtil.isConnectedToHypixel(client)
        ) {
            tickCounter = 0;
            return;
        }

        tickCounter++;
        if (tickCounter >= RECHECK_INTERVAL_TICKS) {
            tickCounter = 0;
            ClientPacketListener handler = client.getConnection();
            if (handler != null) {
                requestLocation(handler);
            }
        }
    }

    private static void requestLocation(ClientPacketListener handler) {
        TilemanLog.debug(DebugCategory.ISLAND, "Sending /locraw to check current island");
        handler.send(new ServerboundChatCommandPacket("locraw"));
    }

    private static boolean onGameMessage(Component message, boolean overlay) {
        if (!TilemanConfig.getInstance().isEnabled()) {
            return true;
        }

        String text = message.getString();
        if (!looksLikeLocrawResponse(text)) {
            return true;
        }

        try {
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            String gameType = json.has("gametype")
                ? json.get("gametype").getAsString()
                : null;
            if (!"SKYBLOCK".equalsIgnoreCase(gameType)) {
                return false;
            }

            String islandId = json.has("mode")
                ? json.get("mode").getAsString()
                : null;
            String displayName = json.has("map")
                ? json.get("map").getAsString()
                : islandId;
            if (islandId != null) {
                onIslandDetected(islandId, displayName);
            }
        } catch (Exception e) {
            TilemanLog.debug(DebugCategory.ISLAND, "Failed to parse /locraw response: " + text, e);
        }

        return false;
    }

    private static boolean looksLikeLocrawResponse(String text) {
        return (
            text.startsWith("{") &&
            text.contains("\"server\"") &&
            text.contains("\"gametype\"")
        );
    }

    private static void onIslandDetected(String islandId, String displayName) {
        TilemanState state = TilemanState.getInstance();

        TilemanLog.debug(DebugCategory.ISLAND,
            "Island detected: {} (current: {}, blocks: {})",
            islandId,
            state.getActiveIsland(),
            state.getUnlockedBlocks().size()
        );

        boolean islandChanged = !islandId.equals(state.getActiveIsland());
        boolean isNewIsland = state.getUnlockedBlocks(state.getActiveProfileId(), islandId).isEmpty();
        state.setActiveIsland(islandId);

        if (islandChanged) {
            TilemanLog.debug(DebugCategory.ISLAND,
                "Tileman detected island change: {} ({})",
                islandId,
                displayName
            );
            TilemanChat.info("Now on island: " + displayName);
            
            if (isNewIsland) {
                MilestoneTracker.getInstance().onIslandVisited();
            }
        }

        if (state.getUnlockedBlocks().isEmpty() && !TilemanFirstBlockMode.isActive()) {
            TilemanFirstBlockMode.activate(displayName);
        }
    }
}
