package com.azur.skyblocktileman.client.tileman;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

// Local chat messages only, never sent to the server
public final class TilemanChat {

    private static final String PREFIX = "[Tileman] ";

    private TilemanChat() {}

    public static void info(String message) {
        send(message, ChatFormatting.GOLD);
    }

    public static void warn(String message) {
        send(message, ChatFormatting.RED);
    }

    public static void debug(String message) {
        send(message, ChatFormatting.GRAY);
    }

    private static void send(String message, ChatFormatting color) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        MutableComponent text = Component.literal(PREFIX + message).withStyle(
            color
        );
        client.player.sendSystemMessage(text);
    }
}
