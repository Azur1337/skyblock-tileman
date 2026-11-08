package com.azur.skyblocktileman.client.tileman;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ShopData {

    public static final int EFFICIENT_SCALING_MAX = 10;

    // One perk: Lucky Unlock. Higher levels refund the token, top tiers also gain one.
    public static final int LUCKY_MAX_LEVEL = 6;
    private static final int[] LUCKY_REFUND_PCT = { 5, 10, 15, 20, 20, 20 };
    private static final int[] LUCKY_GAIN_PCT = { 0, 0, 0, 0, 5, 10 };

    private int luckyUnlockLevel = 0;
    private int efficientScalingLevel = 0;
    private final Map<String, Integer> skillAffinityLevels = new HashMap<>();
    private int frenzyDurationLevel = 0;
    private int frenzyPowerLevel = 0;

    private boolean remoteUnlockPending = false;
    private long xpFrenzyExpiresAtMs = 0;
    private int xpFrenzyActivePower = 0;

    private static final Random RANDOM = new Random();

    public int getLuckyUnlockLevel() {
        return Math.min(luckyUnlockLevel, LUCKY_MAX_LEVEL);
    }

    public void setLuckyUnlockLevel(int level) {
        this.luckyUnlockLevel = Math.min(LUCKY_MAX_LEVEL, Math.max(0, level));
    }

    public int getLuckyRefundPercent() {
        int level = getLuckyUnlockLevel();
        return level <= 0 ? 0 : LUCKY_REFUND_PCT[level - 1];
    }

    public int getLuckyGainPercent() {
        int level = getLuckyUnlockLevel();
        return level <= 0 ? 0 : LUCKY_GAIN_PCT[level - 1];
    }

    public boolean rollLuckyRefund() {
        int pct = getLuckyRefundPercent();
        return pct > 0 && RANDOM.nextInt(100) < pct;
    }

    public boolean rollLuckyGain() {
        int pct = getLuckyGainPercent();
        return pct > 0 && RANDOM.nextInt(100) < pct;
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
        return Math.max(0, xpFrenzyExpiresAtMs - System.currentTimeMillis());
    }

    public int getXpFrenzyActivePower() {
        return xpFrenzyActivePower;
    }

    public boolean isXpFrenzyActive() {
        return System.currentTimeMillis() < xpFrenzyExpiresAtMs;
    }

    public void activateXpFrenzy() {
        int durationMinutes = 15 + frenzyDurationLevel * 5;
        int power = 50 + frenzyPowerLevel * 10;
        // absolute expiry, so lag spikes and logouts cannot freeze or stretch the timer
        this.xpFrenzyExpiresAtMs =
            System.currentTimeMillis() + durationMinutes * 60_000L;
        this.xpFrenzyActivePower = power;
    }

    public void tickFrenzy() {
        if (
            xpFrenzyExpiresAtMs > 0 &&
            System.currentTimeMillis() >= xpFrenzyExpiresAtMs
        ) {
            xpFrenzyExpiresAtMs = 0;
            xpFrenzyActivePower = 0;
        }
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

    // price climbs steeply so the top Lucky tiers stay a meaningful spend
    public static int getLuckyUnlockPrice(int currentLevel) {
        return Balance.getInt(
            "shop.lucky_unlock.price." + currentLevel,
            15 + currentLevel * currentLevel * 10,
            1
        );
    }

    public static int getEfficientScalingPrice(int currentLevel) {
        return Balance.getInt(
            "shop.efficient_scaling.price." + currentLevel,
            20 + currentLevel * 10,
            1
        );
    }

    public static int getSkillAffinityPrice(int currentLevel) {
        return Balance.getInt(
            "shop.skill_affinity.price." + currentLevel,
            8 + currentLevel * 3,
            1
        );
    }

    public static int getFrenzyDurationPrice(int currentLevel) {
        return Balance.getInt(
            "shop.frenzy_duration.price." + currentLevel,
            20 + currentLevel * 15,
            1
        );
    }

    public static int getFrenzyPowerPrice(int currentLevel) {
        return Balance.getInt(
            "shop.frenzy_power.price." + currentLevel,
            25 + currentLevel * 20,
            1
        );
    }

    public static int getRemoteUnlockPrice() {
        return Balance.getInt("shop.remote_unlock.price", 5, 1);
    }

    public static int getXpFrenzyPrice() {
        return Balance.getInt("shop.xp_frenzy.price", 15, 1);
    }
}
