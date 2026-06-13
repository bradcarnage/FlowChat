# Changelog

All notable changes to FlowChat will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.2.0] - 2026-06-13

### Added
- **Sound Notifications documentation** — comprehensive docs for the sound system including all 8 built-in aliases (ding, orb, levelup, level, anvil, note/bell, click, pop), custom Minecraft sound ID passthrough, modded sound namespace support, and legacy field compatibility
- Full rule field reference in README — documented `notifyStyle`, `colorAware`, `matchJson`, `respond`, `server`, `valuestack` fields

### Changed
- README expanded with Sound Notifications section, built-in alias table, usage examples, and legacy field migration guide

## [2.1.2] - 2026-06-01

### Added
- MC 26.1.2 support (new Minecraft versioning scheme)
- Tag selector resolution system (389 selectors)
- MC 1.21.11, 1.21.9, 1.21.6 Fabric support
- Forge and NeoForge modules for 1.21.4+
- Full CI pipeline: 22 versions × 5 tiers = 110/110 passing

### Fixed
- Forge/NeoForge loader version compatibility (loaderVersion `[47,)` → `[54,)` for MC 1.21.11)
- NeoForge mods.toml minecraft version range (`[1.21,)` → `[1.21.11,1.22)`)
- Forge mods.toml minecraft version range (`[1.20.1,)` → `[1.21.11,1.22)`)
- Shadow plugin upgrade 8.3.5 → 8.3.11 for Gradle 9.x compatibility
- Pinned common:test to JDK 17 toolchain

### Changed
- Bumped Forge/NeoForge standalone version to 2.1.1

## [2.1.1] - 2026-05-31

### Added
- Full 18-version test matrix — 198/198 passed
- Integration test infrastructure and harness (46/68 → 52/68 server, 16/16 proxy)
- Bukkit event fallback for pre-1.13 servers
- Features: colorAware (#3), matchJson (#6), advancement notifyStyle (#9)
- PacketEvents AdventureSerializer — eliminated adventure serializer shading issues
- Server/proxy parity — PacketEvents overhaul + commands
- Common module overhaul: SoundResolver, field aliases, test harness

### Fixed
- Legacy server support: Java 8 bytecode, Gson relocation, legacy chat interception
- Proxy tests: AdventureSerializer, Velocity 3.4.0, PluginContainer injection
- Velocity plugin injection (PluginContainer via @Inject)
- Adventure relocation — fixed classloader conflicts and LinkageError
- Common module set to Java 8 target for cross-version compatibility

### Changed
- Comprehensive design spec for FlowChat v2.1.0

## [2.1.0] - 2026-05-31

### Added
- Multi-platform architecture: Fabric, Spigot, BungeeCord, Velocity from single codebase
- Common module with shared regex engine and config parser
- PacketEvents-based chat interception for server/proxy platforms
- Server-side plugin support (Spigot/Paper 1.13+)
- Proxy plugin support (BungeeCord, Velocity 3.x)

### Changed
- Full architecture rewrite from single-platform Fabric mod to multi-module Gradle project

## [2.0.0] - 2026-05-31

### Changed
- Ported to MC 1.21.4
- Modernized codebase for current Fabric toolchain

## [1.0.6-pre] - 2021-07-13

### Changed
- Upgraded Fabric API to 0.29.1
- Refactored mod to reflect new site (flowchat.brads.computer)
- Migrated from `flowchat.properties` to `flowchat.json`
- Config auto-migrates to config directory
- Config reloads with server reload

### Removed
- ModMenu integration
- Legacy `flowchat.properties` format

## [1.0.5] - 2020-12-01

### Fixed
- Hotfix for serverIp refresher (entity event for all entities, not only player)

## [1.0.4] - 2020-12-01

### Added
- `voidFall` option (similar to antiAFK)

### Changed
- Server IP fetched less frequently

## [1.0.3] - 2020-12-01

### Changed
- Improved antiAFK to use tick instead of ChatHudMixin
- Reduced antiAFK logging

### Removed
- LibGui dependency

## [1.0.2] - 2020-11-30

### Fixed
- Better error catching for local messages/toasts

## [1.0.1] - 2020-11-29

### Added
- `serversearch` parameter now optional
- `serversearch` parameter available for incoming messages
- `noAntiSpam` parameter for sent messages
- Anti-spam for duplicate outgoing messages
- `antiAFK` option

### Changed
- Reduced debug logging

## [1.0.0] - 2020-11-24

### Added
- Initial release
- Regex-powered chat message replacement
- Toast notifications for matched messages
- Automatic response handling
- Color code support in replacements
- Sound notifications (bell, note, click)

[2.2.0]: https://github.com/bradcarnage/FlowChat/compare/v2.1.2...v2.2.0
[2.1.2]: https://github.com/bradcarnage/FlowChat/compare/v2.1.1...v2.1.2
[2.1.1]: https://github.com/bradcarnage/FlowChat/compare/v2.1.0...v2.1.1
[2.1.0]: https://github.com/bradcarnage/FlowChat/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/bradcarnage/FlowChat/compare/1.0.6-pre...v2.0.0
[1.0.6-pre]: https://github.com/bradcarnage/FlowChat/compare/1.0.5...1.0.6-pre
[1.0.5]: https://github.com/bradcarnage/FlowChat/compare/1.0.4...1.0.5
[1.0.4]: https://github.com/bradcarnage/FlowChat/compare/1.0.3...1.0.4
[1.0.3]: https://github.com/bradcarnage/FlowChat/compare/1.0.2...1.0.3
[1.0.2]: https://github.com/bradcarnage/FlowChat/compare/1.0.1...1.0.2
[1.0.1]: https://github.com/bradcarnage/FlowChat/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/bradcarnage/FlowChat/releases/tag/1.0.0
