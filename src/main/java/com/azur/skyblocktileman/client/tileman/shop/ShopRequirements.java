package com.azur.skyblocktileman.client.tileman.shop;

import com.azur.skyblocktileman.client.tileman.TilemanState;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneData;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneType;

import java.util.HashMap;
import java.util.Map;

public class ShopRequirements {
    
    private static final Map<String, Requirement> REQUIREMENTS = new HashMap<>();
    
    static {
        // multi unlock requires tiles unlocked milestone
        REQUIREMENTS.put("multi_unlock_2", new Requirement(MilestoneType.TILES_UNLOCKED, 1)); // tier 1 = 25 tiles
        REQUIREMENTS.put("multi_unlock_3", new Requirement(MilestoneType.TILES_UNLOCKED, 2)); // tier 2 = 100 tiles
        REQUIREMENTS.put("multi_unlock_4", new Requirement(MilestoneType.TILES_UNLOCKED, 3)); // tier 3 = 500 tiles
        
        // lucky unlock requires tokens earned milestone
        REQUIREMENTS.put("lucky_unlock_5", new Requirement(MilestoneType.TOKENS_EARNED, 1));  // tier 1 = 25 tokens
        REQUIREMENTS.put("lucky_unlock_10", new Requirement(MilestoneType.TOKENS_EARNED, 2)); // tier 2 = 100 tokens
        REQUIREMENTS.put("lucky_unlock_15", new Requirement(MilestoneType.TOKENS_EARNED, 3)); // tier 3 = 500 tokens
        
        // efficient scaling requires total xp milestone
        REQUIREMENTS.put("efficient_scaling_3", new Requirement(MilestoneType.TOTAL_XP, 1)); // tier 1 = 10k xp
        REQUIREMENTS.put("efficient_scaling_5", new Requirement(MilestoneType.TOTAL_XP, 2)); // tier 2 = 100k xp
        
        // skill affinities require respective skill level milestone
        REQUIREMENTS.put("combat_affinity_3", new Requirement(MilestoneType.COMBAT_LEVEL, 1));   // level 10
        REQUIREMENTS.put("combat_affinity_5", new Requirement(MilestoneType.COMBAT_LEVEL, 2));   // level 25
        REQUIREMENTS.put("mining_affinity_3", new Requirement(MilestoneType.MINING_LEVEL, 1));
        REQUIREMENTS.put("mining_affinity_5", new Requirement(MilestoneType.MINING_LEVEL, 2));
        REQUIREMENTS.put("foraging_affinity_3", new Requirement(MilestoneType.FORAGING_LEVEL, 1));
        REQUIREMENTS.put("foraging_affinity_5", new Requirement(MilestoneType.FORAGING_LEVEL, 2));
        REQUIREMENTS.put("farming_affinity_3", new Requirement(MilestoneType.FARMING_LEVEL, 1));
        REQUIREMENTS.put("farming_affinity_5", new Requirement(MilestoneType.FARMING_LEVEL, 2));
        REQUIREMENTS.put("fishing_affinity_3", new Requirement(MilestoneType.FISHING_LEVEL, 1));
        REQUIREMENTS.put("fishing_affinity_5", new Requirement(MilestoneType.FISHING_LEVEL, 2));
        
        // frenzy upgrades require marathon milestone
        REQUIREMENTS.put("frenzy_duration_2", new Requirement(MilestoneType.MARATHON, 1)); // 30 min
        REQUIREMENTS.put("frenzy_duration_4", new Requirement(MilestoneType.MARATHON, 2)); // 60 min
        REQUIREMENTS.put("frenzy_power_2", new Requirement(MilestoneType.MARATHON, 1));
        REQUIREMENTS.put("frenzy_power_4", new Requirement(MilestoneType.MARATHON, 2));
        
        // xp frenzy consumable requires no mistakes milestone
        REQUIREMENTS.put("xp_frenzy", new Requirement(MilestoneType.NO_MISTAKES, 1)); // 25 tiles streak
        
        // remote unlock requires tiles unlocked milestone
        REQUIREMENTS.put("remote_unlock", new Requirement(MilestoneType.TILES_UNLOCKED, 2)); // tier 2 = 100 tiles
    }
    
    public static boolean canPurchase(String itemKey, int targetLevel) {
        String key = itemKey + "_" + targetLevel;
        Requirement req = REQUIREMENTS.get(key);
        if (req == null) return true;
        return req.isMet();
    }
    
    public static boolean canPurchaseConsumable(String itemKey) {
        Requirement req = REQUIREMENTS.get(itemKey);
        if (req == null) return true;
        return req.isMet();
    }
    
    public static String getRequirementText(String itemKey, int targetLevel) {
        String key = itemKey + "_" + targetLevel;
        Requirement req = REQUIREMENTS.get(key);
        if (req == null) return null;
        if (req.isMet()) return null;
        return req.getDisplayText();
    }
    
    public static String getConsumableRequirementText(String itemKey) {
        Requirement req = REQUIREMENTS.get(itemKey);
        if (req == null) return null;
        if (req.isMet()) return null;
        return req.getDisplayText();
    }
    
    public record Requirement(MilestoneType milestone, int requiredTier) {
        public boolean isMet() {
            MilestoneData data = TilemanState.getInstance().getMilestones();
            return data.getCompletedTier(milestone) >= requiredTier;
        }
        
        public String getDisplayText() {
            String tierNum = switch (requiredTier) {
                case 1 -> "I";
                case 2 -> "II";
                case 3 -> "III";
                case 4 -> "IV";
                case 5 -> "V";
                default -> String.valueOf(requiredTier);
            };
            return "Requires " + milestone.getDisplayName() + " " + tierNum;
        }
    }
}
