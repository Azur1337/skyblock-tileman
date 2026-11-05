package com.azur.skyblocktileman.client.tileman.dungeon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DungeonData {

    private final Set<String> unlockedFloors = new HashSet<>();

    // Tracked locally since install, not from the API
    private final Map<String, FloorStats> floorStats = new HashMap<>();

    // API completion counts already paid out, so later diffs award only new clears
    private final Map<String, Integer> rewardedCompletions = new HashMap<>();

    private boolean baselineFetched = false;

    public DungeonData() {}

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

    public FloorStats getFloorStats(DungeonFloor floor) {
        return getFloorStats(floor.getId());
    }

    // read path must not create entries; UI reads would inflate floorStats with junk
    public FloorStats getFloorStats(String floorId) {
        FloorStats stats = floorStats.get(floorId);
        return stats == null ? new FloorStats() : stats;
    }

    public FloorStats getOrCreateFloorStats(DungeonFloor floor) {
        return getOrCreateFloorStats(floor.getId());
    }

    public FloorStats getOrCreateFloorStats(String floorId) {
        return floorStats.computeIfAbsent(floorId, k -> new FloorStats());
    }

    public int getCompletions(DungeonFloor floor) {
        return getFloorStats(floor).completions;
    }

    public int getTotalCompletions() {
        return floorStats
            .values()
            .stream()
            .mapToInt(s -> s.completions)
            .sum();
    }

    public int getUnlockedNormalFloorCount() {
        int count = 0;
        for (DungeonFloor floor : DungeonFloor.getNormalFloors()) {
            if (isUnlocked(floor)) count++;
        }
        return count;
    }

    public int getUnlockedMasterModeFloorCount() {
        int count = 0;
        for (DungeonFloor floor : DungeonFloor.getMasterModeFloors()) {
            if (isUnlocked(floor)) count++;
        }
        return count;
    }

    public boolean isBaselineFetched() {
        return baselineFetched;
    }

    public void setBaselineFetched(boolean fetched) {
        this.baselineFetched = fetched;
    }

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

    public static class FloorStats {

        public int completions = 0;
    }
}
