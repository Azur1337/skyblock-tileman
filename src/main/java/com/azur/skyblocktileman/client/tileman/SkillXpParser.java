package com.azur.skyblocktileman.client.tileman;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Shared skill xp regex, used by both the action bar packet mixin and the
// system-chat overlay listener, since different servers send this differently.
public final class SkillXpParser {

    private static final Pattern SKILL_XP_PATTERN = Pattern.compile(
        "\\+([0-9]+(?:\\.[0-9]+)?) (Farming|Mining|Combat|Foraging|Fishing|Enchanting|Alchemy|Carpentry|Runecrafting|Social|Taming|Hunting) \\("
    );

    private SkillXpParser() {}

    public static void parse(String text) {
        if (!TilemanConfig.getInstance().isEnabled()) {
            return;
        }

        Matcher matcher = SKILL_XP_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                double xpGained = Double.parseDouble(matcher.group(1));
                String skill = matcher.group(2);
                TilemanState state = TilemanState.getInstance();
                int tokensGranted = state.onSkillXpGained(xpGained);

                TilemanLog.debug(
                    "Detected {} XP gain: +{} (tokens now {}, {} granted this drop)",
                    skill,
                    xpGained,
                    state.getTokens(),
                    tokensGranted
                );

                if (tokensGranted > 0) {
                    TilemanChat.info(
                        "+" +
                            tokensGranted +
                            " Block Unlock Token" +
                            (tokensGranted > 1 ? "s" : "") +
                            "! (Total: " +
                            state.getTokens() +
                            ")"
                    );
                }
            } catch (NumberFormatException ignored) {}
        }
    }
}
