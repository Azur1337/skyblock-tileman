package com.azur.skyblocktileman.client.tileman.milestone;

public enum MilestoneCategory {
    PROGRESSION("Progression"),
    SKILL("Skills"),
    SLAYER("Slayer"),
    CHALLENGE("Challenges"),
    SECRET("Secret");

    private final String displayName;

    MilestoneCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
