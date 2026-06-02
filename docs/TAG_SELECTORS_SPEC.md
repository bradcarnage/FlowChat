# FlowChat v2.1.2 — Tag Selector Variables Spec

## Overview

Tag selectors are replacement variables usable in `replacement`, `respond`, and future string fields. Prefixed with `@`. **Client-side only** (Fabric/Forge/NeoForge). Server plugins (Spigot/Paper/Bungee/Velocity) do NOT resolve these.

## Tags

| Tag | Alias | Description | Example Output |
|-----|-------|-------------|----------------|
| `@s` | — | Local player username | `BradCarnage` |
| `@su` | — | Local player UUID (dashed) | `a1b2c3d4-e5f6-...` |
| `@ip` | — | Current server IP/address | `mc.hypixel.net` or `singleplayer` |
| `@a` | — | All players in `@l` list, separated by configurable delimiter | `Steve, Alex, BradCarnage` |
| `@au` | — | All player UUIDs from `@l`, same delimiter | `uuid1, uuid2, uuid3` |
| `@l` | — | Nearby **real** player count (see Real Player Detection) | `7` |
| `@lu` | — | UUIDs of `@l` players, delimiter-separated | `uuid1, uuid2` |
| `@p` | — | Nearest real player name (closest in `@l` list) | `Steve` |
| `@pu` | — | Nearest real player UUID | `a1b2c3d4-e5f6-...` |
| `@i` | `@in` | Held item name (main hand) | `Diamond Sword` |
| `@ic` | — | Held item stack count (main hand) | `64` |
| `@id` | — | Held item ID (namespaced) | `minecraft:diamond_sword` |
| `@d` | `@ie` | Held item details — name + enchants + lore + attributes | `Diamond Sword [Sharpness V, Unbreaking III]` |
| `@du` | `@dur`, `@idu` | Held item durability, format configurable | `1234/1561` |
| `@c` | — | Player coordinates (format configurable) | `123 64 -456` |
| `@t` | — | Current time (HH:mm:ss) — mirrors `{time}` | `14:32:07` |

## Slot Selector Prefixes

Item tags (`@i`, `@ic`, `@id`, `@d`/`@ie`, `@du`/`@dur`/`@idu`) default to **main hand**. Prefix to target other slots:

| Prefix | Slot | Example Tags |
|--------|------|-------------|
| *(none)* | Main hand (selected hotbar slot) | `@i`, `@ic`, `@d` |
| `o` | Offhand | `@oi`, `@oic`, `@oid`, `@od`, `@odu` |
| `h` | Helmet (armor head) | `@hi`, `@hid`, `@hdu` |
| `c` | Chestplate (armor chest) | `@ci`, `@cid`, `@cdu` |
| `l` | Leggings (armor legs) | `@li`, `@lid`, `@ldu` |
| `b` | Boots (armor feet) | `@bi`, `@bid`, `@bdu` |
| `0`–`8` | Hotbar slots 0–8 | `@0i`, `@3ic`, `@8d` |
| `9`–`35` | Inventory slots 9–35 | `@9i`, `@23ic`, `@35d` |

### Syntax Pattern
```
@[slot_prefix][item_tag_suffix]
```

Where `item_tag_suffix` is one of: `i`, `in`, `ic`, `id`, `d`, `ie`, `du`, `dur`, `idu`

**Armor prefixes (`h`, `c`, `l`, `b`) are ONLY valid when followed by an item suffix.** Bare `@h`, `@b` = unresolved. `@c` alone = coordinates. `@l` alone = player count. No ambiguity.

### Parsing Rules
1. Check for standalone tags first: `@s`, `@su`, `@ip`, `@a`, `@au`, `@l`, `@lu`, `@p`, `@pu`, `@c`, `@t` — these have no item suffix, match exactly
2. Check for prefixed item tags: letter/number prefix + valid item suffix
   - `o` prefix → offhand
   - `h`, `c`, `l`, `b` prefix → armor slot (ONLY if followed by valid suffix)
   - Numeric prefix (greedy) → inventory slot number
   - No prefix → main hand
3. Longest valid suffix wins: `@3id` → slot 3 + suffix `id`, not slot 3 + `i` + leftover
4. Out-of-range numeric slot → unresolved (follows `unresolvedBehavior`)
5. Empty slot → item tags resolve to empty string (not unresolved)

### Slot Mapping
```
Hotbar:    0  1  2  3  4  5  6  7  8
Inventory: 9  10 11 12 13 14 15 16 17
           18 19 20 21 22 23 24 25 26
           27 28 29 30 31 32 33 34 35
Armor:     h(head/39) c(chest/38) l(legs/37) b(feet/36)
Offhand:   o (slot 40)
```

### Examples
| Tag | Meaning |
|-----|---------|
| `@i` | Main hand item name |
| `@oi` | Offhand item name |
| `@oid` | Offhand item ID |
| `@od` | Offhand item details (enchants/lore) |
| `@odu` | Offhand durability |
| `@hi` | Helmet name |
| `@hdu` | Helmet durability |
| `@hd` | Helmet details (enchants/lore) |
| `@ci` | Chestplate name |
| `@cdu` | Chestplate durability |
| `@li` | Leggings name |
| `@ldu` | Leggings durability |
| `@bi` | Boots name |
| `@bd` | Boots details (enchants/lore) |
| `@0i` | Hotbar slot 0 item name |
| `@23ic` | Inventory slot 23 stack count |

## Legacy Tags (kept)

`{username}`, `{serverip}`, `{servername}`, `{time}` — still work. New `@s`, `@ip`, `@t` are shorter equivalents but do NOT deprecate the old ones.

## Config: `tagSettings` (new top-level section)

```json
{
  "incoming": [...],
  "outgoing": [...],
  "tagSettings": {
    "multiPlayerSeparator": ", ",
    "durabilityFormat": "current/max",
    "coordinateFormat": "x y z",
    "unresolvedBehavior": "cancel",
    "unresolvedFallback": ""
  }
}
```

### `multiPlayerSeparator`
- **Default:** `", "` (comma-space)
- Used by `@a`, `@au`, `@lu`
- User-changeable, e.g. `"/"` → `Steve/Alex/Brad`

### `durabilityFormat`
- **Default:** `"current/max"` — outputs `1234/1561`
- Alternatives: `"current"` → `1234`, `"percent"` → `79%`, `"max"` → `1561`
- For items without durability: `@du` resolves to empty string

### `detailFormat`
- **Default:** `"name [enchants] (lore)"` — outputs `Diamond Sword [Sharpness V, Unbreaking III] (My Sword)`
- Enchants listed comma-separated in brackets
- Lore lines joined with `, ` in parens
- Attributes appended if present
- Components omitted if empty (no empty `[]` or `()`)

### `coordinateFormat`
- **Default:** `"x y z"` — outputs `123 64 -456`
- Alternatives: `"x, y, z"` → `123, 64, -456`, `"x y z [dim]"` → `123 64 -456 [overworld]`
- Coordinates are rounded to nearest integer

### `unresolvedBehavior`
- **Default:** `"cancel"` — if ANY `@tag` can't resolve, **cancel the entire chat event** (message not sent/replaced)
- `"passthrough"` — leave unresolved `@tag` as literal text (`@p` stays as `@p`)
- `"fallback"` — replace unresolved tags with `unresolvedFallback` value
- `"strip"` — remove unresolved tags (empty string replacement)

### `unresolvedFallback`
- **Default:** `""` (empty)
- Only used when `unresolvedBehavior` = `"fallback"`
- Example: `"?"` → unresolved `@p` becomes `?`

## Real Player Detection (`@l` / `@a` / `@p`)

### Problem
Tab list and entity list include bots, NPCs, and ghost entries. Need to distinguish real humans.

### Algorithm: Movement Packet Counter
- Track movement packets per player entity within render distance
- **Threshold:** ≥5 movement packets observed in last 5 minutes
- **Decay:** 1 observed packet removed per minute (rolling window)
- Player must be a `PlayerEntity` (or platform equivalent), not any mob/NPC

### Data Structure
```java
// Per-player tracking in common/core
Map<UUID, PlayerActivityTracker> trackedPlayers;

class PlayerActivityTracker {
    UUID uuid;
    String name;
    long[] packetTimestamps; // circular buffer, last 5 min
    int packetCount;         // count within window
    
    boolean isRealPlayer() {
        pruneOldPackets(); // remove > 5 min old
        return packetCount >= 5;
    }
}
```

### Platform Integration
Each client platform (Fabric/Forge/NeoForge) hooks player movement events to feed the tracker:
- **Fabric:** Mixin into `ClientPlayNetworkHandler` or entity tick
- **Forge:** `EntityEvent` / network handler
- **NeoForge:** Same as Forge with NeoForge event bus

### Range
`@l` counts ALL real players in the loaded world (no distance cap — if client can see their entity, they count). `@p` picks closest by Euclidean distance from local player.

## Tag Resolution Flow

1. Rule matches message via `pattern`
2. `replacement` string checked for `@` tags
3. Each tag resolved using client-side data:
   - `@s`/`@su` → `MinecraftClient.player`
   - `@ip` → stored `serverIp`
   - `@a`/`@au`/`@l`/`@lu`/`@p`/`@pu` → `PlayerActivityTracker` map
   - `@i`/`@in` → item display name
   - `@ic` → item stack count
   - `@id` → item namespaced ID
   - `@d`/`@ie` → item details (name + enchants + lore + attrs)
   - `@du`/`@dur`/`@idu` → item durability
   - `@c` → player position
   - `@t` → `LocalTime.now()`
4. If any tag unresolved → apply `unresolvedBehavior`
5. Resolved string passed to next rule (cumulative pipeline, same as existing)

## Where Tags Are Resolved

Tags resolve in `MessageProcessor.replaceTags()` (extended). This is in `common/` so logic is shared. Platform code passes a `TagContext` object with client-side data.

### New: `TagContext`
```java
public class TagContext {
    String username;
    String playerUuid;
    String serverIp;
    String serverName;
    Map<UUID, PlayerActivityTracker> nearbyPlayers;
    double playerX, playerY, playerZ;
    ItemData[] inventorySlots;      // slots 0–39 (hotbar + inv + armor)
    ItemData offhandItem;           // offhand (slot 40)
    int selectedSlot;               // current hotbar selection (0–8)
    TagSettings settings;           // from config
}

public class ItemData {
    String displayName;             // e.g. "Diamond Sword"
    String namespacedId;            // e.g. "minecraft:diamond_sword"
    int count;                      // stack size
    int durability;                 // current
    int maxDurability;              // max
    boolean hasDurability;
    List<String> enchantments;      // e.g. ["Sharpness V", "Unbreaking III"]
    List<String> lore;              // lore lines
    List<String> attributes;        // attribute modifiers
    boolean isEmpty;                // true if slot is empty (Air)
}
```

Platform code constructs `TagContext` each tick or on-demand and passes to processor.

## Server-Side Behavior

On server plugins (Spigot/Paper/Bungee/Velocity):
- `@` tags are NOT resolved (no client-side data available)
- `unresolvedBehavior` applies — default `"cancel"` means rules with `@tags` effectively become no-ops on servers
- Users targeting server should use `"passthrough"` or avoid `@tags` in server rules

## Files to Modify

### `common/` (shared logic)
- `MessageProcessor.java` — extend `replaceTags()` to accept `TagContext`, resolve all `@` tags
- `FlowChatConfig.java` — parse `tagSettings` section
- `FlowChatRule.java` — no changes needed (tags are in replacement strings, not rule fields)
- **NEW** `TagContext.java` — data carrier for client-side state
- **NEW** `TagSettings.java` — parsed config for tag behavior
- **NEW** `PlayerActivityTracker.java` — real player detection logic
- `FlowChatTestRunner.java` — add tag resolution tests

### `fabric/`
- `FlowChatFabric.java` — construct `TagContext` from MC client state
- `mixin/ChatHudMixin.java` — pass `TagContext` to processor
- `mixin/OutgoingChatMixin.java` — pass `TagContext` to processor
- **NEW** movement tracking hook (mixin or event)

### `forge/`
- `FlowChatForge.java` — construct `TagContext`, movement tracking
- Chat event handler — pass `TagContext`

### `neoforge/`
- `FlowChatNeoForge.java` — construct `TagContext`, movement tracking
- Chat event handler — pass `TagContext`

### Server plugins
- No changes. Existing `replaceTags()` signature kept as backward-compatible overload.

## Test Cases (to add to FlowChatTestRunner)

1. `@s` resolves to username
2. `@ip` resolves to server IP
3. `@t` resolves to time format HH:mm:ss
4. `@i`/`@in` resolves to item name
5. `@ic` resolves to stack count number
6. `@id` resolves to namespaced item ID
7. `@d`/`@ie` resolves to item details with enchants + lore
8. `@d` with no enchants/lore → just item name
9. `@du` with `durabilityFormat=current/max` → `"100/200"`
10. `@du` with `durabilityFormat=percent` → `"50%"`
11. `@du`/`@dur`/`@idu` all resolve identically
12. `@du` on item without durability → empty string
13. `@a` with 3 players and `, ` separator → `"A, B, C"`
14. `@a` with `/` separator → `"A/B/C"`
15. `@l` returns count of real players only
16. `@p` returns closest player name
17. `@c` with default format → `"123 64 -456"`
18. Unresolved tag with `cancel` → result.cancelled = true
19. Unresolved tag with `passthrough` → literal `@p` in output
20. Unresolved tag with `fallback` → fallback string
21. Legacy `{username}` still works alongside `@s`
22. Mixed legacy + new tags in same replacement
23. `@ie` alias resolves same as `@d`
24. `@in` alias resolves same as `@i`
25. PlayerActivityTracker: 4 packets → not real, 5 packets → real
26. PlayerActivityTracker: decay removes old packets after 1 min each
27. Item with enchants + lore → `@d` shows both sections
28. `@oi` resolves offhand item name
29. `@odu` resolves offhand durability
30. `@0i` resolves hotbar slot 0 item name
31. `@23ic` resolves inventory slot 23 stack count
32. `@hi` resolves helmet name
33. `@ci` resolves chestplate name
34. `@li` resolves leggings name
35. `@bi` resolves boots name
36. `@hdu` resolves helmet durability
37. `@bd` resolves boots details (enchants/lore)
38. Numeric slot out of range (e.g. `@99i`) → unresolved
39. Empty slot → item tags resolve to empty string
40. Bare `@h`, `@b` → unresolved (must have item suffix)
41. `@c` alone → coordinates (not chestplate)
42. `@ci` → chestplate item name (prefix + suffix, not coordinates)
43. `@l` alone → player count (not leggings)
44. `@li` → leggings item name (prefix + suffix, not player count)
45. Slot prefix parsing: `@3id` → slot 3 + suffix `id`

## Implementation Order

1. `TagSettings.java` + `TagContext.java` (data classes)
2. `PlayerActivityTracker.java` (real player detection)
3. `FlowChatConfig.java` — parse `tagSettings`
4. `MessageProcessor.java` — extend `replaceTags()` with `TagContext` overload
5. Tests in `FlowChatTestRunner.java`
6. Fabric platform integration (TagContext construction + movement hook)
7. Forge platform integration
8. NeoForge platform integration
9. Update `examples/big_example_config.json` with tag examples
10. Rebuild all 51+ JARs
