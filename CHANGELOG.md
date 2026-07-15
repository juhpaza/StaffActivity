# Changelog

All notable changes to StaffActivity will be documented in this file.

## 0.2.6-SNAPSHOT

Current snapshot build.

### Changed

- Reworked `/staffactivity` usage output into clearer per-command chat lines.
- Added tab-completion for subcommands, top-list modes, Discord test and online player arguments.
- Clarified command counters as activity markers instead of implying full command logging.
- Made recent activity sessions show online, active, AFK, activity percentage, marker count and close reason.
- Made recent teleport history easier to read by splitting each teleport into a summary line and a route line.

## 0.2.5-SNAPSHOT

Previous snapshot build.

### Added

- In-game staff summary GUI opened through `/staffactivity view <player>`.
- Click actions in the staff summary for today's stats, weekly stats, recent sessions and recent teleports.
- Dashboard main menu for `/staffactivity gui` with status cards and navigation to existing stats flows.
- Teleport event history with cause, coordinates and detected vanish state.

### Changed

- Improved dashboard diagnostics, teleport counters and startup diagnostics formatting.
- Clarified command counter wording, recent activity period output and GUI placeholder messages.
- Fixed plugin resource version expansion so the built jar reports the Gradle project version.

## 0.1.0-SNAPSHOT

Initial development version. This is kept for project history; the current documented build is `0.2.6-SNAPSHOT`.

### Added

- Paper plugin skeleton for Java 21.
- Permission-based staff tracking with `staffactivity.track`.
- SQLite schema and migration system.
- Staff session persistence and daily/all-time aggregates.
- Active, AFK and online time calculation.
- Command, teleport and gamemode change counters.
- Query commands for summaries, daily stats, weekly stats, top lists and recent sessions.
- Active session autosave and crash recovery table.
- Discord webhook client, scheduled daily/weekly reports and test command.
- Finnish default messages.
- Unit tests for core session, database and Discord formatting behavior.
