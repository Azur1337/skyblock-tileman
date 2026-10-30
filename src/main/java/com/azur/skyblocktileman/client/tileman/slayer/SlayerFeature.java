package com.azur.skyblocktileman.client.tileman.slayer;

import com.azur.skyblocktileman.client.tileman.DebugCategory;
import com.azur.skyblocktileman.client.tileman.TilemanConfig;
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
import net.minecraft.world.scores.PlayerTeam;
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
    private static String lastSidebarDump = null;

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
        List<String> lines = readSidebar(client);
        dumpSidebarIfChanged(lines);
        DETECTOR.onScoreboard(lines);
    }

    /**
     * Dump exactly what the detector sees, but only when the slayer debug category
     * is on and the sidebar actually changed, so the scoreboard path can be verified
     * against a live quest without per-poll spam.
     */
    private static void dumpSidebarIfChanged(List<String> lines) {
        if (!TilemanConfig.getInstance().isDebugEnabled(DebugCategory.SLAYER)) {
            return;
        }
        String signature = String.join(" / ", lines);
        if (signature.equals(lastSidebarDump)) {
            return;
        }
        lastSidebarDump = signature;
        TilemanLog.debug(DebugCategory.SLAYER, "Sidebar: {}", signature);
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

        // the sidebar title is the objective display name, not a score entry, so it
        // has to be added explicitly or the "Slayer Quest" header is never visible
        Component title = sidebar.getDisplayName();
        addLine(lines, cleanText(title == null ? null : title.getString()));

        List<PlayerScoreEntry> entries = scoreboard
            .listPlayerScores(sidebar)
            .stream()
            .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed())
            .toList();

        for (PlayerScoreEntry entry : entries) {
            addLine(lines, sidebarText(scoreboard, entry));
        }
        return lines;
    }

    /**
     * The visible text of one sidebar line, resolved exactly like vanilla's HUD:
     * the owner is an invisible sort token and Hypixel carries the real text in a
     * player team prefix, so it must go through formatNameForTeam(getPlayersTeam(..)).
     */
    private static String sidebarText(Scoreboard scoreboard, PlayerScoreEntry entry) {
        PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
        Component name = PlayerTeam.formatNameForTeam(team, entry.ownerName());
        return cleanText(name.getString());
    }

    private static String cleanText(String raw) {
        if (raw == null) {
            return "";
        }
        String stripped = ChatFormatting.stripFormatting(raw);
        if (stripped == null) {
            stripped = raw;
        }
        // drop Hypixel's invisible sort tokens: a section sign plus one char
        StringBuilder out = new StringBuilder(stripped.length());
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (c == 0xA7) {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString().replace((char) 0xA0, ' ').trim();
    }

    private static void addLine(List<String> lines, String text) {
        if (!text.isEmpty()) {
            lines.add(text);
        }
    }

    private static void onEvent(SlayerEvent event) {
        SlayerTracker tracker = SlayerTracker.getInstance();

        switch (event) {
            case SlayerEvent.QuestStarted e -> tracker.onQuestStarted();
            case SlayerEvent.QuestIdentified e -> {
                TilemanLog.debug(
                    DebugCategory.SLAYER,
                    "Identified {} T{} via {}",
                    e.boss(),
                    e.tier(),
                    e.source()
                );
                tracker.onQuestIdentified(e.boss(), e.tier(), e.source());
            }
            case SlayerEvent.BossSpawned e -> {
                TilemanLog.debug(
                    DebugCategory.SLAYER,
                    "Boss spawned: {} T{}",
                    e.boss(),
                    e.tier()
                );
                tracker.onBossSpawned(e.boss(), e.tier());
            }
            case SlayerEvent.BossSlain e -> tracker.onBossSlain(
                    e.boss(),
                    e.tier()
                );
            case SlayerEvent.QuestTurnedIn e -> {
                TilemanLog.debug(
                    DebugCategory.SLAYER,
                    "Turned in: {} T{}",
                    e.boss(),
                    e.tier()
                );
                tracker.onTurnedIn();
            }
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
