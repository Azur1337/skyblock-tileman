package com.azur.skyblocktileman.client.tileman.milestone;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum MilestoneType {
    
    // progression
    TILES_UNLOCKED("Tiles Unlocked", MilestoneCategory.PROGRESSION, Items.GRASS_BLOCK,
        new long[]{25, 100, 500, 2500, 10000},
        new int[]{2, 5, 10, 25, 50}),
    
    TOTAL_XP("Total XP Earned", MilestoneCategory.PROGRESSION, Items.EXPERIENCE_BOTTLE,
        new long[]{10000, 100000, 1000000, 10000000, 100000000},
        new int[]{2, 5, 15, 30, 50}),
    
    TOKENS_EARNED("Tokens Earned", MilestoneCategory.PROGRESSION, Items.GOLD_NUGGET,
        new long[]{25, 100, 500, 2500, 10000},
        new int[]{3, 5, 15, 30, 50}),
    
    ISLANDS_EXPLORED("Islands Explored", MilestoneCategory.PROGRESSION, Items.FILLED_MAP,
        new long[]{3, 5, 10, 15, 20},
        new int[]{5, 10, 20, 35, 50}),
    
    // skill level milestones
    COMBAT_LEVEL("Combat Level", MilestoneCategory.SKILL, Items.DIAMOND_SWORD,
        new long[]{10, 25, 40, 50, 60},
        new int[]{5, 15, 30, 50, 100},
        "Reach Combat skill levels"),
    
    MINING_LEVEL("Mining Level", MilestoneCategory.SKILL, Items.DIAMOND_PICKAXE,
        new long[]{10, 25, 40, 50, 60},
        new int[]{5, 15, 30, 50, 100},
        "Reach Mining skill levels"),
    
    FORAGING_LEVEL("Foraging Level", MilestoneCategory.SKILL, Items.DIAMOND_AXE,
        new long[]{10, 25, 40, 50, 54},
        new int[]{5, 15, 30, 50, 100},
        "Reach Foraging skill levels"),
    
    FARMING_LEVEL("Farming Level", MilestoneCategory.SKILL, Items.DIAMOND_HOE,
        new long[]{10, 25, 40, 50, 60},
        new int[]{5, 15, 30, 50, 100},
        "Reach Farming skill levels"),
    
    FISHING_LEVEL("Fishing Level", MilestoneCategory.SKILL, Items.FISHING_ROD,
        new long[]{10, 25, 40, 50},
        new int[]{5, 15, 30, 50},
        "Reach Fishing skill levels"),
    
    // slayer milestones - kills (T4+ only, starts at harder amounts)
    REVENANT_KILLS("Revenant Slayer", MilestoneCategory.SLAYER, Items.ROTTEN_FLESH,
        new long[]{10, 50, 150, 500, 1000},
        new int[]{5, 15, 30, 75, 150},
        "Kill T4+ Revenant Horror bosses"),
    
    TARANTULA_KILLS("Tarantula Slayer", MilestoneCategory.SLAYER, Items.STRING,
        new long[]{10, 50, 150, 500, 1000},
        new int[]{5, 15, 30, 75, 150},
        "Kill T4 Tarantula Broodfather bosses"),
    
    SVEN_KILLS("Sven Slayer", MilestoneCategory.SLAYER, Items.BONE,
        new long[]{10, 50, 150, 500, 1000},
        new int[]{5, 15, 30, 75, 150},
        "Kill T4 Sven Packmaster bosses"),
    
    VOIDGLOOM_KILLS("Voidgloom Slayer", MilestoneCategory.SLAYER, Items.ENDER_PEARL,
        new long[]{10, 50, 150, 500, 1000},
        new int[]{5, 15, 30, 75, 150},
        "Kill T4 Voidgloom Seraph bosses"),
    
    INFERNO_KILLS("Inferno Slayer", MilestoneCategory.SLAYER, Items.BLAZE_ROD,
        new long[]{10, 50, 150, 500, 1000},
        new int[]{5, 15, 30, 75, 150},
        "Kill T4+ Inferno Demonlord bosses"),
    
    RIFTSTALKER_KILLS("Riftstalker Slayer", MilestoneCategory.SLAYER, Items.GLASS_BOTTLE,
        new long[]{10, 50, 150, 500, 1000},
        new int[]{5, 15, 30, 75, 150},
        "Kill T4+ Riftstalker Bloodfiend bosses"),
    
    // challenge milestones
    NO_MISTAKES("No Mistakes", MilestoneCategory.CHALLENGE, Items.GOLDEN_APPLE,
        new long[]{25, 100, 500, 1000, 2500},
        new int[]{5, 10, 25, 40, 75},
        "Unlock tiles without rule breaks (resets on break)"),
    
    MARATHON("Marathon", MilestoneCategory.CHALLENGE, Items.CLOCK,
        new long[]{30, 60, 120, 300, 600},
        new int[]{3, 5, 10, 25, 50},
        "Play X minutes without rule breaks"),
    
    SLAYER_STREAK("Slayer Streak", MilestoneCategory.CHALLENGE, Items.WITHER_SKELETON_SKULL,
        new long[]{3, 5, 10, 25, 50},
        new int[]{10, 20, 50, 100, 250},
        "T4+ flawless kills in a row (any slayer)"),
    
    // flawless milestones (xp earned while on tiles, resets on rule break)
    FLAWLESS_MINING("Flawless Mining", MilestoneCategory.FLAWLESS, Items.DIAMOND_PICKAXE,
        new long[]{5000, 25000, 100000, 500000, 2000000},
        new int[]{5, 10, 20, 40, 75},
        "Earn mining XP without leaving tiles"),
    
    FLAWLESS_FORAGING("Flawless Foraging", MilestoneCategory.FLAWLESS, Items.DIAMOND_AXE,
        new long[]{5000, 25000, 100000, 500000, 2000000},
        new int[]{5, 10, 20, 40, 75},
        "Earn foraging XP without leaving tiles"),
    
    FLAWLESS_FARMING("Flawless Farming", MilestoneCategory.FLAWLESS, Items.DIAMOND_HOE,
        new long[]{5000, 25000, 100000, 500000, 2000000},
        new int[]{5, 10, 20, 40, 75},
        "Earn farming XP without leaving tiles"),
    
    FLAWLESS_COMBAT("Flawless Combat", MilestoneCategory.FLAWLESS, Items.DIAMOND_SWORD,
        new long[]{5000, 25000, 100000, 500000, 2000000},
        new int[]{5, 10, 20, 40, 75},
        "Earn combat XP without leaving tiles"),
    
    FLAWLESS_FISHING("Flawless Fishing", MilestoneCategory.FLAWLESS, Items.FISHING_ROD,
        new long[]{5000, 25000, 100000, 500000, 2000000},
        new int[]{5, 10, 20, 40, 75},
        "Earn fishing XP without leaving tiles"),
    
    // flawless slayer (per type) - kill T4+ without stepping on locked tiles
    FLAWLESS_REVENANT("Flawless Revenant", MilestoneCategory.FLAWLESS, Items.ZOMBIE_HEAD,
        new long[]{1, 5, 15, 50, 100},
        new int[]{10, 25, 50, 100, 200},
        "Kill T4+ Revenant without leaving tiles"),
    
    FLAWLESS_TARANTULA("Flawless Tarantula", MilestoneCategory.FLAWLESS, Items.SPIDER_EYE,
        new long[]{1, 5, 15, 50, 100},
        new int[]{10, 25, 50, 100, 200},
        "Kill T4 Tarantula without leaving tiles"),
    
    FLAWLESS_SVEN("Flawless Sven", MilestoneCategory.FLAWLESS, Items.BONE,
        new long[]{1, 5, 15, 50, 100},
        new int[]{10, 25, 50, 100, 200},
        "Kill T4 Sven without leaving tiles"),
    
    FLAWLESS_VOIDGLOOM("Flawless Voidgloom", MilestoneCategory.FLAWLESS, Items.ENDER_EYE,
        new long[]{1, 5, 15, 50, 100},
        new int[]{10, 25, 50, 100, 200},
        "Kill T4 Voidgloom without leaving tiles"),
    
    FLAWLESS_INFERNO("Flawless Inferno", MilestoneCategory.FLAWLESS, Items.BLAZE_POWDER,
        new long[]{1, 5, 15, 50, 100},
        new int[]{10, 25, 50, 100, 200},
        "Kill T4+ Inferno without leaving tiles"),
    
    FLAWLESS_RIFTSTALKER("Flawless Riftstalker", MilestoneCategory.FLAWLESS, Items.REDSTONE,
        new long[]{1, 5, 15, 50, 100},
        new int[]{10, 25, 50, 100, 200},
        "Kill T4+ Riftstalker without leaving tiles"),
    
    // secret milestones
    OOPS("Oops", MilestoneCategory.SECRET, Items.BARRIER,
        new long[]{100},
        new int[]{1},
        "Break rules 100 times"),
    
    HOARDER("Hoarder", MilestoneCategory.SECRET, Items.CHEST,
        new long[]{100},
        new int[]{5},
        "Have 100 unspent tokens"),
    
    BIG_SPENDER("Big Spender", MilestoneCategory.SECRET, Items.EMERALD,
        new long[]{500},
        new int[]{10},
        "Spend 500 tokens in the shop"),
    
    LUCKY_DAY("Lucky Day", MilestoneCategory.SECRET, Items.RABBIT_FOOT,
        new long[]{5},
        new int[]{5},
        "Proc lucky unlock 5x in one session"),
    
    JACKPOT("Jackpot", MilestoneCategory.SECRET, Items.DIAMOND,
        new long[]{1},
        new int[]{10},
        "Proc triple+ unlock");

    private final String displayName;
    private final MilestoneCategory category;
    private final Item icon;
    private final long[] tierTargets;
    private final int[] tierRewards;
    private final String description;

    MilestoneType(String displayName, MilestoneCategory category, Item icon, 
                  long[] tierTargets, int[] tierRewards) {
        this(displayName, category, icon, tierTargets, tierRewards, null);
    }

    MilestoneType(String displayName, MilestoneCategory category, Item icon,
                  long[] tierTargets, int[] tierRewards, String description) {
        this.displayName = displayName;
        this.category = category;
        this.icon = icon;
        this.tierTargets = tierTargets;
        this.tierRewards = tierRewards;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public MilestoneCategory getCategory() {
        return category;
    }

    public Item getIcon() {
        return icon;
    }

    public int getMaxTier() {
        return tierTargets.length;
    }

    public long getTargetForTier(int tier) {
        if (tier < 1 || tier > tierTargets.length) return Long.MAX_VALUE;
        return tierTargets[tier - 1];
    }

    public int getRewardForTier(int tier) {
        if (tier < 1 || tier > tierRewards.length) return 0;
        return tierRewards[tier - 1];
    }

    public String getDescription() {
        return description;
    }

    public boolean hasDescription() {
        return description != null;
    }

    public String getTierNumeral(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(tier);
        };
    }
}
