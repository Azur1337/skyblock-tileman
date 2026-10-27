package com.azur.skyblocktileman.client.tileman.slayer;

import com.azur.skyblocktileman.slayer.SlayerBoss;
import com.azur.skyblocktileman.slayer.SlayerTier;
import com.google.gson.annotations.Expose;
import java.util.HashMap;
import java.util.Map;

public class SlayerData {

    private static final SlayerStats NONE = new SlayerStats();

    @Expose
    private Map<String, SlayerStats> stats = new HashMap<>();

    @Expose
    private int currentFlawlessStreak = 0;

    @Expose
    private int bestFlawlessStreak = 0;

    @Expose
    private int totalFlawlessT4Plus = 0;

    /** Read-only: milestone progress polls this, so it must not create entries. */
    public SlayerStats getStats(SlayerBoss boss) {
        if (boss == null) {
            return NONE;
        }
        SlayerStats s = stats.get(boss.name());
        return s == null ? NONE : s;
    }

    private SlayerStats getOrCreateStats(SlayerBoss boss) {
        return stats.computeIfAbsent(boss.name(), k -> new SlayerStats());
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

    public void recordFlawlessCompletion(SlayerBoss boss, SlayerTier tier) {
        if (boss == null) {
            return;
        }
        SlayerStats s = getOrCreateStats(boss);
        s.totalCompletions++;

        if (tier.atLeast(SlayerTier.FOUR)) {
            s.t4PlusKills++;
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

    public void recordTaintedCompletion(SlayerBoss boss, SlayerTier tier) {
        if (boss == null) {
            return;
        }
        SlayerStats s = getOrCreateStats(boss);
        s.totalCompletions++;

        // tainted kills still count toward kill milestones, just not flawless ones
        if (tier.atLeast(SlayerTier.FOUR)) {
            s.t4PlusKills++;
            currentFlawlessStreak = 0;
        }
    }

    public static class SlayerStats {

        @Expose
        public int totalCompletions = 0;

        @Expose
        public int t4PlusKills = 0;

        @Expose
        public int flawlessT4Plus = 0;

        @Expose
        public int bestStreak = 0;
    }
}
