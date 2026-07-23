package com.azur.skyblocktileman.client.tileman;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class XpReconciler {

    private static XpReconciler instance;

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TilemanXpReconciler");
            t.setDaemon(true);
            return t;
        });

    private UUID playerUuid;
    private String apiKey;
    private boolean initialized;

    private XpReconciler() {}

    public static XpReconciler getInstance() {
        if (instance == null) {
            instance = new XpReconciler();
        }
        return instance;
    }

    public void initialize(UUID playerUuid, String apiKey, double baselineXp) {
        this.playerUuid = playerUuid;
        this.apiKey = apiKey;
        this.initialized = true;

        TilemanLog.debug(
            "XpReconciler initialized (API baseline: {} XP)",
            baselineXp
        );
    }

    public void reset() {
        initialized = false;
        TilemanLog.debug("XpReconciler reset");
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
