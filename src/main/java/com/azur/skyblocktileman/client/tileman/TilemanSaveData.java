package com.azur.skyblocktileman.client.tileman;

import java.util.HashMap;
import java.util.Map;

// Root save object, profile id to profile data
public class TilemanSaveData {

    private final Map<String, ProfileData> profiles = new HashMap<>();

    // Last known active profile/island so we know what to show on startup
    private String lastActiveProfileId = "unknown";
    private String lastActiveIsland = "unknown";

    public Map<String, ProfileData> getProfiles() {
        return profiles;
    }

    public ProfileData getOrCreateProfile(String profileId) {
        return profiles.computeIfAbsent(profileId, id -> new ProfileData());
    }

    public String getLastActiveProfileId() {
        return lastActiveProfileId;
    }

    public void setLastActiveProfileId(String lastActiveProfileId) {
        this.lastActiveProfileId = lastActiveProfileId;
    }

    public String getLastActiveIsland() {
        return lastActiveIsland;
    }

    public void setLastActiveIsland(String lastActiveIsland) {
        this.lastActiveIsland = lastActiveIsland;
    }
}
