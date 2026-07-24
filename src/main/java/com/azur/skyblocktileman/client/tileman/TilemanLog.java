package com.azur.skyblocktileman.client.tileman;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

public final class TilemanLog {

    private static final Logger LOGGER = LoggerFactory.getLogger("Tileman");

    private TilemanLog() {}

    public static void debug(DebugCategory category, String message) {
        LOGGER.info("[{}] {}", category.getDisplayName(), message);
        mirrorToChat(category, message);
    }

    public static void debug(DebugCategory category, String format, Object... args) {
        String formatted = MessageFormatter.arrayFormat(format, args).getMessage();
        LOGGER.info("[{}] {}", category.getDisplayName(), formatted);
        mirrorToChat(category, formatted);
    }

    public static void debug(DebugCategory category, String message, Throwable throwable) {
        LOGGER.info("[{}] {}", category.getDisplayName(), message, throwable);
        mirrorToChat(category, message);
    }

    public static void debug(String message) {
        debug(DebugCategory.ALL, message);
    }

    public static void debug(String format, Object... args) {
        debug(DebugCategory.ALL, format, args);
    }

    public static void debug(String message, Throwable throwable) {
        debug(DebugCategory.ALL, message, throwable);
    }

    private static void mirrorToChat(DebugCategory category, String message) {
        if (TilemanConfig.getInstance().isDebugEnabled(category)) {
            TilemanChat.debug("[" + category.getDisplayName() + "] " + message);
        }
    }
}
