package com.azur.skyblocktileman.client.tileman;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HypixelApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        "HypixelApiClient"
    );

    private static final String PROFILES_URL =
        "https://api.hypixel.net/v2/skyblock/profiles?uuid=%s";

    private static final Set<String> SKILL_XP_KEYS = Set.of(
        "SKILL_FARMING",
        "SKILL_MINING",
        "SKILL_COMBAT",
        "SKILL_FORAGING",
        "SKILL_FISHING",
        "SKILL_ENCHANTING",
        "SKILL_ALCHEMY",
        "SKILL_TAMING",
        "SKILL_CARPENTRY",
        "SKILL_RUNECRAFTING",
        "SKILL_SOCIAL",
        "SKILL_HUNTING"
    );

    private static final Map<String, String> SKILL_KEY_TO_NAME = Map.ofEntries(
        Map.entry("SKILL_FARMING", "Farming"),
        Map.entry("SKILL_MINING", "Mining"),
        Map.entry("SKILL_COMBAT", "Combat"),
        Map.entry("SKILL_FORAGING", "Foraging"),
        Map.entry("SKILL_FISHING", "Fishing"),
        Map.entry("SKILL_ENCHANTING", "Enchanting"),
        Map.entry("SKILL_ALCHEMY", "Alchemy"),
        Map.entry("SKILL_TAMING", "Taming"),
        Map.entry("SKILL_CARPENTRY", "Carpentry"),
        Map.entry("SKILL_RUNECRAFTING", "Runecrafting"),
        Map.entry("SKILL_SOCIAL", "Social"),
        Map.entry("SKILL_HUNTING", "Hunting")
    );

    private static final Map<String, String> LEGACY_KEY_TO_NAME = Map.ofEntries(
        Map.entry("experience_skill_farming", "Farming"),
        Map.entry("experience_skill_mining", "Mining"),
        Map.entry("experience_skill_combat", "Combat"),
        Map.entry("experience_skill_foraging", "Foraging"),
        Map.entry("experience_skill_fishing", "Fishing"),
        Map.entry("experience_skill_enchanting", "Enchanting"),
        Map.entry("experience_skill_alchemy", "Alchemy"),
        Map.entry("experience_skill_taming", "Taming"),
        Map.entry("experience_skill_carpentry", "Carpentry"),
        Map.entry("experience_skill_runecrafting", "Runecrafting"),
        Map.entry("experience_skill_social2", "Social"),
        Map.entry("experience_skill_hunting", "Hunting")
    );

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private HypixelApiClient() {}

    public record BaselineResult(
        boolean success,
        String profileId,
        String cuteName,
        Map<String, Long> skillXp,
        String errorMessage
    ) {
        static BaselineResult failure(String message) {
            return new BaselineResult(false, null, null, null, message);
        }

        static BaselineResult of(
            String profileId,
            String cuteName,
            Map<String, Long> skillXp
        ) {
            return new BaselineResult(
                true,
                profileId,
                cuteName,
                skillXp,
                null
            );
        }

        public long totalSkillXp() {
            if (skillXp == null) return 0;
            return skillXp.values().stream().mapToLong(Long::longValue).sum();
        }
    }

    public record XpFetchResult(
        boolean success,
        Map<String, Long> skillXp,
        String errorMessage
    ) {
        static XpFetchResult failure(String message) {
            return new XpFetchResult(false, null, message);
        }

        static XpFetchResult of(Map<String, Long> skillXp) {
            return new XpFetchResult(true, skillXp, null);
        }

        public long totalSkillXp() {
            if (skillXp == null) return 0;
            return skillXp.values().stream().mapToLong(Long::longValue).sum();
        }
    }

    public static CompletableFuture<BaselineResult> fetchBaselineSkillXp(
        UUID playerUuid,
        String apiKey
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            return CompletableFuture.completedFuture(
                BaselineResult.failure("No Hypixel API key configured.")
            );
        }

        String undashed = playerUuid.toString().replace("-", "");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(String.format(PROFILES_URL, undashed)))
            .header("API-Key", apiKey)
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        return HTTP_CLIENT.sendAsync(
            request,
            HttpResponse.BodyHandlers.ofString()
        )
            .thenApply(response ->
                parseResponse(
                    response.body(),
                    response.statusCode(),
                    playerUuid
                )
            )
            .exceptionally(e -> {
                LOGGER.error(
                    "Failed to fetch SkyBlock profiles from Hypixel API.",
                    e
                );
                return BaselineResult.failure(
                    "Network error: " + e.getMessage()
                );
            });
    }

    public static CompletableFuture<XpFetchResult> fetchTotalSkillXp(
        UUID playerUuid,
        String apiKey
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            return CompletableFuture.completedFuture(
                XpFetchResult.failure("No Hypixel API key configured.")
            );
        }

        String undashed = playerUuid.toString().replace("-", "");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(String.format(PROFILES_URL, undashed)))
            .header("API-Key", apiKey)
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        return HTTP_CLIENT.sendAsync(
            request,
            HttpResponse.BodyHandlers.ofString()
        )
            .thenApply(response ->
                parseXpResponse(
                    response.body(),
                    response.statusCode(),
                    playerUuid
                )
            )
            .exceptionally(e -> {
                LOGGER.error(
                    "Failed to fetch SkyBlock XP from Hypixel API.",
                    e
                );
                return XpFetchResult.failure(
                    "Network error: " + e.getMessage()
                );
            });
    }

    private static XpFetchResult parseXpResponse(
        String body,
        int statusCode,
        UUID playerUuid
    ) {
        JsonObject root;
        try {
            root = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            return XpFetchResult.failure(
                "Malformed response from Hypixel API (status " + statusCode + ")."
            );
        }

        if (!root.has("success") || !root.get("success").getAsBoolean()) {
            String cause = root.has("cause")
                ? root.get("cause").getAsString()
                : "HTTP " + statusCode;
            return XpFetchResult.failure("Hypixel API request failed: " + cause);
        }

        JsonArray profiles = root.getAsJsonArray("profiles");
        if (profiles == null || profiles.isEmpty()) {
            return XpFetchResult.failure("Player has no SkyBlock profiles.");
        }

        JsonObject selected = findSelectedProfile(profiles);
        if (selected == null) {
            selected = profiles.get(0).getAsJsonObject();
        }

        JsonObject members = selected.getAsJsonObject("members");
        if (members == null) {
            return XpFetchResult.failure("Profile response had no member data.");
        }

        JsonObject member = findMember(members, playerUuid);
        if (member == null) {
            return XpFetchResult.failure(
                "Could not find player's member entry in selected profile."
            );
        }

                Map<String, Long> skillXp = getSkillXpMap(member);
                return XpFetchResult.of(skillXp);
    }

    private static BaselineResult parseResponse(
        String body,
        int statusCode,
        UUID playerUuid
    ) {
        JsonObject root;
        try {
            root = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            return BaselineResult.failure(
                "Malformed response from Hypixel API (status " +
                    statusCode +
                    ")."
            );
        }

        if (!root.has("success") || !root.get("success").getAsBoolean()) {
            String cause = root.has("cause")
                ? root.get("cause").getAsString()
                : "HTTP " + statusCode;
            return BaselineResult.failure(
                "Hypixel API request failed: " + cause
            );
        }

        JsonArray profiles = root.getAsJsonArray("profiles");
        if (profiles == null || profiles.isEmpty()) {
            return BaselineResult.failure("Player has no SkyBlock profiles.");
        }

        JsonObject selected = findSelectedProfile(profiles);
        if (selected == null) {
            selected = profiles.get(0).getAsJsonObject();
        }

        String profileId = selected.has("profile_id")
            ? selected.get("profile_id").getAsString()
            : null;
        String cuteName = selected.has("cute_name")
            ? selected.get("cute_name").getAsString()
            : "Unknown";

        JsonObject members = selected.getAsJsonObject("members");
        if (members == null) {
            return BaselineResult.failure(
                "Profile response had no member data."
            );
        }

        JsonObject member = findMember(members, playerUuid);
        if (member == null) {
            return BaselineResult.failure(
                "Could not find this player's member entry in the selected profile."
            );
        }

                Map<String, Long> skillXp = getSkillXpMap(member);
                return BaselineResult.of(profileId, cuteName, skillXp);
    }

    private static JsonObject findSelectedProfile(JsonArray profiles) {
        for (JsonElement element : profiles) {
            JsonObject profile = element.getAsJsonObject();
            if (
                profile.has("selected") &&
                profile.get("selected").getAsBoolean()
            ) {
                return profile;
            }
        }
        return null;
    }

    private static JsonObject findMember(JsonObject members, UUID playerUuid) {
        String dashed = playerUuid.toString();
        String undashed = dashed.replace("-", "");
        for (Map.Entry<String, JsonElement> entry : members.entrySet()) {
            if (
                entry.getKey().equalsIgnoreCase(dashed) ||
                entry.getKey().equalsIgnoreCase(undashed)
            ) {
                return entry.getValue().getAsJsonObject();
            }
        }
        return null;
    }

    private static Map<String, Long> getSkillXpMap(JsonObject member) {
        Map<String, Long> result = new HashMap<>();

        JsonObject playerData = member.has("player_data")
            ? member.getAsJsonObject("player_data")
            : null;
        JsonObject experience =
            playerData != null && playerData.has("experience")
                ? playerData.getAsJsonObject("experience")
                : null;

        if (experience != null) {
            for (Map.Entry<String, String> entry : SKILL_KEY_TO_NAME.entrySet()) {
                if (experience.has(entry.getKey())) {
                    result.put(entry.getValue(), (long) experience.get(entry.getKey()).getAsDouble());
                }
            }
        }

        if (result.isEmpty()) {
            for (Map.Entry<String, String> entry : LEGACY_KEY_TO_NAME.entrySet()) {
                if (member.has(entry.getKey())) {
                    result.put(entry.getValue(), (long) member.get(entry.getKey()).getAsDouble());
                }
            }
        }

        return result;
    }

    /**
     * Collection tier requirements - maps collection item to max tier threshold.
     * A collection is "maxed" when the player reaches this amount.
     * Note: This is a simplified version - some collections have different max tiers.
     */
    private static final Map<String, Long> COLLECTION_MAX_TIERS = Map.ofEntries(
        // Farming
        Map.entry("WHEAT", 100000L),
        Map.entry("CARROT_ITEM", 100000L),
        Map.entry("POTATO_ITEM", 100000L),
        Map.entry("PUMPKIN", 100000L),
        Map.entry("MELON", 250000L),
        Map.entry("SEEDS", 50000L),
        Map.entry("MUSHROOM_COLLECTION", 50000L),
        Map.entry("INK_SACK:3", 250000L), // Cocoa
        Map.entry("CACTUS", 100000L),
        Map.entry("SUGAR_CANE", 100000L),
        Map.entry("NETHER_STALK", 100000L),
        
        // Mining
        Map.entry("COBBLESTONE", 2000000L),
        Map.entry("COAL", 250000L),
        Map.entry("IRON_INGOT", 50000L),
        Map.entry("GOLD_INGOT", 50000L),
        Map.entry("DIAMOND", 50000L),
        Map.entry("LAPIS_LAZULI", 100000L),
        Map.entry("EMERALD", 50000L),
        Map.entry("REDSTONE", 250000L),
        Map.entry("QUARTZ", 100000L),
        Map.entry("OBSIDIAN", 100000L),
        Map.entry("GLOWSTONE_DUST", 100000L),
        Map.entry("GRAVEL", 100000L),
        Map.entry("ICE", 50000L),
        Map.entry("NETHERRACK", 100000L),
        Map.entry("SAND", 100000L),
        Map.entry("ENDER_STONE", 100000L),
        Map.entry("MITHRIL_ORE", 250000L),
        Map.entry("HARD_STONE", 1000000L),
        Map.entry("GEMSTONE_COLLECTION", 250000L),
        Map.entry("MYCEL", 25000L),
        Map.entry("SULPHUR_ORE", 100000L),
        
        // Combat
        Map.entry("ROTTEN_FLESH", 100000L),
        Map.entry("BONE", 100000L),
        Map.entry("STRING", 100000L),
        Map.entry("SPIDER_EYE", 100000L),
        Map.entry("GUNPOWDER", 100000L),
        Map.entry("ENDER_PEARL", 50000L),
        Map.entry("GHAST_TEAR", 25000L),
        Map.entry("SLIME_BALL", 100000L),
        Map.entry("BLAZE_ROD", 50000L),
        Map.entry("MAGMA_CREAM", 50000L),
        
        // Foraging
        Map.entry("LOG", 100000L),
        Map.entry("LOG:1", 100000L),
        Map.entry("LOG:2", 100000L),
        Map.entry("LOG_2:1", 100000L),
        Map.entry("LOG_2", 100000L),
        Map.entry("LOG:3", 100000L),
        
        // Fishing
        Map.entry("RAW_FISH", 100000L),
        Map.entry("RAW_FISH:1", 100000L),
        Map.entry("RAW_FISH:2", 100000L),
        Map.entry("RAW_FISH:3", 100000L),
        Map.entry("PRISMARINE_SHARD", 25000L),
        Map.entry("PRISMARINE_CRYSTALS", 25000L),
        Map.entry("CLAY_BALL", 50000L),
        Map.entry("WATER_LILY", 25000L),
        Map.entry("INK_SACK", 50000L),
        Map.entry("SPONGE", 25000L)
    );

    public record CollectionResult(
        boolean success,
        int maxedCount,
        int totalCollections,
        String errorMessage
    ) {
        static CollectionResult failure(String message) {
            return new CollectionResult(false, 0, 0, message);
        }

        static CollectionResult of(int maxedCount, int totalCollections) {
            return new CollectionResult(true, maxedCount, totalCollections, null);
        }
    }

    public static CompletableFuture<CollectionResult> fetchMaxedCollections(
        UUID playerUuid,
        String apiKey
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            return CompletableFuture.completedFuture(
                CollectionResult.failure("No Hypixel API key configured.")
            );
        }

        String undashed = playerUuid.toString().replace("-", "");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(String.format(PROFILES_URL, undashed)))
            .header("API-Key", apiKey)
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        return HTTP_CLIENT.sendAsync(
            request,
            HttpResponse.BodyHandlers.ofString()
        )
            .thenApply(response ->
                parseCollectionResponse(
                    response.body(),
                    response.statusCode(),
                    playerUuid
                )
            )
            .exceptionally(e -> {
                LOGGER.error(
                    "Failed to fetch SkyBlock collections from Hypixel API.",
                    e
                );
                return CollectionResult.failure(
                    "Network error: " + e.getMessage()
                );
            });
    }

    private static CollectionResult parseCollectionResponse(
        String body,
        int statusCode,
        UUID playerUuid
    ) {
        JsonObject root;
        try {
            root = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            return CollectionResult.failure(
                "Malformed response from Hypixel API (status " + statusCode + ")."
            );
        }

        if (!root.has("success") || !root.get("success").getAsBoolean()) {
            String cause = root.has("cause")
                ? root.get("cause").getAsString()
                : "HTTP " + statusCode;
            return CollectionResult.failure("Hypixel API request failed: " + cause);
        }

        JsonArray profiles = root.getAsJsonArray("profiles");
        if (profiles == null || profiles.isEmpty()) {
            return CollectionResult.failure("Player has no SkyBlock profiles.");
        }

        JsonObject selected = findSelectedProfile(profiles);
        if (selected == null) {
            selected = profiles.get(0).getAsJsonObject();
        }

        JsonObject members = selected.getAsJsonObject("members");
        if (members == null) {
            return CollectionResult.failure("Profile response had no member data.");
        }

        JsonObject member = findMember(members, playerUuid);
        if (member == null) {
            return CollectionResult.failure(
                "Could not find player's member entry in selected profile."
            );
        }

        JsonObject collection = member.has("collection")
            ? member.getAsJsonObject("collection")
            : null;

        if (collection == null) {
            return CollectionResult.of(0, 0);
        }

        int maxedCount = 0;
        int totalChecked = 0;

        for (Map.Entry<String, Long> entry : COLLECTION_MAX_TIERS.entrySet()) {
            String collectionKey = entry.getKey();
            long maxThreshold = entry.getValue();

            if (collection.has(collectionKey)) {
                totalChecked++;
                long playerAmount = collection.get(collectionKey).getAsLong();
                if (playerAmount >= maxThreshold) {
                    maxedCount++;
                }
            }
        }

        return CollectionResult.of(maxedCount, totalChecked);
    }

    // === Dungeon Completions ===

    public record DungeonResult(
        boolean success,
        Map<String, Integer> normalCompletions,  // F1-F7 completions
        Map<String, Integer> masterCompletions,  // M1-M7 completions
        String errorMessage
    ) {
        static DungeonResult failure(String message) {
            return new DungeonResult(false, null, null, message);
        }

        static DungeonResult of(Map<String, Integer> normalCompletions, Map<String, Integer> masterCompletions) {
            return new DungeonResult(true, normalCompletions, masterCompletions, null);
        }
    }

    public static CompletableFuture<DungeonResult> fetchDungeonCompletions(
        UUID playerUuid,
        String apiKey
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            return CompletableFuture.completedFuture(
                DungeonResult.failure("No Hypixel API key configured.")
            );
        }

        String undashed = playerUuid.toString().replace("-", "");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(String.format(PROFILES_URL, undashed)))
            .header("API-Key", apiKey)
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        return HTTP_CLIENT.sendAsync(
            request,
            HttpResponse.BodyHandlers.ofString()
        )
            .thenApply(response ->
                parseDungeonResponse(
                    response.body(),
                    response.statusCode(),
                    playerUuid
                )
            )
            .exceptionally(e -> {
                LOGGER.error(
                    "Failed to fetch SkyBlock dungeon data from Hypixel API.",
                    e
                );
                return DungeonResult.failure(
                    "Network error: " + e.getMessage()
                );
            });
    }

    private static DungeonResult parseDungeonResponse(
        String body,
        int statusCode,
        UUID playerUuid
    ) {
        JsonObject root;
        try {
            root = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            return DungeonResult.failure(
                "Malformed response from Hypixel API (status " + statusCode + ")."
            );
        }

        if (!root.has("success") || !root.get("success").getAsBoolean()) {
            String cause = root.has("cause")
                ? root.get("cause").getAsString()
                : "HTTP " + statusCode;
            return DungeonResult.failure("Hypixel API request failed: " + cause);
        }

        JsonArray profiles = root.getAsJsonArray("profiles");
        if (profiles == null || profiles.isEmpty()) {
            return DungeonResult.failure("Player has no SkyBlock profiles.");
        }

        JsonObject selected = findSelectedProfile(profiles);
        if (selected == null) {
            selected = profiles.get(0).getAsJsonObject();
        }

        JsonObject members = selected.getAsJsonObject("members");
        if (members == null) {
            return DungeonResult.failure("Profile response had no member data.");
        }

        JsonObject member = findMember(members, playerUuid);
        if (member == null) {
            return DungeonResult.failure(
                "Could not find player's member entry in selected profile."
            );
        }

        Map<String, Integer> normalCompletions = new HashMap<>();
        Map<String, Integer> masterCompletions = new HashMap<>();

        // Path: member.dungeons.dungeon_types.catacombs.tier_completions
        JsonObject dungeons = member.has("dungeons")
            ? member.getAsJsonObject("dungeons")
            : null;
        
        if (dungeons != null) {
            JsonObject dungeonTypes = dungeons.has("dungeon_types")
                ? dungeons.getAsJsonObject("dungeon_types")
                : null;
            
            if (dungeonTypes != null) {
                // Normal catacombs
                JsonObject catacombs = dungeonTypes.has("catacombs")
                    ? dungeonTypes.getAsJsonObject("catacombs")
                    : null;
                
                if (catacombs != null && catacombs.has("tier_completions")) {
                    JsonObject tierCompletions = catacombs.getAsJsonObject("tier_completions");
                    for (int floor = 1; floor <= 7; floor++) {
                        String key = String.valueOf(floor);
                        if (tierCompletions.has(key)) {
                            normalCompletions.put("F" + floor, tierCompletions.get(key).getAsInt());
                        }
                    }
                }
                
                // Master mode catacombs
                JsonObject masterCatacombs = dungeonTypes.has("master_catacombs")
                    ? dungeonTypes.getAsJsonObject("master_catacombs")
                    : null;
                
                if (masterCatacombs != null && masterCatacombs.has("tier_completions")) {
                    JsonObject tierCompletions = masterCatacombs.getAsJsonObject("tier_completions");
                    for (int floor = 1; floor <= 7; floor++) {
                        String key = String.valueOf(floor);
                        if (tierCompletions.has(key)) {
                            masterCompletions.put("M" + floor, tierCompletions.get(key).getAsInt());
                        }
                    }
                }
            }
        }

        return DungeonResult.of(normalCompletions, masterCompletions);
    }
}
