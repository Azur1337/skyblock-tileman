# Skyblock Tileman

A Fabric client mod for Hypixel Skyblock that turns the map into a "Tileman" challenge: you start with a single unlocked block and can only stand on tiles you've unlocked. New tiles cost tokens, and tokens are earned by grinding Skill XP. Step off the unlocked path and the mod lets you know about it.

Built for [Derailious](https://www.youtube.com/@Derailious)'s Tileman challenge series.

## How it works

- **Tokens** are earned automatically as you gain Skill XP, tracked live from the action bar. The cost of the next token slowly rises the more total XP you've earned.
- **Unlocking blocks** is done in Unlock Mode (hold the keybind, default `B`): look at a block next to one you've already unlocked, it outlines yellow, left click to spend a token and claim it.
- **Unlocked blocks** are highlighted with a green overlay so you always know where you're allowed to walk.
- **Islands** are detected automatically, and your first block is unlocked for free the moment you land on a new one.
- **Breaking the rules** (standing on a block you haven't unlocked) triggers a big warning, a loud sound, and adds to your on-screen Rule Breaks counter.
- Your progress (tokens, XP, unlocked blocks, rule breaks) is saved per Skyblock profile, so switching or coop-ing doesn't mix up your progress.

## Requirements

- Minecraft 26.2
- [Fabric Loader](https://fabricmc.net/use/) 0.19.3+
- [Fabric API](https://modrinth.com/mod/fabric-api) 0.155.2+26.2
- Java 25+

MoulConfig and its Kotlin runtime are bundled into the mod jar directly, you don't need to install them separately.

## Installing

Grab the `-shadow.jar` from [Releases](../../releases) or the latest [Actions build](../../actions) and drop it into your `mods` folder alongside Fabric API. Don't use the `-slim.jar`, it's missing the bundled dependencies and will fail to load.

## Setup

To get an accurate starting XP total when you log in, Tileman fetches your Skill XP from the Hypixel API. Get a free key from the [Hypixel Developer Dashboard](https://developer.hypixel.net/) and pass it in either as a JVM system property or an environment variable before launching:

```
-Dtileman.hypixelApiKey=your-key-here
```

```
TILEMAN_HYPIXEL_API_KEY=your-key-here
```

If you skip this, the mod still works fine, it just starts tracking from 0 XP instead of your real total.

All other data (unlocked blocks, tokens, rule breaks, settings) is saved in `config/tileman/`.

## Usage

- **Unlock Mode**: hold `B` (rebindable in Controls) to enter unlock mode, look at a block adjacent to an unlocked one, left click to unlock it.
- `/tileman config`: opens the settings menu.
- `/tileman enable [on|off]`: toggles the whole mod.
- `/tileman overlay [on|off]`: toggles the green unlocked-block overlay.
- `/tileman debug [on|off]`: mirrors internal debug logging into chat.

## Building from source

```
./gradlew build
```

The built jars land in `build/libs/`.

## Credits

- [MoulConfig](https://github.com/NotEnoughUpdates/MoulConfig) for the in-game settings menu.
- [Hypixel API](https://api.hypixel.net/) for the Skill XP baseline.

## License

CC0-1.0, see [LICENSE](LICENSE).
