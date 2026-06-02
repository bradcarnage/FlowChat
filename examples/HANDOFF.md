# FlowChat Big Example Config — Agent Handoff

## What This Is

A comprehensive `flowchat.json` example config at `examples/big_example_config.json` that demonstrates **every single feature** of FlowChat v2.1. 52 rules (42 incoming, 10 outgoing) covering all 53 tracked features.

## Project Context

- **Repo:** `~/Developer/FlowChat`
- **Branch with source:** `multiplatform/1.21.5` (use `git checkout --force multiplatform/1.21.5`)
- **Branch with examples dir:** `master` (untracked files — not committed)
- **Config file location in-game:** `.minecraft/config/flowchat.json` (client) or `plugins/FlowChat/flowchat.json` (server)
- **Source package:** `computer.brads.flowchat.core`
- **⚠ `read_file` and `search_files` tools fail for this repo** — use `terminal` with `cat`/`find` instead

## Key Source Files (on `multiplatform/1.21.5`)

| File | Purpose |
|------|---------|
| `common/src/.../FlowChatConfig.java` | Loads `flowchat.json`, parses incoming/outgoing rule arrays, antiAFK, voidFall, disabled flag |
| `common/src/.../FlowChatRule.java` | Rule model — all fields, aliases, pattern compilation |
| `common/src/.../MessageProcessor.java` | Rule evaluation pipeline, tag replacement, value stacking, auto-responses |
| `common/src/.../SoundResolver.java` | Named sound aliases → full MC identifiers |
| `common/src/.../FlowChatTestRunner.java` | 34 in-process tests validating all core logic |
| `docs/DESIGN-SPEC.md` | Full design spec with platform parity tables |
| `example_rules.json` | Original smaller example (repo root) |

## Complete Feature Inventory

### Config Sections
- `incoming[]` — rules for messages received from server
- `outgoing[]` — rules for messages player sends (pre-send transform)
- `antiAFK` — `{afterSeconds, command}` auto-command before idle kick
- `voidFall` — `{yLevel, command}` emergency teleport on void fall

### Rule Fields (canonical → aliases)
| Canonical | Aliases | Type | Default | Description |
|-----------|---------|------|---------|-------------|
| `pattern` | `search`, `msgsearch` | String (regex) | `""` | Java regex to match message |
| `replacement` | `msgreplacement` | String | `"$0"` | Replacement text with `$1` backrefs + tags |
| `server` | `serversearch` | String (regex) | null (all) | Server IP filter; `"singleplayer"` for local |
| `toast` | `toastMe` | boolean | false | Move message to notification overlay |
| `notifyStyle` | — | String | `"actionbar"` | `"actionbar"`, `"toast"`, `"advancement"` |
| `sound` | `playSound`(bool), `soundName`(str) | String/boolean | null | Sound on match (see aliases below) |
| `respond` | `respondMsg` | String or String[] | null | Auto-send message(s) as player |
| `valuestack` | — | Object | null | Numeric value accumulation config |
| `colorAware` | — | boolean | false | Match against text WITH §codes preserved |
| `matchJson` | — | boolean | false | Match against raw JSON component string |

### Sound Aliases
| Alias | Minecraft Sound |
|-------|----------------|
| `ding`, `orb` | `minecraft:entity.experience_orb.pickup` |
| `levelup`, `level` | `minecraft:entity.player.levelup` |
| `anvil` | `minecraft:block.anvil.land` |
| `note`, `bell` | `minecraft:block.note_block.bell` |
| `click` | `minecraft:ui.button.click` |
| `pop` | `minecraft:entity.item.pickup` |
| `none`, `silent` | *(no sound)* |
| `true` (boolean) | default ding |
| Any string with `:` | passthrough (`minecraft:entity.pig.ambient`) |
| Any string without `:` | auto-prefixed (`entity.cow.ambient` → `minecraft:entity.cow.ambient`) |

### Tag Variables (in replacement strings)
`{username}`, `{serverip}`, `{servername}`, `{time}` (HH:mm:ss)

### Value Stacking Object
```json
{
    "stack_values": [2, 4],
    "ignore_diffs": [1],
    "expire_after": 4,
    "seperate_float_with": "."
}
```
- `$^N` in replacement = stacked sum of capture group N
- `$^i` = iteration count since last expiry
- `expire_after` = seconds before stack resets (default 4)

### Message Cancellation
Set `replacement` to `"pleasecancelthismessage"` — the platform handler hides the message entirely.

### Processing Behavior
- Rules evaluated **in order**, cumulative (output feeds next rule)
- Toast flag is **sticky** (once true, stays true)
- Sound is **first-match-wins** (first rule's sound used)
- `colorAware: false` (default) strips `§X` before matching
- `colorAware: true` preserves `§X` in match text
- `matchJson: true` matches against raw JSON component (skips rule if no JSON available)
- Color codes: `&X` in outgoing replacement converted to `§X` by client platforms

## What the Example Config Covers (53/53)

Every item checked: antiAFK, voidFall, incoming/outgoing arrays, all canonical field names, all 6 legacy aliases (one rule uses all of them), toast true/false, all 3 notifyStyles, sound as boolean/string/alias/full-id/no-namespace/silent, all 9 named sound aliases, single + array respond, full valuestack with all subfields + $^N + $^i, colorAware, matchJson, all 4 tags, § and & color codes, $N backreferences, pleasecancelthismessage, replacement omitted (default $0), server: singleplayer.

## Validation

- JSON: valid (python `json.load` passes)
- All 52 regex patterns: valid (python `re.compile` passes — Java regex is superset)
- Unknown JSON keys (`_comment`, `_section`, `description`): safely ignored by Gson parser
