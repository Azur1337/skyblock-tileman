package com.azur.skyblocktileman.slayer;

/**
 * Immutable view of the quest the detector currently believes is active.
 * Read this from an event handler instead of accumulating state yourself.
 */
public record SlayerQuest(
    SlayerBoss boss,
    SlayerTier tier,
    TierSource tierSource,
    SlayerPhase phase,
    long currentCombatXp,
    long requiredCombatXp,
    boolean bossSpawned,
    boolean bossSlain
) {

    public boolean identified() {
        return boss != null && tier.known();
    }

    public boolean tierFromScoreboard() {
        return tierSource == TierSource.SCOREBOARD;
    }

    public double progressFraction() {
        if (requiredCombatXp <= 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) currentCombatXp / requiredCombatXp);
    }
}
