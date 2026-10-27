package com.azur.skyblocktileman.slayer;

/**
 * Facts about a slayer quest, derived from chat and scoreboard text.
 *
 * Events are facts, never policy: BossSlain does not mean "award tokens". Each
 * event fires once per transition, not once per poll, so consumers do not need
 * to debounce.
 */
public sealed interface SlayerEvent {

    /** A quest became active. Boss and tier may not be known yet. */
    record QuestStarted() implements SlayerEvent {}

    /**
     * The boss and tier are now both known. Fires again if the tier is
     * upgraded from an XP guess to a scoreboard reading.
     */
    record QuestIdentified(
        SlayerBoss boss,
        SlayerTier tier,
        TierSource source
    )
        implements SlayerEvent {}

    /** Grind progress, in combat XP. */
    record Progress(long currentCombatXp, long requiredCombatXp)
        implements SlayerEvent {}

    /** A miniboss spawned. Only possible at tier III and above. */
    record MinibossSpawned(String mobName) implements SlayerEvent {}

    /** The real boss spawned. Tier is UNKNOWN when only a renamed form was seen. */
    record BossSpawned(SlayerBoss boss, SlayerTier tier) implements SlayerEvent {}

    /** The boss is dead. This is the kill moment, not QuestTurnedIn. */
    record BossSlain(SlayerBoss boss, SlayerTier tier) implements SlayerEvent {}

    /** The completed quest was handed to Maddox. */
    record QuestTurnedIn(SlayerBoss boss, SlayerTier tier) implements SlayerEvent {}

    /** The quest failed or was abandoned. */
    record QuestFailed(FailReason reason) implements SlayerEvent {}

    /** No quest is active any more, for any reason. */
    record QuestCleared() implements SlayerEvent {}
}
