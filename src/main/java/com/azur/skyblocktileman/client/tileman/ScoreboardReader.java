package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.slayer.SlayerChatListener;
import com.azur.skyblocktileman.client.tileman.slayer.SlayerType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;

public final class ScoreboardReader {

    private ScoreboardReader() {}

    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ScoreboardReader::onTick);
    }

    private static void onTick(Minecraft client) {
        if (!TilemanState.getInstance().isTilemanActive()) {
            return;
        }

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        if (client.level == null || client.player == null) {
            return;
        }

        List<String> lines = getScoreboardLines(client);
        if (lines.isEmpty()) {
            return;
        }

        parseScoreboard(lines);
    }

    private static List<String> getScoreboardLines(Minecraft client) {
        List<String> result = new ArrayList<>();

        Scoreboard scoreboard = client.level.getScoreboard();
        if (scoreboard == null) {
            return result;
        }

        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) {
            return result;
        }

        Collection<PlayerScoreEntry> scores = scoreboard.listPlayerScores(
            sidebar
        );

        List<PlayerScoreEntry> sortedScores = scores
            .stream()
            .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed())
            .limit(15)
            .toList();

        for (PlayerScoreEntry score : sortedScores) {
            String line = score.ownerName().getString();
            String stripped = ChatFormatting.stripFormatting(line);
            if (stripped != null && !stripped.isBlank()) {
                result.add(stripped);
            }
        }

        return result;
    }

    private static void parseScoreboard(List<String> lines) {
        TilemanLog.debug(
            DebugCategory.SLAYER,
            "[SCOREBOARD] Reading {} lines",
            lines.size()
        );
        for (int i = 0; i < lines.size(); i++) {
            TilemanLog.debug(
                DebugCategory.SLAYER,
                "[SCOREBOARD] Line {}: '{}'",
                i,
                lines.get(i)
            );
        }

        boolean foundSlayerQuest = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // matches any line containing "slayer", not just a quest header
            if (line.toLowerCase().contains("slayer")) {
                TilemanLog.debug(
                    DebugCategory.SLAYER,
                    "[SCOREBOARD] Found line containing 'slayer': '{}'",
                    line
                );

                SlayerType typeInLine = SlayerType.fromDisplayName(line);
                int tierInLine = SlayerType.parseTier(line);
                if (typeInLine != null && tierInLine > 0) {
                    TilemanLog.debug(
                        DebugCategory.SLAYER,
                        "[SCOREBOARD] This line has type/tier: {} T{}",
                        typeInLine,
                        tierInLine
                    );
                    SlayerChatListener.onScoreboardSlayerInfo(line);
                    continue;
                }

                foundSlayerQuest = true;
                continue;
            }

            if (foundSlayerQuest) {
                TilemanLog.debug(
                    DebugCategory.SLAYER,
                    "[SCOREBOARD] Checking line after slayer header: '{}'",
                    line
                );

                SlayerType type = SlayerType.fromDisplayName(line);
                int tier = SlayerType.parseTier(line);

                TilemanLog.debug(
                    DebugCategory.SLAYER,
                    "[SCOREBOARD] Parsed: type={}, tier={}",
                    type,
                    tier
                );

                if (type != null && tier > 0) {
                    TilemanLog.debug(
                        DebugCategory.SLAYER,
                        "[SCOREBOARD] Valid slayer detected: {} T{}",
                        type.getDisplayName(),
                        tier
                    );
                    SlayerChatListener.onScoreboardSlayerInfo(line);
                }

                foundSlayerQuest = false;
            }
        }
    }
}
