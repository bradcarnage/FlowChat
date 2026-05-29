# FlowChat

Regex-powered chat processor for Minecraft. Replace, filter, toast, auto-respond, and play sounds on chat messages — works on **clients and servers** across every major platform.

Originally inspired by [ChatFlow](https://github.com/Vazkii/ChatFlow).

## Platforms

| Platform | Versions | Branch Prefix |
|----------|----------|---------------|
| **Fabric** (client mod) | 1.16.5 – 1.21.4 | `multiplatform/*` |
| **Forge** (client mod) | 1.16.5 – 1.21.4 | `multiplatform/*` |
| **NeoForge** (client mod) | 1.21.4 | `multiplatform/*` |
| **Spigot/Paper** (server plugin) | 1.7.10 – 1.21.4 | `multiplatform/*` |
| **BungeeCord** (proxy plugin) | Latest | `multiplatform/*` |
| **Velocity** (proxy plugin) | Latest | `multiplatform/*` |

## Version Branches

Each branch contains the full buildable source for that Minecraft version:

| Minecraft | Branch | Java | Server Interception |
|-----------|--------|------|-------------------|
| 1.21.4 | [`multiplatform/1.21.4`](../../tree/multiplatform/1.21.4) | 21 | PacketEvents |
| 1.21.1 | [`multiplatform/1.21.1`](../../tree/multiplatform/1.21.1) | 21 | PacketEvents |
| 1.20.6 | [`multiplatform/1.20.6`](../../tree/multiplatform/1.20.6) | 21 | PacketEvents |
| 1.20.4 | [`multiplatform/1.20.4`](../../tree/multiplatform/1.20.4) | 17 | PacketEvents |
| 1.20.1 | [`multiplatform/1.20.1`](../../tree/multiplatform/1.20.1) | 17 | PacketEvents |
| 1.19.4 | [`multiplatform/1.19.4`](../../tree/multiplatform/1.19.4) | 17 | PacketEvents |
| 1.19.2 | [`multiplatform/1.19.2`](../../tree/multiplatform/1.19.2) | 17 | PacketEvents |
| 1.18.2 | [`multiplatform/1.18.2`](../../tree/multiplatform/1.18.2) | 17 | PacketEvents |
| 1.17.1 | [`multiplatform/1.17.1`](../../tree/multiplatform/1.17.1) | 17 | PacketEvents |
| 1.16.5 | [`multiplatform/1.16.5`](../../tree/multiplatform/1.16.5) | 11 | PacketEvents |
| 1.15.2 | [`multiplatform/1.15.2`](../../tree/multiplatform/1.15.2) | 11 | PacketEvents |
| 1.14.4 | [`multiplatform/1.14.4`](../../tree/multiplatform/1.14.4) | 11 | PacketEvents |
| 1.12.2 | [`multiplatform/1.12.2`](../../tree/multiplatform/1.12.2) | 8 | Bukkit events |
| 1.11.2 | [`multiplatform/1.11.2`](../../tree/multiplatform/1.11.2) | 8 | Bukkit events |
| 1.10.2 | [`multiplatform/1.10.2`](../../tree/multiplatform/1.10.2) | 8 | Bukkit events |
| 1.9.4 | [`multiplatform/1.9.4`](../../tree/multiplatform/1.9.4) | 8 | Bukkit events |
| 1.8.9 | [`multiplatform/1.8.9`](../../tree/multiplatform/1.8.9) | 8 | Bukkit events |
| 1.7.10 | [`multiplatform/1.7.10`](../../tree/multiplatform/1.7.10) | 8 | Bukkit events |

## Features

- **Regex replacement** — Match incoming/outgoing chat with regex, replace with formatted text
- **Color codes** — `&a`, `&b`, `&l`, etc. in replacements (`colorAware` mode)
- **Toast notifications** — Show matched messages as action bar overlay (`"toast": true`)
- **Auto-responses** — Send replies when a pattern matches (`"respond"` — string or array)
- **Sound alerts** — Play sounds on match (`"sound": "bell"`)
- **JSON matching** — Match against raw JSON chat components (`"matchJson": true`)
- **Tag variables** — `{username}`, `{serverip}`, `{servername}`, `{time}` in replacements
- **Value stacking** — Aggregate numeric values from rapid messages (see below)
- **Server filtering** — Target specific servers via `"serverPattern"` regex
- **Legacy field support** — Old field names (`search`, `toastMe`, etc.) still work via aliases

## Configuration

Config file: `flowchat.json` (location depends on platform)
- **Client mods**: `.minecraft/config/flowchat.json`
- **Server plugins**: `plugins/FlowChat/flowchat.json`

See [example_rules.json](example_rules.json) for a complete reference.

### Rule Format

```json
{
  "incoming": [
    { "pattern": "hello", "replacement": "world" },
    { "pattern": "alert_me", "toast": true, "sound": "bell" },
    { "pattern": "greet", "respond": ["Hi!", "Welcome!"] },
    { "pattern": "\\$([\\d.]+)", "replacement": "&a$$$1", "colorAware": true }
  ],
  "outgoing": [
    { "pattern": "/b", "replacement": "/gamemode creative" },
    { "pattern": "secret", "replacement": "", "toast": true }
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

Groups 2 and 4 accumulate (`$^2` = 64→96→112, `$^4` = 69.42→189.42→689.42), while `$^i` counts iterations. Group 1 (the server-side counter) is in `ignore_diffs` so all three lines share the same cache entry.

After 4 seconds of no new matches, the stack resets.

## Server Architecture

```
Pre-1.13 servers (1.7.10 – 1.12.2):
  FlowChat → Bukkit events (AsyncPlayerChatEvent, ServerCommandEvent)
  No PacketEvents dependency needed

1.13+ servers (1.14.4 – 1.21.4):
  FlowChat → PacketEvents 2.7.0 → packet-level interception
  PacketEvents is a soft dependency — falls back to Bukkit events if unavailable
```

## Building

```bash
git checkout multiplatform/1.21.4  # pick your version
./gradlew build

# Platform-specific JARs:
#   fabric/build/libs/flowchat-fabric-*.jar
#   forge/build/libs/flowchat-forge-*.jar
#   spigot/build/libs/flowchat-spigot-*-all.jar  (includes relocated Gson)
```

## Test Matrix

**198/198 server integration tests passing** across all 18 versions.
See [`test-infra/TEST-MATRIX.md`](../../tree/multiplatform/1.21.4/test-infra/TEST-MATRIX.md) for full details.

Additional: BungeeCord 8/8 ✅, Velocity 8/8 ✅, Unit tests 70/70 ✅

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
