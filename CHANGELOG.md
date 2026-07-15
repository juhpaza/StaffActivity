# Changelog

All notable changes to StaffActivity will be documented in this file.

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
- Finnish default messages.
- Unit tests for core session, database and Discord formatting behavior.
