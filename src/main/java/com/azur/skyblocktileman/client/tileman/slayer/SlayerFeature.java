package com.azur.skyblocktileman.client.tileman.slayer;

import com.azur.skyblocktileman.client.tileman.DebugCategory;
import com.azur.skyblocktileman.client.tileman.TilemanLog;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import com.azur.skyblocktileman.slayer.SlayerDetector;
import com.azur.skyblocktileman.slayer.SlayerEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;

/**
 * The only place slayer detection touches Minecraft: feeds raw chat and sidebar
 * text into {@link SlayerDetector} and routes the resulting events to
 * {@link SlayerTracker}. Keeping it here is what lets the detector stay free of
 * mod types.
 */
public final class SlayerFeature {

    /** Half a second: fast enough that the boss-slain phase is not missed. */
    private static final int SCOREBOARD_POLL_TICKS = 10;

    private static final SlayerDetector DETECTOR = new SlayerDetector();
    private static int pollCounter = 0;

    private SlayerFeature() {}

    public static void register() {
        DETECTOR.events(SlayerFeature::onEvent);

        ClientReceiveMessageEvents.GAME.register(
            SlayerFeature::onMessage
        );
        ClientTickEvents.END_CLIENT_TICK.register(SlayerFeature::onTick);

        // chat is not replayed on reconnect but the sidebar is restored, so the
        // detector is dropped and rebuilt from the scoreboard alone
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            DETECTOR.reset()
        );
    }

    /** Island and profile switches invalidate everything the detector holds. */
    public static void reset() {
        DETECTOR.reset();
        SlayerTracker.getInstance().resetQuestState();
    }

    private static void onMessage(Component message, boolean overlay) {
        if (!TilemanState.getInstance().isTilemanActive()) {
            return;
        }
        String stripped = ChatFormatting.stripFormatting(message.getString());
        if (stripped == null || stripped.isBlank()) {
            return;
        }
        // our own output must not feed the detector back to itself
        if (stripped.startsWith("[Tileman]")) {
            return;
        }
        DETECTOR.onChatLine(stripped);
    }

    private static void onTick(Minecraft client) {
        DETECTOR.onTick();

        if (!TilemanState.getInstance().isTilemanActive()) {
            return;
        }
        if (++pollCounter < SCOREBOARD_POLL_TICKS) {
            return;
        }
        pollCounter = 0;

        if (client.level == null) {
            return;
        }
        DETECTOR.onScoreboard(readSidebar(client));
    }

    private static List<String> readSidebar(Minecraft client) {
        List<String> lines = new ArrayList<>();

        Scoreboard scoreboard = client.level.getScoreboard();
        if (scoreboard == null) {
            return lines;
        }
        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) {
            return lines;
        }

        List<PlayerScoreEntry> entries = scoreboard
            .listPlayerScores(sidebar)
            .stream()
            .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed())
            .limit(15)
            .toList();

        for (PlayerScoreEntry entry : entries) {
            String raw = entry.ownerName().getString();
            String stripped = ChatFormatting.stripFormatting(raw);
            if (stripped != null && !stripped.isBlank()) {
                lines.add(stripped);
            }
        }
        return lines;
    }

    private static void onEvent(SlayerEvent event) {
        SlayerTracker tracker = SlayerTracker.getInstance();

        switch (event) {
            case SlayerEvent.QuestStarted e -> tracker.onQuestStarted();
            case SlayerEvent.QuestIdentified e -> tracker.onQuestIdentified(
                    e.boss(),
                    e.tier(),
                    e.source()
                );
            case SlayerEvent.BossSpawned e -> tracker.onBossSpawned(
                    e.boss(),
                    e.tier()
                );
            case SlayerEvent.BossSlain e -> tracker.onBossSlain(
                    e.boss(),
                    e.tier()
                );
            case SlayerEvent.QuestTurnedIn e -> tracker.onTurnedIn();
            case SlayerEvent.QuestFailed e -> tracker.onFailed(e.reason());
            case SlayerEvent.MinibossSpawned e -> TilemanLog.debug(
                    DebugCategory.SLAYER,
                    "Miniboss spawned: {}",
                    e.mobName()
                );
            case SlayerEvent.Progress e -> TilemanLog.debug(
                DebugCategory.SLAYER,
                "Grind progress {}/{}",
                e.currentCombatXp(),
                e.requiredCombatXp()
            );
            case SlayerEvent.QuestCleared e -> {}
        }
    }
}
