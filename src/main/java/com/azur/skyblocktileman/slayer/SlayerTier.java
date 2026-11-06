package com.azur.skyblocktileman.slayer;

/** Slayer quest tier. UNKNOWN means there is no solid reading yet. */
public enum SlayerTier {
    UNKNOWN(0),
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5);

    private final int value;

    SlayerTier(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public boolean known() {
        return this != UNKNOWN;
    }

    public boolean atLeast(SlayerTier other) {
        return known() && other.known() && value >= other.value;
    }

    public static SlayerTier fromValue(int value) {
        for (SlayerTier tier : values()) {
            if (tier.value == value) {
                return tier;
            }
        }
        return UNKNOWN;
    }
}
