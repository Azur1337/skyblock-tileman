package com.azur.skyblocktileman.client.tileman;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Append-only CSV of economy events, for reviewing a session's real numbers
// when deciding balance changes. Trimmed on first write once it gets large.
public final class BalanceLog {

    private static final Path FILE = TilemanState.getSaveDir().resolve(
        "balance_log.csv"
    );
    private static final int MAX_LINES = 20000;
    private static final int KEEP_LINES = 15000;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss"
    );

    private static boolean hasTrimmed;

    private BalanceLog() {}

    public static void log(
        String event,
        String detail,
        long amount,
        long balance
    ) {
        try {
            if (!hasTrimmed) {
                trimIfNeeded();
            }
            if (!Files.exists(FILE)) {
                Files.createDirectories(FILE.getParent());
                Files.writeString(
                    FILE,
                    "timestamp,profile,island,event,detail,amount,balance\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                );
            }
            TilemanState state = TilemanState.getInstance();
            String line = String.join(
                ",",
                LocalDateTime.now().format(TIME),
                sanitize(state.getActiveProfileId()),
                sanitize(state.getActiveIsland()),
                event,
                sanitize(detail),
                Long.toString(amount),
                Long.toString(balance)
            );
            Files.writeString(
                FILE,
                line + "\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            // logging must never break the economy path it observes
        }
    }

    private static String sanitize(String text) {
        return text == null ? "" : text.replace(",", ";");
    }

    private static void trimIfNeeded() {
        hasTrimmed = true;
        try {
            if (!Files.exists(FILE)) {
                return;
            }
            List<String> lines = Files.readAllLines(FILE);
            if (lines.size() <= MAX_LINES) {
                return;
            }
            List<String> kept = lines.subList(
                lines.size() - KEEP_LINES,
                lines.size()
            );
            Files.write(
                FILE,
                kept,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
        } catch (IOException e) {}
    }
}
