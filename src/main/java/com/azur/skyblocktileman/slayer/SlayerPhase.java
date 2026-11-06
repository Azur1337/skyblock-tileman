package com.azur.skyblocktileman.slayer;

/** Quest phase, read from the third scoreboard line under the boss name. */
public enum SlayerPhase {
    /** Not known. */
    NONE,

    /** "(10/100) Combat XP", killing mobs to summon the boss. */
    GRINDING,

    /** "Slay the boss!", the boss is up and must be killed. */
    BOSS_AVAILABLE,

    /** "Boss slain!", the kill is done and the quest awaits turn-in at Maddox. */
    BOSS_SLAIN,
}
