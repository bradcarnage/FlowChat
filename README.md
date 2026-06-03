# FlowChat

[![GitHub Release](https://img.shields.io/github/v/release/bradcarnage/FlowChat?label=release&color=brightgreen)](https://github.com/bradcarnage/FlowChat/releases/latest)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.7.10--1.21.11-blue)](https://github.com/bradcarnage/FlowChat/releases/latest)
[![License](https://img.shields.io/github/license/bradcarnage/FlowChat)](LICENSE)
[![Modrinth](https://img.shields.io/modrinth/dt/flowchat?logo=modrinth&label=Modrinth)](https://modrinth.com/mod/flowchat)
[![CurseForge](https://img.shields.io/curseforge/dt/422239?logo=curseforge&label=CurseForge)](https://www.curseforge.com/minecraft/mc-mods/flowchat)

Regex-powered chat processor for Minecraft. Replace, filter, toast, auto-respond, and play sounds on chat messages -- works on **clients and servers** across every major platform. Current version: **2.1.2**.

Originally inspired by [ChatFlow](https://github.com/Vazkii/ChatFlow).

## Platforms

| Platform | Type | Versions |
|----------|------|----------|
| **Fabric** | Client mod | 1.14.4 - 1.21.11 |
| **Forge** | Client mod | 1.7.10 - 1.21.11 |
| **NeoForge** | Client mod | 1.20.4 - 1.21.11 |
| **Spigot/Paper** | Server plugin | 1.7.10 - 1.21.11 |
| **BungeeCord** | Proxy plugin | Latest |
| **Velocity** | Proxy plugin | Latest |

## Installation

### Client Mods (Fabric / Forge / NeoForge)

1. Download the JAR for your Minecraft version and mod loader from [Releases](https://github.com/bradcarnage/FlowChat/releases/latest)
2. Drop the JAR into your `mods/` folder
3. Launch the game -- FlowChat creates a default config at `.minecraft/config/flowchat.json`

### Server Plugins (Spigot / Paper / BungeeCord / Velocity)

1. Download the server JAR from [Releases](https://github.com/bradcarnage/FlowChat/releases/latest):
   - **Spigot/Paper**: `FlowChat-Server-Spigot-2.1.2.jar`
   - **BungeeCord**: `FlowChat-Server-Bungee-2.1.2.jar`
   - **Velocity**: `FlowChat-Server-Velocity-2.1.2.jar`
   - **Unified** (all three): `FlowChat-Server-Unified-2.1.2.jar`
2. Drop the JAR into your `plugins/` folder
3. Restart the server -- config is created at `plugins/FlowChat/flowchat.json`
4. **1.14.4+ servers**: Install [PacketEvents](https://www.spigotmc.org/resources/packetevents-api.80279/) for packet-level chat interception (optional, falls back to Bukkit events)

### Reload

Edit `flowchat.json` and run `/flowchat reload` in-game. No restart needed.

## What's New in v2.1.2

- **Tag selectors** -- 389 tags like `{player}`, `{time}`, `{biome}`, `{item_name}` expanded in replacements ([full list](docs/TAG_SELECTORS.md))
- **MC 1.21.9 + 1.21.11** support (Fabric, Forge, NeoForge, server)
- **Bukkit event fallback** for pre-1.13 servers (no PacketEvents needed)
- **215 unit tests** + 198 integration tests across 18 MC versions
- Fixed Forge/NeoForge dependency ranges for 1.21.5+
- Fixed adventure serializer classloader conflicts with PacketEvents
- Fixed Velocity plugin injection
- Fixed legacy server support (Java 8 bytecode, Gson relocation)

## Version Branches

Each branch contains the full buildable source for that Minecraft version:

| Minecraft | Branch | Java | Fabric | Forge | NeoForge | Server |
|-----------|--------|------|--------|-------|----------|--------|
| 1.21.11 | [`multiplatform/1.21.11`](../../tree/multiplatform/1.21.11) | 21 | yes | yes | yes | PacketEvents |
| 1.21.9 | [`multiplatform/1.21.9`](../../tree/multiplatform/1.21.9) | 21 | yes | yes | yes | PacketEvents |
| 1.21.5 | [`multiplatform/1.21.5`](../../tree/multiplatform/1.21.5) | 21 | yes | yes | yes | PacketEvents |
| 1.21.4 | [`multiplatform/1.21.4`](../../tree/multiplatform/1.21.4) | 21 | yes | yes | yes | PacketEvents |
| 1.21.1 | [`multiplatform/1.21.1`](../../tree/multiplatform/1.21.1) | 21 | yes | yes | yes | PacketEvents |
| 1.20.6 | [`multiplatform/1.20.6`](../../tree/multiplatform/1.20.6) | 21 | yes | yes | yes | PacketEvents |
| 1.20.4 | [`multiplatform/1.20.4`](../../tree/multiplatform/1.20.4) | 17 | yes | yes | yes | PacketEvents |
| 1.20.1 | [`multiplatform/1.20.1`](../../tree/multiplatform/1.20.1) | 17 | yes | yes | -- | PacketEvents |
| 1.19.4 | [`multiplatform/1.19.4`](../../tree/multiplatform/1.19.4) | 17 | yes | yes | -- | PacketEvents |
| 1.19.2 | [`multiplatform/1.19.2`](../../tree/multiplatform/1.19.2) | 17 | yes | yes | -- | PacketEvents |
| 1.18.2 | [`multiplatform/1.18.2`](../../tree/multiplatform/1.18.2) | 17 | yes | yes | -- | PacketEvents |
| 1.17.1 | [`multiplatform/1.17.1`](../../tree/multiplatform/1.17.1) | 17 | yes | -- | -- | PacketEvents |
| 1.16.5 | [`multiplatform/1.16.5`](../../tree/multiplatform/1.16.5) | 17 | yes | yes | -- | PacketEvents |
| 1.15.2 | [`multiplatform/1.15.2`](../../tree/multiplatform/1.15.2) | 17 | yes | -- | -- | PacketEvents |
| 1.14.4 | [`multiplatform/1.14.4`](../../tree/multiplatform/1.14.4) | 17 | yes | yes | -- | PacketEvents |
| 1.12.2 | [`multiplatform/1.12.2`](../../tree/multiplatform/1.12.2) | 8 | -- | yes | -- | Bukkit events |
| 1.11.2 | [`multiplatform/1.11.2`](../../tree/multiplatform/1.11.2) | 8 | -- | yes | -- | Bukkit events |
| 1.10.2 | [`multiplatform/1.10.2`](../../tree/multiplatform/1.10.2) | 8 | -- | yes | -- | Bukkit events |
| 1.9.4 | [`multiplatform/1.9.4`](../../tree/multiplatform/1.9.4) | 8 | -- | yes | -- | Bukkit events |
| 1.8.9 | [`multiplatform/1.8.9`](../../tree/multiplatform/1.8.9) | 8 | -- | yes | -- | Bukkit events |
| 1.7.10 | [`multiplatform/1.7.10`](../../tree/multiplatform/1.7.10) | 8 | -- | yes | -- | Bukkit events |

## Features

- **Regex replacement** -- Match incoming chat with regex, replace with formatted text
- **Outgoing message rules** -- Intercept and rewrite messages you send (command aliases, color prefixing)
- **Color-aware matching** -- `colorAware: true` matches against text with color codes preserved; replacements support `&a`, `&l`, etc.
- **Toast notifications** -- Redirect matched messages from chat to overlay display (`"toast": true`)
- **Notification styles** -- `"notifyStyle"`: `"actionbar"` (default), `"toast"` (system toast), or `"advancement"` (achievement popup)
- **Sound alerts** -- Play sounds on match: `"sound": true` (default sound), `"sound": "bell"` (specific sound), or `"sound": "note_pling"`
- **Auto-responses** -- Send a command/message when a pattern matches: `"respond": "/cmd"` (single) or `"respond": ["/cmd1", "/cmd2"]` (multi)
- **JSON matching** -- `matchJson: true` matches against raw JSON chat components instead of displayed text
- **Value stacking** -- Aggregate numeric capture groups across rapid messages into running totals (see below)
- **Tag selectors** -- `{player}`, `{time}`, `{item_name}`, `{biome}`, and [389 more](docs/TAG_SELECTORS.md) expanded in replacements
- **Server filtering** -- `"server": "^mc\\.example\\.com"` restricts a rule to matching servers only
- **Message cancellation** -- Set replacement to `pleasecancelthismessage` to hide a message from chat entirely
- **Hot-reload** -- Edit rules file without restarting the game or server
- **Legacy field support** -- Old field names (`search`, `toastMe`, `respondMsg`, `serversearch`, `msgsearch`, `msgreplacement`) still work via aliases

## Configuration

Config file: `flowchat.json` (location depends on platform)
- **Client mods**: `.minecraft/config/flowchat.json`
- **Server plugins**: `plugins/FlowChat/flowchat.json`

A default config with 7 starter rules ships with the mod. See [example_rules.json](example_rules.json) for a complete reference.

### Rule Format

```json
{
  "incoming": [
    { "pattern": "hello", "replacement": "world" },
    { "pattern": "alert_me", "toast": true, "sound": "bell", "notifyStyle": "advancement" },
    { "pattern": "greet", "respond": ["/say Hi!", "/say Welcome!"] },
    { "pattern": "\\$([\\d.]+)", "replacement": "&a$$1", "colorAware": true },
    { "pattern": "\"text\":\"secret\"", "matchJson": true, "replacement": "REDACTED" },
    { "pattern": "spam", "replacement": "pleasecancelthismessage" },
    { "pattern": "Welcome to (.+)!", "replacement": "Joined $1 as {username} at {time}" },
    { "pattern": "restart", "server": "^mc\\.example\\.com", "toast": true, "sound": true }
  ],
  "outgoing": [
    { "pattern": "/b", "replacement": "/bottle get 64" },
    { "pattern": "^[^/].*", "replacement": "&a&l$0", "server": "^mc\\.example\\.com" }
  ]
}
```

## Value Stacking

Value stacking aggregates numeric capture groups across rapid successive messages. When a shop or game mechanic sends multiple lines quickly (e.g. selling items in bulk), value stacking collapses them into a running total.

### How it works

1. A regex `pattern` captures numeric groups (e.g. `(\d+)` for quantity, `([\d.]+)` for price)
2. The `valuestack` object defines which groups to accumulate
3. On each match, captured values are added to a cache keyed by the "static" parts of the replacement
4. The cache expires after `expire_after` seconds (default 4), resetting the totals

### Replacement tokens

| Token | Meaning |
|-------|---------|
| `$1`, `$2`, ... | Standard regex group references (current match only) |
| `$^1`, `$^2`, ... | **Stacked** value for that group (accumulated total) |
| `$^i` | Iteration count (how many messages have been stacked) |

### Configuration

```json
{
  "pattern": "^(\\d+) You sold (\\d+) (.*?) for \\$([\\d.]+)\\.$",
  "replacement": "You sold $^2 $3 for $^4. ($^i)",
  "toast": true,
  "valuestack": {
    "stack_values": [2, 4],
    "ignore_diffs": [1],
    "expire_after": 4,
    "seperate_float_with": "."
  }
}
```

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `stack_values` | `int[]` | **Required.** Capture group indices to accumulate. |
| `ignore_diffs` | `int[]` | Groups that vary between messages but shouldn't create separate cache entries (e.g. a counter prefix). |
| `expire_after` | `int` | Seconds before the stack resets (default `4`). |
| `seperate_float_with` | `string` | Decimal separator in the matched text (default `"."`). |

### Example

Server sends these lines rapidly when you sell items:

```
1 You sold 64 Iron Block for $69.42.
2 You sold 32 Gold Block for $120.00.
3 You sold 16 Diamond for $500.00.
```

With the rule above, chat displays:

```
You sold 64 Iron Block for $69.42. (1)
You sold 96 Gold/Iron Block for $189.42. (2)
You sold 112 items for $689.42. (3)
```

Groups 2 and 4 accumulate (`$^2` = 64->96->112, `$^4` = 69.42->189.42->689.42), while `$^i` counts iterations. Group 1 (the server-side counter) is in `ignore_diffs` so all three lines share the same cache entry.

After 4 seconds of no new matches, the stack resets.

## Server Architecture

```
Pre-1.13 servers (1.7.10 - 1.12.2):
  FlowChat -> Bukkit events (AsyncPlayerChatEvent, ServerCommandEvent)
  No PacketEvents dependency needed

1.13+ servers (1.14.4 - 1.21.11):
  FlowChat -> PacketEvents 2.7.0 -> packet-level interception
  PacketEvents is a soft dependency -- falls back to Bukkit events if unavailable
```

## Building

```bash
git checkout multiplatform/1.21.11  # pick your version
./gradlew build

# Platform-specific JARs:
#   fabric/build/libs/flowchat-fabric-*.jar
#   forge/build/libs/flowchat-forge-*.jar
#   neoforge/build/libs/flowchat-neoforge-*.jar  (1.20.4+)
#   spigot/build/libs/flowchat-spigot-*-all.jar  (includes relocated Gson)
#   bungee/build/libs/flowchat-bungee-*-all.jar
#   velocity/build/libs/flowchat-velocity-*-all.jar
```

## Testing

**215 unit tests** + **198 server integration tests** across 18 MC versions.

| Suite | Count | Status |
|-------|-------|--------|
| Unit tests | 215 | Passing |
| Server integration (18 versions) | 198 | Passing |
| BungeeCord proxy | 8 | Passing |
| Velocity proxy | 8 | Passing |

See [`test-infra/TEST-MATRIX.md`](../../tree/multiplatform/1.21.11/test-infra/TEST-MATRIX.md) for full details.

## Screenshots

Leaving Walzie's House (WorldGuard travel toasts)
![Left Walzie's House Toast](https://cdn.discordapp.com/attachments/769751221955198997/780700921746817044/unknown.png)

Message prefixing (&c&l added to messages)
![Colour code text](https://cdn.discordapp.com/attachments/769751221955198997/780701590314156032/unknown.png)

AdminShop selling (Toasts instead of ChatMsg)
![Adminshop Toasts](https://cdn.discordapp.com/attachments/769751221955198997/780703852298764298/unknown.png)

## Discord

Want more screenshots? Submit yours to [the discord](https://discord.com/invite/BpVhWNv8hG)!

## License

GPL-3.0
