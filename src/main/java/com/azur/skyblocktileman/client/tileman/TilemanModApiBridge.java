package com.azur.skyblocktileman.client.tileman;

import net.fabricmc.loader.api.FabricLoader;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.client.Minecraft;

// Optional bridge to the official Hypixel mod-api; when present, island detection is fed by location pushes.
public final class TilemanModApiBridge {

    private static boolean present;

    private static volatile String lastServerName = "";
    private static volatile String lastServerType = "";
    private static volatile String lastLobbyName = "";
    private static volatile String lastMode = "";
    private static volatile String lastMap = "";

    private TilemanModApiBridge() {}

    public static void register() {
        present = FabricLoader.getInstance().isModLoaded("hypixel-mod-api");
        if (!present) {
            TilemanLog.debug(
                DebugCategory.ISLAND,
                "Hypixel mod-api not installed, island detection stays on locraw"
            );
            return;
        }
        try {
            Handlers.subscribe();
        } catch (LinkageError | RuntimeException e) {
            // an installed but incompatible mod-api must never take the game down
            present = false;
            TilemanLog.debug(
                DebugCategory.ISLAND,
                "Hypixel mod-api present but unusable: {}",
                e
            );
        }
    }

    public static boolean isPresent() {
        return present && TilemanConfig.getInstance().isModApiEnabled();
    }

    public static String lastServerName() {
        return lastServerName;
    }

    public static String lastServerType() {
        return lastServerType;
    }

    public static String lastLobbyName() {
        return lastLobbyName;
    }

    public static String lastMode() {
        return lastMode;
    }

    public static String lastMap() {
        return lastMap;
    }

    // separate class so mod-api types are only resolved when the mod is actually loaded
    private static final class Handlers {

        private Handlers() {}

        static void subscribe() {
            HypixelModAPI api = HypixelModAPI.getInstance();
            api.subscribeToEventPacket(ClientboundLocationPacket.class);
            api.createHandler(
                ClientboundLocationPacket.class,
                Handlers::onLocation
            );
        }

        private static void onLocation(ClientboundLocationPacket packet) {
            String serverName = packet.getServerName();
            String serverType = packet
                .getServerType()
                .map(Object::toString)
                .orElse("");
            String lobbyName = packet.getLobbyName().orElse("");
            String mode = packet.getMode().orElse("");
            String map = packet.getMap().orElse("");
            Minecraft.getInstance().execute(() ->
                remember(serverName, serverType, lobbyName, mode, map)
            );
        }
    }

    private static void remember(
        String serverName,
        String serverType,
        String lobbyName,
        String mode,
        String map
    ) {
        if (!isPresent()) {
            return;
        }
        lastServerName = serverName;
        lastServerType = serverType;
        lastLobbyName = lobbyName;
        lastMode = mode;
        lastMap = map;
        TilemanLog.debug(
            DebugCategory.ISLAND,
            "ModAPI location: type={} mode={} map={} server={} lobby={} (locraw island is {})",
            serverType,
            mode,
            map,
            serverName,
            lobbyName,
            TilemanState.getInstance().getActiveIsland()
        );
        TilemanIslandHandler.onModApiLocation(serverType, mode, map);
    }
}
