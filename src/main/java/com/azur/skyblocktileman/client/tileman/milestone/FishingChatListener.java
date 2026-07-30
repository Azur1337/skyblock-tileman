package com.azur.skyblocktileman.client.tileman.milestone;

import com.azur.skyblocktileman.client.tileman.TilemanConfig;
import com.azur.skyblocktileman.client.tileman.TilemanLog;
import com.azur.skyblocktileman.client.tileman.DebugCategory;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class FishingChatListener {
    
    private FishingChatListener() {}
    
    // all sea creature spawn messages from hypixel wiki
    private static final List<String> SEA_CREATURE_MESSAGES = List.of(
        // basic
        "A Squid appeared.",
        "You caught a Sea Walker.",
        "You stumbled upon a Sea Guardian.",
        "It looks like you've disrupted the Sea Witch's brewing session. She's furious!",
        "You reeled in a Sea Archer.",
        "The Monster of the Deep has emerged.",
        "Huh? A Catfish!",
        "Is this even a fish? It's a Carrot King!",
        "A Sea Leech has latched onto your hook!",
        "The waters are getting deeper...",
        
        // spooky
        "Phew! It's just a Scarecrow.",
        "You hear trotting from beneath the waves, you caught a Nightmare.",
        "It must be a full moon, a Werewolf appeared.",
        "The spirit of a Phantom Fisher has been disturbed!",
        "This water seems to be... Grim.",
        
        // jerry pond
        "JERRY?!",
        "A tiny fin emerges from the water...",
        "You found a Reindrake!",
        "What is this creature?",
        
        // oasis
        "An Oasis Rabbit appeared from the sand!",
        "A Oasis Sheep appeared from the sand!",
        
        // crimson isle
        "A Plhlegblast appeared from the depths!",
        "Magmafish emerged from the lava!",
        "A Moogma appeared from the lava!",
        "A Lava Leech appeared from the lava!",
        "A Pyroclastic Worm surfaced!",
        "A Lava Flame appeared from the lava!",
        "A Fire Eel emerged from the lava!",
        "A Taurus appeared from the lava!",
        "A Thunderbolt shot out of the lava!",
        "You have awoken Lord Jawbus!",
        
        // crystal hollows
        "A Flaming Worm surfaced from the lava!",
        "A Lava Blaze appeared from the lava!",
        "A Lava Pigman appeared from the lava!",
        
        // park
        "A Water Hydra has come to test your strength.",
        "The Water Hydra has come to test your strength.",
        
        // deep caverns
        "A Worm surfaced!",
        
        // barn/mushroom
        "A Nurse Shark has appeared!",
        "A Blue Shark has appeared!",
        "A Tiger Shark has appeared!",
        "A Great White Shark has appeared!",
        
        // winter island
        "A Frozen Steve surfaced!",
        "A Frosty has surfaced!",
        "What is this creature!?", // yeti
        "You found a Nutcracker!",
        
        // end
        "Uh oh! A Frog surfaced!",
        
        // rift
        "A Bloated Mummy appeared from the water!",
        
        // generic catch patterns for sea creatures not listed
        "appeared from the water",
        "surfaced from the water",
        "emerged from the water"
    );
    
    public static void register() {
        ClientReceiveMessageEvents.GAME.register(FishingChatListener::onGameMessage);
    }
    
    private static void onGameMessage(Component message, boolean overlay) {
        if (overlay) return;
        if (!TilemanConfig.getInstance().isEnabled()) return;
        
        String text = message.getString();
        
        for (String pattern : SEA_CREATURE_MESSAGES) {
            if (text.contains(pattern)) {
                TilemanLog.debug(DebugCategory.ALL, "Detected sea creature catch: " + pattern);
                MilestoneTracker.getInstance().onSeaCreatureCaught();
                return;
            }
        }
    }
}
