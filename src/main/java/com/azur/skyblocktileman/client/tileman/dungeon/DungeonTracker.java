package com.azur.skyblocktileman.client.tileman.dungeon;

import com.azur.skyblocktileman.client.tileman.DebugCategory;
import com.azur.skyblocktileman.client.tileman.HypixelApiClient;
import com.azur.skyblocktileman.client.tileman.HypixelUtil;
import com.azur.skyblocktileman.client.tileman.TilemanChat;
import com.azur.skyblocktileman.client.tileman.TilemanConfig;
import com.azur.skyblocktileman.client.tileman.TilemanLog;
import com.azur.skyblocktileman.client.tileman.TilemanLoginHandler;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

public class DungeonTracker {

    private static DungeonTracker instance;

    private static final long AUTO_SYNC_INTERVAL_MS = 2 * 60 * 1000;
    private static final long MANUAL_SYNC_COOLDOWN_MS = 60 * 1000;

    // time of the last dispatched fetch (auto or manual), used by both timers
    private long lastSyncTimeMs = 0;

    public static DungeonTracker getInstance() {
        if (instance == null) {
            instance = new DungeonTracker();
        }
        return instance;
    }

    private DungeonTracker() {}

    public boolean tryUnlockFloor(DungeonFloor floor) {
        if (floor == null) return false;

        DungeonData data = getDungeonData();

        if (data.isUnlocked(floor)) {
            TilemanChat.warn(floor.getDisplayName() + " is already unlocked!");
            return false;
        }

        int cost = floor.getUnlockCost();
        int tokens = TilemanState.getInstance().getTokens();

        if (tokens < cost) {
            TilemanChat.warn(
                "Not enough tokens! Need " + cost + ", have " + tokens
            );
            return false;
        }

        TilemanState.getInstance().spendTokens(cost);
        data.unlock(floor);
        TilemanState.getInstance().save();

        TilemanChat.info(
            "§6§lFLOOR UNLOCKED! §r" +
                floor.getDisplayName() +
                " §7(-" +
                cost +
                " tokens)"
        );

        Minecraft client = Minecraft.getInstance();
        if (
            client.player != null &&
            TilemanConfig.getInstance().isMilestoneSoundEnabled()
        ) {
            client.player.playSound(
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                1.0f,
                1.0f
            );
        }

        return true;
    }

    private DungeonData getDungeonData() {
        return TilemanState.getInstance().getDungeonData();
    }

    public void fetchAndUpdateCompletions() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        lastSyncTimeMs = System.currentTimeMillis();

        UUID playerUuid = client.player.getUUID();
        String apiKey = TilemanLoginHandler.getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            TilemanLog.debug(
                DebugCategory.DUNGEON,
                "No API key, skipping dungeon fetch"
            );
            return;
        }

        HypixelApiClient.getProfiles(playerUuid, apiKey, 0).thenAccept(
            result -> {
                if (!result.success()) {
                    TilemanLog.debug(
                        DebugCategory.DUNGEON,
                        "Failed to fetch dungeons: {}",
                        result.errorMessage()
                    );
                    return;
                }

                client.execute(() -> processApiResult(result));
            }
        );
    }

    public void applyProfilesResult(HypixelApiClient.ProfilesResult result) {
        lastSyncTimeMs = System.currentTimeMillis();
        if (!result.success()) {
            return;
        }
        processApiResult(result);
    }

    public void tickAutoSync(Minecraft client) {
        if (
            System.currentTimeMillis() - lastSyncTimeMs < AUTO_SYNC_INTERVAL_MS
        ) {
            return;
        }
        if (!HypixelUtil.isConnectedToHypixel(client)) {
            return;
        }
        fetchAndUpdateCompletions();
    }

    public boolean requestManualSync() {
        if (getManualSyncCooldownSeconds() > 0) {
            TilemanChat.warn(
                "Sync on cooldown, try again in " +
                    getManualSyncCooldownSeconds() +
                    "s"
            );
            return false;
        }
        fetchAndUpdateCompletions();
        return true;
    }

    public long getManualSyncCooldownSeconds() {
        long elapsed = System.currentTimeMillis() - lastSyncTimeMs;
        if (elapsed >= MANUAL_SYNC_COOLDOWN_MS) {
            return 0;
        }
        return (MANUAL_SYNC_COOLDOWN_MS - elapsed + 999) / 1000;
    }

    private void processApiResult(HypixelApiClient.ProfilesResult result) {
        // never attribute an API result to the wrong profile (auto/manual sync can race a switch)
        String activeProfile = TilemanState.getInstance().getActiveProfileId();
        if (
            result.profileId() != null &&
            !result.profileId().equals(activeProfile)
        ) {
            TilemanLog.debug(
                DebugCategory.DUNGEON,
                "Skipping dungeon result for profile {} (active is {})",
                result.profileId(),
                activeProfile
            );
            return;
        }

        DungeonData data = getDungeonData();

        // History is never rewarded: the first API result seeds the counter
        if (!data.isBaselineFetched()) {
            TilemanLog.debug(
                DebugCategory.DUNGEON,
                "Setting dungeon baseline from API"
            );

            if (result.normalCompletions() != null) {
                for (Map.Entry<String, Integer> entry : result
                    .normalCompletions()
                    .entrySet()) {
                    data.setRewardedCompletions(
                        entry.getKey(),
                        entry.getValue()
                    );
                }
            }
            if (result.masterCompletions() != null) {
                for (Map.Entry<String, Integer> entry : result
                    .masterCompletions()
                    .entrySet()) {
                    data.setRewardedCompletions(
                        entry.getKey(),
                        entry.getValue()
                    );
                }
            }

            data.setBaselineFetched(true);
            TilemanState.getInstance().save();
            TilemanChat.info("§7Dungeon baseline set from API.");
            return;
        }

        int totalTokensAwarded = 0;

        if (result.normalCompletions() != null) {
            for (DungeonFloor floor : DungeonFloor.getNormalFloors()) {
                int apiCompletions = result
                    .normalCompletions()
                    .getOrDefault(floor.getId(), 0);
                totalTokensAwarded += processFloorCompletions(
                    floor,
                    apiCompletions,
                    data
                );
            }
        }

        if (result.masterCompletions() != null) {
            for (DungeonFloor floor : DungeonFloor.getMasterModeFloors()) {
                int apiCompletions = result
                    .masterCompletions()
                    .getOrDefault(floor.getId(), 0);
                totalTokensAwarded += processFloorCompletions(
                    floor,
                    apiCompletions,
                    data
                );
            }
        }

        if (totalTokensAwarded > 0) {
            TilemanState.getInstance().save();
            TilemanChat.info(
                "§a§lDUNGEON REWARDS! §r+" +
                    totalTokensAwarded +
                    " tokens from completions"
            );

            Minecraft client = Minecraft.getInstance();
            if (
                client.player != null &&
                TilemanConfig.getInstance().isDungeonRewardSoundEnabled()
            ) {
                client.player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }

        TilemanLog.debug(
            DebugCategory.DUNGEON,
            "Dungeon completions updated from API"
        );
    }

    private int processFloorCompletions(
        DungeonFloor floor,
        int currentApiCompletions,
        DungeonData data
    ) {
        int rewardedSoFar = data.getRewardedCompletions(floor);

        if (!data.isUnlocked(floor)) {
            // locked: consume the count so clears done while locked are never paid on unlock
            if (currentApiCompletions > rewardedSoFar) {
                data.setRewardedCompletions(floor, currentApiCompletions);
            }
            return 0;
        }

        int newCompletions = currentApiCompletions - rewardedSoFar;

        if (newCompletions <= 0) {
            return 0;
        }

        TilemanLog.debug(
            DebugCategory.DUNGEON,
            "Floor {} has {} new completions (was {}, now {})",
            floor.getId(),
            newCompletions,
            rewardedSoFar,
            currentApiCompletions
        );

        int totalTokens = 0;

        for (int i = 0; i < newCompletions; i++) {
            int completionNumber = rewardedSoFar + i; // 0-indexed completion count
            int reward = floor.getRewardForCompletion(completionNumber);
            totalTokens += reward;
        }

        // local completion count tracks every clear, not just the rewarded ones
        DungeonData.FloorStats stats = data.getOrCreateFloorStats(floor);
        stats.completions += newCompletions;

        if (totalTokens > 0) {
            TilemanState.getInstance().addTokens(totalTokens);
            TilemanLog.debug(
                DebugCategory.DUNGEON,
                "Awarded {} tokens for {} on {} completions",
                totalTokens,
                floor.getId(),
                newCompletions
            );
        }

        data.setRewardedCompletions(floor, currentApiCompletions);

        return totalTokens;
    }
}
