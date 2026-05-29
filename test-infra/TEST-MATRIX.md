# FlowChat Test Matrix

## Server Integration Tests — 18 versions × 11 tests = 198/198 ✅

All tests run against Paper servers with FlowChat plugin + PacketEvents 2.7.0 (where supported).
Pre-1.13 servers use Bukkit event-based fallback (AsyncPlayerChatEvent + ServerCommandEvent).

### Test Cases (per version)
1. **Plugin loaded** — FlowChat enables without errors
2. **PacketEvents initialized** / **Bukkit event fallback active** — interception layer ready
3. **Config file created** — flowchat.json written to plugins/FlowChat/
4. **Text replacement** — `hello_test` → `world_test`
5. **Color code conversion** — `&a`/`&b` codes applied
6. **Toast/cancel → overlay** — cancelled message sent as actionbar overlay
7. **Legacy field names** — `search` field aliased to `pattern`
8. **Tag {time}** — `{time}` tag resolved to current time
9. **/flowchat reload** — config hot-reload via command
10. **/flowchat toggle** — enable/disable toggle via command
11. **Plugin self-test** — FlowChat remains enabled after all operations

### Results by Version

| Version | Java | Interception | Tests | Notes |
|---------|------|-------------|-------|-------|
| **1.7.10** | 8 | Bukkit events | **11/11** ✅ | RemoteServerCommandEvent for RCON; no ActionBar API (fallback to chat) |
| **1.8.8** | 8 | Bukkit events | **11/11** ✅ | PE 2.7.0 can't inject (api-version: 1.13) |
| **1.8.9** | 8 | Bukkit events | **11/11** ✅ | Uses Paper 1.8.8 server; client connects as 1.8.8 |
| **1.9.4** | 8 | Bukkit events | **11/11** ✅ | |
| **1.10.2** | 8 | Bukkit events | **11/11** ✅ | |
| **1.11.2** | 8 | Bukkit events | **11/11** ✅ | |
| **1.12.2** | 8 | Bukkit events | **11/11** ✅ | Last pre-1.13 version |
| **1.14.4** | 11 | PacketEvents | **11/11** ✅ | Rejects Java 17+; needs JDK 11 |
| **1.15.2** | 11 | PacketEvents | **11/11** ✅ | |
| **1.16.5** | 11 | PacketEvents | **11/11** ✅ | TranslatableComponent extraction for `say` |
| **1.17.1** | 17 | PacketEvents | **11/11** ✅ | |
| **1.18.2** | 17 | PacketEvents | **11/11** ✅ | First version using `tellraw` for tests |
| **1.19.2** | 17 | PacketEvents | **11/11** ✅ | |
| **1.19.4** | 17 | PacketEvents | **11/11** ✅ | |
| **1.20.1** | 17 | PacketEvents | **11/11** ✅ | |
| **1.20.4** | 17 | PacketEvents | **11/11** ✅ | |
| **1.20.6** | 21 | PacketEvents | **11/11** ✅ | Needs 1024M heap for PE block state init |
| **1.21.1** | 21 | PacketEvents | **11/11** ✅ | Needs 1024M heap |
| **1.21.4** | 21 | PacketEvents | **11/11** ✅ | Needs 1024M heap |

### Proxy Tests

| Proxy | Tests | Notes |
|-------|-------|-------|
| **BungeeCord** | **8/8** ✅ | PacketEvents |
| **Velocity** | **8/8** ✅ | PacketEvents |

### Unit Tests

| Module | Tests | Notes |
|--------|-------|-------|
| **Common** | **70/70** ✅ | JUnit 4 — MessageProcessor, FlowChatRule, FlowChatConfig, SoundResolver |

### Architecture

```
Pre-1.13 servers (1.7.10 - 1.12.2):
  FlowChat → Bukkit events (AsyncPlayerChatEvent, ServerCommandEvent, RemoteServerCommandEvent)

1.13+ servers (1.14.4 - 1.21.4):
  FlowChat → PacketEvents 2.7.0 → packet-level interception
    - SYSTEM_CHAT_MESSAGE (1.19.1+)
    - DISGUISED_CHAT (1.19.1+)
    - CHAT_MESSAGE (legacy, pre-1.19)
    - Client CHAT_MESSAGE (outgoing rules)
```

### Java Version Requirements

| Java | Server Versions |
|------|----------------|
| JDK 8 | 1.7.10, 1.8.x, 1.9.4, 1.10.2, 1.11.2, 1.12.2 |
| JDK 11 | 1.14.4, 1.15.2, 1.16.5 |
| JDK 17 | 1.17.1, 1.18.2, 1.19.x, 1.20.1, 1.20.4 |
| JDK 21 | 1.20.6, 1.21.x |

### Known Limitations

- **1.7.10**: No ActionBar API — toast falls back to regular chat message
- **Pre-1.13**: Bukkit events can only intercept player chat and console `say` commands, not arbitrary server-to-client packets from other plugins
- **1.20.5+**: PE 2.7.0 block state initialization requires ≥1024M heap

### Running Tests

```bash
# All versions
node run-integration-tests.js

# Specific versions
node run-integration-tests.js 1.8.8 1.12.2 1.21.4

# With memory limit (recommended)
node --max-old-space-size=512 run-integration-tests.js 1.14.4
```
