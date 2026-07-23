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

// Fetches total skill xp from the Hypixel API for the baseline on login
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
}
