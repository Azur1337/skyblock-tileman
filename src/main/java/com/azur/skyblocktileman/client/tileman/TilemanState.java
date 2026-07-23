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

// Handles all the Tileman save data: tokens, xp, unlocked blocks, active profile/island
public final class TilemanState {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        "TilemanState"
    );

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
                TilemanSaveData loaded = GSON.fromJson(
                    json,
                    TilemanSaveData.class
                );
                data = loaded != null ? loaded : new TilemanSaveData();
                LOGGER.info("Loaded Tileman data from {}", SAVE_FILE);
            } else {
                data = new TilemanSaveData();
                save();
                LOGGER.info("Created new Tileman data file at {}", SAVE_FILE);
            }
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error(
                "Failed to load Tileman data, starting with a blank state.",
                e
            );
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

    // ---- Tokens ----

    public int getTokens() {
        return activeProfile().getTokens();
    }

    public void setTokens(int tokens) {
        activeProfile().setTokens(tokens);
        save();
    }

    public void addTokens(int amount) {
        activeProfile().addTokens(amount);
        save();
    }

    public boolean spendToken() {
        ProfileData profile = activeProfile();
        if (profile.getTokens() <= 0) {
            return false;
        }
        profile.addTokens(-1);
        save();
        return true;
    }

    public double getTotalSkillXp() {
        return activeProfile().getTotalSkillXp();
    }

    public void setTotalSkillXp(double xp) {
        activeProfile().setTotalSkillXp(xp);
        save();
    }

    public void addSkillXp(double delta) {
        activeProfile().addSkillXp(delta);
        save();
    }

    // Only raises totalSkillXp never lowers it, since the Hypixel API can lag behind local tracking
    public void setTotalSkillXpBaseline(double xp) {
        ProfileData profile = activeProfile();
        if (xp > profile.getTotalSkillXp()) {
            profile.setTotalSkillXp(xp);
            save();
        }
    }

    // Cost of the next token. Starts at 1000 xp. +1000 for every 1m total xp earned
    public int getCurrentTokenCost() {
        return tokenCostFor(activeProfile());
    }

    private int tokenCostFor(ProfileData profile) {
        TilemanConfig config = TilemanConfig.getInstance();
        int baseCost = config.getBaseTokenCost();
        int scaleInterval = config.getCostScaleInterval();
        long scaleSteps = scaleInterval > 0
            ? (long) (profile.getTotalSkillXp() / (double) scaleInterval)
            : 0;
        return baseCost + (int) (scaleSteps * baseCost);
    }

    public double getBankedXp() {
        return activeProfile().getBankedXp();
    }

    // Called from the action bar mixin whenever we detect a skill xp gain
    public int onSkillXpGained(double xpGained) {
        if (xpGained <= 0) {
            return 0;
        }

        ProfileData profile = activeProfile();
        profile.addSkillXp(xpGained);
        profile.addBankedXp(xpGained);

        int tokensGranted = 0;
        int cost = tokenCostFor(profile);
        while (profile.getBankedXp() >= cost) {
            profile.addBankedXp(-cost);
            profile.addTokens(1);
            tokensGranted++;
            cost = tokenCostFor(profile);
        }

        if (tokensGranted > 0) {
            TilemanLog.debug(
                "Granted {} Tileman token(s) from Skill XP gain on profile {}",
                tokensGranted,
                activeProfileId
            );
        }

        save();
        return tokensGranted;
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
}
