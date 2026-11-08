package com.azur.skyblocktileman.slayer;

import java.util.Locale;

/**
 * The six Hypixel slayer bosses, with the combat XP needed to spawn each tier.
 * Thresholds are wiki-derived and Hypixel rebalances them, so the scoreboard
 * tier wins over a table match.
 */
public enum SlayerBoss {
    // mob keywords must list irregular plurals: "wolves" does not contain
    // "wolf" and "endermen" does not contain "enderman"
    REVENANT(
        "Revenant Horror",
        "Atoned Horror",
        5,
        new String[] { "zombie" },
        100,
        1000,
        2500,
        5000,
        10000
    ),
    TARANTULA(
        "Tarantula Broodfather",
        "Primordial Broodfather",
        5,
        new String[] { "spider" },
        250,
        650,
        1500,
        3000,
        10000
    ),
    SVEN(
        "Sven Packmaster",
        null,
        4,
        new String[] { "wolf", "wolves" },
        250,
        650,
        1500,
        3000
    ),
    VOIDGLOOM(
        "Voidgloom Seraph",
        null,
        4,
        new String[] { "enderman", "endermen" },
        2750,
        6600,
        11000,
        22000
    ),
    INFERNO(
        "Inferno Demonlord",
        null,
        4,
        new String[] { "blaze" },
        6000,
        10000,
        14000,
        28000
    ),
    RIFTSTALKER(
        "Riftstalker Bloodfiend",
        null,
        5,
        new String[] { "vampire" },
        360,
        450,
        600,
        750,
        900
    );

    private static final int MIN_TIER_TOLERANCE_XP = 50;
    private static final int TIER_TOLERANCE_DIVISOR = 4;

    private final String displayName;
    private final String highTierName;
    private final String[] mobKeywords;
    private final int maxTier;
    private final int[] combatXpToSpawn;

    SlayerBoss(
        String displayName,
        String highTierName,
        int maxTier,
        String[] mobKeywords,
        int... combatXpToSpawn
    ) {
        this.displayName = displayName;
        this.highTierName = highTierName;
        this.maxTier = maxTier;
        this.mobKeywords = mobKeywords;
        this.combatXpToSpawn = combatXpToSpawn;
    }

    public String displayName() {
        return displayName;
    }

    public int maxTier() {
        return maxTier;
    }

    public SlayerTier highestTier() {
        return SlayerTier.fromValue(maxTier);
    }

    /** Top-tier bosses rename (Revenant Horror V spawns as "Atoned Horror"). */
    public boolean matches(String text) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains(displayName.toLowerCase(Locale.ROOT))) {
            return true;
        }
        return (
            highTierName != null &&
            lower.contains(highTierName.toLowerCase(Locale.ROOT))
        );
    }

    /** True only for the renamed top-tier name (e.g. "Atoned Horror"), not the base. */
    public boolean matchesHighTier(String text) {
        return (
            highTierName != null &&
            text != null &&
            text
                .toLowerCase(Locale.ROOT)
                .contains(highTierName.toLowerCase(Locale.ROOT))
        );
    }

    /** Matches the "Slay N Combat XP worth of Zombies" requirement line. */
    public boolean matchesMob(String mobText) {
        if (mobText == null) {
            return false;
        }
        String lower = mobText.toLowerCase(Locale.ROOT);
        for (String keyword : mobKeywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /** Nearest threshold wins; a poor match returns UNKNOWN instead of guessing. */
    public SlayerTier inferTierFromCombatXp(long combatXp) {
        if (combatXp <= 0) {
            return SlayerTier.UNKNOWN;
        }

        SlayerTier best = SlayerTier.UNKNOWN;
        long bestDistance = Long.MAX_VALUE;

        for (int i = 0; i < combatXpToSpawn.length; i++) {
            long distance = Math.abs(combatXpToSpawn[i] - combatXp);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = SlayerTier.fromValue(i + 1);
            }
        }

        // within 25% of the threshold, otherwise we are guessing
        long tolerance = Math.max(
            MIN_TIER_TOLERANCE_XP,
            combatXpToSpawn[best.value() - 1] / TIER_TOLERANCE_DIVISOR
        );
        return bestDistance <= tolerance ? best : SlayerTier.UNKNOWN;
    }

    public static SlayerBoss fromText(String text) {
        for (SlayerBoss boss : values()) {
            if (boss.matches(text)) {
                return boss;
            }
        }
        return null;
    }

    public static SlayerBoss fromMobText(String mobText) {
        for (SlayerBoss boss : values()) {
            if (boss.matchesMob(mobText)) {
                return boss;
            }
        }
        return null;
    }
}
