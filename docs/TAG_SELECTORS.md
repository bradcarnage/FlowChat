# Tag Selectors

Tag selectors are live-data placeholders you can use in replacement strings. Prefix any
tag with `@` and FlowChat swaps it for real game data at runtime - your coordinates,
held item, nearby players, whatever.

```json
{
  "search": "^/pos$",
  "replacement": "I'm at @c holding @i",
  "description": "Share coords and held item"
}
```

Typing `/pos` in chat sends something like: `I'm at 120 64 -340 holding Diamond Sword`

## Client-Side Only

Tag selectors only work on the client (Fabric, Forge, NeoForge). Server plugins pass a
null TagContext so `@` tags stay unresolved. The older curly-brace tags (`{username}`,
`{serverip}`, `{time}`) work everywhere.

---

## Standalone Tags

These resolve to player/server/world data. No prefix needed.

| Tag | Resolves To | Example Output |
|-----|-------------|----------------|
| `@s` | Your username | `Steve` |
| `@su` | Your UUID | `069a79f4-...` |
| `@ip` | Server IP | `mc.example.com` |
| `@t` | Current time (HH:mm:ss) | `14:32:07` |
| `@c` | Your coordinates | `120 64 -340` |
| `@l` | Nearby real player count | `3` |
| `@a` | All nearby player names | `Alex, Steve, Notch` |
| `@au` | All nearby player UUIDs | `069a..., 1a2b...` |
| `@lu` | Same as `@au` | `069a..., 1a2b...` |
| `@p` | Nearest player name | `Alex` |
| `@pu` | Nearest player UUID | `069a79f4-...` |

### Notes

- "Real players" filters out NPCs and bots based on timing heuristics.
- `@l` and `@c` are also armor/coordinate prefixes, but standalone tags always win.
  So `@l` = player count, `@li` = leggings item name.
- `@p` currently returns the first real player found (true distance sorting is planned).

---

## Item Tags

Item tags pull data from your inventory. The base (unprefixed) version reads your
**main hand** item.

| Suffix | Resolves To | Example Output |
|--------|-------------|----------------|
| `@i` | Item display name | `Diamond Sword` |
| `@in` | Item display name (alias) | `Diamond Sword` |
| `@ic` | Stack count | `64` |
| `@id` | Namespaced ID | `minecraft:diamond_sword` |
| `@d` | Full item details | `Diamond Sword [Sharpness V, Unbreaking III]` |
| `@ie` | Full item details (alias) | same as `@d` |
| `@du` | Durability | `1561/1561` |
| `@dur` | Durability (alias) | `1561/1561` |
| `@idu` | Durability (alias) | `1561/1561` |

### Item Details Format

The `@d` / `@ie` tag builds a string from all available item metadata:

```
Diamond Sword [Sharpness V, Unbreaking III] (Battle-ready) {+7 Attack Damage}
              ^--- enchantments               ^--- lore     ^--- attributes
```

Sections only appear if the item has that data. A plain dirt block just shows `Dirt`.

### Empty Slots

If the slot is empty (air), all item tags resolve to `""` (empty string). This counts
as resolved - it won't trigger unresolved behavior.

---

## Slot Prefixes

Add a prefix before the item suffix to read from a different slot.

| Prefix | Slot | Example |
|--------|------|---------|
| *(none)* | Main hand | `@i`, `@dur` |
| `o` | Offhand | `@oi`, `@odur` |
| `h` | Helmet | `@hi`, `@hid` |
| `c` | Chestplate | `@ci`, `@cdur` |
| `l` | Leggings | `@li`, `@ldur` |
| `b` | Boots | `@bi`, `@bdur` |
| `0`-`35` | Inventory slot | `@0i`, `@23ic` |

### Inventory Slot Map

```
Slots  0-8:   Hotbar (0 = leftmost)
Slots  9-35:  Main inventory (top-left to bottom-right)
Slot  36:     Boots
Slot  37:     Leggings
Slot  38:     Chestplate
Slot  39:     Helmet
Slot  40:     Offhand
```

Slots 36-40 are better accessed via letter prefixes (`b`, `l`, `c`, `h`, `o`).
Numeric slots outside 0-35 are invalid and trigger unresolved behavior.

### Prefix Ambiguity

Some letters overlap with standalone tags:

- `@c` = coordinates (standalone wins). `@ci` = chestplate item name.
- `@l` = player count (standalone wins). `@li` = leggings item name.
- `@b` alone is invalid (returns unresolved). `@bi` = boots item name.

---

## TagSettings

Configure tag behavior with a `tagSettings` object in your config root:

```json
{
  "tagSettings": {
    "unresolvedBehavior": "cancel",
    "unresolvedFallback": "N/A",
    "durabilityFormat": "current/max",
    "coordinateFormat": "x y z",
    "multiPlayerSeparator": ", "
  },
  "incoming": [...],
  "outgoing": [...]
}
```

### unresolvedBehavior

What happens when a tag can't resolve (e.g. `@p` with no nearby players).

| Value | Behavior |
|-------|----------|
| `cancel` | **Default.** Cancel the entire message. Nothing is sent. |
| `passthrough` | Leave the `@tag` text as-is in output. |
| `strip` | Remove the tag silently (replace with empty string). |
| `fallback` | Replace with `unresolvedFallback` value. |

### durabilityFormat

How `@du` / `@dur` / `@idu` display durability.

| Value | Example Output |
|-------|----------------|
| `current/max` | `1200/1561` (default) |
| `current` | `1200` |
| `max` | `1561` |
| `percent` | `76%` |

Items without durability (dirt, sticks) resolve to `""`.

### coordinateFormat

How `@c` displays coordinates.

| Value | Example Output |
|-------|----------------|
| `x y z` | `120 64 -340` (default) |
| `x, y, z` | `120, 64, -340` |
| `x y z [dim]` | `120 64 -340 [overworld]` |

Coordinates are rounded to integers.

### multiPlayerSeparator

String between names/UUIDs in `@a`, `@au`, `@lu`. Default: `", "`.

---

## Examples

### Share your coordinates

```json
{
  "search": "^/coords$",
  "replacement": "I'm at @c",
  "description": "Type /coords to share your position"
}
```

Output: `I'm at 120 64 -340`

### Show held item

```json
{
  "search": "^/item$",
  "replacement": "Holding: @i (@dur durability)",
  "description": "Type /item to show what you're holding"
}
```

Output: `Holding: Diamond Sword (1200/1561 durability)`

### Full armor check

```json
{
  "search": "^/gear$",
  "replacement": "Gear: @hi | @ci | @li | @bi | Hand: @i | Off: @oi",
  "description": "Type /gear to list all equipped items"
}
```

Output: `Gear: Iron Helmet | Diamond Chestplate | Iron Leggings | Diamond Boots | Hand: Diamond Sword | Off: Shield`

### Nearby players

```json
{
  "search": "^/nearby$",
  "replacement": "Players nearby (@l): @a",
  "description": "Type /nearby to see who's around you"
}
```

Output: `Players nearby (3): Alex, Steve, Notch`

### Graceful fallback for missing data

```json
{
  "tagSettings": {
    "unresolvedBehavior": "fallback",
    "unresolvedFallback": "N/A"
  }
}
```

With this config, `@p` shows `N/A` when nobody is nearby instead of cancelling the
message.

### Inventory slot inspection

```json
{
  "search": "^/slot (\\d+)$",
  "replacement": "Slot $1: @$1i",
  "description": "Check what's in a specific inventory slot"
}
```

**Note:** This example is illustrative - the `$1` in `@$1i` won't dynamically resolve
because tag selectors run on the already-substituted replacement string. For dynamic
slot access, you'd need one rule per slot.

---

## Processing Order

1. Regex match + group substitution (`$1`, `$2`, etc.)
2. Curly-brace tags (`{username}`, `{serverip}`, `{time}`)
3. `@` tag selectors (this system)

All three run on the replacement string, not the match pattern.
