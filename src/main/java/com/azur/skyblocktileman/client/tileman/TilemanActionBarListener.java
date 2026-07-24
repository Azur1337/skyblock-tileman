package com.azur.skyblocktileman.client.tileman;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

// hypixel sends actionbar via system chat overlay, not the dedicated packet
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
        TilemanLog.debug(DebugCategory.ACTION_BAR, "Raw actionbar overlay text: [{}]", text);
        SkillXpParser.parse(text);
    }
}
