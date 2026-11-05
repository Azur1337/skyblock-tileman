package com.azur.skyblocktileman.client.tileman;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.notenoughupdates.moulconfig.common.IMinecraft;
import io.github.notenoughupdates.moulconfig.managed.ManagedConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TilemanConfig {

    private static TilemanConfig instance;

    private static final Path CONFIG_DIR = Path.of("config", "tileman");
    private static final Path MOULCONFIG_FILE = CONFIG_DIR.resolve(
        "moulconfig.json"
    );
    private static final Path LEGACY_CONFIG_FILE = CONFIG_DIR.resolve(
        "config.json"
    );

    private final ManagedConfig<TilemanMoulConfig> managedConfig;

    private TilemanConfig() {
        migrateLegacyIfNeeded();
        managedConfig = ManagedConfig.create(
            new File(MOULCONFIG_FILE.toString()),
            TilemanMoulConfig.class
        );
    }

    public static TilemanConfig getInstance() {
        if (instance == null) {
            instance = new TilemanConfig();
        }
        return instance;
    }

    public void openConfigScreen() {
        IMinecraft.INSTANCE.openWrappedScreen(managedConfig.getEditor());
    }

    private TilemanMoulConfig config() {
        return managedConfig.getInstance();
    }

    public void save() {
        managedConfig.saveToFile();
    }

    public boolean isEnabled() {
        return config().general.enabled;
    }

    public void setEnabled(boolean enabled) {
        config().general.enabled = enabled;
        save();
    }

    public boolean isDebugMode() {
        return config().general.debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        config().general.debugMode = debugMode;
        save();
    }

    public boolean isDebugEnabled(DebugCategory category) {
        if (!isDebugMode()) {
            return false;
        }
        GeneralCategory.DebugCategories debug =
            config().general.debugCategories;
        return switch (category) {
            case ACTION_BAR -> debug.actionBar;
            case ISLAND -> debug.island;
            case PROFILE -> debug.profile;
            case TOKENS -> debug.tokens;
            case BLOCKS -> debug.blocks;
            case SLAYER -> debug.slayer;
            case DUNGEON -> debug.dungeon;
            case ALL -> true;
        };
    }

    public boolean isShowUnlockedOverlay() {
        return config().rendering.showUnlockedOverlay;
    }

    public void setShowUnlockedOverlay(boolean show) {
        config().rendering.showUnlockedOverlay = show;
        save();
    }

    public int getHighlightRadius() {
        return config().rendering.highlightRadius;
    }

    public boolean isStatsHudEnabled() {
        return config().rendering.showStatsHud;
    }

    public boolean isHudIconMode() {
        return config().rendering.hudIconMode;
    }

    public int getHudX() {
        return config().rendering.hudX;
    }

    public int getHudY() {
        return config().rendering.hudY;
    }

    public void setHudPositionTransient(int hudX, int hudY) {
        config().rendering.hudX = Math.max(0, hudX);
        config().rendering.hudY = Math.max(0, hudY);
    }

    public boolean isPunishmentEnabled() {
        return config().punishment.enabled;
    }

    public boolean isHealthPunishmentEnabled() {
        return config().punishment.healthEnabled;
    }

    public boolean isTileCompassEnabled() {
        return config().rendering.showTileCompass;
    }

    public boolean isMilestonePopupsEnabled() {
        return config().rendering.showMilestonePopups;
    }

    public int getSoundIntervalTicks() {
        return config().punishment.soundIntervalTicks;
    }

    public boolean isThunderSoundEnabled() {
        return config().punishment.thunderSound;
    }

    public boolean isFakeEffectsEnabled() {
        return config().punishment.fakeEffects;
    }

    public boolean isCollapseWarpEnabled() {
        return config().punishment.collapseWarp;
    }

    public boolean isModApiEnabled() {
        return config().general.useModApi;
    }

    public boolean isChatInfoEnabled() {
        return config().general.chatInfo;
    }

    public boolean isSelectionPreviewEnabled() {
        return config().rendering.selectionPreview;
    }

    public boolean isIslandBannersEnabled() {
        return config().rendering.islandBanners;
    }

    public boolean isTilesCountAllIslands() {
        return config().rendering.tilesCountAllIslands;
    }

    public int getPopupDurationMs() {
        return config().rendering.popupDurationMs;
    }

    public boolean isHudRowTokens() {
        return config().rendering.hudRows.tokens;
    }

    public boolean isHudRowTiles() {
        return config().rendering.hudRows.tiles;
    }

    public boolean isHudRowTotalXp() {
        return config().rendering.hudRows.totalXp;
    }

    public boolean isHudRowNextToken() {
        return config().rendering.hudRows.nextToken;
    }

    public boolean isHudRowRuleBreaks() {
        return config().rendering.hudRows.ruleBreaks;
    }

    public boolean isRuleBrokenBannerEnabled() {
        return config().punishment.ruleBrokenBanner;
    }

    public long getOffTileGraceMs() {
        return config().punishment.healthTuning.offTileGraceSeconds * 1000L;
    }

    public float getHealthDrainPerMs() {
        return 1f / (config().punishment.healthTuning.fullDrainSeconds * 1000f);
    }

    public float getHealthRegenPerMs() {
        return (
            1f / (config().punishment.healthTuning.fullRefillSeconds * 1000f)
        );
    }

    public float getNauseaThreshold() {
        return config().punishment.healthTuning.nauseaBelowPercent / 100f;
    }

    // blindness must never trigger above nausea, so clamp it to the nausea point
    public float getBlindnessThreshold() {
        float nausea = getNauseaThreshold();
        float blind =
            config().punishment.healthTuning.blindnessBelowPercent / 100f;
        return Math.min(blind, nausea);
    }

    public boolean isPurchaseSoundEnabled() {
        return config().sounds.enabled && config().sounds.purchaseChime;
    }

    public boolean isMilestoneSoundEnabled() {
        return config().sounds.enabled && config().sounds.milestoneToast;
    }

    public boolean isDungeonRewardSoundEnabled() {
        return config().sounds.enabled && config().sounds.dungeonReward;
    }

    public boolean isFrenzySoundEnabled() {
        return config().sounds.enabled && config().sounds.frenzyExpiry;
    }

    public boolean isCollapseSoundEnabled() {
        return config().sounds.enabled && config().sounds.collapseSnap;
    }

    public KeybindCategory getKeybinds() {
        return config().keybinds;
    }

    private static void migrateLegacyIfNeeded() {
        if (
            Files.exists(MOULCONFIG_FILE) || !Files.exists(LEGACY_CONFIG_FILE)
        ) {
            return;
        }

        try {
            Files.createDirectories(CONFIG_DIR);
            String json = Files.readString(LEGACY_CONFIG_FILE);
            JsonObject legacy = JsonParser.parseString(json).getAsJsonObject();

            TilemanMoulConfig migrated = new TilemanMoulConfig();
            if (legacy.has("debugMode")) {
                migrated.general.debugMode = legacy
                    .get("debugMode")
                    .getAsBoolean();
            }
            if (legacy.has("showUnlockedOverlay")) {
                migrated.rendering.showUnlockedOverlay = legacy
                    .get("showUnlockedOverlay")
                    .getAsBoolean();
            }

            ManagedConfig<TilemanMoulConfig> migrationWriter =
                ManagedConfig.create(
                    new File(MOULCONFIG_FILE.toString()),
                    TilemanMoulConfig.class
                );
            TilemanMoulConfig target = migrationWriter.getInstance();
            target.general.debugMode = migrated.general.debugMode;
            target.rendering.showUnlockedOverlay =
                migrated.rendering.showUnlockedOverlay;
            migrationWriter.saveToFile();
        } catch (
            IOException
            | IllegalStateException
            | JsonSyntaxException ignored
        ) {}
    }
}
