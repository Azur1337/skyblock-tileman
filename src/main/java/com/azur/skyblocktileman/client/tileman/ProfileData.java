package com.azur.skyblocktileman.client.tileman;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Tileman data for a single Skyblock profile
public class ProfileData {

    private int tokens = 0;
    private double totalSkillXp = 0.0;

    // XP saved up toward the next token, since the cost scales over time
    private double bankedXp = 0.0;

    private int ruleBreaks = 0;

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

    public int getRuleBreaks() {
        return ruleBreaks;
    }

    public void addRuleBreak() {
        ruleBreaks++;
    }

    public Map<String, Set<BlockCoord>> getIslands() {
        return islands;
    }

    public Set<BlockCoord> getUnlockedBlocks(String island) {
        return islands.computeIfAbsent(island, key -> new HashSet<>());
    }
}
