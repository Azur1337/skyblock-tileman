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

    private static final Set<String> LEGACY_SKILL_XP_KEYS = Set.of(
        "experience_skill_farming",
        "experience_skill_mining",
        "experience_skill_combat",
        "experience_skill_foraging",
        "experience_skill_fishing",
        "experience_skill_enchanting",
        "experience_skill_alchemy",
        "experience_skill_taming",
        "experience_skill_carpentry",
        "experience_skill_runecrafting",
        "experience_skill_social2",
        "experience_skill_hunting"
    );

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private HypixelApiClient() {}

    public record BaselineResult(
        boolean success,
        String profileId,
        String cuteName,
        double totalSkillXp,
        String errorMessage
    ) {
        static BaselineResult failure(String message) {
            return new BaselineResult(false, null, null, 0, message);
        }

        static BaselineResult of(
            String profileId,
            String cuteName,
            double totalSkillXp
        ) {
            return new BaselineResult(
                true,
                profileId,
                cuteName,
                totalSkillXp,
                null
            );
        }
    }

    public record XpFetchResult(
        boolean success,
        double totalSkillXp,
        String errorMessage
    ) {
        static XpFetchResult failure(String message) {
            return new XpFetchResult(false, 0, message);
        }

        static XpFetchResult of(double totalSkillXp) {
            return new XpFetchResult(true, totalSkillXp, null);
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

        double totalXp = sumSkillXp(member);
        return XpFetchResult.of(totalXp);
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

        double totalXp = sumSkillXp(member);
        return BaselineResult.of(profileId, cuteName, totalXp);
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

    private static double sumSkillXp(JsonObject member) {
        double total = 0;

        JsonObject playerData = member.has("player_data")
            ? member.getAsJsonObject("player_data")
            : null;
        JsonObject experience =
            playerData != null && playerData.has("experience")
                ? playerData.getAsJsonObject("experience")
                : null;

        if (experience != null) {
            for (String key : SKILL_XP_KEYS) {
                if (experience.has(key)) {
                    total += experience.get(key).getAsDouble();
                }
            }
        }

        if (total == 0) {
            for (String key : LEGACY_SKILL_XP_KEYS) {
                if (member.has(key)) {
                    total += member.get(key).getAsDouble();
                }
            }
        }

        return total;
    }
}
