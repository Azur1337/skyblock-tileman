package com.azur.skyblocktileman.client.tileman;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ShopData {

    public static final int EFFICIENT_SCALING_MAX = 10;

    // Multi Unlock base is capped at 10 tiles; Lucky can multiply a click past that
    public static final int MULTI_UNLOCK_MAX_LEVEL = 9;
    public static final int MAX_TILES_PER_UNLOCK = 10;

    private int multiUnlockLevel = 0;
    private int luckyUnlockLevel = 0;
    private int efficientScalingLevel = 0;
    private final Map<String, Integer> skillAffinityLevels = new HashMap<>();
    private int frenzyDurationLevel = 0;
    private int frenzyPowerLevel = 0;

    private boolean remoteUnlockPending = false;
    private long xpFrenzyRemainingMs = 0;
    private int xpFrenzyActivePower = 0;

    private int totalSpent = 0;

    private static final Random RANDOM = new Random();

    public int getMultiUnlockLevel() {
        // clamp so legacy saves that outran the old uncapped shop are still capped
        return Math.min(multiUnlockLevel, MULTI_UNLOCK_MAX_LEVEL);
    }

    public void setMultiUnlockLevel(int level) {
        this.multiUnlockLevel = Math.min(
            MULTI_UNLOCK_MAX_LEVEL,
            Math.max(0, level)
        );
    }

    public int getLuckyUnlockLevel() {
        return luckyUnlockLevel;
    }

    public void setLuckyUnlockLevel(int level) {
        this.luckyUnlockLevel = Math.max(0, level);
    }

    public int getEfficientScalingLevel() {
        return efficientScalingLevel;
    }

    public void setEfficientScalingLevel(int level) {
        this.efficientScalingLevel = Math.min(
            EFFICIENT_SCALING_MAX,
            Math.max(0, level)
        );
    }

    public int getSkillAffinityLevel(String skill) {
        return skillAffinityLevels.getOrDefault(skill, 0);
    }

    public void setSkillAffinityLevel(String skill, int level) {
        skillAffinityLevels.put(skill, Math.max(0, level));
    }

    public int getFrenzyDurationLevel() {
        return frenzyDurationLevel;
    }

    public void setFrenzyDurationLevel(int level) {
        this.frenzyDurationLevel = Math.max(0, level);
    }

    public int getFrenzyPowerLevel() {
        return frenzyPowerLevel;
    }

    public void setFrenzyPowerLevel(int level) {
        this.frenzyPowerLevel = Math.max(0, level);
    }

    public boolean isRemoteUnlockPending() {
        return remoteUnlockPending;
    }

    public void setRemoteUnlockPending(boolean pending) {
        this.remoteUnlockPending = pending;
    }

    public void consumeRemoteUnlock() {
        this.remoteUnlockPending = false;
    }

    public long getXpFrenzyRemainingMs() {
        return xpFrenzyRemainingMs;
    }

    public int getXpFrenzyActivePower() {
        return xpFrenzyActivePower;
    }

    public boolean isXpFrenzyActive() {
        return xpFrenzyRemainingMs > 0;
    }

    public void activateXpFrenzy() {
        int durationMinutes = 15 + frenzyDurationLevel * 5;
        int power = 50 + frenzyPowerLevel * 10;
        this.xpFrenzyRemainingMs = durationMinutes * 60 * 1000L;
        this.xpFrenzyActivePower = power;
    }

    public void tickFrenzy(long deltaMs) {
        if (xpFrenzyRemainingMs > 0) {
            xpFrenzyRemainingMs = Math.max(0, xpFrenzyRemainingMs - deltaMs);
        }
    }

    public void addSpent(int amount) {
        this.totalSpent += amount;
    }

    public int getBaseTilesPerUnlock() {
        return 1 + getMultiUnlockLevel();
    }

    public int getLuckyPercent() {
        return luckyUnlockLevel * 5;
    }

    public int calculateTilesToUnlock() {
        int baseTiles = getBaseTilesPerUnlock();
        int luckyPercent = getLuckyPercent();

        int multiplier = 1;
        int remainingLuck = luckyPercent;

        while (remainingLuck >= 100) {
            multiplier++;
            remainingLuck -= 100;
        }

        if (remainingLuck > 0 && RANDOM.nextInt(100) < remainingLuck) {
            multiplier++;
        }

        return baseTiles * multiplier;
    }

    public boolean didLuckyProc(int baseTiles, int actualTiles) {
        return actualTiles > baseTiles;
    }

    // diminishing toward 100 percent: (1 - 0.9^level) * 100
    public static int getSkillAffinityBonusPercent(int level) {
        if (level <= 0) return 0;
        return (int) Math.round((1.0 - Math.pow(0.9, level)) * 100);
    }

    public double getSkillAffinityMultiplier(String skill) {
        return (
            1.0 +
            getSkillAffinityBonusPercent(getSkillAffinityLevel(skill)) / 100.0
        );
    }

    // percent reduction of the XP cost per token, 2% per level
    public int getEfficientScalingBonus() {
        return efficientScalingLevel * 2;
    }

    public static int getMultiUnlockPrice(int currentLevel) {
        return 25 + currentLevel * 15;
    }

    // total tiles that must already be unlocked to buy the given level:
    // 25, 75, 150, 250, 375, 525, 700, 900, 1125 for levels 1..9
    public static int getMultiUnlockTilesRequired(int targetLevel) {
        return 25 * targetLevel * (targetLevel + 1) / 2;
    }

    public static int getLuckyUnlockPrice(int currentLevel) {
        return 10 + currentLevel * 5;
    }

    public static int getEfficientScalingPrice(int currentLevel) {
        return 20 + currentLevel * 10;
    }

    public static int getSkillAffinityPrice(int currentLevel) {
        return 8 + currentLevel * 3;
    }

    public static int getFrenzyDurationPrice(int currentLevel) {
        return 20 + currentLevel * 15;
    }

    public static int getFrenzyPowerPrice(int currentLevel) {
        return 25 + currentLevel * 20;
    }

    public static int getRemoteUnlockPrice() {
        return 5;
    }

    public static int getXpFrenzyPrice() {
        return 15;
    }
}
