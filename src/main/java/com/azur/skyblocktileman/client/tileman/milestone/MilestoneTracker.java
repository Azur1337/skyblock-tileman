package com.azur.skyblocktileman.client.tileman.milestone;

import com.azur.skyblocktileman.client.tileman.TilemanChat;
import com.azur.skyblocktileman.client.tileman.TilemanState;
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
            
            case COMBAT_XP -> state.getSkillXp("Combat");
            case MINING_XP -> state.getSkillXp("Mining");
            case FORAGING_XP -> state.getSkillXp("Foraging");
            case FARMING_XP -> state.getSkillXp("Farming");
            case FISHING_XP -> state.getSkillXp("Fishing");
            
            case NO_MISTAKES -> milestones.getNoMistakeStreak();
            case MARATHON -> milestones.getMarathonMinutes();
            
            case OOPS -> state.getRuleBreaks();
            case HOARDER -> state.getTokens();
            case BIG_SPENDER -> milestones.getTotalShopSpent();
            case LUCKY_DAY -> milestones.getLuckyProcsThisSession();
            case JACKPOT -> milestones.getProgress(MilestoneType.JACKPOT);
            
            case SLAYER_STREAK -> milestones.getSlayerStreak();
            case FLAWLESS_SLAYER -> milestones.getProgress(MilestoneType.FLAWLESS_SLAYER);
            case FLAWLESS_FISHING -> milestones.getFlawlessFishingCount();
            case FLAWLESS_MINING -> milestones.getFlawlessMiningCount();
            case FLAWLESS_FORAGING -> milestones.getFlawlessForagingCount();
            case FLAWLESS_FARMING -> milestones.getFlawlessFarmingCount();
            case FLAWLESS_COMBAT -> milestones.getFlawlessCombatXp();
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
        
        MilestoneType skillMilestone = switch (skill) {
            case "Combat" -> MilestoneType.COMBAT_XP;
            case "Mining" -> MilestoneType.MINING_XP;
            case "Foraging" -> MilestoneType.FORAGING_XP;
            case "Farming" -> MilestoneType.FARMING_XP;
            case "Fishing" -> MilestoneType.FISHING_XP;
            default -> null;
        };
        
        if (skillMilestone != null) {
            checkMilestone(skillMilestone);
        }
        
        MilestoneData data = TilemanState.getInstance().getMilestones();
        if (data.isFlawlessFishingActive()) {
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
        data.onSteppedOffDuringSlayer();
        
        if (data.isSlayerQuestActive()) {
            data.resetSlayerStreak();
        }
        
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
    
    public void onSlayerQuestStart() {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        data.startSlayerQuest();
        TilemanChat.info("Slayer quest started! Stay on tiles for flawless completion.");
    }
    
    public void onSlayerQuestComplete() {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        boolean wasFlawless = data.wasFlawlessSlayer();
        data.endSlayerQuest(true);
        
        if (wasFlawless) {
            data.addProgress(MilestoneType.FLAWLESS_SLAYER, 1);
            TilemanChat.info("§aFlawless slayer complete! Streak: " + data.getSlayerStreak());
            checkMilestone(MilestoneType.FLAWLESS_SLAYER);
        } else {
            TilemanChat.info("Slayer complete, but you stepped off tiles.");
        }
        
        checkMilestone(MilestoneType.SLAYER_STREAK);
        TilemanState.getInstance().save();
    }
    
    public void onSlayerQuestFailed() {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        data.endSlayerQuest(false);
    }
    
    public void onSeaCreatureCaught() {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        if (data.isFlawlessFishingActive()) {
            data.incrementFlawlessFishing();
            checkMilestone(MilestoneType.FLAWLESS_FISHING);
        }
    }
    
    public void startFlawlessTracking() {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        data.startFlawlessFishing();
    }
    
    public void resetFlawlessTracking() {
        MilestoneData data = TilemanState.getInstance().getMilestones();
        data.resetAllFlawless();
    }
}
