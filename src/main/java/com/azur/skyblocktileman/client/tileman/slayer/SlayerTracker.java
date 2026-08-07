package com.azur.skyblocktileman.client.tileman.slayer;

import com.azur.skyblocktileman.client.tileman.DebugCategory;
import com.azur.skyblocktileman.client.tileman.TilemanChat;
import com.azur.skyblocktileman.client.tileman.TilemanConfig;
import com.azur.skyblocktileman.client.tileman.TilemanLog;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneTracker;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneType;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

/**
 * Tracks active slayer quest state and flawless progress.
 */
public class SlayerTracker {
    
    private static SlayerTracker instance;
    
    // Current quest state
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
    
    /**
     * Called when a slayer quest starts.
     */
    public void onQuestStarted(SlayerType type, int tier) {
        questActive = true;
        currentType = type;
        currentTier = tier;
        bossSpawned = false;
        tainted = false;
        
        TilemanLog.debug(DebugCategory.ALL, "Slayer quest started: {} T{}", 
            type != null ? type.getDisplayName() : "Unknown", tier);
        
        if (tier >= 4) {
            TilemanChat.info("§d§lFLAWLESS TRACKING: §r" + 
                (type != null ? type.getDisplayName() : "Slayer") + " T" + tier);
            TilemanChat.info("§7Don't step on locked tiles to complete flawlessly!");
        }
    }
    
    /**
     * Called when slayer type/tier is detected from scoreboard.
     */
    public void updateQuestInfo(SlayerType type, int tier) {
        if (questActive && currentType == null) {
            currentType = type;
            currentTier = tier;
            TilemanLog.debug(DebugCategory.ALL, "Slayer quest info updated: {} T{}", 
                type != null ? type.getDisplayName() : "Unknown", tier);
        }
    }
    
    /**
     * Called when boss spawns.
     */
    public void onBossSpawned() {
        if (!questActive) {
            // Quest started without us detecting the start message
            questActive = true;
            tainted = false;
        }
        bossSpawned = true;
        
        TilemanLog.debug(DebugCategory.ALL, "Slayer boss spawned, tainted: {}", tainted);
        
        if (currentTier >= 4 && !tainted) {
            TilemanChat.info("§a§lBoss spawned! §7Stay on unlocked tiles for flawless!");
        }
    }
    
    /**
     * Called when player steps on a locked tile during an active quest.
     */
    public void onSteppedOnLockedTile() {
        if (!questActive) return;
        
        if (!tainted && currentTier >= 4) {
            tainted = true;
            TilemanLog.debug(DebugCategory.ALL, "Slayer quest tainted - stepped on locked tile");
            TilemanChat.warn("§c§lFLAWLESS BROKEN! §7You stepped on a locked tile.");
        } else if (!tainted) {
            tainted = true;
        }
    }
    
    /**
     * Called when slayer quest is completed successfully.
     */
    public void onQuestComplete() {
        if (!questActive) return;
        
        SlayerData data = getSlayerData();
        
        if (currentTier >= 4) {
            if (tainted) {
                data.recordTaintedCompletion(currentType, currentTier);
                TilemanChat.info("§e§lSLAYER COMPLETE! §7(not flawless - stepped on locked tiles)");
            } else {
                data.recordFlawlessCompletion(currentType, currentTier);
                
                int streak = data.getCurrentFlawlessStreak();
                int total = data.getTotalFlawlessT4Plus();
                
                TilemanChat.info("§d§l✦ FLAWLESS SLAYER! §r" + 
                    (currentType != null ? currentType.getDisplayName() : "Slayer") + " T" + currentTier);
                TilemanChat.info("§7Streak: §e" + streak + " §7| Total: §e" + total);
                
                // Check slayer milestones
                MilestoneTracker.getInstance().checkMilestone(MilestoneType.FLAWLESS_SLAYER);
                MilestoneTracker.getInstance().checkMilestone(MilestoneType.SLAYER_STREAK);
                
                // Play special sound
                Minecraft client = Minecraft.getInstance();
                if (client.player != null) {
                    client.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                }
            }
        } else {
            // Lower tier, just track completion
            data.recordFlawlessCompletion(currentType, currentTier);
        }
        
        TilemanState.getInstance().save();
        resetQuestState();
    }
    
    /**
     * Called when slayer quest fails.
     */
    public void onQuestFailed() {
        if (!questActive) return;
        
        SlayerData data = getSlayerData();
        
        if (currentTier >= 4) {
            int oldStreak = data.getCurrentFlawlessStreak();
            data.recordFailedQuest();
            
            if (oldStreak > 0) {
                TilemanChat.warn("§c§lSLAYER FAILED! §7Flawless streak lost: " + oldStreak);
            } else {
                TilemanChat.warn("§c§lSLAYER FAILED!");
            }
        }
        
        TilemanState.getInstance().save();
        resetQuestState();
    }
    
    /**
     * Check if a slayer quest is currently active.
     */
    public boolean isQuestActive() {
        return questActive;
    }
    
    /**
     * Check if the current quest is tainted (player stepped on locked tile).
     */
    public boolean isTainted() {
        return tainted;
    }
    
    /**
     * Get the current slayer type.
     */
    public SlayerType getCurrentType() {
        return currentType;
    }
    
    /**
     * Get the current slayer tier.
     */
    public int getCurrentTier() {
        return currentTier;
    }
    
    /**
     * Reset quest state (on completion, failure, or island change).
     */
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
}
