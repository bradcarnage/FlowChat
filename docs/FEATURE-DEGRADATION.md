# Feature Degradation Across Minecraft Versions

FlowChat supports Minecraft 1.7.2 through 26.1.2. Not all features are available on every version. When a feature isn't supported, FlowChat degrades gracefully — falling back to simpler alternatives and logging a warning.

## Degradation Table

| Feature | Min Version | Fallback | Notes |
|---------|-------------|----------|-------|
| **Toast notifications** | 1.21.4+ (Fabric/Forge) | Chat message | `SystemToast` API not available before 1.21.4 |
| **Advancement popup** | 1.21.4+ | Chat message | Custom advancement display requires newer API |
| **Action bar text** | 1.16+ | Chat message | `sendMessage(text, true)` overlay not available pre-1.16 |
| **Sound playback** | 1.14.4+ | XP "ding" fallback | Invalid/unknown sound IDs fall back to `entity.experience_orb.pickup` + log warning |
| **Color-aware regex** | All versions | N/A (always works) | Uses `§` codes which exist in all versions |
| **JSON component matching** | 1.14.4+ | N/A | Raw JSON chat components exist in all supported versions |
| **Value stacking** | All versions | N/A (always works) | Pure core logic, no version-specific APIs |
| **Auto-response** | All versions | N/A (always works) | Pure core logic |
| **Anti-AFK** | All versions | N/A | Uses basic player movement packets |
| **Void fall** | All versions | N/A | Uses player Y coordinate check |

## How Degradation Works

### Notification Style (`notifyStyle`)

The `notifyStyle` field in a rule determines how matched messages are displayed:

1. **`"toast"`** — System toast popup (1.21.4+)
2. **`"advancement"`** — Achievement/advancement popup (1.21.4+)
3. **`"actionbar"`** — Action bar overlay text (1.16+)
4. **Chat message** — Universal fallback

The platform layer checks version capabilities and falls through this chain:

```
toast → advancement → actionbar → chat message
```

Each fallback logs a warning:
```
[FlowChat] Toast notifications not available on 1.16.5 — falling back to action bar
[FlowChat] Action bar not available on 1.14.4 — falling back to chat message
```

### Sound Playback

When `playSound` is true on a rule:

1. Resolve the sound ID via `SoundResolver` (aliases like "ding", "bell", or full IDs like `minecraft:block.note_block.bell`)
2. Attempt to play the resolved sound
3. If the sound ID is invalid or not registered in this MC version → play XP "ding" (`entity.experience_orb.pickup`) + log warning

```
[FlowChat] Sound 'minecraft:block.decorated_pot.shatter' not found — playing default XP ding
```

### Color Codes

`formatColors()` converts `&` codes to `§` codes across all versions. Edge cases:
- `&z` — invalid code, not converted
- `&A` — uppercase, not converted (only lowercase `a-f`, `k-o`, `r`, `0-9`)
- `&&a` — escaped ampersand, not converted

## Testing

All core logic tests pass on Java 8+ regardless of Minecraft version. Platform-specific features (toast, advancement, actionbar, sound playback) are tested per-loader in their respective modules.

Run core tests:
```bash
./gradlew :common:test
```
