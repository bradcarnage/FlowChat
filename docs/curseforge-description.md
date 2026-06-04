Found a bug or have a feature request? Open an issue on GitHub: https://github.com/bradcarnage/FlowChat/issues

# FlowChat

Regex-powered chat processor for Minecraft. Replace, filter, toast, auto-respond, and play sounds on chat messages -- works on **clients and servers** across every major platform. Current version: **2.1.2**.

## Platforms

| Platform | Type | Versions |
|----------|------|----------|
| **Fabric** | Client mod | 1.14.4 - 1.21.11 |
| **Forge** | Client mod | 1.7.10 - 1.21.11 |
| **NeoForge** | Client mod | 1.20.4 - 1.21.11 |
| **Spigot/Paper** | Server plugin | 1.7.10 - 1.21.11 |
| **BungeeCord** | Proxy plugin | Latest |
| **Velocity** | Proxy plugin | Latest |

## Features

- **Regex replacement** -- Match incoming chat with regex, replace with formatted text
- **Outgoing message rules** -- Intercept and rewrite messages you send (command aliases, color prefixing)
- **Color-aware matching** -- `colorAware: true` matches against text with color codes preserved; replacements support `&a`, `&l`, etc.
- **Toast notifications** -- Redirect matched messages from chat to overlay display (`"toast": true`)
- **Notification styles** -- `"notifyStyle"`: `"actionbar"` (default), `"toast"` (system toast), or `"advancement"` (achievement popup)
- **Sound alerts** -- Play sounds on match: `"sound": true` (default sound), `"sound": "bell"` (specific sound), or `"sound": "note_pling"`
- **Auto-responses** -- Send a command/message when a pattern matches: `"respond": "/cmd"` (single) or `"respond": ["/cmd1", "/cmd2"]` (multi)
- **JSON matching** -- `matchJson: true` matches against raw JSON chat components instead of displayed text
- **Value stacking** -- Aggregate numeric capture groups across rapid messages into running totals
- **Tag selectors** -- `{player}`, `{time}`, `{biome}`, `{item_name}`, and 389 more expanded in replacements
- **Server filtering** -- `"server": "^mc\\.example\\.com"` restricts a rule to matching servers only
- **Message cancellation** -- Set replacement to `pleasecancelthismessage` to hide a message from chat entirely
- **Hot-reload** -- Edit rules file without restarting the game or server
- **Legacy field support** -- Old field names (`search`, `toastMe`, `respondMsg`, `serversearch`, `msgsearch`, `msgreplacement`) still work via aliases

## Rule Format

```json
{
  "incoming": [
    { "pattern": "hello", "replacement": "world" },
    { "pattern": "alert_me", "toast": true, "sound": "bell", "notifyStyle": "advancement" },
    { "pattern": "greet", "respond": ["/say Hi!", "/say Welcome!"] }
  ],
  "outgoing": [
    { "pattern": "/b", "replacement": "/bottle get 64" }
  ]
}
```

## Value Stacking

Aggregates numeric capture groups across rapid successive messages. When a shop sends multiple lines quickly (e.g. selling items in bulk), value stacking collapses them into a running total.

| Token | Meaning |
|-------|---------|
| `$1`, `$2`, ... | Standard regex group references (current match only) |
| `$^1`, `$^2`, ... | **Stacked** value for that group (accumulated total) |
| `$^i` | Iteration count (how many messages stacked) |

## Installation

**Client:** Download the JAR for your MC version and loader from [Releases](https://github.com/bradcarnage/FlowChat/releases/latest). Drop it in your `mods/` folder. FlowChat creates a default config at `.minecraft/config/flowchat.json` on first launch.

**Server:** Download the server JAR (Spigot, Bungee, Velocity, or Unified) from [Releases](https://github.com/bradcarnage/FlowChat/releases/latest). Drop it in `plugins/`. For 1.14.4+ servers, install [PacketEvents](https://www.spigotmc.org/resources/packetevents-api.80279/) for packet-level chat interception (optional -- falls back to Bukkit events).

**Reload:** Edit `flowchat.json` and run `/flowchat reload` in-game. No restart needed.

## Links

- [GitHub](https://github.com/bradcarnage/FlowChat)
- [Modrinth](https://modrinth.com/mod/flowchat)
- [Full config reference](https://github.com/bradcarnage/FlowChat/blob/master/example_rules.json)
