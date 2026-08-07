package com.azur.skyblocktileman.client.tileman.dungeon;

/**
 * Enum for all dungeon floors with unlock costs.
 * 
 * Normal floors: Floor × 100 tokens
 * Master Mode: Floor × 1000 tokens
 * 
 * First 10 clears of each floor give enough tokens to unlock the next floor.
 * So F1 (100 cost) → 10 clears × 20 tokens = 200 tokens → unlocks F2 (200 cost)
 */
public enum DungeonFloor {
    // Normal floors
    F1("F1", "Floor 1", 100, 20, false),
    F2("F2", "Floor 2", 200, 30, false),
    F3("F3", "Floor 3", 300, 40, false),
    F4("F4", "Floor 4", 400, 50, false),
    F5("F5", "Floor 5", 500, 60, false),
    F6("F6", "Floor 6", 600, 70, false),
    F7("F7", "Floor 7", 700, 100, false),
    
    // Master Mode floors
    M1("M1", "Master Mode 1", 1000, 200, true),
    M2("M2", "Master Mode 2", 2000, 300, true),
    M3("M3", "Master Mode 3", 3000, 400, true),
    M4("M4", "Master Mode 4", 4000, 500, true),
    M5("M5", "Master Mode 5", 5000, 600, true),
    M6("M6", "Master Mode 6", 6000, 700, true),
    M7("M7", "Master Mode 7", 7000, 0, true); // last floor, no next to unlock

    private final String id;
    private final String displayName;
    private final int unlockCost;
    private final int baseReward; // tokens per clear for first 10 clears
    private final boolean masterMode;

    DungeonFloor(String id, String displayName, int unlockCost, int baseReward, boolean masterMode) {
        this.id = id;
        this.displayName = displayName;
        this.unlockCost = unlockCost;
        this.baseReward = baseReward;
        this.masterMode = masterMode;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getUnlockCost() {
        return unlockCost;
    }

    public int getBaseReward() {
        return baseReward;
    }

    public boolean isMasterMode() {
        return masterMode;
    }

    /**
     * Get the floor number (1-7).
     */
    public int getFloorNumber() {
        return switch (this) {
            case F1, M1 -> 1;
            case F2, M2 -> 2;
            case F3, M3 -> 3;
            case F4, M4 -> 4;
            case F5, M5 -> 5;
            case F6, M6 -> 6;
            case F7, M7 -> 7;
        };
    }

    /**
     * Calculate token reward for a completion based on how many clears the player has.
     * 
     * First 10 clears: full base reward
     * Clears 11-50: 10% of base reward
     * After 50: 0
     */
    public int getRewardForCompletion(int currentCompletions) {
        if (currentCompletions < 10) {
            return baseReward;
        } else if (currentCompletions < 50) {
            return Math.max(1, baseReward / 10);
        }
        return 0;
    }

    /**
     * Get the next floor in progression.
     */
    public DungeonFloor getNextFloor() {
        return switch (this) {
            case F1 -> F2;
            case F2 -> F3;
            case F3 -> F4;
            case F4 -> F5;
            case F5 -> F6;
            case F6 -> F7;
            case F7 -> null; // last normal floor
            case M1 -> M2;
            case M2 -> M3;
            case M3 -> M4;
            case M4 -> M5;
            case M5 -> M6;
            case M6 -> M7;
            case M7 -> null; // last master floor
        };
    }

    /**
     * Get floor by ID string (e.g., "F1", "M3").
     */
    public static DungeonFloor fromId(String id) {
        for (DungeonFloor floor : values()) {
            if (floor.id.equalsIgnoreCase(id)) {
                return floor;
            }
        }
        return null;
    }

    /**
     * Get floor from floor number and master mode flag.
     */
    public static DungeonFloor fromNumber(int floorNumber, boolean masterMode) {
        for (DungeonFloor floor : values()) {
            if (floor.getFloorNumber() == floorNumber && floor.masterMode == masterMode) {
                return floor;
            }
        }
        return null;
    }

    /**
     * Get all normal floors.
     */
    public static DungeonFloor[] getNormalFloors() {
        return new DungeonFloor[] { F1, F2, F3, F4, F5, F6, F7 };
    }

    /**
     * Get all master mode floors.
     */
    public static DungeonFloor[] getMasterModeFloors() {
        return new DungeonFloor[] { M1, M2, M3, M4, M5, M6, M7 };
    }
}
