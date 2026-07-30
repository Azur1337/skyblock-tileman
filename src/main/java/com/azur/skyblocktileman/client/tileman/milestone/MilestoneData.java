package com.azur.skyblocktileman.client.tileman.milestone;

import java.util.HashMap;
import java.util.Map;

public class MilestoneData {
    
    private final Map<String, Integer> completedTiers = new HashMap<>();
    private final Map<String, Long> progress = new HashMap<>();
    
    private int totalTokensFromMilestones = 0;
    private long totalPlaytimeMinutes = 0;
    private int totalShopSpent = 0;
    
    private transient int noMistakeStreak = 0;
    private transient long marathonStartTime = 0;
    private transient int luckyProcsThisSession = 0;
    
    private transient boolean slayerQuestActive = false;
    private transient boolean steppedOffDuringSlayer = false;
    private transient int slayerStreak = 0;
    
    private transient boolean flawlessFishingActive = false;
    private transient long flawlessFishingCount = 0;
    private transient long flawlessMiningXp = 0;
    private transient long flawlessForagingXp = 0;
    private transient long flawlessFarmingXp = 0;
    private transient long flawlessCombatXp = 0;
    
    public int getCompletedTier(MilestoneType type) {
        return completedTiers.getOrDefault(type.name(), 0);
    }
    
    public void setCompletedTier(MilestoneType type, int tier) {
        completedTiers.put(type.name(), tier);
    }
    
    public long getProgress(MilestoneType type) {
        return progress.getOrDefault(type.name(), 0L);
    }
    
    public void setProgress(MilestoneType type, long value) {
        progress.put(type.name(), value);
    }
    
    public void addProgress(MilestoneType type, long amount) {
        long current = getProgress(type);
        setProgress(type, current + amount);
    }
    
    public int getTotalTokensFromMilestones() {
        return totalTokensFromMilestones;
    }
    
    public void addTokensFromMilestone(int tokens) {
        totalTokensFromMilestones += tokens;
    }
    
    public long getTotalPlaytimeMinutes() {
        return totalPlaytimeMinutes;
    }
    
    public void addPlaytimeMinutes(long minutes) {
        totalPlaytimeMinutes += minutes;
    }
    
    public int getTotalShopSpent() {
        return totalShopSpent;
    }
    
    public void addShopSpent(int tokens) {
        totalShopSpent += tokens;
    }
    
    public int getNoMistakeStreak() {
        return noMistakeStreak;
    }
    
    public void incrementNoMistakeStreak() {
        noMistakeStreak++;
    }
    
    public void resetNoMistakeStreak() {
        noMistakeStreak = 0;
    }
    
    public long getMarathonStartTime() {
        return marathonStartTime;
    }
    
    public void startMarathon() {
        marathonStartTime = System.currentTimeMillis();
    }
    
    public void resetMarathon() {
        marathonStartTime = 0;
    }
    
    public long getMarathonMinutes() {
        if (marathonStartTime == 0) return 0;
        return (System.currentTimeMillis() - marathonStartTime) / 60000;
    }
    
    public int getLuckyProcsThisSession() {
        return luckyProcsThisSession;
    }
    
    public void incrementLuckyProcs() {
        luckyProcsThisSession++;
    }
    
    public void resetSessionData() {
        noMistakeStreak = 0;
        marathonStartTime = 0;
        luckyProcsThisSession = 0;
        slayerQuestActive = false;
        steppedOffDuringSlayer = false;
        flawlessFishingActive = false;
        flawlessFishingCount = 0;
    }
    
    public boolean isSlayerQuestActive() {
        return slayerQuestActive;
    }
    
    public void startSlayerQuest() {
        slayerQuestActive = true;
        steppedOffDuringSlayer = false;
    }
    
    public void endSlayerQuest(boolean completed) {
        slayerQuestActive = false;
        if (completed && !steppedOffDuringSlayer) {
            slayerStreak++;
        } else {
            slayerStreak = 0;
        }
        steppedOffDuringSlayer = false;
    }
    
    public void onSteppedOffDuringSlayer() {
        if (slayerQuestActive) {
            steppedOffDuringSlayer = true;
        }
    }
    
    public boolean wasFlawlessSlayer() {
        return !steppedOffDuringSlayer;
    }
    
    public int getSlayerStreak() {
        return slayerStreak;
    }
    
    public void resetSlayerStreak() {
        slayerStreak = 0;
    }
    
    public boolean isFlawlessFishingActive() {
        return flawlessFishingActive;
    }
    
    public void startFlawlessFishing() {
        flawlessFishingActive = true;
    }
    
    public void incrementFlawlessFishing() {
        flawlessFishingCount++;
    }
    
    public long getFlawlessFishingCount() {
        return flawlessFishingCount;
    }
    
    public void resetFlawlessFishing() {
        flawlessFishingActive = false;
        flawlessFishingCount = 0;
    }
    
    public long getFlawlessMiningCount() {
        return flawlessMiningXp;
    }
    
    public void addFlawlessMiningXp(long xp) {
        flawlessMiningXp += xp;
    }
    
    public long getFlawlessForagingCount() {
        return flawlessForagingXp;
    }
    
    public void addFlawlessForagingXp(long xp) {
        flawlessForagingXp += xp;
    }
    
    public long getFlawlessFarmingCount() {
        return flawlessFarmingXp;
    }
    
    public void addFlawlessFarmingXp(long xp) {
        flawlessFarmingXp += xp;
    }
    
    public long getFlawlessCombatXp() {
        return flawlessCombatXp;
    }
    
    public void addFlawlessCombatXp(long xp) {
        flawlessCombatXp += xp;
    }
    
    public void resetAllFlawless() {
        flawlessFishingActive = false;
        flawlessFishingCount = 0;
        flawlessMiningXp = 0;
        flawlessForagingXp = 0;
        flawlessFarmingXp = 0;
        flawlessCombatXp = 0;
    }
}
