package com.azur.skyblocktileman.client.tileman;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class TilemanChat {

    private static final String PREFIX = "[Tileman] ";

    private TilemanChat() {}

    public static void info(String message) {
        send(message, ChatFormatting.GOLD);
    }

    public static void info(MutableComponent message) {
        sendRaw(
            Component.literal(PREFIX)
                .withStyle(ChatFormatting.GOLD)
                .append(message)
        );
    }

    public static void warn(String message) {
        send(message, ChatFormatting.RED);
    }

    public static void debug(String message) {
        send(message, ChatFormatting.GRAY);
    }

    // purchase feedback: green sentence, red item name
    public static void bought(String itemName) {
        MutableComponent text = Component.literal(PREFIX)
            .append(
                Component.literal("You bought ").withStyle(ChatFormatting.GREEN)
            )
            .append(Component.literal(itemName).withStyle(ChatFormatting.RED))
            .append(Component.literal("!").withStyle(ChatFormatting.GREEN));
        sendRaw(text);
    }

    private static void send(String message, ChatFormatting color) {
        sendRaw(Component.literal(PREFIX + message).withStyle(color));
    }

    private static void sendRaw(MutableComponent text) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        client.player.sendSystemMessage(text);
    }
}
