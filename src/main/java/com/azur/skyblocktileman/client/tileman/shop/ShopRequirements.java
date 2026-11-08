package com.azur.skyblocktileman.client.tileman.shop;

import com.azur.skyblocktileman.client.tileman.Balance;
import com.azur.skyblocktileman.client.tileman.TilemanState;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneData;
import com.azur.skyblocktileman.client.tileman.milestone.MilestoneType;
import java.util.HashMap;
import java.util.Map;

public class ShopRequirements {

    private static final Map<String, Requirement> REQUIREMENTS =
        new HashMap<>();

    // total XP required to buy each Lucky Unlock level (1..6)
    private static final long[] LUCKY_XP_GATES = {
        1_000_000L,
        3_000_000L,
        8_000_000L,
        20_000_000L,
        45_000_000L,
        80_000_000L,
    };

    public static long luckyXpGate(int level) {
        if (level < 1 || level > LUCKY_XP_GATES.length) {
            return Long.MAX_VALUE;
        }
        return Balance.getLong(
            "shop.lucky_unlock.gate." + level,
            LUCKY_XP_GATES[level - 1],
            0
        );
    }

    static {
        // lucky_unlock is gated by raw total XP earned (see getRequirementText)

        REQUIREMENTS.put(
            "efficient_scaling_3",
            new Requirement(MilestoneType.TOTAL_XP, 1)
        ); // tier 1 = 10k xp
        REQUIREMENTS.put(
            "efficient_scaling_5",
            new Requirement(MilestoneType.TOTAL_XP, 2)
        ); // tier 2 = 100k xp

        REQUIREMENTS.put(
            "combat_affinity_3",
            new Requirement(MilestoneType.COMBAT_LEVEL, 1)
        ); // level 10
        REQUIREMENTS.put(
            "combat_affinity_5",
            new Requirement(MilestoneType.COMBAT_LEVEL, 2)
        ); // level 25
        REQUIREMENTS.put(
            "mining_affinity_3",
            new Requirement(MilestoneType.MINING_LEVEL, 1)
        );
        REQUIREMENTS.put(
            "mining_affinity_5",
            new Requirement(MilestoneType.MINING_LEVEL, 2)
        );
        REQUIREMENTS.put(
            "foraging_affinity_3",
            new Requirement(MilestoneType.FORAGING_LEVEL, 1)
        );
        REQUIREMENTS.put(
            "foraging_affinity_5",
            new Requirement(MilestoneType.FORAGING_LEVEL, 2)
        );
        REQUIREMENTS.put(
            "farming_affinity_3",
            new Requirement(MilestoneType.FARMING_LEVEL, 1)
        );
        REQUIREMENTS.put(
            "farming_affinity_5",
            new Requirement(MilestoneType.FARMING_LEVEL, 2)
        );
        REQUIREMENTS.put(
            "fishing_affinity_3",
            new Requirement(MilestoneType.FISHING_LEVEL, 1)
        );
        REQUIREMENTS.put(
            "fishing_affinity_5",
            new Requirement(MilestoneType.FISHING_LEVEL, 2)
        );

        REQUIREMENTS.put(
            "frenzy_duration_2",
            new Requirement(MilestoneType.MARATHON, 1)
        ); // 30 min
        REQUIREMENTS.put(
            "frenzy_duration_4",
            new Requirement(MilestoneType.MARATHON, 2)
        ); // 60 min
        REQUIREMENTS.put(
            "frenzy_power_2",
            new Requirement(MilestoneType.MARATHON, 1)
        );
        REQUIREMENTS.put(
            "frenzy_power_4",
            new Requirement(MilestoneType.MARATHON, 2)
        );

        REQUIREMENTS.put(
            "xp_frenzy",
            new Requirement(MilestoneType.NO_MISTAKES, 1)
        ); // 25 tiles streak

        REQUIREMENTS.put(
            "remote_unlock",
            new Requirement(MilestoneType.TILES_UNLOCKED, 2)
        ); // tier 2 = 100 tiles
    }

    public static String getRequirementText(String itemKey, int targetLevel) {
        if (itemKey.equals("lucky_unlock")) {
            if (targetLevel >= 1 && targetLevel <= LUCKY_XP_GATES.length) {
                long required = luckyXpGate(targetLevel);
                long have = TilemanState.getInstance().getTotalSkillXp();
                if (have < required) {
                    return (
                        "Requires " +
                        formatXp(required) +
                        " Total XP (you have " +
                        formatXp(have) +
                        ")"
                    );
                }
            }
            return null;
        }
        String key = itemKey + "_" + targetLevel;
        Requirement requirement = REQUIREMENTS.get(key);
        if (requirement == null) return null;
        if (requirement.isMet()) return null;
        return requirement.getDisplayText();
    }

    private static String formatXp(long xp) {
        if (xp >= 1_000_000) {
            return xp / 1_000_000 + "M";
        }
        if (xp >= 1_000) {
            return xp / 1_000 + "k";
        }
        return String.valueOf(xp);
    }

    public static String getConsumableRequirementText(String itemKey) {
        Requirement requirement = REQUIREMENTS.get(itemKey);
        if (requirement == null) return null;
        if (requirement.isMet()) return null;
        return requirement.getDisplayText();
    }

    // The milestone behind a locked upgrade, for the right click jump. The Lucky Unlock
    // gates are raw total XP, which the Total XP Earned milestone tracks.
    public static MilestoneType getRequirementMilestone(
        String itemKey,
        int targetLevel
    ) {
        if (itemKey.equals("lucky_unlock")) {
            return MilestoneType.TOTAL_XP;
        }
        Requirement requirement = REQUIREMENTS.get(itemKey + "_" + targetLevel);
        if (requirement == null) {
            requirement = REQUIREMENTS.get(itemKey);
        }
        return requirement == null ? null : requirement.milestone();
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
