package com.azur.skyblocktileman.client.tileman;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

// Rotating copies of tileman_data.json under config/tileman/backups. One is taken
// after every successful load and then at most every BACKUP_GAP_MS, so a bad save
// or a mod bug can be rolled back with /tileman restore.
public final class SaveBackups {

    private static final Path DIR = TilemanState.getSaveDir()
        .resolve("backups");
    private static final int KEEP = 24;
    private static final long BACKUP_GAP_MS = 10 * 60 * 1000;
    private static final DateTimeFormatter STAMP = DateTimeFormatter
        .ofPattern("yyyy-MM-dd_HH.mm.ss")
        .withZone(ZoneId.systemDefault());

    private static long lastBackupMs = 0;

    private SaveBackups() {}

    public static void onLoaded() {
        if (Files.exists(TilemanState.getSaveFile())) {
            copyNow();
        }
    }

    public static void maybeOnSave() {
        if (
            System.currentTimeMillis() - lastBackupMs >= BACKUP_GAP_MS &&
            Files.exists(TilemanState.getSaveFile())
        ) {
            copyNow();
        }
    }

    // an explicit copy, used before a restore so the replaced save is recoverable
    public static Path copyNow() {
        try {
            Files.createDirectories(DIR);
            Path source = TilemanState.getSaveFile();
            Path target = DIR.resolve(
                "tileman_data." + STAMP.format(Instant.now()) + ".json"
            );
            Files.copy(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING
            );
            lastBackupMs = System.currentTimeMillis();
            rotate();
            return target;
        } catch (IOException e) {
            TilemanLog.debug(
                DebugCategory.SAVE,
                "Backup failed: {}",
                String.valueOf(e)
            );
            return null;
        }
    }

    public static List<Path> list() {
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(DIR)) {
            return files;
        }
        try (Stream<Path> stream = Files.list(DIR)) {
            stream
                .filter(p ->
                    p.getFileName().toString().startsWith("tileman_data.") &&
                    p.getFileName().toString().endsWith(".json")
                )
                .sorted(Comparator.comparing(Path::getFileName))
                .forEach(files::add);
        } catch (IOException e) {}
        return files;
    }

    public static Path find(String nameOrIndex) {
        if (nameOrIndex == null || nameOrIndex.isEmpty()) {
            return null;
        }
        List<Path> files = list();
        try {
            int index = Integer.parseInt(nameOrIndex);
            if (index >= 1 && index <= files.size()) {
                return files.get(files.size() - index);
            }
            if (index < 0 && -index <= files.size()) {
                return files.get(files.size() + index);
            }
        } catch (NumberFormatException notIndex) {}
        String name = nameOrIndex.endsWith(".json")
            ? nameOrIndex
            : nameOrIndex + ".json";
        for (Path file : files) {
            if (file.getFileName().toString().equals(name)) {
                return file;
            }
        }
        return null;
    }

    private static void rotate() {
        List<Path> files = list();
        for (int i = 0; i < files.size() - KEEP; i++) {
            try {
                Files.delete(files.get(i));
            } catch (IOException e) {}
        }
    }
}
