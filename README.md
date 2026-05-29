# FlowChat

A Fabric client-side mod for Minecraft that lets you replace, filter, and react to chat messages using regex rules. Features toast notifications, auto-responses, sound alerts, value stacking, and more.

Originally inspired by [ChatFlow](https://github.com/Vazkii/ChatFlow).

## Supported Versions

| Minecraft | Branch | Java | Status |
|-----------|--------|------|--------|
| 1.21.4 | [`mc/1.21.4`](../../tree/mc/1.21.4) | 21 | ✅ Latest |
| 1.20.1 | [`mc/1.20.1`](../../tree/mc/1.20.1) | 17 | ✅ |
| 1.19.4 | [`mc/1.19.4`](../../tree/mc/1.19.4) | 17 | ✅ |
| 1.18.2 | [`mc/1.18.2`](../../tree/mc/1.18.2) | 17 | ✅ |
| 1.16.5 | [`mc/1.16.5`](../../tree/mc/1.16.5) | 8+ | Legacy (v1.0.6) |

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download the FlowChat JAR for your MC version from [Releases](../../releases) or build from source
4. Drop the JAR into your `.minecraft/mods/` folder

## Configuration

Config file: `.minecraft/config/flowchat.json`

An empty config is created on first launch. Copy the [example rules](example_rules.json) to get started.

### Features

- **Regex replacement** — Match incoming/outgoing chat with regex, replace with formatted text
- **Toast notifications** — Show matched messages on the action bar instead of chat (`"toastMe": true`)
- **Auto-responses** — Automatically send a reply when a pattern is matched (`"respondMsg"`)
- **Sound alerts** — Play a sound when a rule matches (`"playSound": true, "soundName": "ding"`)
- **Tag variables** — Use `{username}`, `{serverip}`, `{servername}`, `{time}` in replacements
- **Value stacking** — Aggregate numeric values from rapid messages (e.g., shop sales)
- **Anti-AFK** — Send a command after N seconds of inactivity
- **Void fall protection** — Auto-run a command when falling below Y level
- **Server filtering** — Rules can target specific servers via `"serversearch"` regex
- **Anti-spam** — Prevents duplicate auto-responses (bypass with `"noAntiSpam": true`)
- **Command shortcuts** — Remap outgoing messages (e.g., `/b` → `/gamemode creative`)
- **Local-only messages** — Intercept outgoing messages and show locally without sending

### Incoming Rule Properties

```json
{
    "search": "regex pattern to match",
    "replacement": "replacement with $1 capture groups",
    "toastMe": false,
    "respondMsg": "auto-reply message or [\"array\", \"of messages\"]",
    "playSound": false,
    "soundName": "ding",
    "serversearch": "optional server IP regex",
    "noAntiSpam": false,
    "valuestack": { "stack_values": [1, 2], "expire_after": 4 }
}
```

### Outgoing Rule Properties

```json
{
    "msgsearch": "regex to match outgoing",
    "msgreplacement": "replacement text",
    "localOnly": false,
    "toastMe": false,
    "serversearch": "optional server IP regex"
}
```

### Available Sounds

`ding` (default), `levelup`, `anvil`, `note`, `click`, `pop`, `silent`, or any Minecraft sound ID (e.g., `minecraft:entity.villager.yes`)

### Tag Variables

| Tag | Replaced With |
|-----|---------------|
| `{username}` | Your player name |
| `{serverip}` | Current server address |
| `{servername}` | Server name or "Singleplayer" |
| `{time}` | Current time (HH:mm:ss) |

## Building from Source

```bash
git checkout mc/1.21.4  # or mc/1.20.1, mc/1.19.4, etc.
./gradlew build
# Output: build/libs/flowchat-2.0.0.jar
```

## Screenshots

Leaving Walzie's House (WorldGuard travel toasts) - [Ref](example_rules.json)
![Left Walzie's House Toast](https://cdn.discordapp.com/attachments/769751221955198997/780700921746817044/unknown.png)

Message prefixing (&c&l added to messages)
![Colour code text](https://cdn.discordapp.com/attachments/769751221955198997/780701590314156032/unknown.png)

AdminShop selling (Toasts instead of ChatMsg)
![Adminshop Toasts](https://cdn.discordapp.com/attachments/769751221955198997/780703852298764298/unknown.png)

## Discord

Want more screenshots? Submit yours to [the discord](https://discord.com/invite/BpVhWNv8hG)!

## License

GPL-3.0
