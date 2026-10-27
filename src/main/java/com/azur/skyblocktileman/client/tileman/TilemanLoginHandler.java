package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.dungeon.DungeonTracker;
import com.azur.skyblocktileman.client.tileman.ui.ConfirmScreen;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// fetches skill xp baseline from hypixel api on join
public final class TilemanLoginHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        "TilemanLoginHandler"
    );
    private static final String API_KEY_ENV = "TILEMAN_HYPIXEL_API_KEY";
    private static final String API_KEY_PROP = "tileman.hypixelApiKey";

    private static UUID cachedPlayerUuid;
    private static String cachedApiKey;
    // last profile id we ran a baseline fetch for; island warps re-fire JOIN for the
    // same profile, so we only hit the API when the active profile actually changes
    private static String lastFetchedProfileId = null;
    private static HypixelApiClient.ProfilesResult lastProfilesResult = null;
    private static String promptedProfileId = null;

    private TilemanLoginHandler() {}

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register(TilemanLoginHandler::onJoin);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            XpReconciler.getInstance().reset();
        });
    }

    public static String getApiKey() {
        return cachedApiKey;
    }

    private static void onJoin(
        ClientPacketListener handler,
        PacketSender sender,
        Minecraft client
    ) {
        TilemanConfig config = TilemanConfig.getInstance();
        if (!config.isEnabled() || !HypixelUtil.isConnectedToHypixel(client)) {
            return;
        }

        String apiKey = resolveApiKey();
        cachedApiKey = apiKey;
        cachedPlayerUuid = handler.getLocalGameProfile().id();
        lastProfilesResult = null;
        promptedProfileId = null;

        if (apiKey == null) {
            TilemanLog.debug(
                DebugCategory.PROFILE,
                "No app API key found (property, env, .env, or bundled), skipping baseline fetch."
            );
            return;
        }

        // one profiles request covers baseline xp, collections and dungeon completions
        fetchBaselineIfNeeded();
    }

    // only refetch on a real profile change: island warps re-fire JOIN and would burn the shared key
    public static void fetchBaselineIfNeeded() {
        String apiKey = cachedApiKey;
        UUID playerUuid = cachedPlayerUuid;
        if (apiKey == null || playerUuid == null) {
            return;
        }
        String activeProfile = TilemanState.getInstance().getActiveProfileId();
        if (activeProfile.equals(lastFetchedProfileId)) {
            return;
        }
        // profile genuinely changed: never serve the previous profile's cached payload
        HypixelApiClient.invalidateCache();
        lastProfilesResult = null;
        Minecraft client = Minecraft.getInstance();
        HypixelApiClient.getProfiles(playerUuid, apiKey, 0).thenAccept(result ->
            client.execute(() -> applyProfilesResult(result))
        );
    }

    // resolution order: jvm property, env var, .env in game dir, .env bundled into the jar
    private static String resolveApiKey() {
        String fromProperty = System.getProperty(API_KEY_PROP);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        String fromEnv = System.getenv(API_KEY_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromFile = readApiKeyFrom(
            FabricLoader.getInstance().getGameDir().resolve(".env")
        );
        if (fromFile != null) {
            return fromFile;
        }
        try (
            InputStream in = TilemanLoginHandler.class.getResourceAsStream(
                "/tileman.env"
            )
        ) {
            if (in != null) {
                return extractApiKey(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8)
                );
            }
        } catch (IOException ignored) {}
        return null;
    }

    private static String readApiKeyFrom(Path file) {
        try {
            if (Files.isRegularFile(file)) {
                return extractApiKey(Files.readString(file));
            }
        } catch (IOException ignored) {}
        return null;
    }

    // minimal dotenv parsing: KEY=VALUE lines, # comments, optional quotes
    private static String extractApiKey(String content) {
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq < 0) {
                continue;
            }
            if (!trimmed.substring(0, eq).trim().equals(API_KEY_ENV)) {
                continue;
            }
            String value = trimmed.substring(eq + 1).trim();
            if (
                value.length() >= 2 &&
                ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'")))
            ) {
                value = value.substring(1, value.length() - 1);
            }
            if (!value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static void applyProfilesResult(
        HypixelApiClient.ProfilesResult result
    ) {
        if (!result.success()) {
            LOGGER.warn(
                "Could not fetch Tileman profiles baseline: {}",
                result.errorMessage()
            );
            TilemanChat.warn(
                "Could not fetch Skill XP baseline: " + result.errorMessage()
            );
            return;
        }

        TilemanState state = TilemanState.getInstance();
        state.setActiveProfile(result.profileId());
        lastFetchedProfileId = result.profileId();
        lastProfilesResult = result;

        // the opt-in question waits until we know the player is really on Skyblock
        maybeHandleOptIn();

        if (result.skillXp() != null) {
            for (var entry : result.skillXp().entrySet()) {
                state.setSkillXp(entry.getKey(), entry.getValue());
            }
        }

        XpReconciler.getInstance().initialize(
            cachedPlayerUuid,
            cachedApiKey,
            result.totalSkillXp()
        );

        TilemanLog.debug(
            DebugCategory.PROFILE,
            "Loaded Tileman baseline: profile {} ({}), total Skill XP = {}, tokens = {}",
            result.cuteName(),
            result.profileId(),
            result.totalSkillXp(),
            state.getTokens()
        );
        if (state.isTilemanActive()) {
            TilemanChat.info(
                "Loaded baseline for profile " +
                    result.cuteName() +
                    ": " +
                    String.format("%,d", result.totalSkillXp()) +
                    " total Skill XP, " +
                    state.getTokens() +
                    " token(s) available."
            );
        }

        state.setMaxedCollections(result.maxedCollections());
        TilemanLog.debug(
            DebugCategory.PROFILE,
            "Loaded collections: {} maxed out of {} tracked",
            result.maxedCollections(),
            result.totalCollections()
        );

        DungeonTracker.getInstance().applyProfilesResult(result);
    }

    // true only if the API told us the active profile was created after Tileman was installed
    public static boolean isCurrentProfileNew() {
        HypixelApiClient.ProfilesResult result = lastProfilesResult;
        if (result == null || !result.success()) {
            return false;
        }
        TilemanState state = TilemanState.getInstance();
        if (!result.profileId().equals(state.getActiveProfileId())) {
            return false;
        }
        long createdAtMs = result.createdAtMs();
        return createdAtMs > 0 && createdAtMs >= state.getFirstInstallMs();
    }

    // called from the baseline fetch and from /locraw Skyblock detection; either may arrive second
    public static void maybeHandleOptIn() {
        if (!TilemanIslandHandler.isInSkyblock()) {
            return;
        }
        HypixelApiClient.ProfilesResult result = lastProfilesResult;
        if (result == null || !result.success()) {
            return;
        }
        TilemanState state = TilemanState.getInstance();
        if (!result.profileId().equals(state.getActiveProfileId())) {
            return;
        }
        // one prompt per profile per connection; re-opening would fire onClose on the old screen
        if (result.profileId().equals(promptedProfileId)) {
            return;
        }
        promptedProfileId = result.profileId();
        handleOptInDecision(result);
    }

    // Tileman only runs on profiles created after the mod was installed; new ones get asked once
    private static void handleOptInDecision(
        HypixelApiClient.ProfilesResult result
    ) {
        TilemanState state = TilemanState.getInstance();
        if (state.getTilemanMode() != null) {
            return;
        }

        long createdAtMs = result.createdAtMs();
        if (createdAtMs <= 0) {
            TilemanLog.debug(
                DebugCategory.PROFILE,
                "No profile creation date from API, postponing opt-in decision."
            );
            return;
        }

        if (createdAtMs < state.getFirstInstallMs()) {
            state.setTilemanMode(false);
            TilemanChat.warn(
                "Tileman mode is not available on profile " +
                    result.cuteName() +
                    ": it was created before Tileman was installed. No retroactive rewards."
            );
            return;
        }

        ConfirmScreen.open(
            "Enable Tileman mode?",
            "New profile detected. You may only stand on blocks you unlock with tokens, earned from XP from now on.",
            () -> {
                state.setTilemanMode(true);
                TilemanChat.info(
                    "Tileman mode enabled for profile " +
                        result.cuteName() +
                        ". Good luck."
                );
            },
            () -> {
                state.setTilemanMode(false);
                TilemanChat.warn(
                    "Tileman mode declined for profile " +
                        result.cuteName() +
                        ". Use /tileman optin on to change your mind."
                );
            }
        );
    }
}
