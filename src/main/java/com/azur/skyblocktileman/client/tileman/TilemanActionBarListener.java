package com.azur.skyblocktileman.client.tileman;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

// Hypixel sends the health/mana/xp line as a system chat message with the overlay
// flag set, not the modern dedicated action bar packet. This catches that route.
public final class TilemanActionBarListener {

    private TilemanActionBarListener() {}

    public static void register() {
        ClientReceiveMessageEvents.GAME.register(TilemanActionBarListener::onGameMessage);
    }

    private static void onGameMessage(Component message, boolean overlay) {
        if (!overlay) {
            return;
        }

        String text = message.getString();
        TilemanLog.debug("Raw actionbar overlay text: [{}]", text);
        SkillXpParser.parse(text);
    }
}
