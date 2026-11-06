package com.azur.skyblocktileman.slayer;

/** Why a slayer quest ended in failure. */
public enum FailReason {
    /** "You died!" line, or a "You were killed by" line just before the failure. */
    DEATH,

    /** "You failed to slay the boss within the time limit!" */
    TIME_LIMIT,

    /** Failed, but no reason line arrived within the grace window. */
    UNKNOWN,

    /**
     * The quest vanished from the scoreboard with no failure message. Leaving
     * the boss lobby drops the quest silently.
     */
    ABANDONED,
}
