package com.azur.skyblocktileman.client.tileman.slayer;

import com.azur.skyblocktileman.client.tileman.DebugCategory;
import com.azur.skyblocktileman.client.tileman.TilemanChat;
import com.azur.skyblocktileman.client.tileman.TilemanConfig;
import com.azur.skyblocktileman.client.tileman.TilemanLog;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneTracker;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

public class SlayerTracker {

    private static SlayerTracker instance;

    private boolean questActive = false;
    private SlayerType currentType = null;
    private int currentTier = 0;
    private boolean bossSpawned = false;
    private boolean tainted = false; // stepped on locked tile during quest

    public static SlayerTracker getInstance() {
        if (instance == null) {
            instance = new SlayerTracker();
        }
        return instance;
    }

    private SlayerTracker() {}

    public static void register() {
        // disconnecting abandons the quest; stale state would credit a run that no longer exists
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            getInstance().resetQuestState()
        );
    }

    public void onQuestStarted(SlayerType type, int tier) {
        TilemanLog.debug(
            DebugCategory.SLAYER,
            "[TRACKER] onQuestStarted called: type={}, tier={}",
            type,
            tier
        );

        questActive = true;
        currentType = type;
        currentTier = tier;
        bossSpawned = false;
        tainted = false;

        TilemanLog.debug(
            DebugCategory.SLAYER,
            "[TRACKER] Quest state: active={}, type={}, tier={}",
            questActive,
            currentType,
            currentTier
        );

        if (tier >= 4) {
            TilemanChat.info(
                "§d§lFLAWLESS TRACKING: §r" +
                    (type != null ? type.getDisplayName() : "Slayer") +
                    " T" +
                    tier
            );
            TilemanChat.info(
                "§7Don't step on locked tiles to complete flawlessly!"
            );
        }
    }

    public void updateQuestInfo(SlayerType type, int tier) {
        TilemanLog.debug(
            DebugCategory.SLAYER,
            "[TRACKER] updateQuestInfo called: type={}, tier={}, currentType={}, currentTier={}",
            type,
            tier,
            currentType,
            currentTier
        );

        if (type != null && tier > 0) {
            if (currentType == null || currentTier == 0) {
                boolean wasTracking = questActive && currentTier >= 4;
                currentType = type;
                currentTier = tier;
                TilemanLog.debug(
                    DebugCategory.SLAYER,
                    "[TRACKER] Quest info updated: type={}, tier={}",
                    type.getDisplayName(),
                    tier
                );

                if (questActive && tier >= 4 && !wasTracking) {
                    TilemanChat.info(
                        "§d§lFLAWLESS TRACKING: §r" +
                            type.getDisplayName() +
                            " T" +
                            tier
                    );
                    TilemanChat.info(
                        "§7Don't step on locked tiles to complete flawlessly!"
                    );
                }
            } else {
                TilemanLog.debug(
                    DebugCategory.SLAYER,
                    "[TRACKER] Skipping update - already have type/tier"
                );
            }
        } else {
            TilemanLog.debug(
                DebugCategory.SLAYER,
                "[TRACKER] Skipping update - invalid type or tier"
            );
        }
    }

    public void onBossSpawned() {
        TilemanLog.debug(
            DebugCategory.SLAYER,
            "[TRACKER] onBossSpawned called, questActive={}",
            questActive
        );

        if (!questActive) {
            // boss can spawn without a quest-start message ever arriving
            TilemanLog.debug(
                DebugCategory.SLAYER,
                "[TRACKER] Boss spawned but quest not active - activating now"
            );
            questActive = true;
            tainted = false;
        }
        bossSpawned = true;

        TilemanLog.debug(
            DebugCategory.SLAYER,
            "[TRACKER] Boss spawned state: type={}, tier={}, tainted={}",
            currentType,
            currentTier,
            tainted
        );

        if (currentTier >= 4 && !tainted) {
            TilemanChat.info(
                "§a§lBoss spawned! §7Stay on unlocked tiles for flawless!"
            );
        }
    }

    public void onSteppedOnLockedTile() {
        if (!questActive) return;

        if (!tainted && currentTier >= 4) {
            tainted = true;
            TilemanLog.debug(
                DebugCategory.ALL,
                "Slayer quest tainted - stepped on locked tile"
            );
            TilemanChat.warn(
                "§c§lFLAWLESS BROKEN! §7You stepped on a locked tile."
            );
        } else if (!tainted) {
            tainted = true;
        }
    }

    public void onQuestComplete() {
        TilemanLog.debug(
            DebugCategory.SLAYER,
            "[TRACKER] onQuestComplete called: active={}, type={}, tier={}, tainted={}",
            questActive,
            currentType,
            currentTier,
            tainted
        );

        if (!questActive) {
            TilemanLog.debug(
                DebugCategory.SLAYER,
                "[TRACKER] Quest not active, ignoring complete"
            );
            return;
        }

        // a quest can be active with no type if the boss spawned before the accept line parsed
        if (currentType == null) {
            TilemanLog.debug(
                DebugCategory.SLAYER,
                "[TRACKER] Completion with unknown slayer type, not recording"
            );
            resetQuestState();
            return;
        }

        SlayerData data = getSlayerData();

        if (currentTier >= 4) {
            TilemanLog.debug(
                DebugCategory.SLAYER,
                "[TRACKER] T4+ completion: tainted={}",
                tainted
            );
            if (tainted) {
                data.recordTaintedCompletion(currentType, currentTier);
                TilemanLog.debug(
                    DebugCategory.SLAYER,
                    "[TRACKER] Recorded tainted T4+ completion"
                );
                TilemanChat.info(
                    "§e§lSLAYER COMPLETE! §7(not flawless - stepped on locked tiles)"
                );
                // tainted kills still count toward kill milestones, just not flawless ones
                checkSlayerKillMilestone(currentType);
            } else {
                data.recordFlawlessCompletion(currentType, currentTier);
                TilemanLog.debug(
                    DebugCategory.SLAYER,
                    "[TRACKER] Recorded FLAWLESS T4+ completion"
                );

                int streak = data.getCurrentFlawlessStreak();
                int total = data.getTotalFlawlessT4Plus();

                TilemanChat.info(
                    "§d§l✦ FLAWLESS SLAYER! §r" +
                        (currentType != null
                            ? currentType.getDisplayName()
                            : "Slayer") +
                        " T" +
                        currentTier
                );
                TilemanChat.info(
                    "§7Streak: §e" + streak + " §7| Total: §e" + total
                );

                checkSlayerMilestones(currentType);
                MilestoneTracker.getInstance().checkMilestone(
                    MilestoneType.SLAYER_STREAK
                );

                Minecraft client = Minecraft.getInstance();
                if (client.player != null) {
                    client.player.playSound(
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                        1.0f,
                        1.2f
                    );
                }
            }
        } else {
            TilemanLog.debug(
                DebugCategory.SLAYER,
                "[TRACKER] Lower tier completion (T{}), tracking only",
                currentTier
            );
            data.recordFlawlessCompletion(currentType, currentTier);
        }

        TilemanState.getInstance().save();
        resetQuestState();
        TilemanLog.debug(DebugCategory.SLAYER, "[TRACKER] Quest state reset");
    }

    public void onQuestFailed() {
        if (!questActive) return;

        SlayerData data = getSlayerData();

        if (currentTier >= 4) {
            int oldStreak = data.getCurrentFlawlessStreak();
            data.recordFailedQuest();

            if (oldStreak > 0) {
                TilemanChat.warn(
                    "§c§lSLAYER FAILED! §7Flawless streak lost: " + oldStreak
                );
            } else {
                TilemanChat.warn("§c§lSLAYER FAILED!");
            }
        }

        TilemanState.getInstance().save();
        resetQuestState();
    }

    public boolean isQuestActive() {
        return questActive;
    }

    public boolean isTainted() {
        return tainted;
    }

    public SlayerType getCurrentType() {
        return currentType;
    }

    public int getCurrentTier() {
        return currentTier;
    }

    public void resetQuestState() {
        questActive = false;
        currentType = null;
        currentTier = 0;
        bossSpawned = false;
        tainted = false;
    }

    private SlayerData getSlayerData() {
        return TilemanState.getInstance().getSlayerData();
    }

    private void checkSlayerMilestones(SlayerType type) {
        checkSlayerKillMilestone(type);
        checkSlayerFlawlessMilestone(type);
    }

    private void checkSlayerKillMilestone(SlayerType type) {
        if (type == null) return;
        MilestoneType killMilestone = switch (type) {
            case REVENANT -> MilestoneType.REVENANT_KILLS;
            case TARANTULA -> MilestoneType.TARANTULA_KILLS;
            case SVEN -> MilestoneType.SVEN_KILLS;
            case VOIDGLOOM -> MilestoneType.VOIDGLOOM_KILLS;
            case INFERNO -> MilestoneType.INFERNO_KILLS;
            case RIFTSTALKER -> MilestoneType.RIFTSTALKER_KILLS;
        };
        MilestoneTracker.getInstance().checkMilestone(killMilestone);
    }

    private void checkSlayerFlawlessMilestone(SlayerType type) {
        if (type == null) return;
        MilestoneType flawlessMilestone = switch (type) {
            case REVENANT -> MilestoneType.FLAWLESS_REVENANT;
            case TARANTULA -> MilestoneType.FLAWLESS_TARANTULA;
            case SVEN -> MilestoneType.FLAWLESS_SVEN;
            case VOIDGLOOM -> MilestoneType.FLAWLESS_VOIDGLOOM;
            case INFERNO -> MilestoneType.FLAWLESS_INFERNO;
            case RIFTSTALKER -> MilestoneType.FLAWLESS_RIFTSTALKER;
        };
        MilestoneTracker.getInstance().checkMilestone(flawlessMilestone);
    }
}
