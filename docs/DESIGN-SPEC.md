# FlowChat Design Specification

> **Version:** 2.1.0
> **Status:** Draft — derived from source code audit (May 2026)
> **Purpose:** Canonical feature spec for test coverage and feature-parity backfill

---

## 1. Overview

FlowChat is a regex-powered chat processor for Minecraft. It intercepts incoming and outgoing chat messages, applies pattern-matching rules, and can replace text, cancel messages, play sounds, show notifications, send auto-responses, stack numeric values, and perform automated player actions.

It runs on **6 platforms** across **3 platform types**:

| Type | Platforms | Interception Method |
|------|-----------|-------------------|
| **Client mod** | Fabric, Forge, NeoForge | Mixin (Fabric) / Event bus (Forge/NeoForge) |
| **Server plugin** | Spigot (Paper/Bukkit) | PacketEvents outbound packet |
| **Proxy plugin** | BungeeCord, Velocity | PacketEvents outbound packet |

---

## 2. Configuration

### 2.1 Config File

- **File:** `flowchat.json`
- **Format:** JSON
- **Location:**

| Platform | Path |
|----------|------|
| Fabric | `.minecraft/config/flowchat.json` |
| Forge | `.minecraft/config/flowchat.json` (via `FMLPaths.CONFIGDIR` / `mcDataDir/config`) |
| NeoForge | `.minecraft/config/flowchat.json` (via `FMLPaths.CONFIGDIR`) |
| Spigot | `plugins/FlowChat/flowchat.json` |
| BungeeCord | `plugins/FlowChat/flowchat.json` |
| Velocity | `plugins/flowchat/flowchat.json` |

### 2.2 Config Structure

```json
{
  "incoming": [ /* FlowChatRule[] */ ],
  "outgoing": [ /* FlowChatRule[] */ ],
  "antiAFK": { /* AntiAFK config */ },
  "voidFall": { /* VoidFall config */ }
}
```

### 2.3 Default Config

When `flowchat.json` does not exist, the mod creates:
```json
{
  "incoming": [],
  "outgoing": []
}
```

### 2.4 Config Hot-Reload

**Trigger:** On world tick, when the gap between the current tick and the previous tick exceeds 1 second (i.e., the player was paused, disconnected, or the world was loading), the config is reloaded from disk.

**Server-side:** Spigot supports `/flowchat reload` command. BungeeCord and Velocity have no reload mechanism currently.

| Platform | Hot-reload | Mechanism |
|----------|-----------|-----------|
| Fabric | ✅ | Tick gap > 1s |
| Forge (1.16.5+) | ✅ | Tick gap > 1s |
| Forge (≤1.14.4) | ⚠️ Partial | Tick tracking exists but reload logic varies by branch |
| NeoForge | ✅ | Tick gap > 1s |
| Spigot | ✅ | `/flowchat reload` command |
| BungeeCord | ❌ | Not implemented |
| Velocity | ❌ | Not implemented |

---

## 3. Rule System

### 3.1 FlowChatRule Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `pattern` | `String` (regex) | `""` | Java regex pattern to match against message text. **Aliases:** `search`, `msgsearch` (legacy). |
| `replacement` | `String` | `"$0"` | Replacement text. Supports regex backreferences (`$1`, `$2`), tag variables, and color codes. **Alias:** `msgreplacement` (legacy). |
| `server` | `String` (regex) | `null` (match all) | Regex filter for server IP. Rule only fires if the current server IP matches. **Alias:** `serversearch` (legacy). |
| `toast` | `boolean` | `false` | If true, cancel the original message and show it as a notification instead. See `notifyStyle` (§4.4). **Alias:** `toastMe` (legacy). |
| `sound` | `String` or `boolean` | `null` | Sound to play when rule matches. String value = named sound (see §4.3). `true` = default sound. `false`/`null` = no sound. **Aliases:** `soundName` (string, legacy), `playSound` (boolean, legacy). |
| `respond` | `String` or `String[]` | `null` | Auto-response message(s) to send as the player when the rule matches. Supports regex backreferences and tag variables. **Alias:** `respondMsg` (legacy). |
| `notifyStyle` | `String` | `"actionbar"` | Notification style when `toast` is true: `"actionbar"` (text above hotbar) or `"toast"` (system popup, 1.14+). Degrades to actionbar/local chat on versions without toast support. |
| `valuestack` | `Object` | `null` | Value stacking configuration. See §4.6. |

**Field name resolution order:** The parser checks the canonical name first, then aliases in order. First match wins. All legacy field names are supported indefinitely — no config will ever break.

### 3.2 Rule Processing Pipeline

For each message, rules are evaluated **in order**. Processing is cumulative — the output of one rule feeds into the next:

```
input message
  → strip \r \n → strip §X formatting codes
  → for each rule:
      → skip if serversearch doesn't match current server
      → skip if regex doesn't match
      → collect auto-responses (if respondMsg set)
      → set toastMe flag (sticky — once true, stays true)
      → set playSound flag (sticky — first match wins soundName)
      → apply replacement (with valuestack if configured)
      → apply tag replacement on replacement string
      → regex replaceAll on message text
  → if any rule matched:
      → if toastMe: mark cancelled
      → return Result
```

### 3.3 Incoming vs. Outgoing Rules

- **Incoming rules** (`"incoming"` array): Applied to chat messages received from the server (other players' messages, system messages, plugin messages).
- **Outgoing rules** (`"outgoing"` array): Applied to chat messages the player is about to send, *before* they leave the client.

### 3.4 Message Stripping (Pre-processing)

Before matching, the raw message text undergoes:
1. `\r` → literal `\\r`
2. `\n` → literal `\\n`
3. `§X` (section sign + any word char) → removed (strips existing Minecraft formatting)

---

## 4. Features

### 4.1 Text Replacement

**Behavior:** When a rule's regex matches, the message text is transformed using `String.replaceAll(rule.search, replacement)`. The replacement string supports:

- **Regex backreferences:** `$1`, `$2`, etc. — standard Java regex group references
- **Tag variables:** See §4.7

**Color formatting:** Replacements can contain `&X` color codes which are converted to `§X` (Minecraft formatting) by the platform's text helper:
- `&0`–`&9`, `&a`–`&f` — colors
- `&k` — obfuscated
- `&l` — bold
- `&m` — strikethrough
- `&n` — underline
- `&o` — italic
- `&r` — reset

**Platform behavior:**

| Platform | Color code conversion | Applied where |
|----------|----------------------|---------------|
| Fabric | ✅ (implicit — Text.of handles §) | Replacement text |
| Forge | ✅ `ForgeTextHelper.formatColors()` | Replacement text |
| NeoForge | ✅ `NeoForgeTextHelper.formatColors()` | Replacement text |
| Spigot/Bungee/Velocity | ❌ Not implemented | — |

**Parity target:** All platforms should apply `& → §` conversion in replacement text.

### 4.2 Message Cancellation

**Behavior:** A message is cancelled (hidden from the player) when:
- `result.cancelled == true` (set when `toastMe` is true), OR
- The platform-specific handler decides to cancel based on processing results

**Platform behavior:**

| Platform | Incoming cancel | Outgoing cancel |
|----------|----------------|-----------------|
| Fabric | ✅ `ci.cancel()` in Mixin | ✅ Magic string `§flowchat§cancel` + `ci.cancel()` |
| Forge | ✅ `event.setCanceled(true)` | ❌ Not implemented |
| NeoForge | ✅ `event.setCanceled(true)` | ❌ Not implemented |
| Spigot | ✅ `event.setCancelled(true)` | ❌ Not implemented |
| BungeeCord | ✅ `event.setCancelled(true)` | ❌ Not implemented |
| Velocity | ✅ `event.setCancelled(true)` | ❌ Not implemented |

**Parity target:**
- Client mods: Implement outgoing cancellation on Forge and NeoForge
- Server/proxy: Implement outgoing cancellation via client→server packet interception (PacketEvents `CHAT_MESSAGE` / `CHAT_COMMAND` packets)

### 4.3 Sound Playback

**Behavior:** When `playSound: true` on a matching rule, play a notification sound. The first matching rule's `soundName` wins (subsequent rules don't override).

**Sound resolution** is unified across ALL platforms — same logic everywhere. The `sound` field accepts either a named alias or an arbitrary Minecraft sound identifier.

**Named aliases (resolved to full identifiers):**

| Alias | Minecraft Sound Identifier |
|-------|---------------------------|
| `"ding"`, `"orb"` | `minecraft:entity.experience_orb.pickup` |
| `"levelup"`, `"level"` | `minecraft:entity.player.levelup` |
| `"anvil"` | `minecraft:block.anvil.land` |
| `"note"`, `"bell"` | `minecraft:block.note_block.bell` |
| `"click"` | `minecraft:ui.button.click` |
| `"pop"` | `minecraft:entity.item.pickup` |
| `"none"`, `"silent"` | *(no sound played)* |
| `null` / empty / `true` | `minecraft:entity.experience_orb.pickup` *(default)* |

**Arbitrary identifiers:** Any string not matching an alias is treated as a full Minecraft resource identifier (e.g. `"minecraft:block.note_block.harp"`, `"minecraft:entity.ender_dragon.growl"`). If the identifier is invalid or the sound doesn't exist, fail silently (no crash, no sound).

**Sound resolution function** lives in the **common module** (`SoundResolver` or within `FlowChatRule`) so every platform shares identical mapping logic. Platform-specific code only handles the final "play this identifier" step:

| Platform | Play mechanism |
|----------|---------------|
| Fabric | `SoundEvent.of(Identifier.of(id))` → `player.playSound()` |
| Forge (1.14+) | `SoundEvent.createVariableRangeEvent(ResourceLocation.parse(id))` → `getSoundManager().play()` |
| Forge (≤1.13) | `new SoundEvent(new ResourceLocation(id))` → `player.playSound()` |
| NeoForge | Same as modern Forge |
| Spigot/Bungee/Velocity | PacketEvents `WrapperPlayServerSoundEffect` with the resolved identifier |

**Parity target:** All 6 platforms support all 8 named aliases AND arbitrary identifiers.

### 4.4 Toast / Action Bar Notification

**Behavior:** When `toast: true` (or legacy `toastMe: true`) on a matching rule, the original message is cancelled from chat and displayed as a notification instead. The notification style is controlled by the `notifyStyle` field.

**Notification styles:**

| `notifyStyle` | UI | Behavior |
|--------------|-----|----------|
| `"actionbar"` (default) | Text above hotbar | Single line, fades after ~2s. Available on 1.12+; degrades to local chat message on ≤1.11. |
| `"toast"` | System popup, top-right | Multi-line, title "FlowChat", persists ~5s. Available on 1.14+ (`SystemToast`); degrades to actionbar on ≤1.13. |

**Platform implementation targets:**

| Platform | `"actionbar"` | `"toast"` |
|----------|:------------:|:---------:|
| Fabric 1.16+ | ✅ `player.sendMessage(text, true)` | ✅ `SystemToast.multiline()` |
| Fabric 1.14–1.15 | ✅ `player.sendMessage(text, true)` | ✅ `SystemToast.multiline()` |
| Forge 1.14+ | ✅ `player.sendMessage(text, true)` / `sendStatusMessage` | ✅ `SystemToast.multiline()` |
| Forge 1.12–1.13 | ✅ `player.sendStatusMessage(text, true)` | ⬇️ degrades to actionbar |
| Forge ≤1.11 | ⬇️ degrades to local chat | ⬇️ degrades to local chat |
| NeoForge | ✅ `player.sendSystemMessage()` | ✅ `SystemToast.multiline()` |
| Spigot/Bungee/Velocity | ✅ `wrapper.setOverlay(true)` | ⬇️ degrades to actionbar (no toast packet exists) |

**Degradation chain:** `toast` → `actionbar` → `local chat message`

### 4.5 Auto-Response

**Behavior:** When `respondMsg` is set on a matching rule and the regex matches, the mod sends one or more chat messages as the player. The response text supports regex backreferences against the original message and tag variables.

- If `respondMsg` is a string: send one message
- If `respondMsg` is an array: send each message in order

**Anti-echo:** Fabric tracks `lastCmdSent` to avoid re-triggering incoming rules on auto-response messages (prevents infinite loops).

**Platform behavior:**

| Platform | Auto-response | Send mechanism |
|----------|--------------|----------------|
| Fabric | ✅ | `sendChatMessage()` or `sendCommand()` (if starts with `/`) |
| Forge (1.21.4) | ✅ | `connection.sendChat()` or `connection.sendCommand()` |
| Forge (≤1.14.4) | ❌→✅ | `player.sendChatMessage()` |
| NeoForge | ✅ | `connection.sendChat()` |
| Spigot | ❌→✅ | Unsigned `CHAT_MESSAGE` packet injection via PacketEvents (1.19.1+) or `CHAT` packet (pre-1.19) |
| BungeeCord | ❌→✅ | Same — unsigned packet injection |
| Velocity | ❌→✅ | Same — unsigned packet injection |

**Server-side note:** On 1.19.1+ servers, auto-response messages are injected as unsigned chat packets. Other players see them as regular messages (with `enforce-secure-profile=false`, unsigned messages are accepted). Pre-1.19.1 servers — fully transparent, no difference from player-typed chat.

**Anti-echo:** Fabric tracks `lastCmdSent` to avoid re-triggering incoming rules on auto-response messages (prevents infinite loops). All platforms must implement equivalent anti-echo guard.

**Parity target:**
- Forge (legacy): Add `player.sendChatMessage()` calls in onChatReceived handler
- Server/proxy: Inject unsigned chat packet via PacketEvents

### 4.6 Value Stacking

**Behavior:** Accumulates numeric values from repeated matching messages within a time window. Used for combining rapid-fire messages (e.g., stacking damage numbers, combining shop transactions).

**Config structure:**
```json
{
  "valuestack": {
    "stack_values": [1],
    "ignore_diffs": [2],
    "seperate_float_with": ".",
    "expire_after": 4
  }
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `stack_values` | `int[]` | — | Regex capture group indices whose numeric values should be accumulated |
| `ignore_diffs` | `int[]` | — | Capture group indices to exclude from the cache key (so messages that differ only in these groups are stacked together) |
| `seperate_float_with` | `String` | `"."` | Decimal separator in the source text (e.g. `","` for European locales) |
| `expire_after` | `int` | `4` | Seconds after which the accumulated value resets |

**Replacement tokens:**
- `$^N` — accumulated value for capture group N (formatted with locale number format)
- `$^i` — iteration count (how many messages were stacked)

**This feature lives in `MessageProcessor` (common module) — it works identically on ALL platforms** that call `processor.process()`.

### 4.7 Tag Variables

Replacement strings support template variables that are substituted at processing time:

| Tag | Value | Availability |
|-----|-------|-------------|
| `{username}` | Current player's name | Client-side only (when passed to `replaceTags`) |
| `{serverip}` | Current server IP or `"singleplayer"` | All platforms |
| `{servername}` | Server list entry name | Client-side only |
| `{time}` | Current local time (`HH:mm:ss`) | All platforms |

**Note:** `replaceTags()` is called in `MessageProcessor.process()` on every replacement string, but `username` and `serverName` are only passed as non-null by `FabricChatHelper.replaceTags()`. The core processor passes `null` for both — meaning **`{username}` and `{servername}` are currently broken** in the main processing pipeline.

**Parity target:** All platforms should pass username and server name to the tag replacement call.

### 4.8 Outgoing Chat Rules

**Behavior:** Rules in the `"outgoing"` array are applied to messages the player types before they are sent to the server. Can modify, cancel, or redirect outgoing messages.

**Cancellation mechanism (Fabric):** When an outgoing rule cancels a message, the mixin replaces it with the magic string `§flowchat§cancel`, then a separate `@Inject` at HEAD detects this string and calls `ci.cancel()` to prevent the packet from being sent.

**Platform behavior:**

| Platform | Outgoing rules | Mechanism |
|----------|---------------|-----------|
| Fabric (1.19+) | ✅ | Mixin on `ClientPlayNetworkHandler.sendChatMessage()` |
| Fabric (≤1.18) | ✅ | Mixin on `ClientPlayerEntity.sendChatMessage()` |
| Forge (1.12.2+) | ✅ | `ClientChatEvent` event bus |
| Forge (≤1.11.2) | ✅ | Coremod / ASM transform on outgoing chat method |
| NeoForge | ✅ | `ClientChatEvent` event bus |
| Spigot | ✅ | PacketEvents `CHAT_MESSAGE` / `CHAT` client→server interception |
| BungeeCord | ✅ | PacketEvents `CHAT_MESSAGE` / `CHAT` client→server interception |
| Velocity | ✅ | PacketEvents `CHAT_MESSAGE` / `CHAT` client→server interception |

**Server-side signing constraint (1.19.1+):** Modifying a signed `CHAT_MESSAGE` packet server-side invalidates the cryptographic signature. **FlowChat requires `enforce-secure-profile=false` in `server.properties` when installed on a server or proxy.** The plugin should check this property on startup and refuse to enable with a clear error if `enforce-secure-profile=true`.

On pre-1.19.1 servers, `CHAT` packets are unsigned and can be freely modified.

### 4.9 Anti-AFK

**Behavior:** Automatically sends a configured command after the player has been idle for a specified duration. Prevents AFK kicks on servers.

**Config:**
```json
{
  "antiAFK": {
    "serversearch": ".*hypixel\\.net",
    "afterSeconds": 180,
    "command": "/hub"
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `serversearch` | `String` (regex) | Only activate on matching servers. Omit for all servers. |
| `afterSeconds` | `long` | Seconds of inactivity before sending the command |
| `command` | `String` | Chat message or command to send (prefix with `/` for commands) |

**Idle detection:** Compares `whenLastCmdSent` (timestamp of last outgoing message) against current time.

**Platform behavior:**

| Platform | Anti-AFK |
|----------|---------|
| Fabric | ✅ (in world tick handler) |
| Forge | ❌ Not implemented |
| NeoForge | ❌ Not implemented |
| Server/proxy | ❌ N/A (wrong semantics — server can't send commands as client) |

**Parity target:**
- Forge/NeoForge: Implement in `onClientTick` handler (identical logic)
- Server/proxy: **Out of scope** — anti-AFK is inherently a client-side feature

### 4.10 Void Fall Protection

**Behavior:** Automatically sends a command when the player falls below a configurable Y level (e.g., `/spawn` when falling into the void).

**Config:**
```json
{
  "voidFall": {
    "serversearch": ".*skyblock.*",
    "yLevel": -20,
    "command": "/spawn"
  }
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `serversearch` | `String` (regex) | `null` (all servers) | Only activate on matching servers |
| `yLevel` | `double` | `-20` | Y coordinate threshold — triggers when player Y ≤ this value |
| `command` | `String` | — | Chat message or command to send |

**De-duplicate:** Uses a `stillInVoid` flag so the command is sent only once per void fall (resets when player rises above `yLevel`).

**Platform behavior:**

| Platform | Void fall |
|----------|----------|
| Fabric | ✅ (in world tick handler) |
| Forge | ❌ Not implemented (1.21.4 has `stillInVoid` field but no tick logic) |
| NeoForge | ❌ Not implemented (has `stillInVoid` field but no tick logic) |
| Server/proxy | ❌ N/A |

**Parity target:**
- Forge/NeoForge: Implement in `onClientTick` handler
- Server/proxy: **Out of scope** — client knows its own Y position in real-time; server-side Y checking has tick-rate latency and can't send commands as the player

### 4.11 Toggle / Disable

**Behavior:** Runtime toggle that disables all rule processing without unloading the mod.

**Platform behavior:**

| Platform | Toggle mechanism |
|----------|-----------------|
| Fabric | `config.setDisabled()` — checked in mixin + tick handler |
| Forge | `config.setDisabled()` — checked in event handler |
| NeoForge | `config.setDisabled()` — checked in event handler + tick |
| Spigot | `/flowchat toggle` command |
| BungeeCord | ❌ No command registered |
| Velocity | ❌ No command registered |

**Parity target:**
- BungeeCord/Velocity: Register a `/flowchat` command for reload + toggle
- All client mods: Consider keybind or in-game command for toggle (currently only settable programmatically)

### 4.12 Server IP Tracking

**Behavior:** Client mods track the current server IP for `serversearch` rule filtering.

**Detection:** On world tick, if connected to a server, read the server IP from `getCurrentServer()` / `getCurrentServerData()`. If singleplayer, set to `"singleplayer"`.

**Server/proxy:** Uses a static identifier string (e.g., `"CraftServer:25565"`, `"bungee:25577"`, `"velocity:25565"`).

### 4.13 DISGUISED_CHAT Packet Handling

**Behavior:** MC 1.19.3+ introduced `DISGUISED_CHAT` packets for unsigned server-generated messages that look like player chat. Server-side interceptors must handle this in addition to `SYSTEM_CHAT_MESSAGE`.

**Platform behavior:**

| Platform | DISGUISED_CHAT |
|----------|---------------|
| Fabric | ❌ Not needed (Mixin intercepts at ChatHud level, after packet decode) |
| Forge/NeoForge | ❌ Not needed (event fires after packet decode) |
| Spigot | ✅ `handleDisguisedChat()` |
| BungeeCord | ✅ `handleDisguisedChat()` |
| Velocity | ✅ `handleDisguisedChat()` |

### 4.14 Overlay Message Filtering (Server-side)

**Behavior:** The PacketEvents interceptor skips `SYSTEM_CHAT_MESSAGE` packets where `isOverlay() == true` (action bar messages from other plugins). This prevents FlowChat from processing other plugins' action bar text.

---

## 5. Platform Parity Matrix

### 5.1 Current State

| Feature | Fabric | Forge 1.21 | Forge ≤1.14 | NeoForge | Spigot | Bungee | Velocity |
|---------|:------:|:----------:|:-----------:|:--------:|:------:|:------:|:--------:|
| Incoming rules | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Text replacement | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Color codes (& → §) | ✅ | ✅ | ✅ | ✅ | ❌→✅ | ❌→✅ | ❌→✅ |
| Message cancel (in) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Sound (named+arbitrary) | ✅ 8 | ❌→✅ | ⚠️→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ |
| Toast/notification | ✅ AB | ✅ Toast | ❌→✅ | ✅ Toast | ✅ AB | ✅ AB | ✅ AB |
| `notifyStyle` field | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ |
| Auto-response | ✅ | ✅ | ❌→✅ | ✅ | ❌→✅ | ❌→✅ | ❌→✅ |
| Outgoing rules | ✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ |
| Value stacking | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Tag variables | ⚠️→✅ | ⚠️→✅ | ⚠️→✅ | ⚠️→✅ | ⚠️→✅ | ⚠️→✅ | ⚠️→✅ |
| Anti-AFK | ✅ | ❌→✅ | ❌→✅ | ❌→✅ | — | — | — |
| Void fall | ✅ | ❌→✅ | ❌→✅ | ❌→✅ | — | — | — |
| Config hot-reload | ✅ | ✅ | ⚠️→✅ | ✅ | ✅ | ❌→✅ | ❌→✅ |
| Toggle command | — | — | — | — | ✅ | ❌→✅ | ❌→✅ |
| Field aliases | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ |
| Secure profile check | N/A | N/A | N/A | N/A | ❌→✅ | ❌→✅ | ❌→✅ |
| DISGUISED_CHAT | N/A | N/A | N/A | N/A | ✅ | ✅ | ✅ |
| Server filter | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

**Legend:** ✅ = done, ❌→✅ = needs implementation, ⚠️→✅ = partial needs fix, — = N/A, AB = action bar

### 5.2 Parity Targets

**Client mods (Forge/NeoForge) — bring to Fabric feature level:**
- [ ] Sound: expand to full 8-sound palette + arbitrary Identifier
- [ ] Forge legacy (≤1.11): toast degrades to local chat; (1.12–1.13): degrades to actionbar; (1.14+): full toast
- [ ] Forge legacy (≤1.14): add auto-response
- [ ] Forge/NeoForge: add outgoing chat rules (Forge 1.12.2+: `ClientChatEvent`, Forge ≤1.11.2: coremod/ASM, NeoForge: `ClientChatEvent`)
- [ ] Forge/NeoForge: add anti-AFK
- [ ] Forge/NeoForge: add void fall protection (wire up existing `stillInVoid` field)
- [ ] All: fix `{username}` and `{servername}` tag variables in processor pipeline
- [ ] All: add `notifyStyle` field support (actionbar vs toast)
- [ ] All: add field name aliases (`pattern`/`sound`/`toast`/`respond`/`server`)
- [ ] All: remove `localOnly` dead code from `FlowChatRule`

**Server/proxy plugins — add what's architecturally possible:**
- [ ] Color codes: apply `& → §` conversion in replacement text
- [ ] Sound: send sound effect packet via PacketEvents (same alias table as client)
- [ ] Outgoing rules: intercept client→server `CHAT_MESSAGE` / `CHAT` packet, modify freely
- [ ] Enforce `enforce-secure-profile=false` on startup (refuse to enable if `true`)
- [ ] BungeeCord/Velocity: add `/flowchat reload` and `/flowchat toggle` commands
- [ ] Config hot-reload: BungeeCord and Velocity

**Bug fixes (all platforms):**
- [ ] Fix `{username}` and `{servername}` — pass non-null values into `replaceTags()`
- [ ] Add anti-echo recursion depth guard in `MessageProcessor`
- [ ] Update README config examples to use canonical field names

**Out of scope for server/proxy:**
- Anti-AFK (client-initiated action — server can't simulate player input)
- Void fall (client-side Y position + client-initiated command)

---

## 6. Version-Specific API Notes

### 6.1 Chat Interception

| MC Version | Fabric Mixin Target | Forge/NeoForge Event |
|------------|-------------------|---------------------|
| 1.21.x | `addMessage(Text, MessageSignatureData, MessageIndicator)` | `ClientChatReceivedEvent` |
| 1.19.x–1.20.x | `addMessage(Text, MessageSignatureData, MessageIndicator)` | `ClientChatReceivedEvent` |
| 1.16.x–1.18.x | `addMessage(Text)` (1-arg) | `ClientChatReceivedEvent` |
| 1.14.x–1.15.x | `addMessage(Text)` (1-arg) | `ClientChatReceivedEvent` |

**⚠️ CRITICAL (Fabric 1.19+):** Player chat bypasses the 1-arg `addMessage()` and calls the 3-arg overload directly. Must target the 3-arg signature to catch all messages.

### 6.2 Outgoing Chat

| MC Version | Fabric Target | Send Method |
|------------|--------------|-------------|
| 1.19.1+ | `ClientPlayNetworkHandler.sendChatMessage()` | Split: `sendChatMessage()` for chat, `sendCommand()` for `/` commands |
| ≤1.19.0 | `ClientPlayerEntity.sendChatMessage()` | Combined: handles both chat and commands |

### 6.3 Text Construction

| MC Version | Fabric | Forge | NeoForge |
|------------|--------|-------|----------|
| 1.21.x | `Text.of()` | `Component.literal()` | `Component.literal()` |
| 1.19.x–1.20.x | `Text.of()` | `Component.literal()` | `Component.literal()` |
| 1.16.x–1.18.x | `Text.of()` / `new LiteralText()` | `new StringTextComponent()` / `new TextComponent()` | N/A |
| 1.14.x–1.15.x | `new LiteralText()` | `new StringTextComponent()` | N/A |
| 1.12.x and below | N/A | `new TextComponentString()` | N/A |
| 1.8.x and below | N/A | `new ChatComponentText()` | N/A |

### 6.4 Sound API

| MC Version | Sound Reference | Play Method |
|------------|----------------|-------------|
| 1.21.x | `SoundEvents.X.value()` | `getSoundManager().play(SimpleSoundInstance.forUI(...))` |
| 1.19.x–1.20.x | `SoundEvents.X.value()` | `getSoundManager().play(SimpleSoundInstance.forUI(...))` |
| 1.18.x | `SoundEvents.X` (direct) | `getSoundManager().play(...)` |
| ≤1.17.x | `SoundEvents.X` (direct) | `player.playSound(...)` |

### 6.5 Toast / Action Bar API

| MC Version | Toast | Action Bar |
|------------|-------|-----------|
| 1.21.x | `SystemToast.multiline()` + `getToastManager()` | `player.sendMessage(text, true)` |
| 1.20.x–1.21.1 | `SystemToast.multiline()` + `getToasts()` | `player.sendMessage(text, true)` |
| 1.14.x–1.19.x | `SystemToast.multiline()` + `getToasts()` | `player.sendMessage(text, true)` |
| 1.12.x–1.13.x | ❌ No SystemToast | `player.sendStatusMessage(text, true)` |
| ≤1.11.x | ❌ No SystemToast | ❌ No action bar API (use local chat) |

---

## 7. Test Plan Overview

### 7.1 Test Layers

```
┌─────────────────────────────────────────────┐
│  Unit Tests (JUnit 4, pure Java)             │
│  └─ MessageProcessor, FlowChatConfig,        │
│     FlowChatRule, SoundResolver, tags        │
│     Run: ./gradlew :common:test              │
├─────────────────────────────────────────────┤
│  Integration Tests (full server matrix)      │
│  └─ Server plugins: packet interception,     │
│     config reload, command handling          │
│     Matrix: every supported MC version       │
│     Stack: Spigot/Paper + PacketEvents +     │
│            node-minecraft-protocol client    │
│     Proxy: BungeeCord + Velocity per version │
├─────────────────────────────────────────────┤
│  In-Game Tests (runClient + RCON/xdotool)    │
│  └─ Client mods: chat rendering, sound,      │
│     toast, outgoing rules, anti-AFK, void   │
├─────────────────────────────────────────────┤
│  Cross-Version Compilation Matrix            │
│  └─ All 42+ artifacts build successfully     │
└─────────────────────────────────────────────┘
```

### 7.2 Unit Test Scope

These test the **common module** in isolation (no Minecraft dependency):

| Test | Input | Expected |
|------|-------|----------|
| Simple regex match + replace | `search: "hello"`, `replacement: "world"`, message: `"hello there"` | `"world there"` |
| Capture group backreference | `search: "<(\\w+)>"`, `replacement: "[$1]"`, message: `"<Steve>"` | `"[Steve]"` |
| No match → no modification | `search: "xyz"`, message: `"hello"` | `"hello"`, `wasModified() == false` |
| Section sign stripping | Message: `"§aGreen §bBlue"` | Stripped to `"Green Blue"` before matching |
| `toastMe` flag → cancelled | `toastMe: true`, matching message | `cancelled == true` |
| `playSound` flag propagation | `playSound: true, soundName: "bell"` | `playSound == true`, `soundName == "bell"` |
| First sound wins | Two rules both with `playSound: true` | First rule's `soundName` is used |
| Auto-response (single) | `respondMsg: "gg"`, matching message | `autoResponses == ["gg"]` |
| Auto-response (array) | `respondMsg: ["msg1", "msg2"]`, matching message | `autoResponses == ["msg1", "msg2"]` |
| Auto-response with backreference | `respondMsg: "/msg $1 thanks"`, message matches | Response contains captured group |
| Server filter — match | `serversearch: ".*hypixel.*"`, serverIp: `"mc.hypixel.net"` | Rule fires |
| Server filter — no match | `serversearch: ".*hypixel.*"`, serverIp: `"play.cubecraft.net"` | Rule skipped |
| Server filter — null (all) | `serversearch: null` | Rule fires on any server |
| Value stacking — accumulate | Two messages within expiry window | `$^1` shows sum |
| Value stacking — expire | Two messages separated by > expire_after | `$^1` shows only second value |
| Value stacking — iteration count | Three stacked messages | `$^i == 3` |
| Value stacking — ignore_diffs | Messages differ in ignored group but same in others | Stacked together |
| Tag: `{time}` | Replacement contains `{time}` | Replaced with `HH:mm:ss` |
| Tag: `{serverip}` | Replacement contains `{serverip}`, serverIp set | Replaced with server IP |
| Tag: `{username}` | Replacement contains `{username}`, username passed | Replaced with username |
| Config load — valid JSON | Valid `flowchat.json` | `load() == true`, rules parsed |
| Config load — missing file | No file exists | Default created, `load() == true` |
| Config load — malformed JSON | Invalid JSON | `load() == false`, no crash |
| Config load — malformed rule | Valid JSON but rule has invalid regex | Rule skipped with warning, other rules OK |
| Multiple rules — cumulative | Rule A transforms, Rule B transforms result of A | Both applied in order |
| Disabled flag | `config.setDisabled(true)` | Processing skipped |

### 7.3 Integration Test Scope (Server/Proxy — Full Matrix)

Requires a running MC server with the plugin loaded + headless client via `node-minecraft-protocol`.

**Server version matrix:**

| MC Version | Server JAR | Java | Chat Protocol | Why |
|------------|-----------|------|--------------|-----|
| 1.21.4 | Paper | 21 | Signed (`CHAT_MESSAGE` + `SYSTEM_CHAT_MESSAGE` + `DISGUISED_CHAT`) | Latest |
| 1.20.4 | Paper | 17 | Signed | NeoForge boundary |
| 1.20.1 | Paper | 17 | Signed | Forge/NeoForge fork point |
| 1.19.2 | Paper | 17 | Signed (first signed version) | Chat signing boundary |
| 1.18.2 | Paper | 17 | Unsigned (`CHAT` packet) | Pre-signing baseline |
| 1.16.5 | Paper | 8–16 | Unsigned | Legacy baseline |
| 1.12.2 | Spigot | 8 | Unsigned | Oldest Forge with `ClientChatEvent` |
| 1.8.9 | Spigot | 8 | Unsigned | Hypixel community / oldest practical |

**Proxy matrix:**

| Proxy | Versions | Notes |
|-------|----------|-------|
| BungeeCord | Latest | Protocol-agnostic, one version sufficient |
| Velocity | 3.x latest | Same |

**All servers require:** `online-mode=false`, `enable-rcon=true`, `enforce-secure-profile=false` (1.19.2+), PacketEvents installed.

**Test cases per server version:**

| Test | Method | Expected |
|------|--------|----------|
| Plugin loads without errors | Check server log | `FlowChat X.X.X enabled` |
| PacketEvents initializes | Check server log | No `UnknownDependencyException` |
| Config created on first run | Check filesystem | `flowchat.json` exists in plugin data folder |
| Secure profile check (1.19.2+) | Set `enforce-secure-profile=true`, start server | Plugin refuses to enable with clear error |
| SYSTEM_CHAT interception | RCON `/say test`, observe headless client | Client receives processed message |
| DISGUISED_CHAT interception (1.19.3+) | Send disguised chat packet | Client receives processed message |
| Overlay messages skipped | Send action bar packet | Message unchanged |
| Text replacement | Rule replaces `hello` → `world`, send matching chat | Client receives `world` |
| Message cancellation | Rule cancels matching message | Client does NOT receive message |
| Color code conversion | Rule with `&a` in replacement | Client receives `§a` formatted text |
| Sound packet | Rule with `sound: "bell"` | Client receives `SOUND_EFFECT` packet for `minecraft:block.note_block.bell` |
| Sound arbitrary ID | Rule with `sound: "minecraft:entity.pig.ambient"` | Client receives correct sound packet |
| Toast/action bar (notification) | Rule with `toast: true` | Client receives overlay message |
| Outgoing interception | Headless client sends chat matching outgoing rule | Server receives modified text |
| Outgoing cancellation | Headless client sends chat matching cancel rule | Server does NOT receive message |
| Auto-response | Rule with `respond: "gg"`, send matching chat via RCON | Headless client receives auto-response as player chat |
| Auto-response array | Rule with `respond: ["msg1", "msg2"]` | Both messages received in order |
| Value stacking | Send 3 matching messages within 4s | Third message shows accumulated value |
| Tag `{time}` | Rule with `{time}` in replacement | Replacement contains `HH:mm:ss` |
| Tag `{serverip}` | Rule with `{serverip}` in replacement | Replacement contains server identifier |
| `/flowchat reload` | Modify config, run RCON command | New rules active |
| `/flowchat toggle` | Run RCON command, send chat | Processing disabled/enabled |
| Config field aliases | Config uses `pattern` instead of `search` | Rule works identically |
| Legacy field names | Config uses `msgsearch`, `playSound`, `toastMe` | Rule works identically |

### 7.4 In-Game Self-Test Harness (`/flowchat test`)

Built into every mod JAR. Runs a self-test sequence that exercises platform-specific wiring without external tooling.

**Trigger:** `/flowchat test` command (client-side) or `/flowchat test` via RCON (server-side).

**Test sequence (client mods):**

| # | Test | Method | Pass condition |
|---|------|--------|---------------|
| 1 | Mod loaded | Check `config != null` | Config object exists |
| 2 | Config parse | Load test config with all field types | No exceptions, rules parsed |
| 3 | Field aliases | Parse rule with `pattern`/`sound`/`toast`/`respond`/`server` | Fields resolve correctly |
| 4 | Legacy fields | Parse rule with `search`/`msgsearch`/`playSound`/`soundName`/`toastMe`/`respondMsg`/`serversearch` | Fields resolve correctly |
| 5 | Incoming intercept | Inject fake message through pipeline | `MessageProcessor.Result` returned with expected text |
| 6 | Text replacement | Process message matching a test rule | `processedText` matches expected |
| 7 | Color formatting | Apply `& → §` conversion | Output contains `§` codes |
| 8 | Message cancel | Process message with cancel rule | `cancelled == true` |
| 9 | Sound resolve (named) | `SoundResolver.resolve("bell")` | Returns `minecraft:block.note_block.bell` |
| 10 | Sound resolve (arbitrary) | `SoundResolver.resolve("minecraft:entity.pig.ambient")` | Returns same string |
| 11 | Sound resolve (null) | `SoundResolver.resolve(null)` | Returns default sound |
| 12 | Sound playback | Play resolved sound | No exception (audio output not verifiable programmatically) |
| 13 | Toast/actionbar | Show test notification | No exception |
| 14 | Auto-response | Process message with `respond` rule | `autoResponses` list populated |
| 15 | Value stacking | Process 3 messages rapidly | Accumulated value correct |
| 16 | Tag `{time}` | Replace tag | Contains `HH:mm:ss` pattern |
| 17 | Tag `{serverip}` | Replace tag | Contains current server IP |
| 18 | Tag `{username}` | Replace tag | Contains player name |
| 19 | Server filter match | Rule with `server` matching current IP | Rule fires |
| 20 | Server filter skip | Rule with `server` NOT matching | Rule skipped |
| 21 | Outgoing intercept | (Fabric/Forge 1.12.2+/NeoForge) Send test message through outgoing pipeline | Modified text returned |
| 22 | Anti-AFK config | Parse antiAFK config | Fields parsed correctly |
| 23 | Void fall config | Parse voidFall config | Fields parsed correctly |
| 24 | Disabled toggle | Set disabled, process message | Processing skipped |

**Output:** Results displayed in chat as colored pass/fail lines:
```
[FlowChat Test] 24/24 passed ✓
  ✓ 1. Mod loaded
  ✓ 2. Config parse
  ...
  ✗ 12. Sound playback — java.lang.NoSuchMethodError: getToasts()
```

**Test sequence (server plugins):**

| # | Test | Method | Pass condition |
|---|------|--------|---------------|
| 1 | Plugin loaded | Check PacketEvents API initialized | No null |
| 2 | Config parse | Same as client | Rules parsed |
| 3–6 | Field aliases + legacy | Same as client | Fields resolve |
| 7 | Secure profile check | Read `server.properties` | `enforce-secure-profile=false` |
| 8 | SYSTEM_CHAT intercept | Inject test packet through interceptor | Result modified |
| 9 | DISGUISED_CHAT intercept | Inject test packet | Result modified |
| 10 | Overlay skip | Inject overlay packet | Not processed |
| 11 | Outgoing intercept | Inject client→server chat packet | Result modified |
| 12 | Sound packet build | Build `WrapperPlayServerSoundEffect` for `"bell"` | Valid packet constructed |
| 13 | Color codes | Apply `& → §` in replacement | Output correct |
| 14–20 | Value stacking, tags, server filter, toggle | Same as client | Same |

**Output (server):** Sent to the command sender via chat message.

### 7.5 External In-Game Tests (runClient + xdotool + RCON)

Second verification layer — full end-to-end including rendering. Requires `DISPLAY=:0`, `runClient`, xdotool, RCON, and screenshot analysis.

| Test | Setup | Verification |
|------|-------|-------------|
| Chat replacement visible | Config with replacement rule, receive matching chat | Screenshot shows replaced text |
| Color codes rendered | Replacement contains `&e`, `&b` | Screenshot shows colored text |
| Message cancelled | Rule with `toastMe: true` | Original message NOT in chat log |
| Toast displayed | `toastMe: true` on match | Toast popup visible (or action bar) |
| Sound plays | `playSound: true` | Sound event in log or audio output |
| Auto-response sent | `respondMsg` set, matching message received | Outgoing chat packet in log |
| Outgoing rule modifies | Outgoing rule, player types matching message | Server receives modified text |
| Outgoing rule cancels | Outgoing rule with cancel, player types message | Server does NOT receive message |
| Anti-AFK fires | `antiAFK.afterSeconds: 5`, wait 6s idle | Command sent automatically |
| Void fall fires | `voidFall.yLevel: 0`, tp player to Y=-10 | Command sent automatically |
| Void fall de-dupes | Player stays below yLevel | Command sent only once |
| Config hot-reload | Edit config, pause/resume game | New rules active |
| Server IP tracking | Connect to server, check logs | `serverIp` matches actual server |
| Singleplayer detection | Open singleplayer world | `serverIp == "singleplayer"` |

---

## 8. Known Bugs & Cleanup

1. **`{username}` and `{servername}` tags broken in core pipeline.** `MessageProcessor.process()` calls `replaceTags(replStr, serverIp, null, null)` — username and serverName are always null. Only `FabricChatHelper.replaceTags()` passes non-null values, and it's called separately from the main pipeline.

2. **`localOnly` field is dead code.** Parsed in `FlowChatRule` but never checked by any platform. **Action:** Remove from parser. Existing configs with `localOnly` will have it silently ignored (no breakage).

3. **Forge 1.21.4 has `stillInVoid` field but no void fall logic in tick handler.** The field exists, suggesting it was intended but not implemented. **Action:** Implement void fall in tick handler.

4. **NeoForge 1.21.4 same — `stillInVoid` declared, tick handler doesn't use it.** **Action:** Same fix.

5. **Anti-echo incomplete.** Fabric tracks `lastCmdSent` but only checks it in `ChatHudMixin` for auto-responses. If an auto-response triggers another incoming rule that also has `respondMsg`, infinite loop is possible. **Action:** Add recursion depth guard in `MessageProcessor` (max 1 level of auto-response).

6. **Field name aliases needed.** README shows user-friendly names (`pattern`, `sound`, `toast`) but code only reads legacy names (`search`, `playSound`/`soundName`, `toastMe`). **Action:** Add alias resolution in `FlowChatRule` constructor — canonical names first, legacy names as fallback. Update README to show canonical names. Zero breakage for existing configs.

---

## 9. Glossary

| Term | Definition |
|------|-----------|
| **Incoming rule** | Rule applied to messages received from the server |
| **Outgoing rule** | Rule applied to messages the player sends |
| **Toast** | `SystemToast` popup in top-right corner (1.14+) |
| **Action bar** | Single-line text above the hotbar that fades after ~2s |
| **Value stacking** | Accumulating numeric values from repeated messages within a time window |
| **Tag variable** | Template placeholder (`{username}`, `{time}`, etc.) replaced at processing time |
| **Server filter** | `serversearch` regex that restricts a rule to specific server IPs |
| **PacketEvents** | Third-party library for intercepting MC packets on server/proxy |
| **Mixin** | Bytecode injection framework used by Fabric for hooking MC internals |
