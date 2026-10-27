package com.azur.skyblocktileman.slayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns Hypixel slayer chat and scoreboard text into typed {@link SlayerEvent}s.
 *
 * This class deliberately depends on nothing from Minecraft or from the rest of
 * the mod: it takes plain strings and emits plain objects, so it can be unit
 * tested, and lifted out as its own artifact unchanged. Feed it from an adapter
 * that owns the game hooks.
 *
 * Not thread safe. Call it from the client thread.
 */
public final class SlayerDetector {

    private static final Pattern QUEST_STARTED = Pattern.compile(
        "SLAYER QUEST STARTED!",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern QUEST_TURNED_IN = Pattern.compile(
        "SLAYER QUEST COMPLETE!",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern QUEST_FAILED = Pattern.compile(
        "SLAYER QUEST FAILED!",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern REQUIREMENT = Pattern.compile(
        "Slay\\s+([0-9,.kKmM]+)\\s+Combat XP worth of\\s+(.+?)\\.?\\s*$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MINIBOSS_SPAWNED = Pattern.compile(
        "SLAYER MINIBOSS!\\s*The\\s+(.+?)\\s+spawned!",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BOSS_SPAWNED = Pattern.compile(
        "SLAYER BOSS!\\s*The\\s+(.+?)\\s+spawned!",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern KILLED_BY = Pattern.compile(
        "You were killed by\\s+(.+?)\\.?\\s*$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DIED_REASON = Pattern.compile(
        "You died!",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TIME_LIMIT_REASON = Pattern.compile(
        "within the time limit",
        Pattern.CASE_INSENSITIVE
    );

    private static final String QUEST_HEADER = "Slayer Quest";
    private static final Pattern SCOREBOARD_TIER = Pattern.compile(
        "\\s(IV|III|II|I|V)$"
    );
    private static final Pattern SCOREBOARD_PROGRESS = Pattern.compile(
        "\\(\\s*([0-9,.]+)\\s*/\\s*([0-9,.]+)\\s*\\)"
    );

    /** Consecutive scoreboard polls without a quest before calling it abandoned. */
    private static final int ABANDON_AFTER_MISSED_POLLS = 3;

    /** Ticks to wait for the reason line that follows a failure. */
    private static final int FAIL_REASON_GRACE_TICKS = 20;

    /** Ticks a "You were killed by" line stays useful as failure evidence. */
    private static final int DEATH_SIGHTING_TICKS = 40;

    private final List<Consumer<SlayerEvent>> listeners = new ArrayList<>();

    private boolean active;
    private SlayerBoss boss;
    private SlayerTier tier = SlayerTier.UNKNOWN;
    private TierSource tierSource = TierSource.NONE;
    private SlayerPhase phase = SlayerPhase.NONE;
    private long currentCombatXp;
    private long requiredCombatXp;
    private long reportedXp = -1;
    private long reportedRequiredXp = -1;
    private boolean bossSpawned;
    private boolean bossSlain;

    private int missedPolls;
    private int failReasonCountdown = -1;
    private int deathSightingCountdown = -1;
    private String identitySignature = "";

    public Subscription events(Consumer<SlayerEvent> sink) {
        listeners.add(sink);
        return () -> listeners.remove(sink);
    }

    public Optional<SlayerQuest> current() {
        if (!active) {
            return Optional.empty();
        }
        return Optional.of(
            new SlayerQuest(
                boss,
                tier,
                tierSource,
                phase,
                currentCombatXp,
                requiredCombatXp,
                bossSpawned,
                bossSlain
            )
        );
    }

    /** Call on every client tick, whether or not anything was received. */
    public void onTick() {
        if (deathSightingCountdown > 0) {
            deathSightingCountdown--;
        }

        if (failReasonCountdown > 0) {
            failReasonCountdown--;
            if (failReasonCountdown == 0) {
                fail(FailReason.UNKNOWN);
            }
        }
    }

    /** Drops all state, for a disconnect or profile switch. Emits nothing. */
    public void reset() {
        clearQuest();
        failReasonCountdown = -1;
        deathSightingCountdown = -1;
    }

    // ------------------------------------------------------------------ chat

    /**
     * Feed one received chat message here, with colour codes already stripped.
     * Action-bar and title text must not be routed here or the boss-spawn
     * patterns can double count.
     */
    public void onChatLine(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        String text = line.trim();

        if (failReasonCountdown > 0) {
            if (DIED_REASON.matcher(text).find()) {
                fail(FailReason.DEATH);
                return;
            }
            if (TIME_LIMIT_REASON.matcher(text).find()) {
                fail(FailReason.TIME_LIMIT);
                return;
            }
        }

        if (KILLED_BY.matcher(text).find()) {
            deathSightingCountdown = DEATH_SIGHTING_TICKS;
        }

        // MINIBOSS first: the two messages differ by one word and misfiring
        // here would credit a miniboss kill as a boss kill
        Matcher miniboss = MINIBOSS_SPAWNED.matcher(text);
        if (miniboss.find()) {
            if (active) {
                emit(new SlayerEvent.MinibossSpawned(miniboss.group(1)));
            }
            return;
        }

        Matcher bossSpawn = BOSS_SPAWNED.matcher(text);
        if (bossSpawn.find()) {
            onBossSpawned(bossSpawn.group(1));
            return;
        }

        if (QUEST_STARTED.matcher(text).find()) {
            beginQuest();
            return;
        }

        Matcher requirement = REQUIREMENT.matcher(text);
        if (requirement.find()) {
            onRequirement(requirement.group(1), requirement.group(2));
            return;
        }

        if (QUEST_TURNED_IN.matcher(text).find()) {
            onTurnedIn();
            return;
        }

        if (QUEST_FAILED.matcher(text).find()) {
            // the reason arrives on the following line, so hold the failure
            failReasonCountdown = FAIL_REASON_GRACE_TICKS;
            return;
        }
    }

    private void beginQuest() {
        clearQuest();
        active = true;
        emit(new SlayerEvent.QuestStarted());
        tryIdentify();
    }

    private void onRequirement(String xpText, String mobText) {
        long xp = parseNumber(xpText);
        SlayerBoss fromMob = SlayerBoss.fromMobText(mobText);

        if (fromMob != null) {
            boss = fromMob;
        }
        if (xp > 0) {
            requiredCombatXp = xp;
        }
        if (!active) {
            // requirement can reach us before the started line
            beginQuest();
        }

        if (fromMob != null && xp > 0 && tierSource != TierSource.SCOREBOARD) {
            SlayerTier inferred = fromMob.inferTierFromCombatXp(xp);
            if (inferred.known()) {
                tier = inferred;
                tierSource = TierSource.XP_TABLE;
            }
        }
        tryIdentify();
    }

    private void onBossSpawned(String mobName) {
        SlayerBoss named = SlayerBoss.fromText(mobName);
        if (named != null) {
            boss = named;
        }

        if (!active) {
            beginQuest();
        }

        bossSpawned = true;
        if (phase == SlayerPhase.NONE || phase == SlayerPhase.GRINDING) {
            phase = SlayerPhase.BOSS_AVAILABLE;
        }
        emit(new SlayerEvent.BossSpawned(boss, tier));
        tryIdentify();
    }

    private void onTurnedIn() {
        if (!active) {
            return;
        }
        emit(new SlayerEvent.QuestTurnedIn(boss, tier));
        clearQuest();
        emit(new SlayerEvent.QuestCleared());
    }

    private void fail(FailReason reason) {
        failReasonCountdown = -1;
        if (!active) {
            return;
        }
        emit(new SlayerEvent.QuestFailed(reason));
        clearQuest();
        emit(new SlayerEvent.QuestCleared());
    }

    // ------------------------------------------------------------ scoreboard

    /**
     * Feed the sidebar here, top line first, colour codes stripped. An empty
     * list means "no sidebar available", which is deliberately not treated as
     * evidence that the quest disappeared.
     */
    public void onScoreboard(List<String> lines) {
        if (!active) {
            if (hasQuestHeader(lines)) {
                beginQuest();
            } else {
                return;
            }
        }

        int header = indexOfQuestHeader(lines);
        if (header < 0) {
            onQuestMissingFromScoreboard(lines);
            return;
        }

        missedPolls = 0;
        readQuestLines(lines, header);
    }

    private void readQuestLines(List<String> lines, int header) {
        boolean sawBoss = false;

        for (int i = header + 1; i < lines.size() && i <= header + 3; i++) {
            String raw = lines.get(i);
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty()) {
                continue;
            }

            SlayerBoss named = SlayerBoss.fromText(line);
            SlayerTier roman = parseRomanTier(line);

            if (named != null && roman.known()) {
                boss = named;
                sawBoss = true;
                // the scoreboard prints the truth, so it outranks an XP guess
                if (tier != roman || tierSource != TierSource.SCOREBOARD) {
                    tier = roman;
                    tierSource = TierSource.SCOREBOARD;
                }
            }

            Matcher progress = SCOREBOARD_PROGRESS.matcher(line);
            if (progress.find()) {
                phase = SlayerPhase.GRINDING;
                long nowXp = parseNumber(progress.group(1));
                long needXp = parseNumber(progress.group(2));
                currentCombatXp = nowXp;
                requiredCombatXp = needXp;
                // the sidebar is polled, so only report real movement
                if (nowXp != reportedXp || needXp != reportedRequiredXp) {
                    reportedXp = nowXp;
                    reportedRequiredXp = needXp;
                    emit(new SlayerEvent.Progress(nowXp, needXp));
                }
                continue;
            }

            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("slay the boss")) {
                phase = SlayerPhase.BOSS_AVAILABLE;
                bossSpawned = true;
            } else if (lower.contains("boss slain")) {
                phase = SlayerPhase.BOSS_SLAIN;
                bossSpawned = true;
                if (!bossSlain) {
                    bossSlain = true;
                    emit(new SlayerEvent.BossSlain(boss, tier));
                }
            }
        }

        if (sawBoss) {
            tryIdentify();
        }
    }

    private void onQuestMissingFromScoreboard(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return; // no sidebar, so no evidence either way
        }
        if (++missedPolls < ABANDON_AFTER_MISSED_POLLS) {
            return;
        }
        // a silent drop is still a fail, otherwise leaving the boss lobby
        // would reset the quest for free
        fail(FailReason.ABANDONED);
    }

    // ---------------------------------------------------------------- plumbing

    private void clearQuest() {
        active = false;
        boss = null;
        tier = SlayerTier.UNKNOWN;
        tierSource = TierSource.NONE;
        phase = SlayerPhase.NONE;
        currentCombatXp = 0;
        requiredCombatXp = 0;
        reportedXp = -1;
        reportedRequiredXp = -1;
        bossSpawned = false;
        bossSlain = false;
        missedPolls = 0;
        identitySignature = "";
    }

    private void tryIdentify() {
        if (boss == null || !tier.known()) {
            return;
        }
        String signature = boss.name() + tier.name() + tierSource.name();
        if (signature.equals(identitySignature)) {
            return;
        }
        identitySignature = signature;
        emit(new SlayerEvent.QuestIdentified(boss, tier, tierSource));
    }

    private boolean hasQuestHeader(List<String> lines) {
        return indexOfQuestHeader(lines) >= 0;
    }

    private int indexOfQuestHeader(List<String> lines) {
        if (lines == null) {
            return -1;
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line != null && QUEST_HEADER.equalsIgnoreCase(line.trim())) {
                return i;
            }
        }
        return -1;
    }

    private SlayerTier parseRomanTier(String line) {
        Matcher matcher = SCOREBOARD_TIER.matcher(line.trim());
        if (!matcher.find()) {
            return SlayerTier.UNKNOWN;
        }
        return switch (matcher.group(1).toUpperCase(Locale.ROOT)) {
            case "I" -> SlayerTier.ONE;
            case "II" -> SlayerTier.TWO;
            case "III" -> SlayerTier.THREE;
            case "IV" -> SlayerTier.FOUR;
            case "V" -> SlayerTier.FIVE;
            default -> SlayerTier.UNKNOWN;
        };
    }

    static long parseNumber(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        String cleaned = text.replace(",", "").trim().toLowerCase(Locale.ROOT);
        long multiplier = 1;
        if (cleaned.endsWith("k")) {
            multiplier = 1000;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        } else if (cleaned.endsWith("m")) {
            multiplier = 1000000;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        try {
            return (long) (Double.parseDouble(cleaned) * multiplier);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void emit(SlayerEvent event) {
        for (Consumer<SlayerEvent> listener : List.copyOf(listeners)) {
            listener.accept(event);
        }
    }
}
