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
    
    // Pattern to extract slayer type and XP requirement from "» Slay X Combat XP worth of Zombies/Blazes/etc."
    private static final Pattern SLAY_REQUIREMENT = Pattern.compile(
        "Slay ([0-9,.kKmM]+) Combat XP worth of (\\w+)", Pattern.CASE_INSENSITIVE);
    
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
            TilemanLog.debug(DebugCategory.ALL, "Slayer quest started detected: type={}, tier={}", pendingType, pendingTier);
            // Type/tier should have been detected from the previous slay requirement message
            SlayerTracker.getInstance().onQuestStarted(pendingType, pendingTier);
            // Don't reset pending - the requirement message might come after STARTED
            return;
        }
        
        // Check for slay requirement (tells us the mob type and XP which determines tier)
        Matcher slayMatcher = SLAY_REQUIREMENT.matcher(stripped);
        if (slayMatcher.find()) {
            String xpStr = slayMatcher.group(1);
            String mobType = slayMatcher.group(2);
            SlayerType type = SlayerType.fromDisplayName(mobType);
            int xpRequired = parseXpAmount(xpStr);
            int tier = getTierFromXp(type, xpRequired);
            
            TilemanLog.debug(DebugCategory.ALL, "Detected slayer from requirement: type={}, xp={}, tier={}", type, xpRequired, tier);
            
            if (type != null) {
                pendingType = type;
                pendingTier = tier;
                
                SlayerTracker tracker = SlayerTracker.getInstance();
                if (tracker.isQuestActive()) {
                    tracker.updateQuestInfo(type, tier);
                }
            }
            return;
        }
        
        // Check for quest complete
        if (QUEST_COMPLETE.matcher(stripped).find()) {
            TilemanLog.debug(DebugCategory.ALL, "Slayer quest complete detected");
            SlayerTracker.getInstance().onQuestComplete();
            pendingType = null;
            pendingTier = 0;
            return;
        }
        
        // Check for quest failed
        if (QUEST_FAILED.matcher(stripped).find()) {
            TilemanLog.debug(DebugCategory.ALL, "Slayer quest failed detected");
            SlayerTracker.getInstance().onQuestFailed();
            pendingType = null;
            pendingTier = 0;
            return;
        }
        
        // Check for boss spawned in chat (backup)
        if (BOSS_SPAWNED.matcher(stripped).find()) {
            TilemanLog.debug(DebugCategory.ALL, "Slayer boss spawned detected from chat");
            SlayerTracker.getInstance().onBossSpawned();
        }
    }
    
    /**
     * Parse XP amount from string like "150", "33,600", "33.6k", "1.5M"
     */
    private static int parseXpAmount(String xpStr) {
        if (xpStr == null || xpStr.isEmpty()) return 0;
        
        xpStr = xpStr.replace(",", "").toLowerCase();
        
        double multiplier = 1;
        if (xpStr.endsWith("k")) {
            multiplier = 1000;
            xpStr = xpStr.substring(0, xpStr.length() - 1);
        } else if (xpStr.endsWith("m")) {
            multiplier = 1000000;
            xpStr = xpStr.substring(0, xpStr.length() - 1);
        }
        
        try {
            return (int) (Double.parseDouble(xpStr) * multiplier);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Determine tier from XP requirement.
     * XP requirements per slayer:
     * - Zombie (Revenant): T1=150, T2=1500, T3=5000, T4=50000, T5=100000
     * - Spider (Tarantula): T1=250, T2=1000, T3=2500, T4=10000
     * - Wolf (Sven): T1=250, T2=1500, T3=4000, T4=20000
     * - Enderman (Voidgloom): T1=1000, T2=5000, T3=25000, T4=100000
     * - Blaze (Inferno): T1=2000, T2=7500, T3=15000, T4=33600, T5=50000
     * - Vampire (Riftstalker): T1=1000, T2=5000, T3=20000, T4=100000, T5=200000
     */
    private static int getTierFromXp(SlayerType type, int xp) {
        if (type == null || xp <= 0) return 1;
        
        return switch (type) {
            case REVENANT -> {
                if (xp >= 100000) yield 5;
                if (xp >= 50000) yield 4;
                if (xp >= 5000) yield 3;
                if (xp >= 1500) yield 2;
                yield 1;
            }
            case TARANTULA -> {
                if (xp >= 10000) yield 4;
                if (xp >= 2500) yield 3;
                if (xp >= 1000) yield 2;
                yield 1;
            }
            case SVEN -> {
                if (xp >= 20000) yield 4;
                if (xp >= 4000) yield 3;
                if (xp >= 1500) yield 2;
                yield 1;
            }
            case VOIDGLOOM -> {
                if (xp >= 100000) yield 4;
                if (xp >= 25000) yield 3;
                if (xp >= 5000) yield 2;
                yield 1;
            }
            case INFERNO -> {
                if (xp >= 50000) yield 5;
                if (xp >= 33600) yield 4;
                if (xp >= 15000) yield 3;
                if (xp >= 7500) yield 2;
                yield 1;
            }
            case RIFTSTALKER -> {
                if (xp >= 200000) yield 5;
                if (xp >= 100000) yield 4;
                if (xp >= 20000) yield 3;
                if (xp >= 5000) yield 2;
                yield 1;
            }
        };
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
