package com.example.client.tileman;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * All persisted Tileman state for a single Hypixel Skyblock profile
 * (Skyblock profiles have independent Skill XP, so tokens and total XP
 * are tracked per-profile, not per-player).
 */
public class ProfileData {

    private int tokens = 0;
    private double totalSkillXp = 0.0;

    // XP accumulated toward the *next* token that hasn't been converted yet.
    // Kept separate from totalSkillXp so we never lose fractional progress
    // between token thresholds (which scale up as totalSkillXp grows).
    private double bankedXp = 0.0;

    // Island name -> set of unlocked block coordinates on that island.
    private final Map<String, Set<BlockCoord>> islands = new HashMap<>();

    public int getTokens() {
        return tokens;
    }

    public void setTokens(int tokens) {
        this.tokens = Math.max(0, tokens);
    }

    public void addTokens(int amount) {
        this.tokens = Math.max(0, this.tokens + amount);
    }

    public double getTotalSkillXp() {
        return totalSkillXp;
    }

    public void setTotalSkillXp(double totalSkillXp) {
        this.totalSkillXp = Math.max(0, totalSkillXp);
    }

    public void addSkillXp(double delta) {
        this.totalSkillXp = Math.max(0, this.totalSkillXp + delta);
    }

    public double getBankedXp() {
        return bankedXp;
    }

    public void setBankedXp(double bankedXp) {
        this.bankedXp = Math.max(0, bankedXp);
    }

    public void addBankedXp(double delta) {
        this.bankedXp = Math.max(0, this.bankedXp + delta);
    }

    public Map<String, Set<BlockCoord>> getIslands() {
        return islands;
    }

    public Set<BlockCoord> getUnlockedBlocks(String island) {
        return islands.computeIfAbsent(island, key -> new HashSet<>());
    }
}
