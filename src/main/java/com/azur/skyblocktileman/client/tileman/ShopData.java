package com.azur.skyblocktileman.client.tileman;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ShopData {

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
        return multiUnlockLevel;
    }

    public void setMultiUnlockLevel(int level) {
        this.multiUnlockLevel = Math.max(0, level);
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
        this.efficientScalingLevel = Math.min(10, Math.max(0, level));
    }

    public int getSkillAffinityLevel(String skill) {
        return skillAffinityLevels.getOrDefault(skill, 0);
    }

    public void setSkillAffinityLevel(String skill, int level) {
        skillAffinityLevels.put(skill, Math.max(0, level));
    }

    public Map<String, Integer> getAllSkillAffinityLevels() {
        return skillAffinityLevels;
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

    public void setXpFrenzyRemainingMs(long ms) {
        this.xpFrenzyRemainingMs = Math.max(0, ms);
    }

    public int getXpFrenzyActivePower() {
        return xpFrenzyActivePower;
    }

    public boolean isXpFrenzyActive() {
        return xpFrenzyRemainingMs > 0;
    }

    public void activateXpFrenzy() {
        int durationMinutes = 15 + (frenzyDurationLevel * 5);
        int power = 50 + (frenzyPowerLevel * 10);
        this.xpFrenzyRemainingMs = durationMinutes * 60 * 1000L;
        this.xpFrenzyActivePower = power;
    }

    public void tickFrenzy(long deltaMs) {
        if (xpFrenzyRemainingMs > 0) {
            xpFrenzyRemainingMs = Math.max(0, xpFrenzyRemainingMs - deltaMs);
        }
    }

    public int getTotalSpent() {
        return totalSpent;
    }

    public void addSpent(int amount) {
        this.totalSpent += amount;
    }

    public int getBaseTilesPerUnlock() {
        return 1 + multiUnlockLevel;
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

    public double getSkillXpMultiplier(String skill) {
        int level = getSkillAffinityLevel(skill);
        double base = 1.0;
        if (level > 0) {
            base = 1.0 + (1.0 - Math.pow(0.9, level));
        }
        if (isXpFrenzyActive()) {
            base *= 1.0 + (xpFrenzyActivePower / 100.0);
        }
        return base;
    }

    public int getEfficientScalingBonus() {
        return efficientScalingLevel * 100_000;
    }

    public static int getMultiUnlockPrice(int currentLevel) {
        return 25 + (currentLevel * 15);
    }

    public static int getLuckyUnlockPrice(int currentLevel) {
        return 10 + (currentLevel * 5);
    }

    public static int getEfficientScalingPrice(int currentLevel) {
        return 20 + (currentLevel * 10);
    }

    public static int getSkillAffinityPrice(int currentLevel) {
        return 8 + (currentLevel * 3);
    }

    public static int getFrenzyDurationPrice(int currentLevel) {
        return 20 + (currentLevel * 15);
    }

    public static int getFrenzyPowerPrice(int currentLevel) {
        return 25 + (currentLevel * 20);
    }

    public static int getRemoteUnlockPrice() {
        return 5;
    }

    public static int getXpFrenzyPrice() {
        return 15;
    }
}
