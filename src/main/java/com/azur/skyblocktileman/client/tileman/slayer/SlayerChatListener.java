package com.azur.skyblocktileman.client.tileman.slayer;

import com.azur.skyblocktileman.client.tileman.DebugCategory;
import com.azur.skyblocktileman.client.tileman.TilemanLog;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class SlayerChatListener {

    private SlayerChatListener() {}

    private static final Pattern QUEST_STARTED = Pattern.compile(
        "SLAYER QUEST STARTED!",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern QUEST_COMPLETE = Pattern.compile(
        "SLAYER QUEST COMPLETE!",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern QUEST_FAILED = Pattern.compile(
        "SLAYER QUEST FAILED!",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SLAY_REQUIREMENT = Pattern.compile(
        "Slay ([0-9,.kKmM]+) Combat XP worth of (\\w+)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BOSS_SPAWNED = Pattern.compile(
        "Boss Spawned",
        Pattern.CASE_INSENSITIVE
    );

    private static SlayerType pendingType = null;
    private static int pendingTier = 0;

    public static void register() {
        ClientReceiveMessageEvents.GAME.register(
            SlayerChatListener::onGameMessage
        );
    }

    private static void onGameMessage(Component message, boolean overlay) {
        if (!TilemanState.getInstance().isTilemanActive()) {
            return;
        }

        String text = message.getString();
        String stripped = ChatFormatting.stripFormatting(text);
        if (stripped == null) return;

        // the mod's own chat output is not a slayer signal
        if (stripped.startsWith("[Tileman]")) {
            return;
        }

        if (overlay) {
            if (BOSS_SPAWNED.matcher(stripped).find()) {
                TilemanLog.debug(
                    DebugCategory.SLAYER,
                    "[CHAT] Boss spawned detected from overlay: {}",
                    stripped
                );
                SlayerTracker.getInstance().onBossSpawned();
            }
            return;
        }

        if (QUEST_STARTED.matcher(stripped).find()) {
            TilemanLog.debug(
                DebugCategory.SLAYER,
                "[CHAT] Quest started detected, pending type={}, tier={}",
                pendingType,
                pendingTier
            );
            // Type/tier should have been detected from the previous slay requirement message
            SlayerTracker.getInstance().onQuestStarted(
                pendingType,
                pendingTier
            );
            // Don't reset pending - the requirement message might come after STARTED
            return;
        }

        Matcher slayMatcher = SLAY_REQUIREMENT.matcher(stripped);
        if (slayMatcher.find()) {
            String xpStr = slayMatcher.group(1);
            String mobType = slayMatcher.group(2);
            SlayerType type = SlayerType.fromDisplayName(mobType);
            int xpRequired = parseXpAmount(xpStr);
            int tier = getTierFromXp(type, xpRequired);

            TilemanLog.debug(
                DebugCategory.SLAYER,
                "[CHAT] Slay requirement: mob={}, xpStr={}, xp={}, type={}, tier={}",
                mobType,
                xpStr,
                xpRequired,
                type,
                tier
            );

            if (type != null) {
                pendingType = type;
                pendingTier = tier;

                SlayerTracker tracker = SlayerTracker.getInstance();
                if (tracker.isQuestActive()) {
                    TilemanLog.debug(
                        DebugCategory.SLAYER,
                        "[CHAT] Updating active quest with type/tier"
                    );
                    tracker.updateQuestInfo(type, tier);
                }
            }
            return;
        }

        if (QUEST_COMPLETE.matcher(stripped).find()) {
            TilemanLog.debug(
                DebugCategory.SLAYER,
                "[CHAT] Quest COMPLETE detected"
            );
            SlayerTracker.getInstance().onQuestComplete();
            pendingType = null;
            pendingTier = 0;
            return;
        }

        if (QUEST_FAILED.matcher(stripped).find()) {
            TilemanLog.debug(
                DebugCategory.SLAYER,
                "[CHAT] Quest FAILED detected"
            );
            SlayerTracker.getInstance().onQuestFailed();
            pendingType = null;
            pendingTier = 0;
            return;
        }

        if (BOSS_SPAWNED.matcher(stripped).find()) {
            TilemanLog.debug(
                DebugCategory.SLAYER,
                "[CHAT] Boss spawned detected from chat: {}",
                stripped
            );
            SlayerTracker.getInstance().onBossSpawned();
        }
    }

    private static int parseXpAmount(String xpStr) {
        if (xpStr == null || xpStr.isEmpty()) return 0;

        xpStr = xpStr.replace(",", "").toLowerCase();

        double multiplier = 1;
        if (xpStr.endsWith("k")) {
            multiplier = 1000;
            xpStr = xpStr.substring(0, xpStr.length() - 1);
        } else if (xpStr.endsWith("m")) {
            multiplier = 1000000;
            xpStr = xpStr.substring(0, xpStr.length() - 1);
        }

        try {
            return (int) (Double.parseDouble(xpStr) * multiplier);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int getTierFromXp(SlayerType type, int xp) {
        if (type == null || xp <= 0) return 1;

        return switch (type) {
            case REVENANT -> {
                if (xp >= 100000) yield 5;
                if (xp >= 50000) yield 4;
                if (xp >= 5000) yield 3;
                if (xp >= 1500) yield 2;
                yield 1;
            }
            case TARANTULA -> {
                if (xp >= 10000) yield 4;
                if (xp >= 2500) yield 3;
                if (xp >= 1000) yield 2;
                yield 1;
            }
            case SVEN -> {
                if (xp >= 20000) yield 4;
                if (xp >= 4000) yield 3;
                if (xp >= 1500) yield 2;
                yield 1;
            }
            case VOIDGLOOM -> {
                if (xp >= 100000) yield 4;
                if (xp >= 25000) yield 3;
                if (xp >= 5000) yield 2;
                yield 1;
            }
            case INFERNO -> {
                if (xp >= 50000) yield 5;
                if (xp >= 33600) yield 4;
                if (xp >= 15000) yield 3;
                if (xp >= 7500) yield 2;
                yield 1;
            }
            case RIFTSTALKER -> {
                if (xp >= 200000) yield 5;
                if (xp >= 100000) yield 4;
                if (xp >= 20000) yield 3;
                if (xp >= 5000) yield 2;
                yield 1;
            }
        };
    }

    public static void onScoreboardSlayerInfo(String slayerLine) {
        TilemanLog.debug(
            DebugCategory.SLAYER,
            "[CHAT] onScoreboardSlayerInfo called with: '{}'",
            slayerLine
        );

        if (slayerLine == null) return;

        SlayerType type = SlayerType.fromDisplayName(slayerLine);
        int tier = SlayerType.parseTier(slayerLine);

        TilemanLog.debug(
            DebugCategory.SLAYER,
            "[CHAT] Parsed scoreboard info: type={}, tier={}",
            type,
            tier
        );

        if (type != null && tier > 0) {
            pendingType = type;
            pendingTier = tier;

            SlayerTracker tracker = SlayerTracker.getInstance();
            TilemanLog.debug(
                DebugCategory.SLAYER,
                "[CHAT] Tracker state: active={}",
                tracker.isQuestActive()
            );

            if (tracker.isQuestActive()) {
                TilemanLog.debug(
                    DebugCategory.SLAYER,
                    "[CHAT] Calling tracker.updateQuestInfo"
                );
                tracker.updateQuestInfo(type, tier);
            }
        }
    }
}
