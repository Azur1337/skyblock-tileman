package com.azur.skyblocktileman.client.tileman;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
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

    private TilemanSaveData data = new TilemanSaveData();

    private String activeProfileId = "unknown";
    private String activeIsland = "unknown";

    private TilemanState() {
        load();
    }

    public static TilemanState getInstance() {
        if (instance == null) {
            instance = new TilemanState();
        }
        return instance;
    }

    // ---- Persistence ----

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

    // ---- Skill XP ----

    public void setSkillXp(String skill, long xp) {
        ProfileData profile = activeProfile();
        long oldXp = profile.getSkillXp(skill);
        if (xp > oldXp) {
            profile.setSkillXp(skill, xp);
            save();
            
            TilemanLog.debug(
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

    // ---- Tokens ----

    public int getTokensEarned() {
        TilemanConfig config = TilemanConfig.getInstance();
        int baseCost = config.getBaseTokenCost();
        int scaleInterval = config.getCostScaleInterval();
        
        long totalXp = activeProfile().getTotalSkillXp();
        int tokens = 0;
        long xpUsed = 0;
        
        while (true) {
            long scaleSteps = scaleInterval > 0 ? xpUsed / scaleInterval : 0;
            int cost = baseCost + (int) (scaleSteps * baseCost);
            
            if (xpUsed + cost > totalXp) {
                break;
            }
            
            xpUsed += cost;
            tokens++;
        }
        
        return tokens;
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

    // ---- Token Cost ----

    public int getCurrentTokenCost() {
        TilemanConfig config = TilemanConfig.getInstance();
        int baseCost = config.getBaseTokenCost();
        int scaleInterval = config.getCostScaleInterval();
        
        long totalXp = activeProfile().getTotalSkillXp();
        long scaleSteps = scaleInterval > 0 ? totalXp / scaleInterval : 0;
        
        return baseCost + (int) (scaleSteps * baseCost);
    }

    public long getXpToNextToken() {
        TilemanConfig config = TilemanConfig.getInstance();
        int baseCost = config.getBaseTokenCost();
        int scaleInterval = config.getCostScaleInterval();
        
        long totalXp = activeProfile().getTotalSkillXp();
        long xpUsed = 0;
        
        while (true) {
            long scaleSteps = scaleInterval > 0 ? xpUsed / scaleInterval : 0;
            int cost = baseCost + (int) (scaleSteps * baseCost);
            
            if (xpUsed + cost > totalXp) {
                return (xpUsed + cost) - totalXp;
            }
            
            xpUsed += cost;
        }
    }

    // ---- Rule Breaks ----

    public int getRuleBreaks() {
        return activeProfile().getRuleBreaks();
    }

    public void addRuleBreak() {
        activeProfile().addRuleBreak();
        save();
    }

    // ---- Blocks ----

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
}
