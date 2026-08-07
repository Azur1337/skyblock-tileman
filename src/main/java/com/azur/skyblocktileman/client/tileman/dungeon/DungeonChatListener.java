package com.azur.skyblocktileman.client.tileman.dungeon;

import com.azur.skyblocktileman.client.tileman.DebugCategory;
import com.azur.skyblocktileman.client.tileman.TilemanChat;
import com.azur.skyblocktileman.client.tileman.TilemanConfig;
import com.azur.skyblocktileman.client.tileman.TilemanLog;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens to chat messages to detect:
 * 1. Joining a party for a locked dungeon floor
 * 2. Being teleported into a locked dungeon floor
 * 
 * Takes action to prevent playing locked content.
 */
public final class DungeonChatListener {
    
    private DungeonChatListener() {}
    
    // Pattern: "[MVP+] PlayerName entered The Catacombs, Floor II!"
    // Also: "You entered The Catacombs, Floor II!"
    private static final Pattern DUNGEON_ENTER_PATTERN = 
        Pattern.compile("entered The Catacombs, Floor ([IVX]+)!", Pattern.CASE_INSENSITIVE);
    
    // Pattern for master mode: "entered MM The Catacombs, Floor II!"
    private static final Pattern MM_DUNGEON_ENTER_PATTERN = 
        Pattern.compile("entered MM The Catacombs, Floor ([IVX]+)!", Pattern.CASE_INSENSITIVE);
    
    // Pattern: "Party Finder > PlayerName joined the dungeon group! (Floor I)"
    // Or: "Party Finder > PlayerName joined the dungeon group! (Master Mode Floor I)"
    private static final Pattern PARTY_FINDER_JOIN_PATTERN = 
        Pattern.compile("Party Finder > .+ joined the dungeon group! \\((?:Master Mode )?Floor ([IVX]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARTY_FINDER_MM_JOIN_PATTERN = 
        Pattern.compile("Party Finder > .+ joined the dungeon group! \\(Master Mode Floor ([IVX]+)\\)", Pattern.CASE_INSENSITIVE);
    
    // Starting dungeon message
    private static final Pattern DUNGEON_START_PATTERN = 
        Pattern.compile("Starting in \\d+ seconds?\\.", Pattern.CASE_INSENSITIVE);
    
    // Track if we're waiting to leave
    private static DungeonFloor pendingLeaveFloor = null;
    private static long lastWarningTime = 0;
    private static final long WARNING_COOLDOWN_MS = 5000;
    
    public static void register() {
        ClientReceiveMessageEvents.GAME.register(DungeonChatListener::onGameMessage);
    }
    
    private static void onGameMessage(Component message, boolean overlay) {
        // Skip actionbar overlay messages
        if (overlay) {
            return;
        }
        
        if (!TilemanConfig.getInstance().isEnabled()) {
            return;
        }
        
        String text = message.getString();
        // Strip color codes for easier matching
        String stripped = ChatFormatting.stripFormatting(text);
        if (stripped == null) return;
        
        // Skip messages sent by this mod to prevent infinite recursion
        if (stripped.startsWith("[Tileman]")) {
            return;
        }
        
        // Log all chat messages if CHAT debug is enabled (very spammy!)
        TilemanLog.debug(DebugCategory.CHAT, "Chat: {}", stripped);
        
        // Check for dungeon entry
        checkDungeonEntry(stripped);
        
        // Check for party finder join
        checkPartyFinderJoin(stripped);
        
        // Check if dungeon is starting while we have a pending leave
        checkDungeonStart(stripped);
    }
    
    private static void checkDungeonEntry(String text) {
        // Check for master mode entry first
        Matcher mmMatcher = MM_DUNGEON_ENTER_PATTERN.matcher(text);
        if (mmMatcher.find()) {
            int floorNum = romanToInt(mmMatcher.group(1));
            DungeonFloor floor = DungeonFloor.fromNumber(floorNum, true);
            handleDungeonEntry(floor, text);
            return;
        }
        
        // Check for normal entry
        Matcher normalMatcher = DUNGEON_ENTER_PATTERN.matcher(text);
        if (normalMatcher.find()) {
            int floorNum = romanToInt(normalMatcher.group(1));
            DungeonFloor floor = DungeonFloor.fromNumber(floorNum, false);
            handleDungeonEntry(floor, text);
        }
    }
    
    private static void handleDungeonEntry(DungeonFloor floor, String text) {
        if (floor == null) return;
        
        // Check if this is about us (contains "You" or our username)
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        String playerName = client.player.getName().getString();
        boolean isUs = text.contains("You entered") || text.contains(playerName + " entered");
        
        if (!isUs) {
            // Party member entered, not us
            TilemanLog.debug(DebugCategory.ALL, "Party member entered {}", floor.getId());
            return;
        }
        
        DungeonData data = TilemanState.getInstance().getDungeonData();
        
        if (!data.isUnlocked(floor)) {
            TilemanLog.debug(DebugCategory.ALL, "WE entered locked floor {}!", floor.getId());
            pendingLeaveFloor = floor;
            
            // Warn the player and leave
            warnAndLeave(floor);
        } else {
            TilemanLog.debug(DebugCategory.ALL, "Entered unlocked floor {}", floor.getId());
            DungeonTracker.getInstance().onDungeonEnter(floor);
        }
    }
    
    private static void checkPartyFinderJoin(String text) {
        // Check master mode first
        Matcher mmMatcher = PARTY_FINDER_MM_JOIN_PATTERN.matcher(text);
        if (mmMatcher.find()) {
            int floorNum = romanToInt(mmMatcher.group(1));
            DungeonFloor floor = DungeonFloor.fromNumber(floorNum, true);
            handlePartyJoin(floor);
            return;
        }
        
        // Check normal
        Matcher normalMatcher = PARTY_FINDER_JOIN_PATTERN.matcher(text);
        if (normalMatcher.find()) {
            // Need to check if it's actually master mode (pattern also matches)
            boolean isMaster = text.contains("Master Mode");
            int floorNum = romanToInt(normalMatcher.group(1));
            DungeonFloor floor = DungeonFloor.fromNumber(floorNum, isMaster);
            handlePartyJoin(floor);
        }
    }
    
    private static void handlePartyJoin(DungeonFloor floor) {
        if (floor == null) return;
        
        DungeonData data = TilemanState.getInstance().getDungeonData();
        
        if (!data.isUnlocked(floor)) {
            // We joined a party for a locked floor!
            long now = System.currentTimeMillis();
            if (now - lastWarningTime > WARNING_COOLDOWN_MS) {
                lastWarningTime = now;
                TilemanChat.warn("§c§lWARNING: §r" + floor.getDisplayName() + " is locked!");
                TilemanChat.warn("§7You will be removed from the dungeon if it starts.");
                TilemanChat.warn("§7Use §e/p leave §7to leave the party, or unlock the floor first.");
            }
            pendingLeaveFloor = floor;
        }
    }
    
    private static void checkDungeonStart(String text) {
        if (pendingLeaveFloor == null) return;
        
        if (DUNGEON_START_PATTERN.matcher(text).find()) {
            // Dungeon is starting and we're in a locked floor party!
            warnAndLeave(pendingLeaveFloor);
        }
    }
    
    private static void warnAndLeave(DungeonFloor floor) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        TilemanChat.warn("§c§l[TILEMAN] §r" + floor.getDisplayName() + " is LOCKED!");
        TilemanChat.warn("§7Leaving party and warping out...");
        
        // Schedule commands to leave
        // Leave party first
        if (client.player != null && client.getConnection() != null) {
            client.player.connection.sendCommand("p leave");
        }
        
        // Warp to hub after a short delay using scheduled task
        // We need to delay to ensure party leave processes first
        scheduleWarpOut();
        
        pendingLeaveFloor = null;
    }
    
    private static int warpDelayTicks = 0;
    private static boolean warpScheduled = false;
    
    private static void scheduleWarpOut() {
        warpDelayTicks = 10; // 0.5 seconds = 10 ticks
        warpScheduled = true;
    }
    
    /**
     * Called each client tick to handle delayed warp.
     */
    public static void tick() {
        if (!warpScheduled) return;
        
        warpDelayTicks--;
        if (warpDelayTicks <= 0) {
            warpScheduled = false;
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && client.getConnection() != null) {
                client.player.connection.sendCommand("warp hub");
            }
        }
    }
    
    /**
     * Convert Roman numeral to integer.
     */
    private static int romanToInt(String roman) {
        if (roman == null || roman.isEmpty()) return 0;
        roman = roman.toUpperCase();
        
        return switch (roman) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            case "VI" -> 6;
            case "VII" -> 7;
            default -> 0;
        };
    }
    
    /**
     * Clear pending leave state (e.g., when player manually leaves).
     */
    public static void clearPendingLeave() {
        pendingLeaveFloor = null;
    }
}
