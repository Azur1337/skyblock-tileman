package com.azur.skyblocktileman.client.tileman;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.azur.skyblocktileman.client.tileman.dungeon.DungeonData;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneData;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneTracker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TilemanState {

    private static final Logger LOGGER = LoggerFactory.getLogger("TilemanState");

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    private static final Path SAVE_DIR = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("tileman");
    private static final Path SAVE_FILE = SAVE_DIR.resolve("tileman_data.json");

    private static TilemanState instance;

    /**
     * Token cost tiers - each entry is [tokenThreshold, xpPerToken]
     * Cost applies from that threshold until the next one
     * 
     * Balanced for ~80k tokens from main 5 skills at max level:
     * - Combat 60: 111.6M XP
     * - Mining 60: 111.6M XP  
     * - Farming 60: 111.6M XP
     * - Foraging 54: 74.1M XP
     * - Fishing 50: 55.1M XP
     * Total: ~464M XP
     */
    private static final int[][] TOKEN_COST_TIERS = {
        {0,      1000},   // tokens 0-999: 1k XP each (1M total)
        {1000,   2000},   // tokens 1k-5k: 2k XP each (8M total)
        {5000,   4000},   // tokens 5k-20k: 4k XP each (60M total)
        {20000,  6000},   // tokens 20k-50k: 6k XP each (180M total)
        {50000,  7000},   // tokens 50k-80k: 7k XP each (210M total)
        {80000,  10000},  // tokens 80k+: 10k XP each (slower beyond target)
    };

    /**
     * Skyblock skill XP thresholds for levels 1-60.
     * Each entry is the TOTAL XP needed to reach that level.
     */
    private static final long[] SKILL_XP_THRESHOLDS = {
        0,        // level 0
        50,       // level 1
        175,      // level 2
        375,      // level 3
        675,      // level 4
        1175,     // level 5
        1925,     // level 6
        2925,     // level 7
        4425,     // level 8
        6425,     // level 9
        9925,     // level 10
        14925,    // level 11
        22425,    // level 12
        32425,    // level 13
        47425,    // level 14
        67425,    // level 15
        97425,    // level 16
        147425,   // level 17
        222425,   // level 18
        322425,   // level 19
        522425,   // level 20
        822425,   // level 21
        1222425,  // level 22
        1722425,  // level 23
        2322425,  // level 24
        3022425,  // level 25
        3822425,  // level 26
        4722425,  // level 27
        5722425,  // level 28
        6822425,  // level 29
        8022425,  // level 30
        9322425,  // level 31
        10722425, // level 32
        12222425, // level 33
        13822425, // level 34
        15522425, // level 35
        17322425, // level 36
        19222425, // level 37
        21222425, // level 38
        23322425, // level 39
        25522425, // level 40
        27822425, // level 41
        30222425, // level 42
        32722425, // level 43
        35322425, // level 44
        38072425, // level 45
        40972425, // level 46
        44072425, // level 47
        47472425, // level 48
        51172425, // level 49
        55172425, // level 50
        59472425, // level 51
        64072425, // level 52
        68972425, // level 53
        74172425, // level 54
        79672425, // level 55
        85472425, // level 56
        91572425, // level 57
        97972425, // level 58
        104672425, // level 59
        111672425  // level 60
    };

    private TilemanSaveData data = new TilemanSaveData();

    private String activeProfileId = "unknown";
    private String activeIsland = "unknown";
    
    private long xpTowardNextToken = 0;

    private TilemanState() {
        load();
    }

    public static TilemanState getInstance() {
        if (instance == null) {
            instance = new TilemanState();
        }
        return instance;
    }

    public void load() {
        try {
            if (Files.exists(SAVE_FILE)) {
                String json = Files.readString(SAVE_FILE);
                TilemanSaveData loaded = GSON.fromJson(json, TilemanSaveData.class);
                data = loaded != null ? loaded : new TilemanSaveData();
                LOGGER.info("Loaded Tileman data from {}", SAVE_FILE);
            } else {
                data = new TilemanSaveData();
                save();
                LOGGER.info("Created new Tileman data file at {}", SAVE_FILE);
            }
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("Failed to load Tileman data, starting with a blank state.", e);
            data = new TilemanSaveData();
        }

        activeProfileId = data.getLastActiveProfileId();
        activeIsland = data.getLastActiveIsland();
        xpTowardNextToken = activeProfile().getXpTowardNextToken();
    }

    public void save() {
        try {
            Files.createDirectories(SAVE_DIR);
            Files.writeString(SAVE_FILE, GSON.toJson(data));
        } catch (IOException e) {
            LOGGER.error("Failed to save Tileman data.", e);
        }
    }

    public void setActiveProfile(String profileId) {
        this.activeProfileId = profileId == null ? "unknown" : profileId;
        data.setLastActiveProfileId(this.activeProfileId);
        xpTowardNextToken = activeProfile().getXpTowardNextToken();
        save();
    }

    public void setActiveIsland(String island) {
        this.activeIsland = island == null ? "unknown" : island;
        data.setLastActiveIsland(this.activeIsland);
        save();
    }

    public String getActiveProfileId() {
        return activeProfileId;
    }

    public String getActiveIsland() {
        return activeIsland;
    }

    private ProfileData activeProfile() {
        return data.getOrCreateProfile(activeProfileId);
    }

    public void setSkillXp(String skill, long xp) {
        ProfileData profile = activeProfile();
        long oldXp = profile.getSkillXp(skill);
        if (xp > oldXp) {
            profile.setSkillXp(skill, xp);
            save();
            
            TilemanLog.debug(DebugCategory.TOKENS,
                "Updated {} XP: {} -> {} (total now {})",
                skill,
                oldXp,
                xp,
                profile.getTotalSkillXp()
            );
        }
    }

    public long getSkillXp(String skill) {
        return activeProfile().getSkillXp(skill);
    }

    public long getTotalSkillXp() {
        return activeProfile().getTotalSkillXp();
    }

    /**
     * Calculate skill level from XP using Skyblock's XP thresholds.
     */
    public int getSkillLevel(String skill) {
        long xp = getSkillXp(skill);
        return xpToLevel(xp);
    }

    /**
     * Convert XP to level using the threshold table.
     */
    public static int xpToLevel(long xp) {
        for (int level = SKILL_XP_THRESHOLDS.length - 1; level >= 0; level--) {
            if (xp >= SKILL_XP_THRESHOLDS[level]) {
                return level;
            }
        }
        return 0;
    }

    /**
     * Get the number of maxed collections.
     */
    public int getMaxedCollections() {
        return activeProfile().getMaxedCollections();
    }

    /**
     * Set the number of maxed collections (from API fetch).
     */
    public void setMaxedCollections(int count) {
        activeProfile().setMaxedCollections(count);
        save();
    }

    /**
     * Called when XP is gained. Converts XP to tokens based on tiered costs.
     * Tokens are stored directly, so changes only affect future XP gains.
     */
    public void onXpGained(long xpAmount) {
        if (xpAmount <= 0) return;
        
        // apply any active XP multipliers from shop
        double multiplier = getEffectiveXpMultiplier();
        long effectiveXp = (long) (xpAmount * multiplier);
        
        xpTowardNextToken += effectiveXp;
        
        int tokensEarned = 0;
        int tokenCost = getCurrentTokenCost();
        
        while (xpTowardNextToken >= tokenCost) {
            xpTowardNextToken -= tokenCost;
            tokensEarned++;
            activeProfile().addTokensEarned(1);
            tokenCost = getCurrentTokenCost(); // recalculate - may have crossed tier
        }
        
        activeProfile().setXpTowardNextToken(xpTowardNextToken);
        
        if (tokensEarned > 0) {
            for (int i = 0; i < tokensEarned; i++) {
                MilestoneTracker.getInstance().onTokenEarned();
            }
            TilemanChat.info(
                "+" + tokensEarned + " Block Unlock Token" +
                (tokensEarned > 1 ? "s" : "") +
                "! (Total: " + getTokens() + ")"
            );
        }
        
        save();
    }
    
    private double getEffectiveXpMultiplier() {
        ShopData shop = activeProfile().getShop();
        double multiplier = 1.0;
        
        if (shop.isXpFrenzyActive()) {
            multiplier *= (1.0 + shop.getXpFrenzyActivePower() / 100.0);
        }
        
        return multiplier;
    }

    public int getTokensEarned() {
        return activeProfile().getTokensEarned();
    }

    public int getTokensSpent() {
        return activeProfile().getTokensSpent();
    }

    public int getTokens() {
        return getTokensEarned() - getTokensSpent();
    }

    public boolean spendToken() {
        if (getTokens() <= 0) {
            return false;
        }
        activeProfile().addTokensSpent(1);
        save();
        return true;
    }

    /**
     * Gets the current XP cost for the next token based on tiered scaling.
     */
    public int getCurrentTokenCost() {
        int tokensEarned = getTokensEarned();
        int cost = TOKEN_COST_TIERS[0][1]; // default to first tier
        
        for (int[] tier : TOKEN_COST_TIERS) {
            if (tokensEarned >= tier[0]) {
                cost = tier[1];
            } else {
                break;
            }
        }
        
        // apply shop bonus if any
        int bonus = activeProfile().getShop().getEfficientScalingBonus();
        if (bonus > 0) {
            // bonus reduces cost by a percentage
            cost = Math.max(100, cost - (cost * bonus / 100));
        }
        
        return cost;
    }

    /**
     * Gets info about the current tier for display.
     */
    public String getCurrentTierName() {
        int tokensEarned = getTokensEarned();
        int tierIndex = 0;
        
        for (int i = 0; i < TOKEN_COST_TIERS.length; i++) {
            if (tokensEarned >= TOKEN_COST_TIERS[i][0]) {
                tierIndex = i;
            } else {
                break;
            }
        }
        
        return switch (tierIndex) {
            case 0 -> "Beginner";
            case 1 -> "Novice";
            case 2 -> "Intermediate";
            case 3 -> "Advanced";
            case 4 -> "Expert";
            default -> "Unknown";
        };
    }

    /**
     * Gets tokens until next tier, or -1 if at max tier.
     */
    public int getTokensUntilNextTier() {
        int tokensEarned = getTokensEarned();
        
        for (int i = 1; i < TOKEN_COST_TIERS.length; i++) {
            if (tokensEarned < TOKEN_COST_TIERS[i][0]) {
                return TOKEN_COST_TIERS[i][0] - tokensEarned;
            }
        }
        
        return -1; // at max tier
    }

    public long getXpToNextToken() {
        return getCurrentTokenCost() - xpTowardNextToken;
    }

    public long getXpSinceLastToken() {
        return xpTowardNextToken;
    }

    public long getXpForNextToken() {
        return getCurrentTokenCost();
    }

    public int getRuleBreaks() {
        return activeProfile().getRuleBreaks();
    }

    public void addRuleBreak() {
        activeProfile().addRuleBreak();
        save();
    }

    public Set<BlockCoord> getUnlockedBlocks() {
        return activeProfile().getUnlockedBlocks(activeIsland);
    }

    public Set<BlockCoord> getUnlockedBlocks(String profileId, String island) {
        return data.getOrCreateProfile(profileId).getUnlockedBlocks(island);
    }

    public boolean isUnlocked(BlockPos pos) {
        return getUnlockedBlocks().contains(BlockCoord.of(pos));
    }

    public boolean unlockBlock(BlockPos pos) {
        boolean added = getUnlockedBlocks().add(BlockCoord.of(pos));
        if (added) {
            save();
        }
        return added;
    }

    public int getTotalUnlockedBlocks() {
        return activeProfile().getTotalUnlockedBlocks();
    }

    public int getFreeBlocksCount() {
        return activeProfile().getFreeBlocksCount();
    }

    public ShopData getShop() {
        return activeProfile().getShop();
    }

    public MilestoneData getMilestones() {
        return activeProfile().getMilestones();
    }

    public DungeonData getDungeonData() {
        return activeProfile().getDungeons();
    }

    public com.azur.skyblocktileman.client.tileman.slayer.SlayerData getSlayerData() {
        return activeProfile().getSlayers();
    }

    public int getLifetimeTokensEarned() {
        return activeProfile().getTokensEarned();
    }

    public void addTokens(int amount) {
        activeProfile().addTokensEarned(amount);
        for (int i = 0; i < amount; i++) {
            MilestoneTracker.getInstance().onTokenEarned();
        }
        save();
    }

    public int getExploredIslandCount() {
        return activeProfile().getIslands().size();
    }

    public boolean spendTokens(int amount) {
        if (getTokens() < amount) {
            return false;
        }
        activeProfile().addTokensSpent(amount);
        activeProfile().getShop().addSpent(amount);
        MilestoneTracker.getInstance().onShopPurchase(amount);
        save();
        return true;
    }

    public void tickShop(long deltaMs) {
        ShopData shop = activeProfile().getShop();
        boolean wasActive = shop.isXpFrenzyActive();
        shop.tickFrenzy(deltaMs);
        if (wasActive && !shop.isXpFrenzyActive()) {
            onXpFrenzyExpired();
        }
    }

    private void onXpFrenzyExpired() {
        TilemanChat.warn("XP Frenzy has expired!");
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.playSound(
                net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                1.0f,
                0.5f
            );
        }
    }
    
    /**
     * Calculate total XP needed to reach a given number of tokens.
     * Useful for progress display.
     */
    public static long calculateXpForTokens(int targetTokens) {
        long totalXp = 0;
        int currentTokens = 0;
        
        for (int i = 0; i < TOKEN_COST_TIERS.length; i++) {
            int tierStart = TOKEN_COST_TIERS[i][0];
            int tierCost = TOKEN_COST_TIERS[i][1];
            int nextTierStart = (i + 1 < TOKEN_COST_TIERS.length) 
                ? TOKEN_COST_TIERS[i + 1][0] 
                : Integer.MAX_VALUE;
            
            int tokensInTier = Math.min(targetTokens, nextTierStart) - Math.max(currentTokens, tierStart);
            if (tokensInTier > 0) {
                totalXp += (long) tokensInTier * tierCost;
            }
            
            currentTokens = nextTierStart;
            if (currentTokens >= targetTokens) break;
        }
        
        return totalXp;
    }
}
