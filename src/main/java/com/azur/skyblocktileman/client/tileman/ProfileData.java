package com.azur.skyblocktileman.client.tileman;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProfileData {

    private int tokensSpent = 0;
    private int ruleBreaks = 0;
    private final Map<String, Long> skillXp = new HashMap<>();
    private final Map<String, Set<BlockCoord>> islands = new HashMap<>();

    public int getTokensSpent() {
        return tokensSpent;
    }

    public void setTokensSpent(int tokensSpent) {
        this.tokensSpent = Math.max(0, tokensSpent);
    }

    public void addTokensSpent(int amount) {
        this.tokensSpent = Math.max(0, this.tokensSpent + amount);
    }

    public long getSkillXp(String skill) {
        return skillXp.getOrDefault(skill, 0L);
    }

    public void setSkillXp(String skill, long xp) {
        skillXp.put(skill, Math.max(0, xp));
    }

    public Map<String, Long> getAllSkillXp() {
        return skillXp;
    }

    public long getTotalSkillXp() {
        return skillXp.values().stream().mapToLong(Long::longValue).sum();
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

    public int getTotalUnlockedBlocks() {
        return islands.values().stream().mapToInt(Set::size).sum();
    }

    public int getFreeBlocksCount() {
        return islands.size();
    }
}
