package com.azur.skyblocktileman.client.tileman;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkillXpParser {

    private static final Pattern SKILL_XP_PATTERN = Pattern.compile(
        "\\+[0-9,]+(?:\\.[0-9]+)? (Farming|Mining|Combat|Foraging|Fishing|Enchanting|Alchemy|Carpentry|Runecrafting|Social|Taming|Hunting) \\(([0-9,.]+[kKmMbB]?)/([0-9,.]+[kKmMbB]?)\\)"
    );

    private static final Set<String> TOKEN_SKILLS = Set.of(
        "Combat", "Mining", "Foraging", "Farming", "Fishing"
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

    private static int lastTokenCount = -1;

    private SkillXpParser() {}

    public static void parse(String text) {
        if (!TilemanConfig.getInstance().isEnabled()) {
            return;
        }

        Matcher matcher = SKILL_XP_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                String skill = matcher.group(1);
                long current = parseXpNumber(matcher.group(2));
                long needed = parseXpNumber(matcher.group(3));

                if (needed == 0) {
                    continue;
                }

                if (!TOKEN_SKILLS.contains(skill)) {
                    continue;
                }

                Integer level = NEEDED_TO_LEVEL.get(needed);
                if (level == null) {
                    TilemanLog.debug(
                        "Unknown XP requirement {} for {}, skipping",
                        needed,
                        skill
                    );
                    continue;
                }

                long totalXp = CUMULATIVE_XP[level] + current;
                TilemanState state = TilemanState.getInstance();
                long oldXp = state.getSkillXp(skill);

                if (totalXp <= oldXp) {
                    continue;
                }

                int oldTokens = state.getTokens();
                if (lastTokenCount < 0) {
                    lastTokenCount = oldTokens;
                }

                state.setSkillXp(skill, totalXp);

                int newTokens = state.getTokens();
                int tokensGranted = newTokens - lastTokenCount;
                lastTokenCount = newTokens;

                TilemanLog.debug(
                    "Detected {} XP: {} -> {} (level {}, {}/{}, tokens: {} available)",
                    skill,
                    oldXp,
                    totalXp,
                    level,
                    current,
                    needed,
                    newTokens
                );

                if (tokensGranted > 0) {
                    TilemanChat.info(
                        "+" + tokensGranted + " Block Unlock Token" +
                        (tokensGranted > 1 ? "s" : "") +
                        "! (Total: " + newTokens + ")"
                    );
                }
            } catch (NumberFormatException ignored) {}
        }
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
