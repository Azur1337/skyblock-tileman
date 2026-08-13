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

- [ ] `ProfileData`: add `optIn` (null = undecided, true/false) and `cuteName` (set from API baseline)
- [ ] On profile detect with `optIn == null`: new profile (API `created_at` recent + ~0 XP; fallback: "Welcome to SkyBlock" title detection, check Fabric title events on 26.2, mixin only if no event exists) -> show client-side `ConfirmScreen` "Enable Tileman for profile X?"; existing profile -> `optIn = false` silently
- [ ] Gate all features on `config.enabled && profile.optIn` (renderer, punishment, XP parser, island handler, HUD, selection mode)
- [ ] DELETE the retroactive award branch in `SkillXpParser.processXpUpdate` (oldXp == 0 awards nothing, only seeds baseline) -> closes the retroactive-policy and baseline-asymmetry TODOs by construction
- [ ] Chat listener for `Done! Your (\w+) profile was deleted!` -> map name to profile id via stored cuteName/API list, delete from save, reset activeProfileId to unknown if it was the active one
- [ ] Decide co-op behavior: RESOLVED - purely client-side, optIn is a local flag per profile id; guests on the same profile run their own clients and are unaffected. No multiplayer surface exists

## Economy bugs

- [x] Efficient Scaling fixed: bonus is now `level * 2` percent (was `level * 100_000` used as percent -> floored every token to 100 XP); no int overflow possible; `EFFICIENT_SCALING_MAX` constant
- [x] Skill Affinity now applies: `onXpGained(skill, amount)` routes through `getEffectiveXpMultiplier(skill)` = affinity x frenzy; shop lore uses the same helper (no longer lies during frenzy)
- [x] Max-level purchase guard for Efficient Scaling (was: paying 120 tokens at level 10 for nothing); `spendTokens` now rejects amounts <= 0
- [x] Wired `MilestoneTracker.onTick()` in TilemanShopTicker (every 20 ticks) -> MARATHON completes -> frenzy upgrades are buyable
- [x] TILES_UNLOCKED milestone uses `getTotalUnlockedBlocks()` (lifetime across islands)
- [x] Deleted dead `XpTokensCategory` config UI + its getters in TilemanConfig (tier table is the economy, sliders were no-ops)

## Slayer (detection partly live, real-run data still pending)

- [ ] Capture real chat samples per `meow.md` (accept, requirement, boss spawn, kill, fail, despawn, island switch)
- [ ] Fix `SlayerType.fromDisplayName`: "Endermen" (plural) does not contain "enderman", Voidgloom detection fails
- [ ] Update `getTierFromXp` thresholds: add T5 for Tarantula, Sven, Voidgloom (stale table)
- [ ] Guard `onQuestComplete` against `currentType == null` (live NPE crash path)
- [ ] Reset quest state on island change and disconnect (hook does not exist)
- [ ] Consider API `player_data.slayer_bosses.<boss>.kills` for kill milestones (like dungeons); chat only needed for flawless streaks
- [ ] Route dungeon logging to `DebugCategory.DUNGEON` (exists but unused, everything dumps to ALL)

## Crashes and UX

- [ ] Punishment: grace period on respawn, teleport, dimension change (server-driven warps instantly book a rule break)
- [ ] Unify the two `findStandingBlock` implementations (punishment vs BlockValidation have different tolerances -> contradictory UI signals on stairs/slabs)
- [ ] Frenzy timer: store expiry timestamp instead of accumulating deltas (lag spikes and logout currently freeze it)
- [ ] `ProfileData.getUnlockedBlocks(island)`: no `computeIfAbsent` on read paths (inflates ISLANDS_EXPLORED, persists junk `"unknown"` entries)
- [ ] Corrupt save file: rename to backup instead of silently overwriting on next save
- [ ] First-block mode: gate on `isEnabled()`, handle non-standable fallback block case
- [ ] Selection mode: debounce hole when attack key is already held when B is pressed
- [ ] Verify token accounting against a long real grinding session (max-level overflow, level-up rollovers)

## UI networking and design system

- [x] UI design system spec written: `UI-DESIGN.md` (repo root; docs/ is gitignored)
- [x] Built the system core in `client/tileman/ui/`: `Theme` (color roles + standard parts), `Fmt` (locale-stable numbers), `TilemanItem` (anatomy builder), `TilemanChestScreen` (frame geometry, footer builder, slot->handler routing, 10-tick refresh, sound vocabulary)
- [x] Built generic tiny `ConfirmScreen` (chest-style yes/no, ESC/barrier = No) - ready for the profile opt-in gating
- [x] Migrated Shop/Milestone/Dungeon screens onto the base (UI-DESIGN.md section 10 done: frame panes via buildFrame, slot->handler routing replaces name-string dispatch, footer geometry with CLOSE always present, lore via TilemanItem anatomy, deny = silent + TilemanChat.warn, refresh() keeps balances/cooldowns live)
- [ ] HUD: XP bar toward next token replacing raw xp/xpNeeded text (UI-DESIGN.md section 7)
- [x] Token icon resolved: HUD uses the existing custom coin texture `textures/hud/tokens.png`; chest screens express tokens as text (items can't carry custom textures)
- [x] Chest screens send no packets: VERIFIED for 26.2, all click paths funnel through `slotClicked` and the packet send (`MultiPlayerGameMode.handleContainerInput`) is inside the base impl, which our overrides never call. The earlier container-id-0 concern was based on the old `handleMouseClick` API and does not apply
- [ ] Design system: base TilemanChestScreen (open/init/border/item factories), shared `(row, col) -> slot` helper, consistent colors, replace magic slot numbers -> superseded by UI-DESIGN.md plan above
- [ ] Replace hover-name string dispatch with tagged ItemStacks (data component marker) in shop/milestone/dungeon screens
- [ ] MoulConfig design pass (categories, ordering, remove dead controls)
- [ ] Fix "Unlock required milestone first!" wording (repeated 7x)

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
- [ ] Remove decorative unicode: `✦ ▶ ✓ ✗ ★ ☆ ▌`
- [ ] Delete dead code: `XpReconciler` shell (or implement, see below), `fetchTotalSkillXp`, `checkAllMilestones`, `modifyTooltip`, `getFloorLockStatus`, `clearPendingLeave`, `extractBackground` override, `SkyblockTileman.id()`, unused imports/locals, `MilestoneData` playtime fields, `bestFlawlessStreak` (wire or drop), rank fields/methods in `DungeonData` now that the display is gone
- [ ] Dedupe: `romanToInt` (2x) + numeral switch (3rd copy in `ShopRequirements`), shop prices vs their accessors, frenzy formulas (3 sites), reward tiering (2 sites), `getTargetedBlock`, `HORIZONTAL_DIRECTIONS`, menu title constants
- [ ] `TilemanShopTicker`: rename/split (it ticks frenzy + dungeon chat + API poll)
- [ ] `new Random()` per unlock -> `level.random`
- [x] Update AGENTS.md once decisions landed (API key policy, mixin removal)

## XpReconciler decision

- [ ] Either implement or delete. Intended: periodically fetch API skill XP totals, compare to local tracked totals, correct drift (missed actionbar drops, restarts). `fetchTotalSkillXp` already exists for this but is never called
