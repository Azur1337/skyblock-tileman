package com.azur.skyblocktileman.client.tileman.slayer;

import com.google.gson.annotations.Expose;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistent data for slayer tracking per profile.
 */
public class SlayerData {
    
    @Expose
    private Map<String, SlayerStats> stats = new HashMap<>();
    
    @Expose
    private int currentFlawlessStreak = 0;
    
    @Expose
    private int bestFlawlessStreak = 0;
    
    @Expose
    private int totalFlawlessT4Plus = 0;
    
    public SlayerStats getStats(SlayerType type) {
        return stats.computeIfAbsent(type.name(), k -> new SlayerStats());
    }
    
    public int getCurrentFlawlessStreak() {
        return currentFlawlessStreak;
    }
    
    public int getBestFlawlessStreak() {
        return bestFlawlessStreak;
    }
    
    public int getTotalFlawlessT4Plus() {
        return totalFlawlessT4Plus;
    }
    
    public void recordFlawlessCompletion(SlayerType type, int tier) {
        SlayerStats s = getStats(type);
        s.totalCompletions++;
        
        if (tier >= 4) {
            s.flawlessT4Plus++;
            totalFlawlessT4Plus++;
            currentFlawlessStreak++;
            if (currentFlawlessStreak > bestFlawlessStreak) {
                bestFlawlessStreak = currentFlawlessStreak;
            }
            if (currentFlawlessStreak > s.bestStreak) {
                s.bestStreak = currentFlawlessStreak;
            }
        }
    }
    
    public void recordFailedQuest() {
        currentFlawlessStreak = 0;
    }
    
    public void recordTaintedCompletion(SlayerType type, int tier) {
        SlayerStats s = getStats(type);
        s.totalCompletions++;
        
        // Tainted completion breaks the streak
        if (tier >= 4) {
            currentFlawlessStreak = 0;
        }
    }
    
    public static class SlayerStats {
        @Expose
        public int totalCompletions = 0;
        
        @Expose
        public int flawlessT4Plus = 0;
        
        @Expose
        public int bestStreak = 0;
    }
}
