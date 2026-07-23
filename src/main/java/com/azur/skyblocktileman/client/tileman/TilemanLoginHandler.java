package com.azur.skyblocktileman.client.tileman;

import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Fetches a skill xp baseline from the Hypixel API when player joims Hypixel
public final class TilemanLoginHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        "TilemanLoginHandler"
    );
    private static final String API_KEY_ENV = "TILEMAN_HYPIXEL_API_KEY";
    private static final String API_KEY_PROP = "tileman.hypixelApiKey";

    private static UUID cachedPlayerUuid;
    private static String cachedApiKey;

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

    public static UUID getPlayerUuid() {
        return cachedPlayerUuid;
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

        if (apiKey == null) {
            TilemanLog.debug(
                "No app API key configured (system property '{}' or env '{}'), skipping baseline fetch.",
                API_KEY_PROP,
                API_KEY_ENV
            );
            return;
        }

        UUID playerUuid = cachedPlayerUuid;

        HypixelApiClient.fetchBaselineSkillXp(
            playerUuid,
            apiKey
        ).thenAccept(result -> client.execute(() -> applyResult(result)));
    }

    private static String resolveApiKey() {
        String fromProperty = System.getProperty(API_KEY_PROP);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        String fromEnv = System.getenv(API_KEY_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        return "801dfaca-f395-45d7-8a69-f1b3333721f3";
    }

    private static void applyResult(HypixelApiClient.BaselineResult result) {
        if (!result.success()) {
            LOGGER.warn(
                "Could not fetch Tileman Skill XP baseline: {}",
                result.errorMessage()
            );
            TilemanChat.warn(
                "Could not fetch Skill XP baseline: " + result.errorMessage()
            );
            return;
        }

        TilemanState state = TilemanState.getInstance();
        state.setActiveProfile(result.profileId());

        XpReconciler.getInstance().initialize(
            cachedPlayerUuid,
            cachedApiKey,
            result.totalSkillXp()
        );

        TilemanLog.debug(
            "Loaded Tileman baseline: profile {} ({}), total Skill XP = {}, tokens = {}",
            result.cuteName(),
            result.profileId(),
            result.totalSkillXp(),
            state.getTokens()
        );
        TilemanChat.info(
            "Loaded baseline for profile " +
                result.cuteName() +
                ": " +
                String.format("%,.0f", result.totalSkillXp()) +
                " total Skill XP, " +
                state.getTokens() +
                " token(s) available."
        );
    }
}
