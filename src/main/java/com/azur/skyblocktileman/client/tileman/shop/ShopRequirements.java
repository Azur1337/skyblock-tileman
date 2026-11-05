package com.azur.skyblocktileman.client.tileman.shop;

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
        80_000_000L
    };

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
            int idx = targetLevel - 1;
            if (idx >= 0 && idx < LUCKY_XP_GATES.length) {
                long required = LUCKY_XP_GATES[idx];
                long have = TilemanState.getInstance().getTotalSkillXp();
                if (have < required) {
                    return (
                        "Requires " +
                        formatXp(required) +
                        " total XP (you have " +
                        formatXp(have) +
                        ")"
                    );
                }
            }
            return null;
        }
        String key = itemKey + "_" + targetLevel;
        Requirement req = REQUIREMENTS.get(key);
        if (req == null) return null;
        if (req.isMet()) return null;
        return req.getDisplayText();
    }

    private static String formatXp(long xp) {
        if (xp >= 1_000_000) {
            return (xp / 1_000_000) + "M";
        }
        if (xp >= 1_000) {
            return (xp / 1_000) + "k";
        }
        return String.valueOf(xp);
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
