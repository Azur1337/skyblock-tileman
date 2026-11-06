# Skyblock Tileman

Fabric client mod for Hypixel Skyblock that enforces a Tileman challenge: you may only stand on blocks you have unlocked. Unlocks cost tokens, tokens are earned from Skill XP. Built for [Derailious](https://www.youtube.com/@Derailious)'s Tileman series, and generally usable by anyone playing the challenge.

Everything is client side. The mod never sends gameplay packets; the only things it sends to the server are a `/locraw` for island detection, a `/warp hub` when the health bar collapses, and whatever you trigger yourself.

## Gameplay

- Skill XP from the five main skills (Combat, Mining, Foraging, Farming, Fishing) mints unlock tokens. The XP cost per token rises as your skill levels go up.
- Unlock mode (hold `B`) outlines the aimed tile; left click spends one token. Tiles must be adjacent to one you already own.
- Every island grants one free starting block the first time you land on it.
- Unlocked tiles are highlighted, and a compass points at the nearest one while you are standing where you should not be.
- Standing on a locked tile counts a rule break: warning banner, thunder, and a health bar that drains until you are back on your tiles. An empty bar warps you to the hub.
- XP gained while off your tiles is tracked but never pays tokens, including retroactively through the API.
- Dungeon instances are exempt: walk freely, no enforcement, XP still counts.
- Shop (`/tileman shop`) sells upgrades and consumables for saved-up tokens. Milestones (`/tileman milestones`) pay tokens for progression, skills, slayer and clean-run challenges. Dungeon floors (`/tileman dungeons`) cost tokens to unlock and pay tokens per clear.
- Progress is saved per Skyblock profile in `config/tileman/tileman_data.json`.

## Setup

None. Install the mod, play Skyblock on a profile created after installing it, and answer the opt-in prompt. Profiles that existed before installation are declined automatically; `/tileman optin on|off` overrides where the challenge rules allow.

The mod ships with a Hypixel app API key for the Skill XP baseline and dungeon completion sync. If the shared key is rate limited, those two features pause quietly; token earning from the action bar keeps working.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3+
- Fabric API 0.155.2+26.2
- Java 25+

Optional: [Hypixel Mod API](https://modrinth.com/mod/hypixel-mod-api) for instant, push-based island detection instead of the `/locraw` fallback.

## Installing

Grab `skyblocktileman-<version>-shadow.jar` from [Releases](../../releases) and drop it in your `mods` folder alongside Fabric API. Also published on Modrinth. Use the shadow jar; the slim one has no bundled dependencies and will crash on load. Keep only one Tileman jar in the folder.

## Commands

| Command                                     | What it does                      |
| ------------------------------------------- | --------------------------------- |
| `/tileman`                                  | Opens the hub menu                |
| `/tileman shop` / `milestones` / `dungeons` | Jump straight to a menu           |
| `/tileman unlock <floor>`                   | Unlock a dungeon floor, e.g. `M7` |
| `/tileman config`                           | Settings                          |
| `/tileman enable [on                        | off]`                             | Master toggle                        |
| `/tileman optin [on                         | off]`                             | Tileman mode for the current profile |
| `/tileman overlay [on                       | off]`                             | Unlocked tile highlight              |
| `/tileman debug [on                         | off]`                             | Mirror debug logs into chat          |

## Keybinds

- Unlock mode: `B` (hold)
- Open hub: `K`
- Move HUD: unbound by default

Rebindable in vanilla Controls or the config screen; the two stay in sync.

## Settings

`/tileman config`. Worth knowing: turning off tile rule enforcement also disables the Flawless milestone family, since nothing is being tracked.

## Building from source

```
cp .env.example .env   # add a Hypixel app key; the build fails without one
./gradlew build
```

The key is bundled into the jar as a resource, which is why the build requires it. Output lands in `build/libs/`.

## Credits

- [MoulConfig](https://github.com/NotEnoughUpdates/MoulConfig) for the settings menu (bundled)
- [Hypixel Public API](https://api.hypixel.net/) for Skill XP baselines and dungeon completions
- [Hypixel Mod API](https://github.com/HypixelDev/ModAPI) for optional server location pushes

## License

CC0-1.0, see [LICENSE](LICENSE).
