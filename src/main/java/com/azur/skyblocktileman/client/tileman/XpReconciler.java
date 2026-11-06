package com.azur.skyblocktileman.client.tileman;

import com.azur.skyblocktileman.client.tileman.milestone.MilestoneTracker;
import com.azur.skyblocktileman.client.tileman.ui.Fmt;
import java.util.Map;

// Corrects XP drift against the API's absolute skill totals on every profiles payload.
// A Tileman profile can only have been created after install, so its API XP was all earned
// under the challenge: missed action bar drops are re-awarded instead of anchoring silently.
public final class XpReconciler {

    private static XpReconciler instance;

    private XpReconciler() {}

    public static XpReconciler getInstance() {
        if (instance == null) {
            instance = new XpReconciler();
        }
        return instance;
    }

    // client thread only; called from the dungeon result path, which already matched the profile
    public void reconcile(HypixelApiClient.ProfilesResult result) {
        if (result == null || !result.success() || result.skillXp() == null) {
            return;
        }

        TilemanState state = TilemanState.getInstance();

        // undecided or declined profiles only anchor the stored totals; nothing is awarded
        if (!Boolean.TRUE.equals(state.getTilemanMode())) {
            for (Map.Entry<String, Long> entry : result.skillXp().entrySet()) {
                state.setSkillXp(entry.getKey(), entry.getValue());
            }
            return;
        }

        long correctedXp = 0;
        int correctedSkills = 0;

        for (Map.Entry<String, Long> entry : result.skillXp().entrySet()) {
            String skill = entry.getKey();
            long apiXp = entry.getValue();
            long stored = state.getSkillXp(skill);

            if (apiXp > stored) {
                state.setSkillXp(skill, apiXp);
            }

            if (!SkillXpParser.isTokenSkill(skill)) {
                continue;
            }

            // the API total includes off-tile grinding, which must never mint tokens:
            // the grantable ceiling is the API total minus counted off-tile XP, and
            // awardedXp is everything that already entered the token system
            long cap = SkillXpParser.maxXPFor(skill);
            long grantable = Math.min(apiXp, cap) - state.getOffTileXp(skill);
            long missed = grantable - state.getAwardedXp(skill);
            if (missed <= 0) {
                continue;
            }

            correctedXp += missed;
            correctedSkills++;
            state.onXpGained(skill, missed);
            MilestoneTracker.getInstance().onXpGained(skill, missed);
        }

        if (correctedXp > 0) {
            TilemanLog.debug(
                DebugCategory.TOKENS,
                "XpReconciler awarded {} XP across {} token skills",
                correctedXp,
                correctedSkills
            );
            TilemanChat.info(
                "XpReconciler recovered " +
                    Fmt.xp(correctedXp) +
                    " missed Skill XP across " +
                    correctedSkills +
                    " skill(s)."
            );
        }
    }
}
