# FlowChat — Work Scope

## Objective

Build FlowChat for **every supported Minecraft version from 1.7.2 to 26.1.2**, across **all available mod loaders, server plugins, and proxy loaders**, with the minimum possible number of distribution JARs.

---

## Target Minecraft Versions

All major releases from 1.7.2 through 26.1.2, including sub-versions where the game code or modding API changed. Versions are consolidated into `.x` branches when sub-versions share identical mod-facing APIs.

| MC Version Range | Branch Name | Java Target | Notes |
|---|---|---|---|
| 1.7.2–1.7.10 | `multiplatform/1.7.x` | 8 | Legacy FML/cpw, ForgeGradle 1.x |
| 1.8–1.8.9 | `multiplatform/1.8.x` | 8 | |
| 1.9–1.9.4 | `multiplatform/1.9.x` | 8 | |
| 1.10–1.10.2 | `multiplatform/1.10.x` | 8 | |
| 1.11–1.11.2 | `multiplatform/1.11.x` | 8 | |
| 1.12–1.12.2 | `multiplatform/1.12.x` | 8 | |
| 1.13–1.13.2 | `multiplatform/1.13.x` | 8 | The Flattening; Forge unstable but attempted |
| 1.14–1.14.4 | `multiplatform/1.14.x` | 8 | First Fabric version |
| 1.15–1.15.2 | `multiplatform/1.15.x` | 8 | |
| 1.16–1.16.5 | `multiplatform/1.16.x` | 8 | |
| 1.17–1.17.1 | `multiplatform/1.17.x` | 16 | Java 16+ required by MC |
| 1.18–1.18.2 | `multiplatform/1.18.x` | 17 | |
| 1.19–1.19.4 | `multiplatform/1.19.x` | 17 | |
| 1.20–1.20.6 | `multiplatform/1.20.x` | 17 | |
| 1.21–1.21.11 | `multiplatform/1.21.x` | 21 | |
| 26.1–26.1.2 | `multiplatform/26.x` | 25 | New Mojang versioning |

> **Branch consolidation:** Old per-subversion branches (e.g. `multiplatform/1.18`, `multiplatform/1.18.1`, `multiplatform/1.18.2`) are merged into the `.x` branch and deleted.

---

## Loader Coverage

### Client Mod Loaders

| Loader | MC Version Range | Notes |
|---|---|---|
| Forge | 1.7.2–26.1.2 | Skip MC versions with no stable Forge release. Target latest recommended Forge build per MC version. |
| Fabric | 1.14.4–26.1.2 | No Fabric before 1.14.4 |
| NeoForge | 1.20.2–26.1.2 | NeoForge fork started at 1.20.2 |

### Server Plugins

| Platform | Scope |
|---|---|
| Spigot (Bukkit API) | Universal JAR, all MC versions with Spigot support |
| BungeeCord | Universal proxy JAR (standard BungeeCord, not Waterfall) |
| Velocity 3.x | Universal proxy JAR |

### Server JAR Strategy

**One fat "server" JAR** containing Spigot + BungeeCord + Velocity support. Runtime class detection determines which platform is active:
- `org.bukkit.*` → Spigot/Bukkit mode
- `net.md_5.bungee.*` → BungeeCord mode
- `com.velocitypowered.*` → Velocity mode

---

## Distribution JAR Strategy

### Goal
Minimize total JAR count. Ideally:
- **One Forge JAR** covering 1.7.2–26.1.2 (or split into ranges where API breaks force it)
- **One Fabric JAR** covering 1.14.4–26.1.2 (or split into ranges)
- **One NeoForge JAR** covering 1.20.2–26.1.2 (or split into ranges)
- **One Server JAR** (Spigot + BungeeCord + Velocity, universal)

### Multi-Release JAR
Attempt Java multi-release JARs (`META-INF/versions/`) to ship a single JAR that works across Java 8/16/17/21/25. If multi-release JARs prove impractical, fall back to range-based splitting.

### JAR Naming Convention
```
FlowChat-<Loader>-<MinVersion>to<MaxVersion>.jar
```
Examples:
- `FlowChat-Forge-1.7.2to26.1.2.jar` (ideal single JAR)
- `FlowChat-Forge-1.7.2to1.16.5.jar` + `FlowChat-Forge-1.17to26.1.2.jar` (if split needed)
- `FlowChat-Fabric-1.14.4to26.1.2.jar`
- `FlowChat-NeoForge-1.20.2to26.1.2.jar`
- `FlowChat-Server.jar` (universal)

Actual ranges discovered through cross-compatibility testing.

---

## Configuration

- **Identical config format** across all versions and loaders
- JSON-based config file
- No version-specific config keys
- Config must load/reload consistently on every supported version

---

## Build Order & Workflow

Work **top-down from latest MC version (26.1.2) to oldest (1.7.2)**:

```
For each MC version (descending):
  1. Ensure branch exists and builds for all applicable loaders
  2. Run common:test (unit tests)
  3. Fix compilation errors, referencing adjacent version branches as needed
  4. Apply unified bug fixes (formatColors, \r\n stripping, etc.)
  5. Verify feature degradation fallbacks
  6. Mark version as PASS in test matrix
  7. Move to next older version
```

### Bug Fix Propagation
These fixes apply to **all** branches:
- `formatColors` regex: `(?<!&)&([0-9a-fk-or])` — prevents `&&a` conversion
- `\r\n` stripping: `.replace("\r", "")` — strips before pattern matching, not escapes

---

## Test Matrix

### Legend
- ✅ Pass
- ❌ Fail
- ⬜ Not applicable (loader doesn't exist for this version)
- 🔲 Not yet tested

### Build & Unit Test Matrix

| MC Version | common:test | Forge Build | Fabric Build | NeoForge Build | Server Build |
|---|---|---|---|---|---|
| 26.1.2 | 🔲 | 🔲 | 🔲 | 🔲 | 🔲 |
| 1.21.x | 🔲 | 🔲 | 🔲 | 🔲 | 🔲 |
| 1.20.x | 🔲 | 🔲 | 🔲 | 🔲 | 🔲 |
| 1.19.x | 🔲 | 🔲 | 🔲 | ⬜ | 🔲 |
| 1.18.x | 🔲 | 🔲 | 🔲 | ⬜ | 🔲 |
| 1.17.x | 🔲 | 🔲 | 🔲 | ⬜ | 🔲 |
| 1.16.x | 🔲 | 🔲 | 🔲 | ⬜ | 🔲 |
| 1.15.x | 🔲 | 🔲 | 🔲 | ⬜ | 🔲 |
| 1.14.x | 🔲 | 🔲 | 🔲 | ⬜ | 🔲 |
| 1.13.x | 🔲 | 🔲 | ⬜ | ⬜ | 🔲 |
| 1.12.x | 🔲 | 🔲 | ⬜ | ⬜ | 🔲 |
| 1.11.x | 🔲 | 🔲 | ⬜ | ⬜ | 🔲 |
| 1.10.x | 🔲 | 🔲 | ⬜ | ⬜ | 🔲 |
| 1.9.x | 🔲 | 🔲 | ⬜ | ⬜ | 🔲 |
| 1.8.x | 🔲 | 🔲 | ⬜ | ⬜ | 🔲 |
| 1.7.x | 🔲 | 🔲 | ⬜ | ⬜ | 🔲 |

### Cross-Compatibility Matrix

Tests whether a JAR built on one sub-version loads on adjacent sub-versions within the same `.x` range.

| Built On | Tested On | Result |
|---|---|---|
| 🔲 | 🔲 | 🔲 |

> Populated during cross-compat testing phase.

### Feature Degradation Matrix

| MC Version | Toast Notification | Advancement Bar | Action Bar | Sound | Chat Fallback |
|---|---|---|---|---|---|
| 26.1.2 | 🔲 native | 🔲 native | 🔲 native | 🔲 native | 🔲 always |
| 1.21.4+ | 🔲 native | 🔲 native | 🔲 native | 🔲 native | 🔲 always |
| 1.21–1.21.3 | ⬜ fallback→chat | 🔲 native | 🔲 native | 🔲 native | 🔲 always |
| 1.17–1.20.x | ⬜ fallback→chat | 🔲 native | 🔲 native | 🔲 native | 🔲 always |
| 1.16.x | ⬜ fallback→chat | 🔲 native | 🔲 native | 🔲 native | 🔲 always |
| 1.14–1.15.x | ⬜ fallback→chat | 🔲 native | 🔲 native | 🔲 native | 🔲 always |
| 1.9–1.13.x | ⬜ fallback→chat | ⬜ fallback→chat | 🔲 native | 🔲 native | 🔲 always |
| 1.7–1.8.x | ⬜ fallback→chat | ⬜ fallback→chat | ⬜ fallback→chat | 🔲 native | 🔲 always |

> Unsupported features fall back to chat message + log warning. See `docs/FEATURE-DEGRADATION.md`.

### In-Game Integration Test Matrix

Real server + client testing on the **first stable sub-version of each major release**:

| MC Version | Test Version | Forge Client | Fabric Client | NeoForge Client | Spigot Server | BungeeCord Proxy | Velocity Proxy |
|---|---|---|---|---|---|---|---|
| 1.7.x | 1.7.10 | 🔲 | ⬜ | ⬜ | 🔲 | 🔲 | 🔲 |
| 1.8.x | 1.8.9 | 🔲 | ⬜ | ⬜ | 🔲 | 🔲 | 🔲 |
| 1.9.x | 1.9.4 | 🔲 | ⬜ | ⬜ | 🔲 | 🔲 | 🔲 |
| 1.10.x | 1.10.2 | 🔲 | ⬜ | ⬜ | 🔲 | 🔲 | 🔲 |
| 1.11.x | 1.11.2 | 🔲 | ⬜ | ⬜ | 🔲 | 🔲 | 🔲 |
| 1.12.x | 1.12.2 | 🔲 | ⬜ | ⬜ | 🔲 | 🔲 | 🔲 |
| 1.13.x | 1.13.2 | 🔲 | ⬜ | ⬜ | 🔲 | 🔲 | 🔲 |
| 1.14.x | 1.14.4 | 🔲 | 🔲 | ⬜ | 🔲 | 🔲 | 🔲 |
| 1.15.x | 1.15.2 | 🔲 | 🔲 | ⬜ | 🔲 | 🔲 | 🔲 |
| 1.16.x | 1.16.5 | 🔲 | 🔲 | ⬜ | 🔲 | 🔲 | 🔲 |
| 1.17.x | 1.17.1 | 🔲 | 🔲 | ⬜ | 🔲 | 🔲 | 🔲 |
| 1.18.x | 1.18.2 | 🔲 | 🔲 | ⬜ | 🔲 | 🔲 | 🔲 |
| 1.19.x | 1.19.4 | 🔲 | 🔲 | ⬜ | 🔲 | 🔲 | 🔲 |
| 1.20.x | 1.20.1 | 🔲 | 🔲 | 🔲 | 🔲 | 🔲 | 🔲 |
| 1.21.x | 1.21.4 | 🔲 | 🔲 | 🔲 | 🔲 | 🔲 | 🔲 |
| 26.x | 26.1.2 | 🔲 | 🔲 | 🔲 | 🔲 | 🔲 | 🔲 |

#### In-Game Test Pass Criteria
1. Mod/plugin loads without crash
2. Chat messages intercepted and processed
3. Config loads and reloads correctly
4. Auto-response triggers on matching pattern
5. Feature degradation works (unsupported features fall back to chat + log warning)
6. Server plugin loads and relays cross-server chat (proxy tests)

---

## Forge Version Research

Skip MC versions with no stable/complete Forge release. Known problematic:
- Research needed for each MC version to confirm Forge availability on files.minecraftforge.net

| MC Version | Forge Available | Forge Status | Notes |
|---|---|---|---|
| 1.7.10 | 🔲 | 🔲 | Legacy, well-supported |
| 1.8.9 | 🔲 | 🔲 | |
| 1.9.4 | 🔲 | 🔲 | |
| 1.10.2 | 🔲 | 🔲 | |
| 1.11.2 | 🔲 | 🔲 | |
| 1.12.2 | 🔲 | 🔲 | Most popular legacy Forge |
| 1.13.x | 🔲 | 🔲 | Potentially unstable |
| 1.14.4 | 🔲 | 🔲 | |
| 1.15.2 | 🔲 | 🔲 | |
| 1.16.5 | 🔲 | 🔲 | |
| 1.17.1 | 🔲 | 🔲 | |
| 1.18.2 | 🔲 | 🔲 | |
| 1.19.4 | 🔲 | 🔲 | |
| 1.20.6 | 🔲 | 🔲 | |
| 1.21.4 | 🔲 | 🔲 | |
| 26.1.2 | 🔲 | 🔲 | FG7, eventbus7 |

> Populated during Forge research phase. Versions with no stable Forge are removed from scope.

---

## Build Infrastructure

### JDK Installations
| JDK | Path | Used For |
|---|---|---|
| JDK 8 (8u492) | `~/jdk/jdk8u492-b09` | MC 1.7.x–1.16.x |
| JDK 17 (17.0.19) | `~/jdk/jdk-17.0.19+10` | MC 1.17.x–1.20.x |
| JDK 21 (21.0.11) | `~/jdk/jdk-21.0.11+10` | MC 1.21.x |
| JDK 25 (25.0.3) | `~/jdk/jdk-25.0.3+9` | MC 26.x |

### Build Script
`scripts/build-all.sh` — iterates all branches, runs applicable builds.

### Server Test Infrastructure
- MC server JARs: downloaded from Mojang/PaperMC as needed
- BungeeCord: standard distribution
- Velocity 3.x: latest stable
- Test directory: `test-servers/` (gitignored)

---

## Phases

### Phase 1: Build Sweep (Top-Down)
Starting from 26.1.2, work down to 1.7.2:
1. Fix all build failures per version
2. Run `common:test` on each
3. Apply bug fixes (formatColors, \r\n)
4. Update test matrix

### Phase 2: Branch Consolidation
1. Identify sub-versions with identical mod-facing code
2. Merge into `.x` branches
3. Delete old per-subversion branches
4. Verify builds still pass after consolidation

### Phase 3: Cross-Compatibility Testing
1. Build JAR on one sub-version
2. Test loading on all sub-versions in that `.x` range
3. Document compatible ranges
4. Determine minimum JAR set

### Phase 4: Multi-Release JAR Assembly
1. Attempt multi-release JAR (`META-INF/versions/`)
2. If impractical, fall back to range-based JARs
3. Name JARs per convention: `FlowChat-<Loader>-<Min>to<Max>.jar`

### Phase 5: Server Plugin Unification
1. Build fat server JAR with Spigot + BungeeCord + Velocity
2. Runtime platform detection via class presence
3. Test on each platform independently
4. Verify cross-server chat relay

### Phase 6: Feature Degradation Verification
1. Implement platform-layer fallbacks on each version branch
2. Toast → Advancement → ActionBar → Chat fallback chain
3. Verify with in-game tests per feature degradation matrix
4. Log warnings for degraded features

### Phase 7: In-Game Integration Testing
1. Download server JARs for each major MC version
2. Launch test server + client for each
3. Run pass criteria checklist
4. Fix issues found, re-test
5. Update in-game test matrix

### Phase 8: Final JAR Packaging
1. Build final distribution JARs
2. Verify naming convention
3. Document download instructions per version/loader
4. Final pass on all matrices — everything ✅ or ⬜

---

## Constraints

- **No uploads** — strictly local development, no GitHub/CurseForge/Modrinth pushes
- **No paid API calls** without explicit permission
- **Config format identical** across all versions
- **Invalid sounds** → play XP "ding" + log warning
- **Empty/malformed patterns** → ignore entry + log warning
- **Feature degradation** → fall back to chat message + log warning
- **`wasModified()`** → only true if visible output changed
- **`formatColors`** → don't convert `&z`, uppercase `&A`, `&&a`
- **Value stacking** → uses "/" separator (configurable)
- **Auto-response recursion** → MAX_AUTO_RESPONSE_DEPTH=1
