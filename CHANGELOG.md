# Changelog

All notable changes to StaffActivity will be documented in this file.

## 0.2.5-SNAPSHOT

Current snapshot build.

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

Initial development version.

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
- In-game management GUI with status cards and safe admin actions.
- Graphical staff summary view through `/staffactivity view <player>` for in-game users.
- Click actions in the graphical staff summary for today's stats, weekly stats and recent sessions.
- Dashboard main menu for `/staffactivity gui` with status cards and navigation to existing stats flows.
- Shorter styled GUI titles, current gamemode labels and clearer placeholder messaging for unavailable teleport history.
- Teleport event history with cause, coordinates and detected vanish state.
- Finnish default messages.
- Unit tests for core session, database and Discord formatting behavior.
