package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.slayer.SlayerFeature;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

public final class TilemanProfileDetector {

    private static final Set<String> PROFILE_NAMES = Set.of(
        "Apple",
        "Banana",
        "Blueberry",
        "Coconut",
        "Cucumber",
        "Grapes",
        "Kiwi",
        "Lemon",
        "Lime",
        "Mango",
        "Orange",
        "Papaya",
        "Peach",
        "Pear",
        "Pineapple",
        "Pomegranate",
        "Raspberry",
        "Strawberry",
        "Tomato",
        "Watermelon",
        "Zucchini"
    );

    private static final Pattern PROFILE_SWITCH_PATTERN = Pattern.compile(
        "Switching to profile (\\w+)\\.\\.\\."
    );

    private static final Pattern PROFILE_ID_PATTERN = Pattern.compile(
        "Profile ID: ([a-f0-9-]+)"
    );

    private static final long PENDING_NAME_EXPIRY_MS = 60_000;

    private static String pendingProfileName = null;
    private static long pendingProfileNameAtMs = 0;
    private static String pendingApiProfileId = null;

    private TilemanProfileDetector() {}

    public static void register() {
        ClientReceiveMessageEvents.GAME.register(
            TilemanProfileDetector::onGameMessage
        );
    }

    private static void onGameMessage(Component message, boolean overlay) {
        if (overlay) {
            return;
        }

        String text = message.getString();

        if (text.contains("[Tileman]")) {
            return;
        }

        Matcher switchMatcher = PROFILE_SWITCH_PATTERN.matcher(text);
        if (switchMatcher.find()) {
            String profileName = switchMatcher.group(1);
            if (PROFILE_NAMES.contains(profileName)) {
                pendingProfileName = profileName;
                pendingProfileNameAtMs = System.currentTimeMillis();
                TilemanLog.debug(
                    DebugCategory.PROFILE,
                    "Detected profile switch to {}, waiting for ID",
                    profileName
                );
            }
            return;
        }

        Matcher idMatcher = PROFILE_ID_PATTERN.matcher(text);
        if (idMatcher.find()) {
            handleProfileId(idMatcher.group(1), null, "chat");
        }
    }

    // Adopts a profile id from any authoritative source: the chat Profile ID line or
    // the profile field in a /locraw reply, so chat mods can't strand Tileman on a
    // stale profile.
    static void handleProfileId(
        String profileId,
        String cuteName,
        String source
    ) {
        if (profileId == null || profileId.isEmpty()) {
            return;
        }
        TilemanState state = TilemanState.getInstance();
        String currentProfile = state.getActiveProfileId();
        if (profileId.equals(currentProfile)) {
            pendingProfileName = null;
            pendingApiProfileId = null;
            return;
        }

        boolean firstDetection = "unknown".equals(currentProfile);
        TilemanLog.debug(
            DebugCategory.PROFILE,
            "Profile switch detected via {}: {} -> {}",
            source,
            currentProfile,
            profileId
        );
        state.setActiveProfile(profileId);
        // a live slayer quest belongs to the previous profile, drop it
        SlayerFeature.reset();
        // profile changed after JOIN already ran its skip-check; re-sync baseline, unless the
        // adoption came from the API result itself (it already is the fresh baseline)
        if (!"api".equals(source)) {
            TilemanLoginHandler.fetchBaselineIfNeeded();
        }
        pendingApiProfileId = null;

        String name = cuteName != null ? cuteName : pendingProfileName;
        if (name != null && !firstDetection) {
            TilemanChat.info("Switched to Tileman profile: " + name);
        } else if (name != null) {
            TilemanChat.info("Loaded Tileman profile: " + name);
        } else {
            TilemanChat.info("Loaded Tileman profile: " + profileId);
        }
        pendingProfileName = null;
    }

    // True while chat saw a profile switch but the Profile ID line never arrived, e.g. a
    // chat mod ate it. Expires so a lost line cannot pin the warp-skip on the baseline fetch.
    static boolean isAwaitingProfileId() {
        return (
            pendingProfileName != null &&
            System.currentTimeMillis() - pendingProfileNameAtMs <
                PENDING_NAME_EXPIRY_MS
        );
    }

    // Backstop when chat mods eat both the Profile ID line and the locraw reply: the
    // profiles API knows which profile is selected. Adopt on a cute-name match with a
    // pending switch, else require two consecutive identical mismatches.
    public static void confirmFromApi(String profileId, String cuteName) {
        if (profileId == null || profileId.isEmpty()) {
            return;
        }
        String current = TilemanState.getInstance().getActiveProfileId();
        if (profileId.equals(current)) {
            pendingApiProfileId = null;
            return;
        }
        if (
            (cuteName != null && cuteName.equals(pendingProfileName)) ||
            profileId.equals(pendingApiProfileId)
        ) {
            handleProfileId(profileId, cuteName, "api");
        } else {
            pendingApiProfileId = profileId;
            TilemanLog.debug(
                DebugCategory.PROFILE,
                "API selected profile {} differs from active {}, awaiting confirmation",
                profileId,
                current
            );
        }
    }
}
