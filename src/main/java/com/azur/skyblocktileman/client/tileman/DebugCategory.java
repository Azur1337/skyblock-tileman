package com.azur.skyblocktileman.client.tileman;

public enum DebugCategory {
    ACTION_BAR("Action Bar", "XP parsing from action bar"),
    ISLAND("Island Detection", "Island changes and /locraw responses"),
    PROFILE("Profile", "Profile switches and API responses"),
    TOKENS("Tokens", "Token earning and spending"),
    BLOCKS("Blocks", "Block unlocking and first block mode"),
    SLAYER("Slayer", "Slayer quest detection and tracking"),
    DUNGEON("Dungeon", "Dungeon floor detection and blocking"),
    ALL("All", "All debug messages");

    private final String displayName;
    private final String description;

    DebugCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
