package com.azur.skyblocktileman.client.tileman.dungeon;

import com.azur.skyblocktileman.client.tileman.DebugCategory;
import com.azur.skyblocktileman.client.tileman.HypixelApiClient;
import com.azur.skyblocktileman.client.tileman.TilemanChat;
import com.azur.skyblocktileman.client.tileman.TilemanConfig;
import com.azur.skyblocktileman.client.tileman.TilemanLog;
import com.azur.skyblocktileman.client.tileman.TilemanLoginHandler;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

import java.util.Map;
import java.util.UUID;

/**
 * Manages dungeon floor unlocking and completion rewards.
 */
public class DungeonTracker {
    
    private static DungeonTracker instance;
    
    // Currently active floor (if in dungeon)
    private DungeonFloor activeFloor = null;
    
    // Last known API completions (updated periodically)
    private Map<String, Integer> lastApiNormalCompletions;
    private Map<String, Integer> lastApiMasterCompletions;
    
    public static DungeonTracker getInstance() {
        if (instance == null) {
            instance = new DungeonTracker();
        }
        return instance;
    }
    
    private DungeonTracker() {}
    
    /**
     * Check if a floor is unlocked.
     */
    public boolean isFloorUnlocked(DungeonFloor floor) {
        return getDungeonData().isUnlocked(floor);
    }
    
    /**
     * Attempt to unlock a floor. Returns true if successful.
     */
    public boolean tryUnlockFloor(DungeonFloor floor) {
        if (floor == null) return false;
        
        DungeonData data = getDungeonData();
        
        if (data.isUnlocked(floor)) {
            TilemanChat.warn(floor.getDisplayName() + " is already unlocked!");
            return false;
        }
        
        int cost = floor.getUnlockCost();
        int tokens = TilemanState.getInstance().getTokens();
        
        if (tokens < cost) {
            TilemanChat.warn("Not enough tokens! Need " + cost + ", have " + tokens);
            return false;
        }
        
        // Spend tokens and unlock
        TilemanState.getInstance().spendTokens(cost);
        data.unlock(floor);
        TilemanState.getInstance().save();
        
        TilemanChat.info("§6§lFLOOR UNLOCKED! §r" + floor.getDisplayName() + " §7(-" + cost + " tokens)");
        
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
        
        return true;
    }
    
    /**
     * Called when player enters a dungeon floor.
     */
    public void onDungeonEnter(DungeonFloor floor) {
        activeFloor = floor;
        TilemanLog.debug(DebugCategory.ALL, "Entered dungeon: {}", floor.getId());
    }
    
    /**
     * Called when player leaves dungeon.
     */
    public void onDungeonExit() {
        activeFloor = null;
        TilemanLog.debug(DebugCategory.ALL, "Exited dungeon");
    }
    
    /**
     * Check if player can enter a floor.
     */
    public boolean canEnterFloor(DungeonFloor floor) {
        return getDungeonData().isUnlocked(floor);
    }
    
    /**
     * Called when a dungeon run is completed (detected via chat).
     * 
     * @param floor The floor that was completed
     * @param sRank Whether S rank was achieved
     * @param sPlusRank Whether S+ rank was achieved
     */
    public void onDungeonComplete(DungeonFloor floor, boolean sRank, boolean sPlusRank) {
        if (floor == null) return;
        
        DungeonData data = getDungeonData();
        int completionsBefore = data.getCompletions(floor);
        
        // Record the completion locally
        data.recordCompletion(floor, sRank, sPlusRank);
        
        // Calculate and award tokens
        int reward = floor.getRewardForCompletion(completionsBefore);
        
        if (reward > 0) {
            TilemanState.getInstance().addTokens(reward);
            
            String rankStr = sPlusRank ? " §d§l(S+)" : (sRank ? " §6§l(S)" : "");
            TilemanChat.info("§a§lDUNGEON COMPLETE! §r" + floor.getDisplayName() + rankStr + 
                " §7(+" + reward + " tokens)");
            
            // Show progress towards next floor unlock if applicable
            DungeonFloor nextFloor = floor.getNextFloor();
            if (nextFloor != null && !data.isUnlocked(nextFloor)) {
                int completionsAfter = data.getCompletions(floor);
                if (completionsAfter <= 10) {
                    int tokensNeeded = nextFloor.getUnlockCost();
                    int tokensFromThisFloor = completionsAfter * floor.getBaseReward();
                    TilemanChat.info("§7Progress to " + nextFloor.getId() + ": " + 
                        tokensFromThisFloor + "/" + tokensNeeded + " tokens from " + floor.getId() + " clears");
                }
            }
        } else {
            String rankStr = sPlusRank ? " §d§l(S+)" : (sRank ? " §6§l(S)" : "");
            TilemanChat.info("§a§lDUNGEON COMPLETE! §r" + floor.getDisplayName() + rankStr + 
                " §7(max rewards reached)");
        }
        
        TilemanState.getInstance().save();
        
        // Check dungeon milestones
        MilestoneTracker.getInstance().onDungeonComplete(floor, sRank, sPlusRank);
        
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        }
    }
    
    /**
     * Get the currently active floor (if in dungeon).
     */
    public DungeonFloor getActiveFloor() {
        return activeFloor;
    }
    
    /**
     * Check if player is currently in a dungeon.
     */
    public boolean isInDungeon() {
        return activeFloor != null;
    }
    
    private DungeonData getDungeonData() {
        return TilemanState.getInstance().getDungeonData();
    }
    
    /**
     * Fetch dungeon completions from API, set baseline if needed, and award tokens for new completions.
     */
    public void fetchAndUpdateCompletions() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        UUID playerUuid = client.player.getUUID();
        String apiKey = TilemanLoginHandler.getApiKey();
        
        if (apiKey == null || apiKey.isBlank()) {
            TilemanLog.debug(DebugCategory.ALL, "No API key, skipping dungeon fetch");
            return;
        }
        
        HypixelApiClient.fetchDungeonCompletions(playerUuid, apiKey)
            .thenAccept(result -> {
                if (!result.success()) {
                    TilemanLog.debug(DebugCategory.ALL, "Failed to fetch dungeons: {}", result.errorMessage());
                    return;
                }
                
                client.execute(() -> processApiResult(result));
            });
    }
    
    /**
     * Process API result - set baseline and/or award tokens for new completions.
     */
    private void processApiResult(HypixelApiClient.DungeonResult result) {
        DungeonData data = getDungeonData();
        
        lastApiNormalCompletions = result.normalCompletions();
        lastApiMasterCompletions = result.masterCompletions();
        
        // First time: Set baseline and rewarded completions to current values
        if (!data.isBaselineFetched()) {
            TilemanLog.debug(DebugCategory.ALL, "Setting dungeon baseline from API");
            
            if (result.normalCompletions() != null) {
                for (Map.Entry<String, Integer> entry : result.normalCompletions().entrySet()) {
                    data.setApiBaselineCompletion(entry.getKey(), entry.getValue());
                    data.setRewardedCompletions(entry.getKey(), entry.getValue());
                }
            }
            if (result.masterCompletions() != null) {
                for (Map.Entry<String, Integer> entry : result.masterCompletions().entrySet()) {
                    data.setApiBaselineCompletion(entry.getKey(), entry.getValue());
                    data.setRewardedCompletions(entry.getKey(), entry.getValue());
                }
            }
            
            data.setBaselineFetched(true);
            TilemanState.getInstance().save();
            TilemanChat.info("§7Dungeon baseline set from API.");
            return;
        }
        
        // Already have baseline: Check for new completions and award tokens
        int totalTokensAwarded = 0;
        
        // Process normal floors
        if (result.normalCompletions() != null) {
            for (DungeonFloor floor : DungeonFloor.getNormalFloors()) {
                int apiCompletions = result.normalCompletions().getOrDefault(floor.getId(), 0);
                totalTokensAwarded += processFloorCompletions(floor, apiCompletions, data);
            }
        }
        
        // Process master mode floors
        if (result.masterCompletions() != null) {
            for (DungeonFloor floor : DungeonFloor.getMasterModeFloors()) {
                int apiCompletions = result.masterCompletions().getOrDefault(floor.getId(), 0);
                totalTokensAwarded += processFloorCompletions(floor, apiCompletions, data);
            }
        }
        
        if (totalTokensAwarded > 0) {
            TilemanState.getInstance().save();
            TilemanChat.info("§a§lDUNGEON REWARDS! §r+" + totalTokensAwarded + " tokens from completions");
            
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }
        
        TilemanLog.debug(DebugCategory.ALL, "Dungeon completions updated from API");
    }
    
    /**
     * Process completions for a single floor and award tokens.
     * Returns total tokens awarded.
     */
    private int processFloorCompletions(DungeonFloor floor, int currentApiCompletions, DungeonData data) {
        // Only award for unlocked floors
        if (!data.isUnlocked(floor)) {
            return 0;
        }
        
        int rewardedSoFar = data.getRewardedCompletions(floor);
        int newCompletions = currentApiCompletions - rewardedSoFar;
        
        if (newCompletions <= 0) {
            return 0;
        }
        
        TilemanLog.debug(DebugCategory.ALL, "Floor {} has {} new completions (was {}, now {})", 
            floor.getId(), newCompletions, rewardedSoFar, currentApiCompletions);
        
        int totalTokens = 0;
        
        // Calculate tokens for each new completion
        for (int i = 0; i < newCompletions; i++) {
            int completionNumber = rewardedSoFar + i; // 0-indexed completion count
            int reward = floor.getRewardForCompletion(completionNumber);
            totalTokens += reward;
        }
        
        if (totalTokens > 0) {
            TilemanState.getInstance().addTokens(totalTokens);
            TilemanLog.debug(DebugCategory.ALL, "Awarded {} tokens for {} on {} completions", 
                totalTokens, floor.getId(), newCompletions);
            
            // Update local stats
            DungeonData.FloorStats stats = data.getFloorStats(floor);
            stats.completions += newCompletions;
        }
        
        // Mark these completions as rewarded
        data.setRewardedCompletions(floor, currentApiCompletions);
        
        return totalTokens;
    }
    
    /**
     * Get last known API completions for a floor.
     */
    public int getApiCompletions(DungeonFloor floor) {
        Map<String, Integer> map = floor.isMasterMode() ? lastApiMasterCompletions : lastApiNormalCompletions;
        if (map == null) return 0;
        return map.getOrDefault(floor.getId(), 0);
    }
    
    /**
     * Get a summary of dungeon progress for display.
     */
    public String getProgressSummary() {
        DungeonData data = getDungeonData();
        StringBuilder sb = new StringBuilder();
        
        sb.append("§6=== Dungeon Progress ===\n");
        sb.append("§eNormal Floors: ");
        for (DungeonFloor floor : DungeonFloor.getNormalFloors()) {
            sb.append(data.isUnlocked(floor) ? "§a✓" : "§c✗");
            sb.append(floor.getId()).append(" ");
        }
        sb.append("\n§eMaster Mode: ");
        for (DungeonFloor floor : DungeonFloor.getMasterModeFloors()) {
            sb.append(data.isUnlocked(floor) ? "§a✓" : "§c✗");
            sb.append(floor.getId()).append(" ");
        }
        sb.append("\n§7Total completions: ").append(data.getTotalCompletions());
        sb.append("\n§7S ranks: ").append(data.getTotalSRanks());
        sb.append("\n§7S+ ranks: ").append(data.getTotalSPlusRanks());
        
        DungeonFloor nextNormal = data.getNextNormalFloorToUnlock();
        if (nextNormal != null) {
            sb.append("\n§7Next normal unlock: §e").append(nextNormal.getId())
              .append(" §7(").append(nextNormal.getUnlockCost()).append(" tokens)");
        }
        
        DungeonFloor nextMaster = data.getNextMasterModeFloorToUnlock();
        if (nextMaster != null) {
            sb.append("\n§7Next master unlock: §e").append(nextMaster.getId())
              .append(" §7(").append(nextMaster.getUnlockCost()).append(" tokens)");
        }
        
        return sb.toString();
    }
}
