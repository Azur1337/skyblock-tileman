package com.azur.skyblocktileman.slayer;

/**
 * How the detector arrived at a tier. Consumers that gate rewards should insist
 * on SCOREBOARD: the XP table is derived from wiki data that Hypixel rebalances.
 */
public enum TierSource {

    /** Not resolved yet. */
    NONE,

    /** Inferred from the "Slay N Combat XP" requirement line. */
    XP_TABLE,

    /** Read from the scoreboard, which prints the boss name and roman numeral. */
    SCOREBOARD;

    public boolean strongerThan(TierSource other) {
        return ordinal() > other.ordinal();
    }
}
