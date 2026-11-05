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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// one profiles request serves baseline xp, collections and dungeon completions;
// every live endpoint shares the same 300 req / 5 min counter, so we never fetch twice
public final class HypixelApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        "HypixelApiClient"
    );

    private static final String PROFILES_URL =
        "https://api.hypixel.net/v2/skyblock/profiles?uuid=%s";

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

    // after a 429 we skip all requests until the window resets; a 429 still costs 1 request
    private static volatile long backoffUntilMs = 0;

    // last successful profiles payload, shared by all consumers to avoid duplicate requests
    private static UUID cachedUuid;
    private static ProfilesResult cachedResult;
    private static long cachedAtMs;
    private static CompletableFuture<ProfilesResult> inFlight;

    // bug protection: one client must never hammer the shared key, worst case 120 req/h
    private static final long CLIENT_MIN_INTERVAL_MS = 30_000;
    private static long lastRequestStartedMs = 0;

    private HypixelApiClient() {}

    // Cached entry point: serves the payload if younger than maxAgeMs, else joins or starts a fetch.
    public static synchronized CompletableFuture<ProfilesResult> getProfiles(
        UUID playerUuid,
        String apiKey,
        long maxAgeMs
    ) {
        long now = System.currentTimeMillis();
        if (
            cachedResult != null &&
            cachedResult.success() &&
            playerUuid.equals(cachedUuid) &&
            now - cachedAtMs < maxAgeMs
        ) {
            return CompletableFuture.completedFuture(cachedResult);
        }
        if (inFlight != null) {
            return inFlight;
        }
        if (now - lastRequestStartedMs < CLIENT_MIN_INTERVAL_MS) {
            // serve stale data rather than spam the shared counter; processing is delta-based
            if (cachedResult != null && playerUuid.equals(cachedUuid)) {
                return CompletableFuture.completedFuture(cachedResult);
            }
            return CompletableFuture.completedFuture(
                ProfilesResult.failure(
                    "Requests too frequent, try again later."
                )
            );
        }
        lastRequestStartedMs = now;

        CompletableFuture<ProfilesResult> request = fetchProfiles(
            playerUuid,
            apiKey
        ).whenComplete((result, error) -> {
            synchronized (HypixelApiClient.class) {
                inFlight = null;
                if (result != null && result.success()) {
                    cachedUuid = playerUuid;
                    cachedResult = result;
                    cachedAtMs = System.currentTimeMillis();
                }
            }
        });
        inFlight = request;
        return request;
    }

    public static synchronized ProfilesResult peekProfiles() {
        return cachedResult;
    }

    // Used on a real profile switch so the next fetch always goes to the network.
    public static synchronized void invalidateCache() {
        cachedResult = null;
        cachedUuid = null;
        lastRequestStartedMs = 0;
        // do not let a switch join an in-flight request started for the old profile;
        // if one still applies its stale result afterwards, the fresh fetch follows it
        inFlight = null;
    }

    public record ProfilesResult(
        boolean success,
        String profileId,
        String cuteName,
        Map<String, Long> skillXp,
        Map<String, Integer> normalCompletions,
        Map<String, Integer> masterCompletions,
        long createdAtMs,
        String errorMessage
    ) {
        static ProfilesResult failure(String message) {
            return new ProfilesResult(
                false,
                null,
                null,
                null,
                null,
                null,
                0L,
                message
            );
        }

        public long totalSkillXp() {
            if (skillXp == null) return 0;
            return skillXp.values().stream().mapToLong(Long::longValue).sum();
        }
    }

    private static CompletableFuture<ProfilesResult> fetchProfiles(
        UUID playerUuid,
        String apiKey
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            return CompletableFuture.completedFuture(
                ProfilesResult.failure("No Hypixel API key configured.")
            );
        }

        long now = System.currentTimeMillis();
        if (now < backoffUntilMs) {
            long remaining = (backoffUntilMs - now + 999) / 1000;
            return CompletableFuture.completedFuture(
                ProfilesResult.failure(
                    "Rate limited, backing off for " + remaining + "s."
                )
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
            .thenApply(response -> {
                if (response.statusCode() == 429) {
                    applyBackoff(response);
                    return ProfilesResult.failure("Rate limited (HTTP 429).");
                }
                return parseProfiles(
                    response.body(),
                    response.statusCode(),
                    playerUuid
                );
            })
            .exceptionally(e -> {
                LOGGER.error(
                    "Failed to fetch SkyBlock profiles from Hypixel API.",
                    e
                );
                return ProfilesResult.failure(
                    "Network error: " + e.getMessage()
                );
            });
    }

    private static void applyBackoff(HttpResponse<String> response) {
        long resetSeconds = response
            .headers()
            .firstValue("ratelimit-reset")
            .map(HypixelApiClient::parseLongOr)
            .orElse(60L);
        backoffUntilMs = System.currentTimeMillis() + (resetSeconds + 5) * 1000;
        LOGGER.warn(
            "Hypixel API rate limit hit, backing off for {} seconds",
            resetSeconds
        );
    }

    private static long parseLongOr(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 60L;
        }
    }

    private static ProfilesResult parseProfiles(
        String body,
        int statusCode,
        UUID playerUuid
    ) {
        JsonObject root;
        try {
            root = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            return ProfilesResult.failure(
                "Malformed response from Hypixel API (status " +
                    statusCode +
                    ")."
            );
        }

        if (!root.has("success") || !root.get("success").getAsBoolean()) {
            String cause = root.has("cause")
                ? root.get("cause").getAsString()
                : "HTTP " + statusCode;
            return ProfilesResult.failure(
                "Hypixel API request failed: " + cause
            );
        }

        JsonElement profilesElement = root.get("profiles");
        // unknown or fresh accounts answer 200 with profiles:null
        if (
            profilesElement == null ||
            !profilesElement.isJsonArray() ||
            profilesElement.getAsJsonArray().isEmpty()
        ) {
            return ProfilesResult.failure("Player has no SkyBlock profiles.");
        }
        JsonArray profiles = profilesElement.getAsJsonArray();

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
            return ProfilesResult.failure(
                "Profile response had no member data."
            );
        }

        JsonObject member = findMember(members, playerUuid);
        if (member == null) {
            return ProfilesResult.failure(
                "Could not find this player's member entry in the selected profile."
            );
        }

        Map<String, Long> skillXp = getSkillXpMap(member);

        Map<String, Integer> normalCompletions = new HashMap<>();
        Map<String, Integer> masterCompletions = new HashMap<>();
        extractDungeonCompletions(member, normalCompletions, masterCompletions);

        // profile created_at when present, else this player's first join on the profile
        long createdAtMs = 0L;
        if (selected.has("created_at")) {
            createdAtMs = selected.get("created_at").getAsLong();
        } else if (member.has("first_join")) {
            createdAtMs = member.get("first_join").getAsLong();
        }

        return new ProfilesResult(
            true,
            profileId,
            cuteName,
            skillXp,
            normalCompletions,
            masterCompletions,
            createdAtMs,
            null
        );
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
            for (Map.Entry<
                String,
                String
            > entry : SKILL_KEY_TO_NAME.entrySet()) {
                if (experience.has(entry.getKey())) {
                    result.put(
                        entry.getValue(),
                        (long) experience.get(entry.getKey()).getAsDouble()
                    );
                }
            }
        }

        if (result.isEmpty()) {
            for (Map.Entry<
                String,
                String
            > entry : LEGACY_KEY_TO_NAME.entrySet()) {
                if (member.has(entry.getKey())) {
                    result.put(
                        entry.getValue(),
                        (long) member.get(entry.getKey()).getAsDouble()
                    );
                }
            }
        }

        return result;
    }

    // member.dungeons.dungeon_types.[master_]catacombs.tier_completions
    private static void extractDungeonCompletions(
        JsonObject member,
        Map<String, Integer> normalOut,
        Map<String, Integer> masterOut
    ) {
        JsonObject dungeons = member.has("dungeons")
            ? member.getAsJsonObject("dungeons")
            : null;
        if (dungeons == null) return;

        JsonObject dungeonTypes = dungeons.has("dungeon_types")
            ? dungeons.getAsJsonObject("dungeon_types")
            : null;
        if (dungeonTypes == null) return;

        readTierCompletions(dungeonTypes, "catacombs", "F", normalOut);
        readTierCompletions(dungeonTypes, "master_catacombs", "M", masterOut);
    }

    private static void readTierCompletions(
        JsonObject dungeonTypes,
        String typeKey,
        String floorPrefix,
        Map<String, Integer> out
    ) {
        JsonObject type = dungeonTypes.has(typeKey)
            ? dungeonTypes.getAsJsonObject(typeKey)
            : null;
        if (type == null || !type.has("tier_completions")) return;

        JsonObject tierCompletions = type.getAsJsonObject("tier_completions");
        for (int floor = 1; floor <= 7; floor++) {
            String key = String.valueOf(floor);
            if (tierCompletions.has(key)) {
                out.put(
                    floorPrefix + floor,
                    tierCompletions.get(key).getAsInt()
                );
            }
        }
    }
}
