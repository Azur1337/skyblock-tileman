package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.dungeon.DungeonData;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneData;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneTracker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static Path getSaveDir() {
        return SAVE_DIR;
    }

    public static Path getSaveFile() {
        return SAVE_FILE;
    }

    private static TilemanState instance;

    // token cost per skill-level band (level / 10), sqrt-scaled; balance.json can override
    private static final int[] TOKEN_BAND_COSTS = {
        100, // levels 0-10
        800, // levels 10-20
        3100, // levels 20-30
        4800, // levels 30-40
        6200, // levels 40-50
        8500, // levels 50-60
    };

    private static final String[] TOKEN_SKILLS = {
        "Combat",
        "Mining",
        "Foraging",
        "Farming",
        "Fishing",
    };

    // cumulative XP totals required to reach each level
    private static final long[] SKILL_XP_THRESHOLDS = {
        0, // level 0
        50, // level 1
        175, // level 2
        375, // level 3
        675, // level 4
        1175, // level 5
        1925, // level 6
        2925, // level 7
        4425, // level 8
        6425, // level 9
        9925, // level 10
        14925, // level 11
        22425, // level 12
        32425, // level 13
        47425, // level 14
        67425, // level 15
        97425, // level 16
        147425, // level 17
        222425, // level 18
        322425, // level 19
        522425, // level 20
        822425, // level 21
        1222425, // level 22
        1722425, // level 23
        2322425, // level 24
        3022425, // level 25
        3822425, // level 26
        4722425, // level 27
        5722425, // level 28
        6822425, // level 29
        8022425, // level 30
        9322425, // level 31
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
        111672425, // level 60
    };

    private TilemanSaveData data = new TilemanSaveData();

    private String activeProfileId = "unknown";
    private String activeIsland = "unknown";

    private final UnlockIndex unlockIndex = new UnlockIndex();

    public static int bandCount() {
        return TOKEN_BAND_COSTS.length;
    }

    public static int tokenBandCost(int band) {
        return Balance.getInt("token.band." + band, TOKEN_BAND_COSTS[band], 50);
    }

    // bumped when unlocked tiles or the active island change, so cached views can detect staleness
    private int unlockGeneration = 0;

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
                TilemanSaveData loaded = GSON.fromJson(
                    json,
                    TilemanSaveData.class
                );
                data = loaded != null ? loaded : new TilemanSaveData();
                LOGGER.info("Loaded Tileman data from {}", SAVE_FILE);
                SaveBackups.onLoaded();
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
            backupUnreadableSave();
            data = new TilemanSaveData();
        }

        activeProfileId = data.getLastActiveProfileId();
        activeIsland = data.getLastActiveIsland();
        migrateOptIn();
        migrateAwardedXp();
        rebuildUnlockIndex();
    }

    // swap live state for a backup copy; the save being replaced is backed up first
    public String restoreFrom(Path backup) {
        try {
            String json = Files.readString(backup);
            TilemanSaveData restored = GSON.fromJson(
                json,
                TilemanSaveData.class
            );
            if (restored == null) {
                return "Backup is empty or unreadable.";
            }
            SaveBackups.copyNow();
            data = restored;
            activeProfileId = data.getLastActiveProfileId();
            activeIsland = data.getLastActiveIsland();
            migrateOptIn();
            migrateAwardedXp();
            rebuildUnlockIndex();
            save();
            LOGGER.info("Restored Tileman data from {}", backup);
            return null;
        } catch (IOException | JsonSyntaxException e) {
            return "Restore failed: " + e;
        }
    }

    // awardedXp only exists since off-tile tracking; legacy totals were all awarded
    private void migrateAwardedXp() {
        boolean changed = false;
        for (ProfileData profile : data.getProfiles().values()) {
            changed |= profile.backfillAwardedXp();
        }
        if (changed) {
            save();
        }
    }

    // profiles with existing progress keep working; firstInstallMs anchors the new-profile check
    private void migrateOptIn() {
        boolean changed = false;
        if (data.getFirstInstallMs() <= 0) {
            data.setFirstInstallMs(System.currentTimeMillis());
            changed = true;
        }
        for (ProfileData profile : data.getProfiles().values()) {
            if (
                profile.getTilemanMode() == null &&
                (profile.getTokensEarned() > 0 ||
                    profile.getTotalUnlockedBlocks() > 0 ||
                    profile.getRuleBreaks() > 0)
            ) {
                profile.setTilemanMode(true);
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    // save() would overwrite the only copy of the progress, so leave it recoverable by hand
    private void backupUnreadableSave() {
        try {
            if (!Files.exists(SAVE_FILE)) {
                return;
            }
            Path backup = SAVE_FILE.resolveSibling(
                "tileman_data.unreadable-" +
                    System.currentTimeMillis() +
                    ".json"
            );
            Files.move(SAVE_FILE, backup);
            LOGGER.warn("Moved unreadable Tileman data to {}", backup);
        } catch (IOException moveFailed) {
            LOGGER.error(
                "Could not move the unreadable Tileman data aside.",
                moveFailed
            );
        }
    }

    public void save() {
        try {
            Files.createDirectories(SAVE_DIR);
            // write to a temp file then atomic-move, so a crash mid-write never destroys the only copy
            Path tmp = SAVE_FILE.resolveSibling(
                SAVE_FILE.getFileName() + ".tmp"
            );
            Files.writeString(tmp, GSON.toJson(data));
            try {
                Files.move(
                    tmp,
                    SAVE_FILE,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, SAVE_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
            SaveBackups.maybeOnSave();
        } catch (IOException e) {
            LOGGER.error("Failed to save Tileman data.", e);
        }
    }

    public void setActiveProfile(String profileId) {
        this.activeProfileId = profileId == null ? "unknown" : profileId;
        data.setLastActiveProfileId(this.activeProfileId);
        rebuildUnlockIndex();
        save();
    }

    public void setActiveIsland(String island) {
        this.activeIsland = island == null ? "unknown" : island;
        data.setLastActiveIsland(this.activeIsland);
        rebuildUnlockIndex();
        save();
    }

    private void rebuildUnlockIndex() {
        unlockIndex.rebuild(getUnlockedBlocks());
        unlockGeneration++;
    }

    public int unlockGeneration() {
        return unlockGeneration;
    }

    public void forEachUnlockedWithin(
        BlockPos center,
        int radius,
        java.util.function.Consumer<BlockPos> visitor
    ) {
        unlockIndex.forEachWithin(center, radius, visitor);
    }

    public BlockPos nearestUnlockedTo(BlockPos center) {
        return unlockIndex.nearestTo(center);
    }

    public String getActiveProfileId() {
        return activeProfileId;
    }

    public String getActiveIsland() {
        return activeIsland;
    }

    public Boolean getTilemanMode() {
        return activeProfile().getTilemanMode();
    }

    public void setTilemanMode(boolean mode) {
        activeProfile().setTilemanMode(mode);
        save();
    }

    public long getFirstInstallMs() {
        return data.getFirstInstallMs();
    }

    // a profile that already has Tileman history (grandfathered saves, or a played run)
    public boolean hasProgress() {
        ProfileData profile = activeProfile();
        return (
            profile.getTokensEarned() > 0 ||
            profile.getTotalUnlockedBlocks() > 0 ||
            profile.getRuleBreaks() > 0
        );
    }

    // off Hypixel the opt-in and Skyblock checks are skipped so singleplayer testing works
    public boolean isTilemanActive() {
        if (!TilemanConfig.getInstance().isEnabled()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (
            client.getConnection() == null ||
            !HypixelUtil.isConnectedToHypixel(client)
        ) {
            return true;
        }
        // lobby and other Hypixel games are not Skyblock: nothing Tileman runs there
        if (!TilemanIslandHandler.isInSkyblock()) {
            return false;
        }
        return Boolean.TRUE.equals(activeProfile().getTilemanMode());
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

            TilemanLog.debug(
                DebugCategory.TOKENS,
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

    public int getSkillLevel(String skill) {
        long xp = getSkillXp(skill);
        return xpToLevel(xp);
    }

    public static int xpToLevel(long xp) {
        for (int level = SKILL_XP_THRESHOLDS.length - 1; level >= 0; level--) {
            if (xp >= SKILL_XP_THRESHOLDS[level]) {
                return level;
            }
        }
        return 0;
    }

    public void onXpGained(String skill, long xpAmount) {
        if (xpAmount <= 0) return;

        activeProfile().addAwardedXp(skill, xpAmount);

        double multiplier = getEffectiveXpMultiplier(skill);
        long effectiveXp = (long) (xpAmount * multiplier);

        ProfileData profile = activeProfile();
        long pool = profile.getTokenPool(skill) + effectiveXp;

        int tokensEarned = 0;
        int tokenCost = getSkillTokenCost(skill);

        while (pool >= tokenCost) {
            pool -= tokenCost;
            tokensEarned++;
            profile.addTokensEarned(1);
        }

        profile.setTokenPool(skill, pool);

        if (tokensEarned > 0) {
            BalanceLog.log("token_mint", skill, tokensEarned, getTokens());
            for (int i = 0; i < tokensEarned; i++) {
                MilestoneTracker.getInstance().onTokenEarned();
            }
            TilemanChat.info(
                "+" +
                    tokensEarned +
                    " Block Unlock Token" +
                    (tokensEarned > 1 ? "s" : "") +
                    "! (Total: " +
                    getTokens() +
                    ")"
            );
        }

        save();
    }

    private double getEffectiveXpMultiplier(String skill) {
        ShopData shop = activeProfile().getShop();
        double multiplier = shop.getSkillAffinityMultiplier(skill);

        if (shop.isXpFrenzyActive()) {
            multiplier *= 1.0 + shop.getXpFrenzyActivePower() / 100.0;
        }

        return multiplier;
    }

    // off-tile XP is counted so the reconciler can subtract it from the API total,
    // but it never enters the token system
    public void addOffTileXp(String skill, long amount) {
        if (amount <= 0) {
            return;
        }
        activeProfile().addOffTileXp(skill, amount);
        save();
    }

    public long getAwardedXp(String skill) {
        return activeProfile().getAwardedXp(skill);
    }

    public long getOffTileXp(String skill) {
        return activeProfile().getOffTileXp(skill);
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
        BalanceLog.log("token_spend", "unlock", 1, getTokens());
        save();
        return true;
    }

    public int getSkillTokenCost(String skill) {
        int cost = getSkillTokenCostInternal(skill);

        int bonus = activeProfile().getShop().getEfficientScalingBonus();
        if (bonus > 0) {
            cost = Math.max(50, cost - (cost * bonus) / 100);
        }

        return cost;
    }

    private int getSkillTokenCostInternal(String skill) {
        int band = Math.min(
            getSkillLevel(skill) / 10,
            TOKEN_BAND_COSTS.length - 1
        );
        return tokenBandCost(band);
    }

    public String getClosestTokenSkill() {
        String best = null;
        long bestRemaining = Long.MAX_VALUE;
        ProfileData profile = activeProfile();

        for (String skill : TOKEN_SKILLS) {
            long remaining =
                getSkillTokenCost(skill) - profile.getTokenPool(skill);
            if (remaining < bestRemaining) {
                bestRemaining = remaining;
                best = skill;
            }
        }

        return best;
    }

    public long getXpToNextToken() {
        String skill = getClosestTokenSkill();
        if (skill == null) return 0;
        return getSkillTokenCost(skill) - activeProfile().getTokenPool(skill);
    }

    public long getXpSinceLastToken() {
        String skill = getClosestTokenSkill();
        if (skill == null) return 0;
        return activeProfile().getTokenPool(skill);
    }

    public long getXpForNextToken() {
        String skill = getClosestTokenSkill();
        if (skill == null) return 0;
        return getSkillTokenCost(skill);
    }

    public int getRuleBreaks() {
        return activeProfile().getRuleBreaks();
    }

    public void addRuleBreak() {
        activeProfile().addRuleBreak();
        BalanceLog.log(
            "rule_break",
            "",
            activeProfile().getRuleBreaks(),
            getTokens()
        );
        save();
    }

    public Set<BlockCoord> getUnlockedBlocks() {
        return activeProfile().getUnlockedBlocks(activeIsland);
    }

    public Set<BlockCoord> getUnlockedBlocks(String profileId, String island) {
        return data.getOrCreateProfile(profileId).getUnlockedBlocks(island);
    }

    // exploring an island earns the count, so the entry is created here not as a read side effect
    public void markIslandVisited(String island) {
        if (island == null || island.isEmpty()) {
            return;
        }
        activeProfile().getOrCreateUnlockedBlocks(island);
    }

    // unlocked anywhere in the column: an unlocked block shares X,Z with an unobstructed vertical path to pos
    public boolean isUnlockedColumn(BlockPos pos, Level level) {
        int x = pos.getX();
        int z = pos.getZ();
        int playerY = pos.getY();

        for (int y : unlockIndex.columnYs(x, z)) {
            if (hasVerticalClearance(level, x, z, playerY, y)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasVerticalClearance(
        Level level,
        int x,
        int z,
        int y1,
        int y2
    ) {
        if (level == null) {
            return true;
        }

        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);

        if (maxY - minY <= 1) {
            return true;
        }

        for (int y = minY + 1; y < maxY; y++) {
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(checkPos);

            if (isFullSolidBlock(state, level, checkPos)) {
                return false;
            }
        }
        return true;
    }

    private boolean isFullSolidBlock(
        BlockState state,
        Level level,
        BlockPos pos
    ) {
        if (state.isAir()) {
            return false;
        }

        var shape = state.getCollisionShape(
            level,
            pos,
            net.minecraft.world.phys.shapes.CollisionContext.empty()
        );
        if (shape.isEmpty()) {
            return false;
        }

        double minX = shape.min(net.minecraft.core.Direction.Axis.X);
        double maxX = shape.max(net.minecraft.core.Direction.Axis.X);
        double minZ = shape.min(net.minecraft.core.Direction.Axis.Z);
        double maxZ = shape.max(net.minecraft.core.Direction.Axis.Z);
        double minY = shape.min(net.minecraft.core.Direction.Axis.Y);
        double maxY = shape.max(net.minecraft.core.Direction.Axis.Y);

        boolean fullHorizontal = maxX - minX >= 0.9 && maxZ - minZ >= 0.9;
        boolean significantHeight = maxY - minY >= 0.9;

        return fullHorizontal && significantHeight;
    }

    public boolean isUnlocked(BlockPos pos) {
        return getUnlockedBlocks().contains(BlockCoord.of(pos));
    }

    public boolean unlockBlock(BlockPos pos) {
        BlockCoord coord = BlockCoord.of(pos);
        boolean added = activeProfile()
            .getOrCreateUnlockedBlocks(activeIsland)
            .add(coord);
        if (added) {
            unlockIndex.add(coord);
            unlockGeneration++;
            save();
        }
        return added;
    }

    public int getTotalUnlockedBlocks() {
        return activeProfile().getTotalUnlockedBlocks();
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
        if (amount != 0) {
            BalanceLog.log("token_grant", "reward", amount, getTokens());
        }
        for (int i = 0; i < amount; i++) {
            MilestoneTracker.getInstance().onTokenEarned();
        }
        save();
    }

    public int getExploredIslandCount() {
        return activeProfile().getIslands().size();
    }

    public boolean spendTokens(int amount) {
        return spendTokens(amount, "shop");
    }

    public boolean spendTokens(int amount, String reason) {
        if (amount <= 0) {
            return false;
        }
        if (getTokens() < amount) {
            return false;
        }
        activeProfile().addTokensSpent(amount);
        BalanceLog.log("token_spend", reason, amount, getTokens());
        MilestoneTracker.getInstance().onShopPurchase(amount);
        save();
        return true;
    }

    public void tickShop() {
        ShopData shop = activeProfile().getShop();
        boolean wasActive = shop.isXpFrenzyActive();
        shop.tickFrenzy();
        if (wasActive && !shop.isXpFrenzyActive()) {
            onXpFrenzyExpired();
        }
    }

    private void onXpFrenzyExpired() {
        TilemanChat.warn("XP Frenzy has expired!");
        Minecraft client = Minecraft.getInstance();
        if (
            client.player != null &&
            TilemanConfig.getInstance().isFrenzySoundEnabled()
        ) {
            client.player.playSound(
                net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                1.0f,
                0.5f
            );
        }
    }
}
