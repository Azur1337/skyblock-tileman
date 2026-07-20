package com.example.client.tileman;

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

/**
 * Central manager for all Tileman persistence and in-memory state.
 * <p>
 * Data is stored on disk as a single JSON file at
 * {@code <config>/tileman/tileman_data.json}, shaped as:
 * Profile ID -> { tokens, totalSkillXp, islands: { Island Name -> Set of BlockCoord } }.
 * <p>
 * This class is a lazily-initialized singleton so it can be accessed from
 * anywhere (mixins, render events, HUD, etc.) via {@link #getInstance()}.
 */
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

    // The player's currently active Skyblock profile + island.
    // These are set by other systems (profile detection / island-join handling)
    // which we'll wire up in a later step. Default to placeholders so nothing
    // crashes before that wiring exists.
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

    /** Loads save data from disk, creating a fresh file if none exists yet. */
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
    }

    /** Writes the current in-memory state to disk, overwriting the existing file. */
    public void save() {
        try {
            Files.createDirectories(SAVE_DIR);
            Files.writeString(SAVE_FILE, GSON.toJson(data));
        } catch (IOException e) {
            LOGGER.error("Failed to save Tileman data.", e);
        }
    }

    // ---- Active profile/island context ----

    public void setActiveProfile(String profileId) {
        this.activeProfileId = profileId == null ? "unknown" : profileId;
    }

    public void setActiveIsland(String island) {
        this.activeIsland = island == null ? "unknown" : island;
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

    /** Attempts to spend one token. Returns true if a token was available and spent. */
    public boolean spendToken() {
        ProfileData profile = activeProfile();
        if (profile.getTokens() <= 0) {
            return false;
        }
        profile.addTokens(-1);
        save();
        return true;
    }

    // ---- Skill XP / token cost scaling ----

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

    /**
     * Cost, in Skill XP, of the next token at the current total XP.
     * Base cost is 1,000 XP, increasing by 1,000 XP for every 1,000,000 total XP earned
     * (e.g. at 10,000,000 total XP, one token costs 11,000 XP).
     */
    public int getCurrentTokenCost() {
        return tokenCostFor(activeProfile());
    }

    private int tokenCostFor(ProfileData profile) {
        long millionsEarned = (long) (profile.getTotalSkillXp() / 1_000_000.0);
        return 1000 + (int) (millionsEarned * 1000);
    }

    public double getBankedXp() {
        return activeProfile().getBankedXp();
    }

    // ---- Real-time XP intake (called by the action bar mixin) ----

    /**
     * Called whenever the action bar mixin detects a Skill XP gain.
     * Adds the XP to the active profile's lifetime total, banks it toward
     * the next token, and converts banked XP into tokens using the current
     * (scaling) token cost. A single large XP gain can grant multiple tokens.
     *
     * @return the number of tokens granted from this XP gain (usually 0 or 1).
     */
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
            // Cost may shift mid-loop if totalSkillXp just crossed a new million-XP threshold.
            cost = tokenCostFor(profile);
        }

        if (tokensGranted > 0) {
            LOGGER.info(
                "Granted {} Tileman token(s) from Skill XP gain on profile {}",
                tokensGranted,
                activeProfileId
            );
        }

        save();
        return tokensGranted;
    }

    // ---- Unlocked blocks ----

    /** Unlocked blocks for the currently active profile + island. */
    public Set<BlockCoord> getUnlockedBlocks() {
        return activeProfile().getUnlockedBlocks(activeIsland);
    }

    /** Unlocked blocks for an arbitrary profile + island. */
    public Set<BlockCoord> getUnlockedBlocks(String profileId, String island) {
        return data.getOrCreateProfile(profileId).getUnlockedBlocks(island);
    }

    public boolean isUnlocked(BlockPos pos) {
        return getUnlockedBlocks().contains(BlockCoord.of(pos));
    }

    /** Adds a block to the active profile/island's unlocked set and saves. Returns false if already unlocked. */
    public boolean unlockBlock(BlockPos pos) {
        boolean added = getUnlockedBlocks().add(BlockCoord.of(pos));
        if (added) {
            save();
        }
        return added;
    }
}
