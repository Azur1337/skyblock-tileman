package com.azur.skyblocktileman.slayer;

/** How the detector arrived at a tier. Only SCOREBOARD is safe for gating rewards. */
public enum TierSource {
    /** Not resolved yet. */
    NONE,

    /** Inferred from the "Slay N Combat XP" requirement line. */
    XP_TABLE,

    /** Read from the boss spawn line, which prints the tier ("Revenant Horror III"). */
    SPAWN_MESSAGE,

    /** Read from the scoreboard, which prints the boss name and roman numeral. */
    SCOREBOARD;

    public boolean strongerThan(TierSource other) {
        return ordinal() > other.ordinal();
    }
}
