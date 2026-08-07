package com.azur.skyblocktileman.client.tileman;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import io.github.notenoughupdates.moulconfig.common.IMinecraft;
import io.github.notenoughupdates.moulconfig.managed.ManagedConfig;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TilemanConfig {

    private static TilemanConfig instance;

    private static final Path CONFIG_DIR = Path.of("config", "tileman");
    private static final Path MOULCONFIG_FILE = CONFIG_DIR.resolve("moulconfig.json");
    private static final Path LEGACY_CONFIG_FILE = CONFIG_DIR.resolve("config.json");

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
        GeneralCategory.DebugCategories debug = config().general.debugCategories;
        return switch (category) {
            case ACTION_BAR -> debug.actionBar;
            case ISLAND -> debug.island;
            case PROFILE -> debug.profile;
            case TOKENS -> debug.tokens;
            case BLOCKS -> debug.blocks;
            case RENDERING -> debug.rendering;
            case SLAYER -> debug.slayer;
            case DUNGEON -> debug.dungeon;
            case CHAT -> debug.chat;
            case ALL -> true;
        };
    }

    public int getXpPerToken() {
        return config().xpTokens.xpPerToken;
    }

    public boolean isScalingEnabled() {
        return config().xpTokens.enableScaling;
    }

    public int getScaleInterval() {
        return config().xpTokens.scaleInterval;
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

    public void setHudPosition(int hudX, int hudY) {
        config().rendering.hudX = Math.max(0, hudX);
        config().rendering.hudY = Math.max(0, hudY);
        save();
    }

    public void setHudPositionTransient(int hudX, int hudY) {
        config().rendering.hudX = Math.max(0, hudX);
        config().rendering.hudY = Math.max(0, hudY);
    }

    public boolean isPunishmentEnabled() {
        return config().punishment.enabled;
    }

    public boolean isTileCompassEnabled() {
        return config().rendering.showTileCompass;
    }

    public int getSoundIntervalTicks() {
        return config().punishment.soundIntervalTicks;
    }

    private static void migrateLegacyIfNeeded() {
        if (Files.exists(MOULCONFIG_FILE) || !Files.exists(LEGACY_CONFIG_FILE)) {
            return;
        }

        try {
            Files.createDirectories(CONFIG_DIR);
            String json = Files.readString(LEGACY_CONFIG_FILE);
            JsonObject legacy = JsonParser.parseString(json).getAsJsonObject();

            TilemanMoulConfig migrated = new TilemanMoulConfig();
            if (legacy.has("debugMode")) {
                migrated.general.debugMode = legacy.get("debugMode").getAsBoolean();
            }
            if (legacy.has("showUnlockedOverlay")) {
                migrated.rendering.showUnlockedOverlay = legacy.get("showUnlockedOverlay").getAsBoolean();
            }

            ManagedConfig<TilemanMoulConfig> migrationWriter = ManagedConfig.create(
                new File(MOULCONFIG_FILE.toString()),
                TilemanMoulConfig.class
            );
            TilemanMoulConfig target = migrationWriter.getInstance();
            target.general.debugMode = migrated.general.debugMode;
            target.rendering.showUnlockedOverlay = migrated.rendering.showUnlockedOverlay;
            migrationWriter.saveToFile();
        } catch (IOException | IllegalStateException | JsonSyntaxException ignored) {
        }
    }
}
