package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.dungeon.DungeonFloor;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneType;
import com.azur.skyblocktileman.client.tileman.shop.ShopRequirements;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

// config/tileman/balance.json overlays the hardcoded defaults, so rebalancing is
// a file edit plus /tileman balance reload, not a new jar. Missing keys keep defaults.
public final class Balance {

    private static final Path FILE = TilemanState.getSaveDir().resolve(
        "balance.json"
    );

    private static JsonObject overrides = new JsonObject();
    private static String lastError = null;

    // every key read during the session, with its effective value, for dump()
    private static final Map<String, Long> seen = new LinkedHashMap<>();

    // how deep dump() samples per-level price keys for perks without a max-level constant
    private static final int PRICE_TEMPLATE_LEVELS = 10;

    private Balance() {}

    public static int getInt(String key, int def, int min) {
        long value = resolve(key, def);
        return (int) Math.max(min, value);
    }

    public static long getLong(String key, long def, long min) {
        return Math.max(min, resolve(key, def));
    }

    private static long resolve(String key, long def) {
        long value = def;
        JsonElement element = overrides.get(key);
        if (element != null && element.isJsonPrimitive()) {
            try {
                value = (long) element.getAsDouble();
            } catch (RuntimeException ignored) {}
        }
        seen.put(key, value);
        return value;
    }

    public static int overrideCount() {
        return overrides.size();
    }

    public static String lastError() {
        return lastError;
    }

    public static String reload() {
        try {
            if (!Files.exists(FILE)) {
                overrides = new JsonObject();
                lastError = null;
                return "No balance.json at " + FILE + ", defaults restored.";
            }
            JsonElement parsed = JsonParser.parseString(Files.readString(FILE));
            if (!parsed.isJsonObject()) {
                lastError = "balance.json is not a JSON object";
                return (
                    "Reload failed: " +
                    lastError +
                    " (keeping the previous values)."
                );
            }
            overrides = parsed.getAsJsonObject();
            lastError = null;
            return (
                "Balance reloaded: " +
                overrides.size() +
                " overrides active from " +
                FILE.getFileName() +
                "."
            );
        } catch (IOException | RuntimeException e) {
            lastError = e.getMessage();
            return "Reload failed: " + e + " (keeping the previous values).";
        }
    }

    // writes every known key with its current effective value so the file can be
    // edited instead of hand-guessed; touches the getters to populate the template
    public static String dump() {
        registerDefaults();
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject out = new JsonObject();
            for (Map.Entry<String, Long> entry : seen.entrySet()) {
                out.addProperty(entry.getKey(), entry.getValue());
            }
            Files.writeString(
                FILE,
                new GsonBuilder().setPrettyPrinting().create().toJson(out)
            );
            return (
                "Wrote " +
                seen.size() +
                " balance keys to " +
                FILE +
                ". Override any value and run /tileman balance reload."
            );
        } catch (IOException e) {
            return "Dump failed: " + e;
        }
    }

    // touches every tunable so dump() covers keys that were never read in game
    private static void registerDefaults() {
        for (int band = 0; band < TilemanState.bandCount(); band++) {
            TilemanState.tokenBandCost(band);
        }
        for (DungeonFloor floor : DungeonFloor.values()) {
            floor.getUnlockCost();
            floor.getBaseReward();
        }
        for (MilestoneType type : MilestoneType.values()) {
            for (int tier = 1; tier <= type.getMaxTier(); tier++) {
                type.getRewardForTier(tier);
            }
        }
        for (int level = 0; level < ShopData.LUCKY_MAX_LEVEL; level++) {
            ShopData.getLuckyUnlockPrice(level);
            ShopRequirements.luckyXpGate(level + 1);
        }
        for (int level = 0; level < ShopData.EFFICIENT_SCALING_MAX; level++) {
            ShopData.getEfficientScalingPrice(level);
        }
        for (int level = 0; level < PRICE_TEMPLATE_LEVELS; level++) {
            ShopData.getSkillAffinityPrice(level);
            ShopData.getFrenzyDurationPrice(level);
            ShopData.getFrenzyPowerPrice(level);
        }
        ShopData.getRemoteUnlockPrice();
        ShopData.getXpFrenzyPrice();
        SkillXpParser.maxReasonableXpGain();
    }

    static {
        reload();
    }
}
