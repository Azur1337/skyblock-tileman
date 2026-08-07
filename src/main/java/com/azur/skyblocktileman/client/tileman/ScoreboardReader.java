package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.slayer.SlayerChatListener;
import com.azur.skyblocktileman.client.tileman.slayer.SlayerType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Reads the Hypixel Skyblock scoreboard to detect game state.
 */
public final class ScoreboardReader {
    
    private ScoreboardReader() {}
    
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // Check every second
    
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ScoreboardReader::onTick);
    }
    
    private static void onTick(Minecraft client) {
        if (!TilemanConfig.getInstance().isEnabled()) {
            return;
        }
        
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) {
            return;
        }
        tickCounter = 0;
        
        if (client.level == null || client.player == null) {
            return;
        }
        
        List<String> lines = getScoreboardLines(client);
        if (lines.isEmpty()) {
            return;
        }
        
        parseScoreboard(lines);
    }
    
    /**
     * Get all lines from the sidebar scoreboard.
     */
    private static List<String> getScoreboardLines(Minecraft client) {
        List<String> result = new ArrayList<>();
        
        Scoreboard scoreboard = client.level.getScoreboard();
        if (scoreboard == null) {
            return result;
        }
        
        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) {
            return result;
        }
        
        Collection<PlayerScoreEntry> scores = scoreboard.listPlayerScores(sidebar);
        
        // Sort by score descending (top of scoreboard first)
        List<PlayerScoreEntry> sortedScores = scores.stream()
            .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed())
            .limit(15)
            .toList();
        
        for (PlayerScoreEntry score : sortedScores) {
            String line = score.ownerName().getString();
            // Strip color codes for easier parsing
            String stripped = ChatFormatting.stripFormatting(line);
            if (stripped != null && !stripped.isBlank()) {
                result.add(stripped);
            }
        }
        
        return result;
    }
    
    /**
     * Parse scoreboard lines to detect slayer quest info.
     */
    private static void parseScoreboard(List<String> lines) {
        boolean foundSlayerQuest = false;
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            // Look for "Slayer Quest" header
            if (line.contains("Slayer Quest")) {
                foundSlayerQuest = true;
                continue;
            }
            
            // If we just found "Slayer Quest", the next line should be the slayer type/tier
            if (foundSlayerQuest) {
                // Try to parse slayer type and tier from lines like "Revenant Horror I" or "Inferno Demonlord IV"
                SlayerType type = SlayerType.fromDisplayName(line);
                int tier = SlayerType.parseTier(line);
                
                if (type != null && tier > 0) {
                    TilemanLog.debug(DebugCategory.ALL, "Scoreboard slayer detected: {} T{}", type.getDisplayName(), tier);
                    SlayerChatListener.onScoreboardSlayerInfo(line);
                }
                
                // Only check the line immediately after "Slayer Quest"
                foundSlayerQuest = false;
            }
        }
    }
}
