# FlowChat

A powerful chat rules engine for Minecraft. Filter, replace, redirect, and auto-respond to chat messages with regex, toast notifications, sounds, and tag selectors.

Originally inspired by [ChatFlow](https://github.com/Vazkii/ChatFlow) — now a full multiplatform mod supporting **29 Minecraft versions** from 1.7.10 to 1.21.11.

## Platforms

| Loader | Versions |
|--------|----------|
| Fabric | 1.14.4 – 1.21.11 |
| Forge | 1.7.10 – 1.21.11 |
| NeoForge | 1.20.4+ |
| Spigot | 1.7.10 – 1.21.11 |
| BungeeCord | 1.7.10+ |
| Velocity | 1.14.4+ |

> **Current version:** 2.1.2

## Features

- **Regex matching** on incoming and outgoing chat
- **Toast notifications** — advancement, task, challenge, or goal popups
- **Sound effects** — play any Minecraft sound on match
- **Auto-responses** — send messages or commands on match
- **Tag selectors** — dynamic text like `{player}`, `{time}`, `{item_name}`, `{biome}`, and [300+ more](docs/TAG_SELECTORS.md)
- **AntiAFK** — configurable idle prevention
- **Color-aware matching** — strip or preserve formatting codes
- **JSON message matching** — match against raw JSON chat components
- **Field aliases** — `re` → `regex`, `msg` → `message`, `sub` → `substitution`, etc.

## Installation

FlowChat requires the appropriate mod loader for your platform. For Fabric, install the [Fabric API](https://github.com/FabricMC/fabric#using-fabric-api-to-play-with-mods).

Drop the jar into your `mods/` (or `plugins/`) folder.

### Configuration

Config file: `.minecraft/config/flowchat.json`

A default config with 7 starter rules is included on first run. You can also grab the full [example rules](example_rules.json) for more advanced patterns.

Edit the config with any text editor, then click "Confirm" in the mod config screen to reload. If the config has errors, the confirm button won't work — check your JSON syntax.

### Documentation

- [Tag Selectors Reference](docs/TAG_SELECTORS.md) — all available `{tags}` with examples
- [Example Rules](example_rules.json) — real-world rule patterns

## Screenshots

Leaving Walzie's House (WorldGuard travel toasts) — [Rule](example_rules.json)
![Left Walzie's House Toast](https://cdn.discordapp.com/attachments/769751221955198997/780700921746817044/unknown.png)

Message prefixing (`&c&l` added to messages) — [Rule](example_rules.json)
![Colour code text](https://cdn.discordapp.com/attachments/769751221955198997/780701590314156032/unknown.png)

AdminShop selling (toasts instead of chat) — [Rule](example_rules.json)
![Adminshop Toasts](https://cdn.discordapp.com/attachments/769751221955198997/780703852298764298/unknown.png)

## Building

```bash
./gradlew build
```

Tests:
```bash
./gradlew :common:test
```

## Discord

Questions, screenshots, or rule-sharing → [join the Discord](https://discord.com/invite/BpVhWNv8hG)

## Downloads

- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/flowchat) (Project ID: 422239)
