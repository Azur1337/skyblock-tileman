package com.azur.skyblocktileman.client.tileman;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

// Logs to console always, mirrors to chat only when debug mode is on
public final class TilemanLog {

    private static final Logger LOGGER = LoggerFactory.getLogger("Tileman");

    private TilemanLog() {}

    public static void debug(String message) {
        LOGGER.info(message);
        mirrorToChat(message);
    }

    public static void debug(String format, Object... args) {
        String formatted = MessageFormatter.arrayFormat(
            format,
            args
        ).getMessage();
        LOGGER.info(formatted);
        mirrorToChat(formatted);
    }

    public static void debug(String message, Throwable throwable) {
        LOGGER.info(message, throwable);
        mirrorToChat(message);
    }

    private static void mirrorToChat(String message) {
        if (TilemanConfig.getInstance().isDebugMode()) {
            TilemanChat.debug(message);
        }
    }
}
