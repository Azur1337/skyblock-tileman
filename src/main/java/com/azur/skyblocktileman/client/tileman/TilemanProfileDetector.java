package com.azur.skyblocktileman.client.tileman;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

public final class TilemanProfileDetector {

    private static final Set<String> PROFILE_NAMES = Set.of(
        "Apple", "Banana", "Blueberry", "Coconut", "Cucumber", "Grapes",
        "Kiwi", "Lemon", "Lime", "Mango", "Orange", "Papaya", "Peach",
        "Pear", "Pineapple", "Pomegranate", "Raspberry", "Strawberry",
        "Tomato", "Watermelon", "Zucchini"
    );

    private static final Pattern PROFILE_SWITCH_PATTERN = Pattern.compile(
        "Switching to profile (\\w+)\\.\\.\\."
    );

    private static final Pattern PROFILE_ID_PATTERN = Pattern.compile(
        "Profile ID: ([a-f0-9-]+)"
    );

    private static String pendingProfileName = null;

    private TilemanProfileDetector() {}

    public static void register() {
        ClientReceiveMessageEvents.GAME.register(TilemanProfileDetector::onGameMessage);
    }

    private static void onGameMessage(Component message, boolean overlay) {
        if (overlay) {
            return;
        }

        String text = message.getString();
        
        // Skip messages sent by this mod to prevent infinite recursion
        if (text.contains("[Tileman]")) {
            return;
        }

        Matcher switchMatcher = PROFILE_SWITCH_PATTERN.matcher(text);
        if (switchMatcher.find()) {
            String profileName = switchMatcher.group(1);
            if (PROFILE_NAMES.contains(profileName)) {
                pendingProfileName = profileName;
                TilemanLog.debug(DebugCategory.PROFILE, "Detected profile switch to {}, waiting for ID", profileName);
            }
            return;
        }

        Matcher idMatcher = PROFILE_ID_PATTERN.matcher(text);
        if (idMatcher.find()) {
            String profileId = idMatcher.group(1);
            String currentProfile = TilemanState.getInstance().getActiveProfileId();
            
            if (!profileId.equals(currentProfile)) {
                TilemanState.getInstance().setActiveProfile(profileId);
                SkillXpParser.resetTracking();
                
                if (pendingProfileName != null) {
                    TilemanLog.debug(DebugCategory.PROFILE, "Switched to profile {} ({})", pendingProfileName, profileId);
                    TilemanChat.info("Switched to Tileman profile: " + pendingProfileName);
                } else {
                    TilemanLog.debug(DebugCategory.PROFILE, "Joined profile {}", profileId);
                    TilemanChat.info("Loaded Tileman profile: " + profileId);
                }
            }
            pendingProfileName = null;
        }
    }
}
