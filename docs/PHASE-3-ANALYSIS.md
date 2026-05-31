# FlowChat Phase 3: Cross-Compatibility Analysis

## Summary

Phase 3 examines whether the planned 13-JAR distribution scheme is achievable with the current per-branch source code, and documents API boundaries between branches.

## Metadata Fixes Applied

| Branch | Loader | Fix Applied |
|--------|--------|-------------|
| 1.8.9 | Forge | Removed stale mods.toml (FG2 uses mcmod.info) |
| 1.9.4 | Forge | Removed stale mods.toml |
| 1.10.2 | Forge | Removed stale mods.toml |
| 1.11.2 | Forge | Removed stale mods.toml |
| 1.19.2 | Fabric | Fixed `~1.19.1` → `~1.19.2` |
| 1.20.4 | NeoForge | Fixed `[1.21,)` → `[1.20.4,1.20.5)` |
| 1.20.6 | NeoForge | Fixed `[1.21,)` → `[1.20.6,1.21)` |
| 1.21.1 | NeoForge | Fixed `[1.21,)` → `[1.21.1,1.22)` |
| 1.21.5 | Forge+Neo | Fixed `[1.20.1,)` → `[1.21.5,1.22)` |
| 1.21.9 | All 3 | Fixed fabric `~1.21.11` → `>=1.21.9 <=1.21.11`, forge+neo ranges |
| 1.21.11 | Forge+Neo | Fixed `[1.20.1,)` → `[1.21.11,1.22)` |

## Identical-Source Groups (Binary JAR Reuse Possible)

These branch groups have **identical source code** per loader. A single JAR built from one branch can serve all versions in the group, provided metadata allows it.

### Fabric
| Group | Branches | Can Share JAR? |
|-------|----------|----------------|
| 1.14.4–1.15.2 | 1.14.4, 1.15.2 | ✅ Yes |
| 1.17.1–1.19 | 1.17.1, 1.18.2, 1.19 | ✅ Yes |
| 1.19.1–1.19.2 | 1.19.1, 1.19.2 | ✅ Yes |
| 1.19.4–1.20.6 | 1.19.4, 1.20.1, 1.20.6 | ✅ Yes |
| 1.21.9–1.21.11 | 1.21.9, 1.21.11 | ✅ Yes |

### Forge
| Group | Branches | Can Share JAR? |
|-------|----------|----------------|
| 1.7.10–1.12.2 (FG5.1) | 1.7.10, 1.12.2 | ✅ Same source, same build plugin |
| 1.16.1–1.16.5 | 1.16.1, 1.16.5 | ✅ Yes |
| 1.19–1.19.2 | 1.19, 1.19.2 | ✅ Yes |
| 1.19.4–1.20.1 | 1.19.4, 1.20.1 | ✅ Yes |
| 1.21.5–1.21.11 | 1.21.5, 1.21.9, 1.21.11 | ✅ Yes |

### NeoForge
| Group | Branches | Can Share JAR? |
|-------|----------|----------------|
| 1.21.5–1.21.11 | 1.21.5, 1.21.9, 1.21.11 | ✅ Yes |

## Different-Source Branches Within Same JAR Range

These branches are in the same planned JAR range but have **different source code** due to Minecraft API changes. A single binary JAR will NOT work across these boundaries.

### Key API Boundaries

#### Fabric: `sendChatMessage()` signature changes
- **1.19 → 1.19.1**: `sendChatMessage(String)` → `sendChatMessage(String, Text)`
- **1.19.2 → 1.19.4**: `sendChatMessage(String, Text)` → `networkHandler.sendChatMessage(String)`
- **1.20.6 → 1.21.1**: Component/Text API changes
- **1.21.1 → 1.21.5**: Further API evolution

#### Forge: Mod descriptor format changes
- **1.7.10–1.11.2 → 1.12.2**: `cpw.mods.fml` → `net.minecraftforge.fml`
- **FG2 (1.8.9–1.11.2) vs FG5.1 (1.7.10, 1.12.2)**: Different build systems, mcmod.info vs mods.toml

#### NeoForge
- **1.20.6 → 1.21.1**: NeoForge API evolution

## JAR Manifest Feasibility

### Original Plan: 13 JARs
The WORK-SCOPE.md proposes 13 distribution JARs. This requires source reconciliation within each range.

### Current Reality: 30+ Unique Binaries
Without source reconciliation, the project produces ~30 unique binaries across all branches and loaders.

### Recommended Path: Source Reconciliation (Phase 4)
For each JAR range, create **version-adaptive code** that handles API differences at runtime:
1. Use reflection or compile-time version flags
2. Abstract platform-specific calls behind version-aware adapters
3. Widen metadata ranges to cover the full JAR scope

## Known Issues

### 1.20.4 Forge — BROKEN
The forge module on `multiplatform/1.20.4` uses RetroFuturaGradle 1.4.1 targeting MC 1.7.10. This needs a complete rewrite with ForgeGradle 6 targeting MC 1.20.4. Tracked for Phase 4.

### 1.7.10 Forge — Misleading
Branch `multiplatform/1.7.10` uses FG5.1 (javafml), which produces a JAR that only works on MC 1.12.2+, NOT on actual MC 1.7.10. The metadata says `[1.7.10,1.13)` but realistically should be `[1.12.2,1.13)`.

### FG2 Branches — Isolated
Branches 1.8.9, 1.9.4, 1.10.2, 1.11.2 use ForgeGradle 2.x with mcmod.info. These produce JARs that are version-locked to their specific MC version (no cross-compat possible with FG2).

## Phase 3 Verification Status

- [x] Metadata audit complete
- [x] Wrong version ranges fixed (11 branches)
- [x] Stale mods.toml removed from FG2 branches (4 branches)
- [x] Identical-source groups identified
- [x] API boundaries documented
- [x] Build verification after metadata fixes (11/11 PASS)
- [x] Push fixes to remotes (forgejo-brad + github)

## Next Steps (Phase 4)

1. Fix 1.20.4 forge module (FG6 rewrite)
2. Source reconciliation for range JARs
3. Version-adaptive API abstraction layer
4. Widen metadata ranges for consolidated JARs
5. Build and test range JARs
