# FlowChat Build Scope & Version Matrix

## Project Scope
- Java Edition Minecraft ONLY
- MC 1.7.2 through latest release (currently 26.1.2)
- Minimize distribution files via cross-compilation testing
- Clear JAR naming for user download ease

## Loader Availability Rules
| Loader | Available From | Notes |
|--------|---------------|-------|
| Forge | 1.7.2+ | Oldest supported loader |
| Fabric | 1.14.4+ | No Fabric pre-1.14.4 |
| NeoForge | 1.20.2+ | Fork of Forge, didn't exist before |
| Spigot | Universal | Server plugin, version-agnostic via PacketEvents |
| BungeeCord | Universal | Proxy plugin, version-agnostic |
| Velocity | Universal | Proxy plugin, version-agnostic |

## Branch Inventory (42 branches)
```
1.7.10, 1.8.9, 1.9.4, 1.10.2, 1.11.2, 1.12.2,
1.14.4, 1.15.2,
1.16.1, 1.16.2, 1.16.3, 1.16.4, 1.16.5,
1.17, 1.17.1,
1.18, 1.18.1, 1.18.2,
1.19, 1.19.1, 1.19.2, 1.19.3, 1.19.4,
1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.5, 1.20.6,
1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5,
1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11,
26.1.2
```

## Missing MC Versions (no branch yet)
- 1.7.2–1.7.9 (may be covered by 1.7.10 cross-compat)
- 1.8–1.8.8 (may be covered by 1.8.9)
- 1.9–1.9.3, 1.10–1.10.1, 1.11–1.11.1, 1.12–1.12.1
- 1.13–1.13.2 (Forge only era, need to determine if branch needed)
- 1.14–1.14.3, 1.15–1.15.1, 1.16
- 26.1, 26.1.1

## Build Architecture
- **Root `./gradlew`**: Builds `common`, `packetevents-common`, `fabric`, `spigot`, `bungee`, `velocity`
- **`forge/gradlew`** (standalone): Builds forge separately (most branches)
- **`neoforge/gradlew`** (standalone): Builds neoforge separately (1.20.2+ branches)
- Some branches (1.19.4, 1.20, 1.20.1) have forge in root build (no standalone gradlew)

## JDK Requirements
| MC Version Range | JDK |
|-----------------|-----|
| 1.7.x–1.16.x | JDK 8 (`~/jdk/jdk8u492-b09`) |
| 1.17.x–1.20.4 | JDK 17 (`~/jdk/jdk-17.0.19+10`) |
| 1.20.5–1.21.x | JDK 21 (`~/jdk/jdk-21.0.11+10`) |
| 26.x | JDK 25 (`~/jdk/jdk-25.0.3+9`) |

## Source Identity Groups (identical source = cross-compile candidates)
Testing required to confirm binary compatibility across versions.

### Forge-only era (no Fabric/NeoForge)
| Build Branch | Same Forge Source | Same NeoForge Source |
|---|---|---|
| 1.7.10 | 1.7.10, 1.12.2 | N/A (cleaned) |
| 1.8.9 | 1.8.9 only | N/A |
| 1.9.4 | 1.9.4 only | N/A |
| 1.10.2 | 1.10.2 only | N/A |
| 1.11.2 | 1.11.2 only | N/A |

### Fabric+Forge era
| Build Branch | Same Fabric Source | Same Forge Source |
|---|---|---|
| 1.14.4 | 1.14.4, 1.15.2 | 1.14.4 only |
| 1.15.2 | (same as 1.14.4) | NO FORGE SOURCE — needs port |
| 1.16.1 | 1.16.1 only | 1.16.1–1.16.5 |
| 1.16.5 | 1.16.2–1.16.5 | (same as 1.16.1) |
| 1.17 | 1.17, 1.17.1 | NO FORGE SOURCE — needs port |
| 1.18 | 1.17–1.19 | 1.18–1.19.1 |
| 1.19.2 | 1.19.2–1.20.1 | 1.19.2 only |
| 1.19.4 | (same as 1.19.2) | 1.19.3–1.20.1 |

### Fabric+Forge+NeoForge era
| Build Branch | Same Fabric | Same Forge | Same NeoForge |
|---|---|---|---|
| 1.20.4 | 1.19.2–1.20.6 | 1.20.2–1.20.6 | 1.20.2–1.20.6 |
| 1.21 | 1.21, 1.21.1 | 1.21, 1.21.1 | 1.21, 1.21.1 |
| 1.21.4 | 1.21.2–1.21.5 | 1.21.2–1.21.11 | 1.21.2–1.21.11 |
| 1.21.6 | 1.21.6–1.21.8, .10–.11 | (same as 1.21.4) | (same as 1.21.4) |
| 1.21.9 | 1.21.9 ONLY (anomaly) | (same as 1.21.4) | (same as 1.21.4) |
| 26.1.2 | unique | unique | unique |

## Cleanup Tasks
- [x] Document scope (this file)
- [ ] Remove stale NeoForge source from pre-1.20.2 branches
- [ ] Port Forge loader to 1.15.2, 1.17, 1.17.1 (missing source)
- [ ] Create 1.13.x branch if Forge supports it
- [ ] Create 1.7.2 branch or confirm 1.7.10 covers it
- [ ] Build ALL branches, ALL valid loaders
- [ ] Test cross-version binary compatibility
- [ ] Build universal server plugin JARs
- [ ] Define final JAR naming convention
- [ ] Push all branches to remotes

## JAR Naming Convention
```
FlowChat-<version_range>-<Loader>.jar     (client mod, specific loader)
FlowChat-<version_range>.jar              (if covers all loaders for that range)
FlowChat-Server-Spigot.jar                (universal server plugin)
FlowChat-Server-BungeeCord.jar            (universal proxy plugin)
FlowChat-Server-Velocity.jar              (universal proxy plugin)
```
Version ranges: `1.20.2-1.20.6` or single `1.7.10`

## Build Results Log
_To be filled as builds complete_

| Branch | Fabric | Forge | NeoForge | Notes |
|--------|--------|-------|----------|-------|
| 26.1.2 | | | | |
| 1.21.11 | | | | |
| ... | | | | |
