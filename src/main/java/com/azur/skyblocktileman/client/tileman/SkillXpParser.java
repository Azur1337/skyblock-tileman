package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.milestone.MilestoneTracker;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkillXpParser {

    private static final Pattern SKILL_XP_ABSOLUTE_PATTERN = Pattern.compile(
        "\\+([0-9,]+(?:\\.[0-9]+)?) (Farming|Mining|Combat|Foraging|Fishing|Enchanting|Alchemy|Carpentry|Runecrafting|Social|Taming|Hunting) \\(([0-9,.]+[kKmMbB]?)/([0-9,.]+[kKmMbB]?)\\)"
    );

    private static final Pattern SKILL_XP_PERCENT_PATTERN = Pattern.compile(
        "\\+([0-9,]+(?:\\.[0-9]+)?) (Farming|Mining|Combat|Foraging|Fishing|Enchanting|Alchemy|Carpentry|Runecrafting|Social|Taming|Hunting) \\(([0-9.]+)%\\)"
    );

    private static final Set<String> TOKEN_SKILLS = Set.of(
        "Combat", "Mining", "Foraging", "Farming", "Fishing"
    );
    
    private static final Map<String, Integer> SKILL_MAX_LEVELS = Map.ofEntries(
        Map.entry("Combat", 60),
        Map.entry("Mining", 60),
        Map.entry("Farming", 60),
        Map.entry("Foraging", 54),
        Map.entry("Fishing", 50),
        Map.entry("Enchanting", 60),
        Map.entry("Alchemy", 50),
        Map.entry("Carpentry", 50),
        Map.entry("Taming", 60),
        Map.entry("Runecrafting", 25),
        Map.entry("Social", 25)
    );

    private static final long[] XP_REQUIRED = {
        0L,         // 0
        50L,        // 1
        125L,       // 2
        200L,       // 3
        300L,       // 4
        500L,       // 5
        750L,       // 6
        1_000L,     // 7
        1_500L,     // 8
        2_000L,     // 9
        3_500L,     // 10
        5_000L,     // 11
        7_500L,     // 12
        10_000L,    // 13
        15_000L,    // 14
        20_000L,    // 15
        30_000L,    // 16
        50_000L,    // 17
        75_000L,    // 18
        100_000L,   // 19
        200_000L,   // 20
        300_000L,   // 21
        400_000L,   // 22
        500_000L,   // 23
        600_000L,   // 24
        700_000L,   // 25
        800_000L,   // 26
        900_000L,   // 27
        1_000_000L, // 28
        1_100_000L, // 29
        1_200_000L, // 30
        1_300_000L, // 31
        1_400_000L, // 32
        1_500_000L, // 33
        1_600_000L, // 34
        1_700_000L, // 35
        1_800_000L, // 36
        1_900_000L, // 37
        2_000_000L, // 38
        2_100_000L, // 39
        2_200_000L, // 40
        2_300_000L, // 41
        2_400_000L, // 42
        2_500_000L, // 43
        2_600_000L, // 44
        2_750_000L, // 45
        2_900_000L, // 46
        3_100_000L, // 47
        3_400_000L, // 48
        3_700_000L, // 49
        4_000_000L, // 50
        4_300_000L, // 51
        4_600_000L, // 52
        4_900_000L, // 53
        5_200_000L, // 54
        5_500_000L, // 55
        5_800_000L, // 56
        6_100_000L, // 57
        6_400_000L, // 58
        6_700_000L, // 59
        7_000_000L, // 60
    };

    private static final long[] CUMULATIVE_XP;

    static {
        CUMULATIVE_XP = new long[XP_REQUIRED.length];
        CUMULATIVE_XP[0] = 0;
        for (int i = 1; i < XP_REQUIRED.length; i++) {
            CUMULATIVE_XP[i] = CUMULATIVE_XP[i - 1] + XP_REQUIRED[i];
        }
    }

    private static final Map<Long, Integer> NEEDED_TO_LEVEL = new HashMap<>();

    static {
        for (int i = 1; i < XP_REQUIRED.length; i++) {
            NEEDED_TO_LEVEL.put(XP_REQUIRED[i], i);
        }
    }

    private SkillXpParser() {}

    public static void resetTracking() {
        TilemanLog.debug(DebugCategory.ACTION_BAR, "Reset skill XP tracking for profile switch");
    }

    public static int getLevelForXp(long totalXp) {
        for (int i = CUMULATIVE_XP.length - 1; i >= 0; i--) {
            if (totalXp >= CUMULATIVE_XP[i]) {
                return i;
            }
        }
        return 0;
    }

    public static void parse(String text) {
        if (!TilemanConfig.getInstance().isEnabled()) {
            return;
        }

        Matcher absoluteMatcher = SKILL_XP_ABSOLUTE_PATTERN.matcher(text);
        while (absoluteMatcher.find()) {
            try {
                long xpGainedFromBar = parseXpNumber(absoluteMatcher.group(1));
                String skill = absoluteMatcher.group(2);
                long current = parseXpNumber(absoluteMatcher.group(3));
                long needed = parseXpNumber(absoluteMatcher.group(4));

                if (!TOKEN_SKILLS.contains(skill)) {
                    continue;
                }

                long totalXp;
                int level;
                
                if (needed == 0) {
                    // max level - current shows overflow xp beyond max
                    // totalXp = cumulative to max level + overflow
                    int maxLevel = SKILL_MAX_LEVELS.getOrDefault(skill, 60);
                    level = maxLevel;
                    totalXp = CUMULATIVE_XP[maxLevel] + current;
                    
                    // At max level, use direct XP gain since baseline tracking can be tricky
                    // with the overflow values
                    TilemanState state = TilemanState.getInstance();
                    long oldXp = state.getSkillXp(skill);
                    
                    if (oldXp == 0) {
                        // First detection at max level - set baseline, use direct gain
                        state.setSkillXp(skill, totalXp);
                        TilemanLog.debug(DebugCategory.TOKENS,
                            "Initial {} XP baseline at max level set to {} (overflow: {})",
                            skill, totalXp, current);
                        processDirectXpGain(skill, xpGainedFromBar);
                        continue;
                    }
                } else {
                    Integer detectedLevel = NEEDED_TO_LEVEL.get(needed);
                    if (detectedLevel == null) {
                        TilemanLog.debug(DebugCategory.ACTION_BAR,
                            "Unknown XP requirement {} for {}, skipping",
                            needed,
                            skill
                        );
                        continue;
                    }
                    level = detectedLevel;
                    totalXp = CUMULATIVE_XP[level] + current;
                }

                processXpUpdate(skill, totalXp, level, current, needed);
            } catch (NumberFormatException ignored) {}
        }

        Matcher percentMatcher = SKILL_XP_PERCENT_PATTERN.matcher(text);
        while (percentMatcher.find()) {
            try {
                long xpGainedFromBar = parseXpNumber(percentMatcher.group(1));
                String skill = percentMatcher.group(2);
                double percent = Double.parseDouble(percentMatcher.group(3));

                if (!TOKEN_SKILLS.contains(skill)) {
                    continue;
                }

                TilemanState state = TilemanState.getInstance();
                long storedXp = state.getSkillXp(skill);

                if (storedXp == 0) {
                    // No baseline, but we CAN use the XP gain directly from the actionbar
                    // We can't determine total XP, but we can award tokens for what we see
                    TilemanLog.debug(DebugCategory.ACTION_BAR,
                        "No stored XP for {}, using direct XP gain from actionbar: +{}",
                        skill, xpGainedFromBar
                    );
                    processDirectXpGain(skill, xpGainedFromBar);
                    continue;
                }

                int level = getLevelForXp(storedXp);
                if (level >= XP_REQUIRED.length - 1) {
                    // At max level with baseline - use direct XP gain
                    processDirectXpGain(skill, xpGainedFromBar);
                    continue;
                }

                long xpForNextLevel = XP_REQUIRED[level + 1];
                long currentInLevel = (long) (percent / 100.0 * xpForNextLevel);
                long totalXp = CUMULATIVE_XP[level] + currentInLevel;

                processXpUpdate(skill, totalXp, level, currentInLevel, xpForNextLevel);
            } catch (NumberFormatException ignored) {}
        }
    }

    // max reasonable xp gain per actionbar update when we have existing baseline
    private static final long MAX_REASONABLE_XP_GAIN = 100_000;

    /**
     * Process XP gain directly from actionbar when we don't have/need a baseline.
     * Used for percentage format when storedXp == 0, or at max level.
     * We can't track total XP, but we CAN award tokens for what we see.
     */
    private static void processDirectXpGain(String skill, long xpGained) {
        if (xpGained <= 0 || xpGained > MAX_REASONABLE_XP_GAIN) {
            TilemanLog.debug(DebugCategory.TOKENS,
                "Skipping direct XP gain for {} (amount: {})",
                skill, xpGained);
            return;
        }

        TilemanState state = TilemanState.getInstance();
        
        // Award tokens for the XP gain
        state.onXpGained(xpGained);

        TilemanLog.debug(DebugCategory.TOKENS,
            "Direct {} XP gain: +{}, tokens: {} available",
            skill,
            xpGained,
            state.getTokens()
        );

        // Track for milestones
        MilestoneTracker.getInstance().onXpGained(skill, xpGained);
    }

    private static void processXpUpdate(String skill, long totalXp, int level, long current, long needed) {
        TilemanState state = TilemanState.getInstance();
        long oldXp = state.getSkillXp(skill);

        if (totalXp <= oldXp) {
            return;
        }

        long xpGained = totalXp - oldXp;
        
        // update stored XP regardless
        state.setSkillXp(skill, totalXp);
        
        // if no baseline existed (oldXp == 0), this is initial detection - just set baseline
        if (oldXp == 0) {
            TilemanLog.debug(DebugCategory.TOKENS,
                "Initial {} XP baseline set to {} (level {})",
                skill, totalXp, level);
            return;
        }
        
        // if gain is unreasonably large even with baseline, something is wrong - skip
        if (xpGained > MAX_REASONABLE_XP_GAIN) {
            TilemanLog.debug(DebugCategory.TOKENS,
                "Unusually large XP gain for {} ({} -> {}, +{}), skipping token award",
                skill, oldXp, totalXp, xpGained);
            return;
        }
        
        // process XP gain for tokens
        state.onXpGained(xpGained);

        TilemanLog.debug(DebugCategory.TOKENS,
            "Detected {} XP: {} -> {} (+{}, level {}, {}/{}, tokens: {} available)",
            skill,
            oldXp,
            totalXp,
            xpGained,
            level,
            current,
            needed,
            state.getTokens()
        );

        // track for milestones
        MilestoneTracker.getInstance().onXpGained(skill, xpGained);
    }

    private static long parseXpNumber(String str) {
        String cleaned = str.replace(",", "").toLowerCase();
        double multiplier = 1;
        if (cleaned.endsWith("k")) {
            multiplier = 1_000;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        } else if (cleaned.endsWith("m")) {
            multiplier = 1_000_000;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        } else if (cleaned.endsWith("b")) {
            multiplier = 1_000_000_000;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return (long) (Double.parseDouble(cleaned) * multiplier);
    }
}
