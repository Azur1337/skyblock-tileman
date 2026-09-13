package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.dungeon.DungeonData;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneData;
import com.azur.skyblocktileman.client.tileman.slayer.SlayerData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProfileData {

    // null = opt-in question not answered yet
    private Boolean tilemanMode;

    private int tokensEarned = 0;
    private int tokensSpent = 0;
    private int ruleBreaks = 0;
    private int maxedCollections = 0;
    private final Map<String, Long> skillXp = new HashMap<>();
    private final Map<String, Long> tokenPools = new HashMap<>();
    private final Map<String, Set<BlockCoord>> islands = new HashMap<>();
    private ShopData shop = new ShopData();
    private MilestoneData milestones = new MilestoneData();
    private DungeonData dungeons = new DungeonData();
    private SlayerData slayers = new SlayerData();

    // legacy field - kept for migration but no longer used
    private int lifetimeTokensEarned = 0;

    public Boolean getTilemanMode() {
        return tilemanMode;
    }

    public void setTilemanMode(Boolean tilemanMode) {
        this.tilemanMode = tilemanMode;
    }

    public int getTokensEarned() {
        if (tokensEarned == 0 && lifetimeTokensEarned > 0) {
            tokensEarned = lifetimeTokensEarned;
        }
        return tokensEarned;
    }

    public void addTokensEarned(int amount) {
        this.tokensEarned += amount;
    }

    public int getTokensSpent() {
        return tokensSpent;
    }

    public void setTokensSpent(int tokensSpent) {
        this.tokensSpent = Math.max(0, tokensSpent);
    }

    public void addTokensSpent(int amount) {
        this.tokensSpent = Math.max(0, this.tokensSpent + amount);
    }

    public long getTokenPool(String skill) {
        return tokenPools.getOrDefault(skill, 0L);
    }

    public void setTokenPool(String skill, long xp) {
        tokenPools.put(skill, Math.max(0, xp));
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

    public int getMaxedCollections() {
        return maxedCollections;
    }

    public void setMaxedCollections(int count) {
        this.maxedCollections = Math.max(0, count);
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

    public ShopData getShop() {
        if (shop == null) {
            shop = new ShopData();
        }
        return shop;
    }

    public MilestoneData getMilestones() {
        if (milestones == null) {
            milestones = new MilestoneData();
        }
        return milestones;
    }

    public DungeonData getDungeons() {
        if (dungeons == null) {
            dungeons = new DungeonData();
        }
        return dungeons;
    }

    public SlayerData getSlayers() {
        if (slayers == null) {
            slayers = new SlayerData();
        }
        return slayers;
    }

    public int getLifetimeTokensEarned() {
        return getTokensEarned();
    }

    public void addLifetimeTokensEarned(int amount) {
        addTokensEarned(amount);
    }
}
