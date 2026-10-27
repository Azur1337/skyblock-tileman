# TODO

Track of concrete work items from the 2026-10 code review. Check off when done.

## API key and Hypixel API

- [x] Remove hardcoded API key from `TilemanLoginHandler` (old key still in git history, revoke it at hypixel.net/account/keys)
- [x] Key resolution now: system property, env var, `.env` in game dir, bundled `tileman.env` jar resource (repo `.env` gitignored, `build.gradle` bundles it; put the App key in repo-root `.env`, see `.env.example`)
- [x] Tested what the App key counts against: ONE shared 300 req / 5min counter for ALL live endpoints, SkyBlock included. Even 429 responses cost 1. `maxProfileFields` costs the same 1. 404/invalid-key requests are free. `/v2/status` has an extra per-player throttle. `/v2/key/info` and `/v2/users/uuid` are retired (404)
- [x] Recalculated frequency for 100 users: merged join (1 req) + 2min poll (30/h) = 31/h per user = ~3100/h, fits the 3600/h budget. 2min poll stays
- [x] Add 429 / backoff handling to `HypixelApiClient` (429 burns 1 global request; now skips all requests until `ratelimit-reset` + 5s elapsed)
- [x] Merged `fetchBaselineSkillXp`, `fetchMaxedCollections`, `fetchDungeonCompletions` into ONE `fetchProfiles` call parsed three ways (also deleted dead `fetchTotalSkillXp`)
- [x] Per-client bug gate: `getProfiles` enforces a 30s minimum between real network hits (serves stale cache otherwise); worst-case buggy client = 120 req/h, and the 429 backoff self-limits the whole fleet at ~300/5min. Cannot protect against someone extracting the key from the jar (only a server proxy could; not worth it)
- [x] Tested `maxProfileFields=members` on a real 3.4MB profile: parameter is SILENTLY IGNORED (byte-identical responses, 7 variants probed). Not added, nothing to save
- [x] Hardened `parseProfiles` against `profiles:null` (Hypixel answers unknown accounts with 200/success:true/profiles:null; the old getAsJsonArray cast threw IllegalStateException)
- [x] In-flight guard + payload cache: `HypixelApiClient.getProfiles(uuid, key, maxAgeMs)` dedups concurrent fetches and caches the last parsed payload; `peekProfiles()` lets future features (XpReconciler, slayer milestones) read poll data without spending requests
- [x] Retroactive-token policy RESOLVED by design: new-profile-only opt-in makes retroactive awards impossible; the award branch gets deleted (see New-profile gating section)
- [x] Change auto-sync interval: kept at 2min since the merged fetch fits the budget at 100 users; manual sync 60s cooldown stays

## Dungeon (decision: API is the single source of truth)

- [x] Remove S/S+ rank display from the progress screen (no API source; floor colors carry the info)
- [x] Fix MM floor click routing: slotClicked now dispatches by slot id, not name strings
- [x] Manual sync button in DungeonProgressScreen: 60s cooldown, also restarts the 2min auto-sync interval (timer moved into DungeonTracker)
- [x] Remove local completion detection: deleted `DungeonTracker.onDungeonComplete` and the
      `MilestoneTracker.onDungeonComplete` stub, `DungeonData.recordCompletion` and the S/S+ rank
      fields, and the write-only `apiBaselineCompletions` map. Tokens now come only from the API
      completion diff against `rewardedCompletions`
- [x] Clear/expire `pendingLeaveFloor` on party leave, disconnect, world exit -> moot: the
      field no longer exists anywhere in the tree, removed with local detection
- [x] Fix reward math in the dungeon progress message -> moot: that message lived in
      `onDungeonComplete` and was deleted with it; `DungeonProgressScreen` owns the 10/50 tiering
- [ ] Cross-check API `selected` profile id against local `activeProfileId` before writing tokens (wrong-profile attribution)

## New-profile gating (replaces retroactive-token policy)

Decision: Tileman is opt-in at profile creation only; no retroactive rewards ever.

- [x] `ProfileData`: add the opt-in decision and the profile name. Landed as `tilemanMode`
      (Boolean, null = undecided) and `cuteName` read off `HypixelApiClient.ProfilesResult`
      rather than persisted, since the prompt only needs it while the payload is live
- [x] On profile detect with `tilemanMode == null`: `TilemanLoginHandler.maybeHandleOptIn` ->
      `handleOptInDecision` shows the client-side `ConfirmScreen`; old profiles decline silently.
      Fired from both the API result and locraw, so whichever arrives second prompts
- [x] Gate all features on `config.enabled && profile.tilemanMode`: `isTilemanActive()` is the
      single gate (master toggle, Hypixel opt-in, locraw Skyblock; off-Hypixel skips the last two)
- [x] Kill the retroactive award in `SkillXpParser.processXpUpdate`. Fixed by routing the
      `oldXp == 0` branch through `processDirectXpGain(skill, visibleGain)` rather than awarding
      nothing: it was crediting `totalXp` (every token the whole skill was ever worth) and
      returning before the `MAX_REASONABLE_XP_GAIN` guard, so a missing API baseline became an
      unbounded windfall. Crediting only the visible action-bar drop keeps a new profile's first
      gain intact, bounds the award, and matches what the max-level and percentage paths already
      did for the same case, which also closes the baseline-asymmetry note
- [ ] Chat listener for `Done! Your (\w+) profile was deleted!` -> map name to profile id via
      stored cuteName/API list, delete from save, reset activeProfileId to unknown if it was the
      active one. No such listener exists yet; orphaned profile blobs accumulate in the save
- [x] Decide co-op behavior: RESOLVED - purely client-side, tilemanMode is a local flag per
      profile id; guests on the same profile run their own clients and are unaffected

## Economy bugs

- [x] Efficient Scaling fixed: bonus is now `level * 2` percent (was `level * 100_000` used as percent -> floored every token to 100 XP); no int overflow possible; `EFFICIENT_SCALING_MAX` constant
- [x] Skill Affinity now applies: `onXpGained(skill, amount)` routes through `getEffectiveXpMultiplier(skill)` = affinity x frenzy; shop lore uses the same helper (no longer lies during frenzy)
- [x] Max-level purchase guard for Efficient Scaling (was: paying 120 tokens at level 10 for nothing); `spendTokens` now rejects amounts <= 0
- [x] Wired `MilestoneTracker.onTick()` in TilemanShopTicker (every 20 ticks) -> MARATHON completes -> frenzy upgrades are buyable
- [x] TILES_UNLOCKED milestone uses `getTotalUnlockedBlocks()` (lifetime across islands)
- [x] Deleted dead `XpTokensCategory` config UI + its getters in TilemanConfig (tier table is the economy, sliders were no-ops)

## Slayer (detection rebuilt as a standalone package, verified against real captures)

- [x] Capture real chat samples per `meow.md`: done via the Derailious Discord call. Revenant
      T1-T5 and Voidgloom T1/T2 covered, including accept, requirement, boss spawn, miniboss
      spawn, scoreboard in all three phases, kill, death fail and timeout fail
- [x] Fix boss detection never firing: the code matched the contiguous phrase "Boss Spawned",
      which Hypixel never sends. Real form is `SLAYER BOSS! The <name> <roman> spawned!`, and
      minibosses use `SLAYER MINIBOSS!`. Discriminated by prefix word, not mob name, because
      T5 renames the boss (Atoned Horror, Primordial Broodfather) and its minibosses share the stem
- [x] Tier table was wrong for all six bosses and locked in the error. `getTierFromXp` used a
      `>=` chain that a 2026-09-30 rebalance invalidated, and `updateQuestInfo` refused to
      overwrite, so a Revenant T4 stayed T3 forever and its milestones were unreachable.
      Replaced with nearest-match plus a rejection band, and the scoreboard now always outranks it
- [x] Scoreboard is now the authoritative tier and phase source: header, `<name> <roman>`, then
      `(a/b) Combat XP` / `Slay the boss!` / `Boss slain!`. Poll is 10 ticks, events fire on
      transition only
- [x] Silent lobby-swap fail now detected (3 missed polls => `ABANDONED`), with an empty or
      hidden sidebar explicitly not treated as evidence, so hidden-scoreboard players are not
      falsely failed. Closes the hole that made flawless farmable by warping out
- [x] Fail reason parsed from the line _after_ `SLAYER QUEST FAILED!`, matching the `You died!`
      prefix since the suffix varies per boss ("Death is not permitted!" vs "What a noob!")
- [x] `SLAYER QUEST COMPLETE!` correctly treated as Maddox turn-in, not the kill. Kill is
      `BossSlain`; cleanliness is still judged at turn-in so the walk back counts
- [x] Reconnect recovery: chat is not replayed but the sidebar is, so quest state rebuilds from
      the scoreboard alone
- [x] Dead-code and coupling cleanup: `SlayerType`, `SlayerChatListener` and `ScoreboardReader`
      deleted; `SlayerData.getStats` no longer creates entries on read (same bug class as the
      island fix, it was inflating milestone progress)
- [x] `currentType == null` NPE on turn-in: gone, the tracker refuses to record an unidentified quest
- [ ] Phase-tied challenges built on the new state machine. Agreed shortlist: _Flawless Turn-in_
      (kill then reach Maddox with no tile violation) and _No Retreat_ (no server change, death
      or warp between `BossSpawned` and `BossSlain`). Deferred for lack of data: Inferno
      attunement/fire-pillar phases, timer-based challenges, no-damage challenges
- [ ] Unconfirmed behaviour worth a field look: whether a party member's kill credits everyone,
      and whether abandoning a quest at Maddox emits anything
- [ ] Consider API `player_data.slayer_bosses.<boss>.kills` for kill milestones (like dungeons); chat only needed for flawless streaks
- [ ] Route dungeon logging to `DebugCategory.DUNGEON` (exists but unused, everything dumps to ALL)

## Crashes and UX

- [ ] Punishment: grace period on respawn, teleport, dimension change (server-driven warps instantly book a rule break)
- [ ] Unify the two `findStandingBlock` implementations (punishment vs BlockValidation have different tolerances -> contradictory UI signals on stairs/slabs)
- [ ] Frenzy timer: store expiry timestamp instead of accumulating deltas (lag spikes and logout currently freeze it)
- [x] `ProfileData.getUnlockedBlocks(island)`: reads no longer create entries. Split into a
      non-mutating `getUnlockedBlocks` and `getOrCreateUnlockedBlocks` for the write path, with
      `TilemanState.markIslandVisited` as the one place an island is registered. Previously every
      read (including `rebuildUnlockIndex` with island "unknown") inflated ISLANDS_EXPLORED and
      persisted a junk blob
- [x] Corrupt save file: `backupUnreadableSave` moves it to `tileman_data.unreadable-<ms>.json`
      instead of letting the next `save()` destroy the only copy
- [ ] First-block mode: gate on `isEnabled()`, handle non-standable fallback block case
- [x] Selection mode: a rising edge on the unlock key sets `clickHandled`, so entering unlock
      mode with attack already held no longer spends that hold as an unlock
- [ ] Verify token accounting against a long real grinding session (max-level overflow, level-up rollovers)

## UI networking and design system

- [x] UI design system spec written: `UI-DESIGN.md` (repo root; docs/ is gitignored)
- [x] Built the system core in `client/tileman/ui/`: `Theme` (color roles + standard parts), `Fmt` (locale-stable numbers), `TilemanItem` (anatomy builder), `TilemanChestScreen` (frame geometry, footer builder, slot->handler routing, 10-tick refresh, sound vocabulary)
- [x] Built generic tiny `ConfirmScreen` (chest-style yes/no, ESC/barrier = No) - ready for the profile opt-in gating
- [x] Migrated Shop/Milestone/Dungeon screens onto the base (UI-DESIGN.md section 10 done: frame panes via buildFrame, slot->handler routing replaces name-string dispatch, footer geometry with CLOSE always present, lore via TilemanItem anatomy, deny = silent + TilemanChat.warn, refresh() keeps balances/cooldowns live)
- [ ] HUD: XP bar toward next token replacing raw xp/xpNeeded text (UI-DESIGN.md section 7)
- [x] Token icon resolved: HUD uses the existing custom coin texture `textures/hud/tokens.png`; chest screens express tokens as text (items can't carry custom textures)
- [x] Chest screens send no packets: VERIFIED for 26.2, all click paths funnel through `slotClicked` and the packet send (`MultiPlayerGameMode.handleContainerInput`) is inside the base impl, which our overrides never call. The earlier container-id-0 concern was based on the old `handleMouseClick` API and does not apply
- [x] Design system base: `TilemanChestScreen` provides `slot(row, col)`, `buildFrame()`,
      `onClick(slotId, Runnable)` and the footer builder; screens use `Theme` roles (5-15 calls
      each across shop/dungeon/milestone/hub) and there are zero `setItem(<literal>)` slot
      numbers left in the tree
- [ ] Replace hover-name string dispatch with tagged ItemStacks (data component marker) in shop/milestone/dungeon screens
- [ ] MoulConfig design pass: Keybinds category and the dead `XpTokensCategory` sliders are done;
      remaining category ordering and any other dead controls are not
- [x] "Unlock required milestone first!" wording: the string no longer appears anywhere in the
      tree, resolved by the chest UI migration (denials are now silent + `TilemanChat.warn` per
      UI-DESIGN.md section 10)

## Type safety

- [ ] `(SimpleContainer) menu.getContainer()` casts: add instanceof guard or restructure screens
- [x] Band model implemented: per-skill level bands (level/10) with `TOKEN_BAND_COSTS` 100/800/3100/4800/6200/8500 XP per token (multiples of 100, sqrt-scaled, normalized to ~80.5k tokens at max); per-skill pools in `ProfileData.tokenPools`; XP past max level earns nothing; old tier table + dead tier-display methods deleted
- [ ] `getUnlockedBlocks()` returns live mutable set -> return unmodifiable view
- [ ] Remove no-op `@Expose` annotations and dead legacy `lifetimeTokensEarned` from save shape; move migration out of the getter
- [ ] `Boolean`/`null` sentinels (`getFloorLockStatus`, `getRequirementText`) -> explicit result types
- [ ] Parse failures must be distinguishable from valid values (`parseXpAmount` 0 = both error and tier 1)
- [ ] `toUpperCase()` -> `toUpperCase(Locale.ROOT)` in commands
- [ ] HUD parallel arrays (`ICONS`/`LABELS`/`TOOLTIPS`) -> record list

## Performance

- [x] Index unlocked blocks by column (`packed x|z -> ys`), maintained on unlock; kills the O(N) `isUnlockedColumn` scan -> `UnlockIndex`
- [x] Cache compass nearest-tile; invalidate on unlock / player block change (currently O(N) per frame) -> generation + player-block cache in `TilemanCompassRenderer`
- [x] `TilemanRenderer`: stop iterating all unlocked blocks per frame -> radius query walks only the cells overlapping the highlight box
- [ ] `onXpGained` / `addTokens`: closed-form tier math instead of per-token loops (first-detection catch-up can mean tens of thousands of iterations)
- [ ] Milestone checks: one call with the token delta, not N calls
- [ ] Debounced save (dirty flag, max once per second + on disconnect); remove double-saves in purchase/unlock paths
- [ ] `DungeonFloor.getNormalFloors()`/`getMasterModeFloors()`: `static final` arrays
- [ ] Gate `TilemanLog.debug` console output by category (currently every message is formatted + logged at INFO 24/7)

- [x] Island-warp API leak fixed: warps re-fire ClientPlayConnectionEvents.JOIN (server transfer) which re-fetched the profiles payload each time. JOIN now skips when the active profile is unchanged (`lastFetchedProfileId`); real profile switches go through `TilemanProfileDetector` -> `fetchBaselineIfNeeded()` -> `invalidateCache()` so the previous profile's cached payload can never be re-applied
- [x] Pane filler tooltip hidden via `TooltipDisplay(true, LinkedHashSet)` (26.2 has no `TooltipVisibility`; `ImmutableSet` doesn't implement `SequencedSet`)
- [x] Dungeon unlock confirm/cancel moved to row 1 cols 3/5 (flanking the skull with one gap, two from the walls)

## Style and hygiene

- [x] Decide tabs vs spaces: resolved on 4-space, the whole tree is clean of tab-indented code and AGENTS.md now says so. `.editorconfig` still not added
- [x] Delete dead code: `TilemanActionBarMixin` removed (class + mixins.json entry); the GAME event listener covers both the Hypixel overlay path and the modern actionbar packet
- [x] Strip multi-paragraph javadocs to one line max or nothing: swept the whole tree; remaining comments are single-line and carry invariants, engine quirks or cost rationale only
- [ ] Remove decorative unicode: only one glyph is left, the `✦` in the "FLAWLESS SLAYER!"
      chat banner (`SlayerTracker`); `▶ ✓ ✗ ★ ☆ ▌` are already gone
- [x] Delete dead code: swept with a reachability analysis over the whole tree. Removed 31
      unreferenced members and 7 unused imports (~260 lines): `checkAllMilestones`,
      `modifyTooltip`, `getFloorLockStatus`, `getNextNormalFloorToUnlock`,
      `getNextMasterModeFloorToUnlock`, `isFloorUnlocked`, `toBlockPos`, `isNonStandableBlock`,
      `setTokensSpent`, `getAllSkillXp`, `addLifetimeTokensEarned`, `setXpFrenzyRemainingMs`,
      `getTotalSpent`, `getAllSkillAffinityLevels`, `setHudPosition`, `getPlayerUuid`,
      `getHudHeight` + `lastHudHeight`, `addProgress`, `resetSessionData`, the `MilestoneData`
      playtime field and its accessors, `canPurchase`, `canPurchaseConsumable`, `isTainted`,
      `getCurrentType`, `getCurrentTier`, `getShortName` + the enum's short-name column,
      `addInfo`, `addNext`, `playAchievement`, and the unused `SkyblockTileman.LOGGER`.
      `fetchTotalSkillXp`, `clearPendingLeave` and the `extractBackground` override were already
      gone; `bestFlawlessStreak` is live (it feeds the SLAYER_STREAK milestone) so it stays
- [x] `SkyblockTileman.id()` was dead because 9 call sites hardcoded the mod id. Kept it and
      pointed `TilemanHudOverlay`, `TilemanPunishmentHandler` and `TilemanSelectionMode` at it
      instead of deleting, which removes the duplication rather than the helper
- [ ] Dedupe: `romanToInt` (2x) + numeral switch (3rd copy in `ShopRequirements`), shop prices vs their accessors, frenzy formulas (3 sites), reward tiering (2 sites), `getTargetedBlock`, `HORIZONTAL_DIRECTIONS`, menu title constants
- [ ] `TilemanShopTicker`: rename/split (it ticks frenzy + dungeon chat + API poll)
- [ ] `new Random()` per unlock -> `level.random`
- [x] Update AGENTS.md once decisions landed (API key policy, mixin removal)

## XpReconciler decision

- [ ] Either implement or delete. Intended: periodically fetch API skill XP totals, compare to local tracked totals, correct drift (missed actionbar drops, restarts). `fetchTotalSkillXp` already exists for this but is never called
