package com.azur.skyblocktileman.client.tileman.slayer;

public enum SlayerType {
    REVENANT("Revenant Horror", "Zombie", 5),
    TARANTULA("Tarantula Broodfather", "Spider", 4),
    SVEN("Sven Packmaster", "Wolf", 4),
    VOIDGLOOM("Voidgloom Seraph", "Enderman", 4),
    INFERNO("Inferno Demonlord", "Blaze", 5),
    RIFTSTALKER("Riftstalker Bloodfiend", "Vampire", 5);

    private final String displayName;
    private final String shortName;
    private final int maxTier;

    SlayerType(String displayName, String shortName, int maxTier) {
        this.displayName = displayName;
        this.shortName = shortName;
        this.maxTier = maxTier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortName() {
        return shortName;
    }

    public int getMaxTier() {
        return maxTier;
    }

    public static SlayerType fromDisplayName(String text) {
        if (text == null) return null;
        // Locale.ROOT: a Turkish default locale lowercases I to a dotless i and breaks detection
        String lower = text.toLowerCase(java.util.Locale.ROOT);

        if (lower.contains("revenant") || lower.contains("zombie")) {
            return REVENANT;
        } else if (lower.contains("tarantula") || lower.contains("spider")) {
            return TARANTULA;
        } else if (lower.contains("sven") || lower.contains("wolf")) {
            return SVEN;
        } else if (lower.contains("voidgloom") || lower.contains("ender")) {
            // "ender" stem: Hypixel writes the mob plural, so "enderman" alone never matched
            return VOIDGLOOM;
        } else if (
            lower.contains("inferno") ||
            lower.contains("demonlord") ||
            lower.contains("blaze")
        ) {
            return INFERNO;
        } else if (
            lower.contains("riftstalker") ||
            lower.contains("bloodfiend") ||
            lower.contains("vampire")
        ) {
            return RIFTSTALKER;
        }

        return null;
    }

    public static int parseTier(String text) {
        if (text == null) return 0;

        text = text.trim();
        if (
            text.endsWith(" I") &&
            !text.endsWith(" II") &&
            !text.endsWith(" III") &&
            !text.endsWith(" IV")
        ) {
            return 1;
        } else if (text.endsWith(" II") && !text.endsWith(" III")) {
            return 2;
        } else if (text.endsWith(" III")) {
            return 3;
        } else if (text.endsWith(" IV")) {
            return 4;
        } else if (text.endsWith(" V")) {
            return 5;
        }

        if (
            text.contains(" V ") || text.contains(" V]") || text.contains("(V)")
        ) {
            return 5;
        } else if (
            text.contains(" IV ") ||
            text.contains(" IV]") ||
            text.contains("(IV)")
        ) {
            return 4;
        } else if (
            text.contains(" III ") ||
            text.contains(" III]") ||
            text.contains("(III)")
        ) {
            return 3;
        } else if (
            text.contains(" II ") ||
            text.contains(" II]") ||
            text.contains("(II)")
        ) {
            return 2;
        } else if (
            text.contains(" I ") || text.contains(" I]") || text.contains("(I)")
        ) {
            return 1;
        }

        return 0;
    }
}
