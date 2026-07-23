package com.azur.skyblocktileman.client.tileman;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class HypixelUtil {

    private HypixelUtil() {}

    public static boolean isConnectedToHypixel(Minecraft client) {
        ServerData server = client.getCurrentServer();
        return (
            server != null &&
            server.ip != null &&
            server.ip.toLowerCase(Locale.ROOT).contains("hypixel.net")
        );
    }
}
