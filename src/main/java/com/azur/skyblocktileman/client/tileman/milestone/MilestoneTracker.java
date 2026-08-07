package com.azur.skyblocktileman.client.tileman.milestone;

import com.azur.skyblocktileman.client.tileman.TilemanChat;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import com.azur.skyblocktileman.client.tileman.dungeon.DungeonFloor;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

public class MilestoneTracker {
    
    private static MilestoneTracker instance;
    
    public static MilestoneTracker getInstance() {
        if (instance == null) {
            instance = new MilestoneTracker();
        }
        return instance;
    }
    
    public void checkAllMilestones() {
        for (MilestoneType type : MilestoneType.values()) {
            checkMilestone(type);
        }
    }
    
    public void checkMilestone(MilestoneType type) {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        int currentTier = data.getCompletedTier(type);
        int maxTier = type.getMaxTier();
        
        if (currentTier >= maxTier) return;
        
        long progress = getProgressForType(type);
        int nextTier = currentTier + 1;
        long target = type.getTargetForTier(nextTier);
        
        if (progress >= target) {
            completeTier(type, nextTier);
            checkMilestone(type);
        }
    }
    
    private void completeTier(MilestoneType type, int tier) {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        int reward = type.getRewardForTier(tier);
        
        data.setCompletedTier(type, tier);
        data.addTokensFromMilestone(reward);
        TilemanState.getInstance().addTokens(reward);
        TilemanState.getInstance().save();
        
        String tierNum = type.getTierNumeral(tier);
        TilemanChat.info("§6§lMILESTONE COMPLETE! §r" + type.getDisplayName() + " " + tierNum + " §7(+" + reward + " tokens)");
        
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }
    
    public long getProgressForType(MilestoneType type) {
        TilemanState state = TilemanState.getInstance();
        MilestoneData milestones = state.getMilestones();
        
        return switch (type) {
            case TILES_UNLOCKED -> state.getUnlockedBlocks().size();
            case TOTAL_XP -> state.getTotalSkillXp();
            case TOKENS_EARNED -> state.getLifetimeTokensEarned();
            case ISLANDS_EXPLORED -> state.getExploredIslandCount();
            
            case COMBAT_LEVEL -> state.getSkillLevel("Combat");
            case MINING_LEVEL -> state.getSkillLevel("Mining");
            case FORAGING_LEVEL -> state.getSkillLevel("Foraging");
            case FARMING_LEVEL -> state.getSkillLevel("Farming");
            case FISHING_LEVEL -> state.getSkillLevel("Fishing");
            
            // Slayer kill milestones (T4+ kills)
            case REVENANT_KILLS -> state.getSlayerData().getStats(com.azur.skyblocktileman.client.tileman.slayer.SlayerType.REVENANT).t4PlusKills;
            case TARANTULA_KILLS -> state.getSlayerData().getStats(com.azur.skyblocktileman.client.tileman.slayer.SlayerType.TARANTULA).t4PlusKills;
            case SVEN_KILLS -> state.getSlayerData().getStats(com.azur.skyblocktileman.client.tileman.slayer.SlayerType.SVEN).t4PlusKills;
            case VOIDGLOOM_KILLS -> state.getSlayerData().getStats(com.azur.skyblocktileman.client.tileman.slayer.SlayerType.VOIDGLOOM).t4PlusKills;
            case INFERNO_KILLS -> state.getSlayerData().getStats(com.azur.skyblocktileman.client.tileman.slayer.SlayerType.INFERNO).t4PlusKills;
            case RIFTSTALKER_KILLS -> state.getSlayerData().getStats(com.azur.skyblocktileman.client.tileman.slayer.SlayerType.RIFTSTALKER).t4PlusKills;
            
            // Flawless slayer milestones (per type)
            case FLAWLESS_REVENANT -> state.getSlayerData().getStats(com.azur.skyblocktileman.client.tileman.slayer.SlayerType.REVENANT).flawlessT4Plus;
            case FLAWLESS_TARANTULA -> state.getSlayerData().getStats(com.azur.skyblocktileman.client.tileman.slayer.SlayerType.TARANTULA).flawlessT4Plus;
            case FLAWLESS_SVEN -> state.getSlayerData().getStats(com.azur.skyblocktileman.client.tileman.slayer.SlayerType.SVEN).flawlessT4Plus;
            case FLAWLESS_VOIDGLOOM -> state.getSlayerData().getStats(com.azur.skyblocktileman.client.tileman.slayer.SlayerType.VOIDGLOOM).flawlessT4Plus;
            case FLAWLESS_INFERNO -> state.getSlayerData().getStats(com.azur.skyblocktileman.client.tileman.slayer.SlayerType.INFERNO).flawlessT4Plus;
            case FLAWLESS_RIFTSTALKER -> state.getSlayerData().getStats(com.azur.skyblocktileman.client.tileman.slayer.SlayerType.RIFTSTALKER).flawlessT4Plus;
            
            case NO_MISTAKES -> milestones.getNoMistakeStreak();
            case MARATHON -> milestones.getMarathonMinutes();
            case SLAYER_STREAK -> state.getSlayerData().getBestFlawlessStreak();
            
            case FLAWLESS_MINING -> milestones.getFlawlessMiningXp();
            case FLAWLESS_FORAGING -> milestones.getFlawlessForagingXp();
            case FLAWLESS_FARMING -> milestones.getFlawlessFarmingXp();
            case FLAWLESS_COMBAT -> milestones.getFlawlessCombatXp();
            case FLAWLESS_FISHING -> milestones.getFlawlessFishingXp();
            
            case OOPS -> state.getRuleBreaks();
            case HOARDER -> state.getTokens();
            case BIG_SPENDER -> milestones.getTotalShopSpent();
            case LUCKY_DAY -> milestones.getLuckyProcsThisSession();
            case JACKPOT -> milestones.getProgress(MilestoneType.JACKPOT);
        };
    }
    
    public void onTileUnlocked() {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        data.incrementNoMistakeStreak();
        
        if (data.getMarathonStartTime() == 0) {
            data.startMarathon();
        }
        
        checkMilestone(MilestoneType.TILES_UNLOCKED);
        checkMilestone(MilestoneType.NO_MISTAKES);
    }
    
    public void onXpGained(String skill, long amount) {
        checkMilestone(MilestoneType.TOTAL_XP);
        
        MilestoneType skillLevelMilestone = switch (skill) {
            case "Combat" -> MilestoneType.COMBAT_LEVEL;
            case "Mining" -> MilestoneType.MINING_LEVEL;
            case "Foraging" -> MilestoneType.FORAGING_LEVEL;
            case "Farming" -> MilestoneType.FARMING_LEVEL;
            case "Fishing" -> MilestoneType.FISHING_LEVEL;
            default -> null;
        };
        
        if (skillLevelMilestone != null) {
            checkMilestone(skillLevelMilestone);
        }
        
        // flawless xp tracking
        MilestoneData data = TilemanState.getInstance().getMilestones();
        if (data.isFlawlessActive()) {
            switch (skill) {
                case "Combat" -> {
                    data.addFlawlessCombatXp(amount);
                    checkMilestone(MilestoneType.FLAWLESS_COMBAT);
                }
                case "Mining" -> {
                    data.addFlawlessMiningXp(amount);
                    checkMilestone(MilestoneType.FLAWLESS_MINING);
                }
                case "Foraging" -> {
                    data.addFlawlessForagingXp(amount);
                    checkMilestone(MilestoneType.FLAWLESS_FORAGING);
                }
                case "Farming" -> {
                    data.addFlawlessFarmingXp(amount);
                    checkMilestone(MilestoneType.FLAWLESS_FARMING);
                }
                case "Fishing" -> {
                    data.addFlawlessFishingXp(amount);
                    checkMilestone(MilestoneType.FLAWLESS_FISHING);
                }
            }
        }
    }
    
    public void onTokenEarned() {
        checkMilestone(MilestoneType.TOKENS_EARNED);
        checkMilestone(MilestoneType.HOARDER);
    }
    
    public void onRuleBreak() {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        data.resetNoMistakeStreak();
        data.resetMarathon();
        data.resetFlawless();
        checkMilestone(MilestoneType.OOPS);
    }
    
    public void onIslandVisited() {
        checkMilestone(MilestoneType.ISLANDS_EXPLORED);
    }
    
    public void onShopPurchase(int cost) {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        data.addShopSpent(cost);
        checkMilestone(MilestoneType.BIG_SPENDER);
    }
    
    public void onLuckyProc(int multiplier) {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        data.incrementLuckyProcs();
        checkMilestone(MilestoneType.LUCKY_DAY);
        
        if (multiplier >= 3) {
            data.setProgress(MilestoneType.JACKPOT, 1);
            checkMilestone(MilestoneType.JACKPOT);
        }
    }
    
    public void onTick() {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        if (data.getMarathonStartTime() > 0) {
            checkMilestone(MilestoneType.MARATHON);
        }
    }
    
    public void startFlawlessTracking() {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        data.startFlawless();
    }
    
    public void resetFlawlessTracking() {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        data.resetFlawless();
    }
    
    public void onDungeonComplete(DungeonFloor floor, boolean sRank, boolean sPlusRank) {
        // dungeon milestones tracked via api
    }
}
