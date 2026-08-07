package com.azur.skyblocktileman.client.tileman.dungeon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Persistent dungeon data for a profile.
 * Stored as part of ProfileData.
 */
public class DungeonData {
    
    // Floors the player has permanently unlocked
    private final Set<String> unlockedFloors = new HashSet<>();
    
    // Stats per floor (local tracking since mod install)
    private final Map<String, FloorStats> floorStats = new HashMap<>();
    
    // Baseline completions from API when mod was first installed
    // Used to calculate completions since mod start
    private final Map<String, Integer> apiBaselineCompletions = new HashMap<>();
    
    // Last API completions that we've rewarded tokens for
    // When API shows more completions than this, we award tokens for the difference
    private final Map<String, Integer> rewardedCompletions = new HashMap<>();
    
    // Flag to know if we've fetched the baseline
    private boolean baselineFetched = false;
    
    public DungeonData() {
        // No floors unlocked by default - F1 costs 100 tokens
    }
    
    public boolean isUnlocked(DungeonFloor floor) {
        return unlockedFloors.contains(floor.getId());
    }
    
    public boolean isUnlocked(String floorId) {
        return unlockedFloors.contains(floorId);
    }
    
    public void unlock(DungeonFloor floor) {
        unlockedFloors.add(floor.getId());
    }
    
    public void unlock(String floorId) {
        unlockedFloors.add(floorId);
    }
    
    public Set<String> getUnlockedFloors() {
        return new HashSet<>(unlockedFloors);
    }
    
    public FloorStats getFloorStats(DungeonFloor floor) {
        return floorStats.computeIfAbsent(floor.getId(), k -> new FloorStats());
    }
    
    public FloorStats getFloorStats(String floorId) {
        return floorStats.computeIfAbsent(floorId, k -> new FloorStats());
    }
    
    public int getCompletions(DungeonFloor floor) {
        return getFloorStats(floor).completions;
    }
    
    public int getSRanks(DungeonFloor floor) {
        return getFloorStats(floor).sRanks;
    }
    
    public int getSPlusRanks(DungeonFloor floor) {
        return getFloorStats(floor).sPlusRanks;
    }
    
    public void recordCompletion(DungeonFloor floor, boolean sRank, boolean sPlusRank) {
        FloorStats stats = getFloorStats(floor);
        stats.completions++;
        if (sRank) stats.sRanks++;
        if (sPlusRank) stats.sPlusRanks++;
    }
    
    /**
     * Get total completions across all floors.
     */
    public int getTotalCompletions() {
        return floorStats.values().stream()
            .mapToInt(s -> s.completions)
            .sum();
    }
    
    /**
     * Get total S ranks across all floors.
     */
    public int getTotalSRanks() {
        return floorStats.values().stream()
            .mapToInt(s -> s.sRanks)
            .sum();
    }
    
    /**
     * Get total S+ ranks across all floors.
     */
    public int getTotalSPlusRanks() {
        return floorStats.values().stream()
            .mapToInt(s -> s.sPlusRanks)
            .sum();
    }
    
    /**
     * Get count of unlocked normal floors.
     */
    public int getUnlockedNormalFloorCount() {
        int count = 0;
        for (DungeonFloor floor : DungeonFloor.getNormalFloors()) {
            if (isUnlocked(floor)) count++;
        }
        return count;
    }
    
    /**
     * Get count of unlocked master mode floors.
     */
    public int getUnlockedMasterModeFloorCount() {
        int count = 0;
        for (DungeonFloor floor : DungeonFloor.getMasterModeFloors()) {
            if (isUnlocked(floor)) count++;
        }
        return count;
    }
    
    /**
     * Get the next floor to unlock (normal mode).
     */
    public DungeonFloor getNextNormalFloorToUnlock() {
        for (DungeonFloor floor : DungeonFloor.getNormalFloors()) {
            if (!isUnlocked(floor)) {
                return floor;
            }
        }
        return null;
    }
    
    /**
     * Get the next floor to unlock (master mode).
     */
    public DungeonFloor getNextMasterModeFloorToUnlock() {
        for (DungeonFloor floor : DungeonFloor.getMasterModeFloors()) {
            if (!isUnlocked(floor)) {
                return floor;
            }
        }
        return null;
    }
    
    // === API Baseline tracking ===
    
    public boolean isBaselineFetched() {
        return baselineFetched;
    }
    
    public void setBaselineFetched(boolean fetched) {
        this.baselineFetched = fetched;
    }
    
    public void setApiBaselineCompletion(String floorId, int completions) {
        apiBaselineCompletions.put(floorId, completions);
    }
    
    public int getApiBaselineCompletion(String floorId) {
        return apiBaselineCompletions.getOrDefault(floorId, 0);
    }
    
    public int getApiBaselineCompletion(DungeonFloor floor) {
        return getApiBaselineCompletion(floor.getId());
    }
    
    public Map<String, Integer> getAllApiBaselineCompletions() {
        return new HashMap<>(apiBaselineCompletions);
    }
    
    /**
     * Calculate completions since mod was installed.
     * This is: current API completions - baseline completions
     */
    public int getCompletionsSinceModInstall(DungeonFloor floor, int currentApiCompletions) {
        int baseline = getApiBaselineCompletion(floor);
        return Math.max(0, currentApiCompletions - baseline);
    }
    
    // === Rewarded completions tracking ===
    
    public int getRewardedCompletions(String floorId) {
        return rewardedCompletions.getOrDefault(floorId, 0);
    }
    
    public int getRewardedCompletions(DungeonFloor floor) {
        return getRewardedCompletions(floor.getId());
    }
    
    public void setRewardedCompletions(String floorId, int completions) {
        rewardedCompletions.put(floorId, completions);
    }
    
    public void setRewardedCompletions(DungeonFloor floor, int completions) {
        setRewardedCompletions(floor.getId(), completions);
    }
    
    /**
     * Get number of completions that haven't been rewarded yet.
     * This is: current API completions - rewarded completions
     */
    public int getUnrewardedCompletions(DungeonFloor floor, int currentApiCompletions) {
        int rewarded = getRewardedCompletions(floor);
        return Math.max(0, currentApiCompletions - rewarded);
    }
    
    /**
     * Per-floor statistics.
     */
    public static class FloorStats {
        public int completions = 0;
        public int sRanks = 0;
        public int sPlusRanks = 0;
    }
}
