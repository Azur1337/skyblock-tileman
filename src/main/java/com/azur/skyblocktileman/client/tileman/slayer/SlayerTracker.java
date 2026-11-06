package com.azur.skyblocktileman.client.tileman.slayer;

import com.azur.skyblocktileman.client.tileman.DebugCategory;
import com.azur.skyblocktileman.client.tileman.TilemanChat;
import com.azur.skyblocktileman.client.tileman.TilemanConfig;
import com.azur.skyblocktileman.client.tileman.TilemanLog;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneTracker;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneType;
import com.azur.skyblocktileman.slayer.FailReason;
import com.azur.skyblocktileman.slayer.SlayerBoss;
import com.azur.skyblocktileman.slayer.SlayerTier;
import com.azur.skyblocktileman.slayer.TierSource;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

/**
 * Slayer policy: what a clean run means, what earns a milestone.
 * Parsing lives in {@link com.azur.skyblocktileman.slayer}.
 */
public class SlayerTracker {

    private static SlayerTracker instance;

    private boolean questActive = false;
    private SlayerBoss boss = null;
    private SlayerTier tier = SlayerTier.UNKNOWN;
    private boolean bossSpawned = false;
    private boolean bossSlain = false;
    private boolean tainted = false; // stepped on a locked tile during the quest

    public static SlayerTracker getInstance() {
        if (instance == null) {
            instance = new SlayerTracker();
        }
        return instance;
    }

    private SlayerTracker() {}

    public void onQuestStarted() {
        questActive = true;
        boss = null;
        tier = SlayerTier.UNKNOWN;
        bossSpawned = false;
        bossSlain = false;
        tainted = false;
    }

    public void onQuestIdentified(
        SlayerBoss identified,
        SlayerTier identifiedTier,
        TierSource source
    ) {
        boolean wasTracking = tier.atLeast(SlayerTier.FOUR);
        boss = identified;
        tier = identifiedTier;

        if (!wasTracking && tier.atLeast(SlayerTier.FOUR)) {
            TilemanChat.info(
                "§d§lFLAWLESS TRACKING: §r" +
                    identified.displayName() +
                    " T" +
                    identifiedTier.value()
            );
            TilemanChat.info(
                "§7Don't step on locked tiles to complete flawlessly!"
            );
        }
    }

    public void onBossSpawned(SlayerBoss spawned, SlayerTier spawnedTier) {
        bossSpawned = true;
        if (spawnedTier.known() && !tier.known()) {
            tier = spawnedTier;
        }
        if (tier.atLeast(SlayerTier.FOUR) && !tainted) {
            TilemanChat.info(
                "§a§lBoss spawned! §7Stay on unlocked tiles for flawless!"
            );
        }
    }

    /** Kill moment. Cleanliness is judged at turn-in, not here. */
    public void onBossSlain(SlayerBoss slain, SlayerTier slainTier) {
        bossSlain = true;
        TilemanLog.debug(
            DebugCategory.SLAYER,
            "[TRACKER] Boss slain: {} T{} tainted={}",
            slain,
            slainTier,
            tainted
        );
    }

    public void onSteppedOnLockedTile() {
        if (!questActive || tainted) {
            return;
        }
        tainted = true;
        if (tier.atLeast(SlayerTier.FOUR)) {
            TilemanLog.debug(
                DebugCategory.SLAYER,
                "Slayer quest tainted - stepped on locked tile"
            );
            TilemanChat.warn(
                "§c§lFLAWLESS BROKEN! §7You stepped on a locked tile."
            );
        }
    }

    /** "SLAYER QUEST COMPLETE!" arrives when the finished quest is handed to Maddox. */
    public void onTurnedIn() {
        if (!questActive) {
            return;
        }
        if (boss == null || !tier.known()) {
            TilemanLog.debug(
                DebugCategory.SLAYER,
                "[TRACKER] Turn-in with unknown boss or tier, not recording"
            );
            resetQuestState();
            return;
        }

        SlayerData data = getSlayerData();

        if (tier.atLeast(SlayerTier.FOUR)) {
            // flawless is meaningless without enforcement, so an unenforced turn-in never records it
            if (tainted || !TilemanConfig.getInstance().isPunishmentEnabled()) {
                data.recordTaintedCompletion(boss, tier);
                TilemanChat.info(
                    tainted
                        ? "§e§lSLAYER COMPLETE! §7(not flawless - stepped on locked tiles)"
                        : "§e§lSLAYER COMPLETE! §7(not flawless - tile rules are off)"
                );
                checkSlayerKillMilestone(boss);
            } else {
                data.recordFlawlessCompletion(boss, tier);
                TilemanChat.info(
                    "§d§lFLAWLESS SLAYER! §r" +
                        boss.displayName() +
                        " T" +
                        tier.value()
                );
                TilemanChat.info(
                    "§7Streak: §e" +
                        data.getCurrentFlawlessStreak() +
                        " §7| Total: §e" +
                        data.getTotalFlawlessT4Plus()
                );
                checkSlayerMilestones(boss);
                MilestoneTracker.getInstance().checkMilestone(
                    MilestoneType.SLAYER_STREAK
                );
                playCompletionSound();
            }
        } else {
            data.recordFlawlessCompletion(boss, tier);
        }

        TilemanState.getInstance().save();
        resetQuestState();
    }

    public void onFailed(FailReason reason) {
        if (!questActive) {
            return;
        }

        if (tier.atLeast(SlayerTier.FOUR)) {
            int lost = getSlayerData().getCurrentFlawlessStreak();
            getSlayerData().recordFailedQuest();
            TilemanChat.warn(
                "§c§lSLAYER FAILED! §7" +
                    reasonLabel(reason) +
                    (lost > 0 ? " Flawless streak lost: " + lost : "")
            );
        }

        TilemanState.getInstance().save();
        resetQuestState();
    }

    private static String reasonLabel(FailReason reason) {
        return switch (reason) {
            case DEATH -> "You died.";
            case TIME_LIMIT -> "Out of time.";
            // silent: Hypixel drops the quest with no message when you leave the
            // boss lobby, and the player must not be able to farm around it
            case ABANDONED -> "Quest abandoned.";
            case UNKNOWN -> "";
        };
    }

    public void resetQuestState() {
        questActive = false;
        boss = null;
        tier = SlayerTier.UNKNOWN;
        bossSpawned = false;
        bossSlain = false;
        tainted = false;
    }

    private SlayerData getSlayerData() {
        return TilemanState.getInstance().getSlayerData();
    }

    private void playCompletionSound() {
        Minecraft client = Minecraft.getInstance();
        if (
            client.player != null &&
            TilemanConfig.getInstance().isMilestoneSoundEnabled()
        ) {
            client.player.playSound(
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                1.0f,
                1.2f
            );
        }
    }

    private void checkSlayerMilestones(SlayerBoss boss) {
        checkSlayerKillMilestone(boss);
        checkSlayerFlawlessMilestone(boss);
    }

    private void checkSlayerKillMilestone(SlayerBoss boss) {
        MilestoneTracker.getInstance().checkMilestone(killMilestone(boss));
    }

    private void checkSlayerFlawlessMilestone(SlayerBoss boss) {
        MilestoneTracker.getInstance().checkMilestone(flawlessMilestone(boss));
    }

    public static MilestoneType killMilestone(SlayerBoss boss) {
        return switch (boss) {
            case REVENANT -> MilestoneType.REVENANT_KILLS;
            case TARANTULA -> MilestoneType.TARANTULA_KILLS;
            case SVEN -> MilestoneType.SVEN_KILLS;
            case VOIDGLOOM -> MilestoneType.VOIDGLOOM_KILLS;
            case INFERNO -> MilestoneType.INFERNO_KILLS;
            case RIFTSTALKER -> MilestoneType.RIFTSTALKER_KILLS;
        };
    }

    public static MilestoneType flawlessMilestone(SlayerBoss boss) {
        return switch (boss) {
            case REVENANT -> MilestoneType.FLAWLESS_REVENANT;
            case TARANTULA -> MilestoneType.FLAWLESS_TARANTULA;
            case SVEN -> MilestoneType.FLAWLESS_SVEN;
            case VOIDGLOOM -> MilestoneType.FLAWLESS_VOIDGLOOM;
            case INFERNO -> MilestoneType.FLAWLESS_INFERNO;
            case RIFTSTALKER -> MilestoneType.FLAWLESS_RIFTSTALKER;
        };
    }
}
