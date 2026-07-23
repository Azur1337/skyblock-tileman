package com.azur.skyblocktileman.client.tileman;

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

// Detects the current island via /locraw and autounlocks the spawn block on a new island
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
        TilemanLog.debug("Sending /locraw to check current island");
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
            TilemanLog.debug("Failed to parse /locraw response: " + text, e);
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

        if (islandId.equals(state.getActiveIsland())) {
            return;
        }

        state.setActiveIsland(islandId);
        TilemanLog.debug(
            "Tileman detected island change: {} ({})",
            islandId,
            displayName
        );
        TilemanChat.info("Now on island: " + displayName);

        if (!state.getUnlockedBlocks().isEmpty()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        BlockPos spawnBlock = client.player.blockPosition().below();
        boolean added = state.unlockBlock(spawnBlock);
        if (added) {
            TilemanLog.debug(
                "Auto-unlocked first free block on {} at {}",
                islandId,
                spawnBlock
            );
            TilemanChat.info(
                "Auto-unlocked your spawn block on " + displayName + "!"
            );
        }
    }
}
