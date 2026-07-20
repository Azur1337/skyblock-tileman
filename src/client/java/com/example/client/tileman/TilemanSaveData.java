package com.example.client.tileman;

import java.util.HashMap;
import java.util.Map;

/**
 * Root object persisted to disk as JSON.
 * Maps Hypixel Skyblock Profile ID -> that profile's Tileman data.
 */
public class TilemanSaveData {

	private final Map<String, ProfileData> profiles = new HashMap<>();

	public Map<String, ProfileData> getProfiles() {
		return profiles;
	}

	public ProfileData getOrCreateProfile(String profileId) {
		return profiles.computeIfAbsent(profileId, id -> new ProfileData());
	}
}
