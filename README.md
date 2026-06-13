# FlowChat

[![CurseForge](https://img.shields.io/curseforge/dt/422239?logo=curseforge&label=CurseForge)](https://www.curseforge.com/minecraft/bukkit-plugins/flowchat)
[![Modrinth](https://img.shields.io/modrinth/dt/Gt3d2pOM?logo=modrinth&label=Modrinth)](https://modrinth.com/plugin/flowchat)

Regex-powered chat processor for Minecraft. Intercept, reformat, and enhance chat messages with pattern matching, color codes, sound notifications, toast popups, and auto-responses.

Works on **client-side** (Fabric mod) and **server-side** (Spigot, BungeeCord, Velocity plugins).

## Downloads

- **CurseForge:** https://www.curseforge.com/minecraft/bukkit-plugins/flowchat
- **Modrinth:** https://modrinth.com/plugin/flowchat

## Supported Versions

| Platform | MC Versions | Branch |
|----------|-------------|--------|
| Fabric (client) | 1.7.10, 1.8.9, 1.9.4, 1.10.2, 1.11.2, 1.12.2, 1.14.4, 1.15.2, 1.16.5, 1.17.1, 1.18.2, 1.19.2, 1.19.4, 1.20.1, 1.20.4, 1.20.6, 1.21.1, 1.21.4, 1.21.5, 1.21.9, 1.21.11, 26.1.2 | `multiplatform/<version>` |
| Forge (client) | 1.21.4+ | `multiplatform/1.21.4` |
| NeoForge (client) | 1.21.4+ | `multiplatform/1.21.4` |
| Spigot / Paper (server) | 1.7.10+ | `multiplatform/<version>` |
| BungeeCord (proxy) | Any | `multiplatform/1.21.4` |
| Velocity (proxy) | 3.x | `multiplatform/1.21.4` |

**22 version branches** — each builds and passes CI across 5 platform tiers (110/110).

## Installation

### Fabric (Client-Side Mod)

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for your MC version
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) to your `mods/` folder
3. Download the FlowChat JAR for your MC version from [CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/flowchat) or [Modrinth](https://modrinth.com/plugin/flowchat)
4. Drop it in your `.minecraft/mods/` folder

### Spigot / Paper (Server Plugin)

1. Install [PacketEvents 2.7.0+](https://modrinth.com/plugin/packetevents) in your `plugins/` folder
2. Download `flowchat-spigot-2.1.2.jar` from [CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/flowchat) or [Modrinth](https://modrinth.com/plugin/flowchat)
3. Drop it in your server's `plugins/` folder
4. Restart the server

> **PacketEvents is required.** FlowChat uses packet-level interception to catch ALL outgoing chat messages, including those from other plugins. Without PacketEvents installed, the plugin will fail to load with `UnknownDependencyException`.

### BungeeCord (Proxy Plugin)

1. Install [PacketEvents for BungeeCord](https://modrinth.com/plugin/packetevents) in your proxy's `plugins/` folder
2. Download `flowchat-bungee-2.1.2.jar` from [CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/flowchat) or [Modrinth](https://modrinth.com/plugin/flowchat)
3. Drop it in your BungeeCord `plugins/` folder
4. Restart the proxy

### Velocity (Proxy Plugin)

1. Download `flowchat-velocity-2.1.2.jar` from [CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/flowchat) or [Modrinth](https://modrinth.com/plugin/flowchat)
2. Drop it in your Velocity `plugins/` folder
3. Restart the proxy

## Configuration

Config file: `flowchat.json` (location depends on platform)

| Platform | Config Path |
|----------|------------|
| Fabric | `.minecraft/config/flowchat.json` |
| Spigot | `plugins/FlowChat/flowchat.json` |
| BungeeCord | `plugins/FlowChat/flowchat.json` |
| Velocity | `plugins/flowchat/flowchat.json` |

### Example Config

```json
{
  "rules": [
    {
      "name": "color-chat",
      "pattern": "<(\\w+)> (.*)",
      "replacement": "&b<&e{1}&b> &f{2}",
      "sound": "bell"
    },
    {
      "name": "hide-join-messages",
      "pattern": "\\w+ joined the game",
      "replacement": "",
      "sound": "none"
    },
    {
      "name": "shop-toast",
      "pattern": "You sold (\\d+) (.+) for \\$(\\S+)",
      "replacement": "",
      "toast": "Sold {1}x {2} for ${3}",
      "sound": "note"
    }
  ]
}
```

### Rule Fields

| Field | Description | Required |
|-------|-------------|----------|
| `name` | Rule identifier | Yes |
| `pattern` | Java regex to match against chat messages | Yes |
| `replacement` | Replacement text. Use `{1}`, `{2}` etc. for capture groups. Use `{username}` for sender name. Empty string = hide message. | Yes |
| `sound` | Sound to play: `bell`, `note`, `click`, `none` | No |
| `toast` | Show a toast notification with this text (Fabric only) | No |

### Color Codes

Use `&` color codes in replacements: `&a` green, `&b` aqua, `&c` red, `&d` pink, `&e` yellow, `&f` white, `&l` bold, `&o` italic, `&n` underline, `&r` reset.

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/flowchat reload` | `flowchat.admin` | Reload config from disk |
| `/flowchat toggle` | `flowchat.admin` | Enable/disable processing |

## Architecture

```
FlowChat
├── common/                 # Pure Java: regex engine, config parser
├── packetevents-common/    # PacketEvents chat interceptor (shared)
├── fabric/                 # Fabric client mod (Mixin + ChatHelper)
├── forge/                  # Forge client mod (1.21.4+)
├── neoforge/               # NeoForge client mod (1.21.4+)
├── spigot/                 # Bukkit/Spigot/Paper server plugin
├── bungee/                 # BungeeCord proxy plugin
└── velocity/               # Velocity proxy plugin
```

- **Fabric mod** uses Mixins to intercept `ChatHud.addMessage()` on the client
- **Forge/NeoForge** use event-based chat interception
- **Server/proxy plugins** use PacketEvents to intercept outgoing chat packets - catches ALL messages including those from other plugins

## Building

```bash
# Build everything (requires Java 21+)
./gradlew build

# Build specific module
./gradlew :fabric:build
./gradlew :spigot:build
./gradlew :bungee:build
./gradlew :velocity:build
./gradlew :forge:build
./gradlew :neoforge:build
```

For older MC versions, check out the corresponding branch:
```bash
git checkout multiplatform/1.18.2
./gradlew :fabric:build
```

## Screenshots

Leaving Walzie's House (WorldGuard travel toasts):
![Left Walzie's House Toast](https://cdn.discordapp.com/attachments/769751221955198997/780700921746817044/unknown.png)

Message prefixing (&c&l added to messages):
![Colour code text](https://cdn.discordapp.com/attachments/769751221955198997/780701590314156032/unknown.png)

AdminShop selling (Toasts instead of ChatMsg):
![Adminshop Toasts](https://cdn.discordapp.com/attachments/769751221955198997/780703852298764298/unknown.png)

## Discord

Want more screenshots? Submit yours to [the Discord](https://discord.com/invite/BpVhWNv8hG)!

## License

GPL-3.0
