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

// Wraps MoulConfig's ManagedConfig and provides convenient access to config values.
// Replaces the old hand-rolled JSON config system.
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

    // ---- GUI ----

    /** Opens the MoulConfig GUI screen. Call from the client thread. */
    public void openConfigScreen() {
        IMinecraft.INSTANCE.openWrappedScreen(managedConfig.getEditor());
    }

    // ---- Convenience getters/setters ----

    private TilemanMoulConfig config() {
        return managedConfig.getInstance();
    }

    public void save() {
        managedConfig.saveToFile();
    }

    // General
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

    // XP & Tokens
    public int getBaseTokenCost() {
        return config().xpTokens.baseTokenCost;
    }

    public int getCostScaleInterval() {
        return config().xpTokens.costScaleInterval;
    }

    // Rendering
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

    // Punishment
    public boolean isPunishmentEnabled() {
        return config().punishment.enabled;
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
            // If migration fails we keep defaults and continue; users can edit in /tileman config.
        }
    }
}
