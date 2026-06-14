# FlowChat v2.2.0 In-Game Test Plan

## JARs to Test

| Loader | JAR | MC Version | Java | Type |
|--------|-----|------------|------|------|
| Fabric | `flowchat-fabric-2.2.0.jar` | 1.16.5 | 8+ | Client-side |
| Forge | `flowchat-forge-2.2.0.jar` | 1.16.5 | 8+ | Client-side |
| Spigot | `flowchat-spigot-2.2.0.jar` | 1.21.4+ | 17+ | Server plugin |
| BungeeCord | `flowchat-bungee-2.2.0.jar` | 1.21+ | 17+ | Proxy plugin |
| Velocity | `flowchat-velocity-2.2.0.jar` | 3.4+ | 17+ | Proxy plugin |

**Dependencies:** Server plugins require [PacketEvents](https://github.com/retrooper/packetevents) installed.

---

## Test Config (`flowchat.json`)

Place in config directory (Fabric: `config/`, Forge: `config/`, Spigot/Bungee/Velocity: `plugins/FlowChat/`):

```json
{
  "incoming": [
    {
      "search": "\\btest123\\b",
      "replacement": "§a[MATCHED]§r test123",
      "comment": "T1: basic regex match + color replacement"
    },
    {
      "search": "\\bhello\\b",
      "replacement": "§6[HELLO]§r",
      "playSound": true,
      "soundId": "ding",
      "comment": "T2: sound alias 'ding'"
    },
    {
      "search": "\\bbell\\b",
      "replacement": "§e[BELL]§r",
      "playSound": true,
      "soundId": "bell",
      "comment": "T3: sound alias 'bell'"
    },
    {
      "search": "\\blevelup\\b",
      "replacement": "§b[LEVEL UP]§r",
      "playSound": true,
      "soundId": "levelup",
      "comment": "T4: sound alias 'levelup'"
    },
    {
      "search": "\\bsilent\\b",
      "replacement": "§7[SILENT]§r",
      "playSound": true,
      "soundId": "none",
      "comment": "T5: 'none' should suppress sound"
    },
    {
      "search": "\\bcustomsound\\b",
      "replacement": "§d[CUSTOM]§r",
      "playSound": true,
      "soundId": "minecraft:entity.villager.ambient",
      "comment": "T6: full resource location sound"
    },
    {
      "search": "\\bcancel_this\\b",
      "cancelled": true,
      "comment": "T7: message cancellation"
    },
    {
      "search": "\\btoast_test\\b",
      "replacement": "§5[TOAST]§r",
      "toast": true,
      "notifyStyle": "actionbar",
      "comment": "T8: actionbar notification"
    },
    {
      "search": "\\bserver_only\\b",
      "replacement": "§c[SERVER MATCH]§r",
      "serverSearch": "localhost|127\\.0\\.0\\.1",
      "comment": "T9: server IP filter (should match on localhost)"
    },
    {
      "search": "\\bno_match_server\\b",
      "replacement": "§c[SHOULD NOT APPEAR]§r",
      "serverSearch": "some\\.other\\.server",
      "comment": "T10: server IP filter (should NOT match)"
    }
  ],
  "outgoing": [
    {
      "search": "\\b!test\\b",
      "replacement": "outgoing_modified",
      "comment": "T11: outgoing message rewrite"
    },
    {
      "search": "\\b!cancel\\b",
      "cancelled": true,
      "comment": "T12: outgoing message cancellation"
    }
  ]
}
```

---

## Test Checklist

### Phase 1: Installation & Startup
- [ ] **1.1** Mod/plugin loads without errors in console
- [ ] **1.2** Version prints correctly (`FlowChat 2.2.0`)
- [ ] **1.3** Config file created on first run (if missing)
- [ ] **1.4** Config loads without parse errors

### Phase 2: Incoming Message Rules (Client-Side: Fabric/Forge)
- [ ] **2.1** T1: Type `test123` in chat → see `[MATCHED] test123` in green
- [ ] **2.2** T2: Have another player say `hello` → see `[HELLO]` in gold + hear ding sound
- [ ] **2.3** T3: Receive `bell` → see `[BELL]` in yellow + hear bell sound
- [ ] **2.4** T4: Receive `levelup` → see `[LEVEL UP]` in aqua + hear level-up sound
- [ ] **2.5** T5: Receive `silent` → see `[SILENT]` in gray + NO sound
- [ ] **2.6** T6: Receive `customsound` → see `[CUSTOM]` in purple + hear villager sound
- [ ] **2.7** T7: Receive `cancel_this` → message does NOT appear in chat
- [ ] **2.8** T8: Receive `toast_test` → see `[TOAST]` in action bar
- [ ] **2.9** T9: On localhost, receive `server_only` → see `[SERVER MATCH]`
- [ ] **2.10** T10: On localhost, receive `no_match_server` → message unchanged

### Phase 3: Outgoing Message Rules (Client-Side: Fabric/Forge)
- [ ] **3.1** T11: Type `!test` → sent as `outgoing_modified` (verify in server log or other client)
- [ ] **3.2** T12: Type `!cancel` → message NOT sent at all

### Phase 4: Server Plugin (Spigot)
- [ ] **4.1** Plugin loads, `FlowChat 2.2.0 enabled` in console
- [ ] **4.2** `/flowchat reload` reloads config, prints confirmation
- [ ] **4.3** `/flowchat toggle` disables/enables processing
- [ ] **4.4** `/flowchat test` runs self-test, all pass
- [ ] **4.5** Incoming rules process chat packets (visible to all connected clients)
- [ ] **4.6** Non-op players cannot use `/flowchat` (permission: `flowchat.admin`)

### Phase 5: Proxy Plugins (BungeeCord/Velocity)
- [ ] **5.1** Plugin loads without errors
- [ ] **5.2** Chat packets intercepted across backend servers
- [ ] **5.3** Rules apply to messages passing through proxy

### Phase 6: Edge Cases
- [ ] **6.1** Empty config `{"incoming":[],"outgoing":[]}` → no errors, no processing
- [ ] **6.2** Malformed rule (missing `search`) → skipped with warning, others still work
- [ ] **6.3** Invalid sound ID → no crash, sound silently skipped
- [ ] **6.4** Rapid messages → no lag/freezing
- [ ] **6.5** `/flowchat toggle` → disabled state persists until toggled back
- [ ] **6.6** Config hot-reload (edit file, `/flowchat reload` or wait for world tick on client)
- [ ] **6.7** Color codes `&6` format correctly as `§6`
- [ ] **6.8** Regex with groups/backreferences works in replacement

### Phase 7: Cross-Version (if applicable)
- [ ] **7.1** Fabric 1.16.5 client connects to vanilla server → mod works
- [ ] **7.2** Forge 1.16.5 client connects to vanilla server → mod works
- [ ] **7.3** Spigot 1.21.4 server with PacketEvents → plugin works
- [ ] **7.4** Multiple rules match same message → all applied in order

---

## Pass Criteria
- All Phase 1-3 tests pass on both Fabric AND Forge
- All Phase 4 tests pass on Spigot
- Phase 5 tests pass on at least one proxy (BungeeCord or Velocity)
- No crashes, no silent failures in any phase
- Sound aliases (ding, bell, levelup, none) resolve correctly on both Fabric and Forge
