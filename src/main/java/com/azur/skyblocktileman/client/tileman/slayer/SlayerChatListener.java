package com.azur.skyblocktileman.client.tileman.slayer;

import com.azur.skyblocktileman.client.tileman.DebugCategory;
import com.azur.skyblocktileman.client.tileman.TilemanConfig;
import com.azur.skyblocktileman.client.tileman.TilemanLog;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens to chat messages and titles to detect slayer events.
 */
public final class SlayerChatListener {
    
    private SlayerChatListener() {}
    
    // Chat patterns
    private static final Pattern QUEST_STARTED = Pattern.compile(
        "SLAYER QUEST STARTED!", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern QUEST_COMPLETE = Pattern.compile(
        "SLAYER QUEST COMPLETE!", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern QUEST_FAILED = Pattern.compile(
        "SLAYER QUEST FAILED!", Pattern.CASE_INSENSITIVE);
    
    // Pattern to extract slayer type from "Slay X Combat XP worth of Zombies/Blazes/etc."
    private static final Pattern SLAY_REQUIREMENT = Pattern.compile(
        "Slay ([0-9,]+) Combat XP worth of (\\w+)", Pattern.CASE_INSENSITIVE);
    
    // Boss spawned title
    private static final Pattern BOSS_SPAWNED = Pattern.compile(
        "Boss Spawned", Pattern.CASE_INSENSITIVE);
    
    // Track last detected slayer info for when quest starts
    private static SlayerType pendingType = null;
    private static int pendingTier = 0;
    
    public static void register() {
        ClientReceiveMessageEvents.GAME.register(SlayerChatListener::onGameMessage);
    }
    
    private static void onGameMessage(Component message, boolean overlay) {
        if (!TilemanConfig.getInstance().isEnabled()) {
            return;
        }
        
        String text = message.getString();
        String stripped = ChatFormatting.stripFormatting(text);
        if (stripped == null) return;
        
        // Skip messages sent by this mod to prevent infinite recursion
        if (stripped.startsWith("[Tileman]")) {
            return;
        }
        
        // Check for boss spawned (can appear as title/subtitle)
        if (overlay) {
            if (BOSS_SPAWNED.matcher(stripped).find()) {
                TilemanLog.debug(DebugCategory.ALL, "Slayer boss spawned detected from overlay");
                SlayerTracker.getInstance().onBossSpawned();
            }
            return;
        }
        
        // Check for quest started
        if (QUEST_STARTED.matcher(stripped).find()) {
            TilemanLog.debug(DebugCategory.ALL, "Slayer quest started detected");
            // Type/tier will be detected from scoreboard or next message
            SlayerTracker.getInstance().onQuestStarted(pendingType, pendingTier);
            pendingType = null;
            pendingTier = 0;
            return;
        }
        
        // Check for slay requirement (tells us the mob type)
        Matcher slayMatcher = SLAY_REQUIREMENT.matcher(stripped);
        if (slayMatcher.find()) {
            String mobType = slayMatcher.group(2);
            SlayerType type = SlayerType.fromDisplayName(mobType);
            if (type != null) {
                TilemanLog.debug(DebugCategory.ALL, "Detected slayer type from requirement: {}", type);
                pendingType = type;
                SlayerTracker tracker = SlayerTracker.getInstance();
                if (tracker.isQuestActive() && tracker.getCurrentType() == null) {
                    tracker.updateQuestInfo(type, pendingTier);
                }
            }
            return;
        }
        
        // Check for quest complete
        if (QUEST_COMPLETE.matcher(stripped).find()) {
            TilemanLog.debug(DebugCategory.ALL, "Slayer quest complete detected");
            SlayerTracker.getInstance().onQuestComplete();
            return;
        }
        
        // Check for quest failed
        if (QUEST_FAILED.matcher(stripped).find()) {
            TilemanLog.debug(DebugCategory.ALL, "Slayer quest failed detected");
            SlayerTracker.getInstance().onQuestFailed();
            return;
        }
        
        // Check for boss spawned in chat (backup)
        if (BOSS_SPAWNED.matcher(stripped).find()) {
            TilemanLog.debug(DebugCategory.ALL, "Slayer boss spawned detected from chat");
            SlayerTracker.getInstance().onBossSpawned();
        }
    }
    
    /**
     * Called from scoreboard parser when slayer quest info is detected.
     */
    public static void onScoreboardSlayerInfo(String slayerLine) {
        if (slayerLine == null) return;
        
        SlayerType type = SlayerType.fromDisplayName(slayerLine);
        int tier = SlayerType.parseTier(slayerLine);
        
        if (type != null && tier > 0) {
            pendingType = type;
            pendingTier = tier;
            
            SlayerTracker tracker = SlayerTracker.getInstance();
            if (tracker.isQuestActive()) {
                tracker.updateQuestInfo(type, tier);
            }
        }
    }
}
