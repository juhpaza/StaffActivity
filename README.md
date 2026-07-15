# StaffActivity

StaffActivity is a Paper plugin for tracking staff presence and operational activity on a Minecraft server.

The plugin is built for server owners who want a clear, low-noise view of staff online time, active time, AFK time and basic moderation activity without giving the plugin broad audit-log responsibilities. It only tracks players who have an explicit tracking permission.

Current build: `0.2.6-SNAPSHOT`.

## Project Description

**Short GitHub description:**

```text
Paper plugin for tracking Minecraft staff sessions, active time, AFK time, safe activity counters and Discord reports.
```

StaffActivity is designed around a few practical rules:

- Only staff members with `staffactivity.track` are tracked.
- SQLite writes run off the Minecraft server thread.
- Active sessions are autosaved so crashes do not leave sessions permanently open.
- Command tracking stores only safe root commands, not full arguments.
- Discord webhook URLs are treated as secrets and are never shown in debug output.

## Features

- Permission-based staff tracking.
- Session history with start time, end time and close reason.
- Online, active and AFK time calculation.
- Configurable AFK timeout and movement/rotation thresholds.
- Counters for commands, teleports, gamemode changes and future staff actions.
- Recent teleport history with cause, worlds, coordinates and detected vanish state.
- Daily and weekly aggregate stats.
- SQLite persistence in `plugins/StaffActivity/staffactivity.db`.
- Autosave for active sessions.
- Query commands for self, player, day, week, top lists and recent sessions.
- Discord webhook support for daily reports, weekly reports, optional join/quit messages and safe test messages.
- In-game Dashboard GUI for safe status checks, navigation and common admin actions.
- Graphical staff summary GUI with click actions for today, week, recent sessions and recent teleports.
- Safe debug command that avoids leaking secrets.

## Requirements

- Java 21 runtime on the Minecraft server.
- Paper 1.21.x compatible server.
- Gradle wrapper is included for local builds.

The project currently compiles against:

```text
Paper API 1.21.8-R0.1-SNAPSHOT
Java release target 21
SQLite JDBC 3.49.1.0
```

## Installation

1. Build the plugin or download a release jar when releases are available.
2. Copy the jar to the server:

   ```text
   plugins/StaffActivity-0.2.6-SNAPSHOT.jar
   ```

3. Start the server once so `config.yml` and `messages.yml` are generated.
4. Grant tracking and command permissions through LuckPerms or another permission plugin.
5. Edit `plugins/StaffActivity/config.yml`.
6. Restart the server or run:

   ```text
   /staffactivity reload
   ```

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/staffactivity` | Show your own all-time summary | `staffactivity.command.self` |
| `/staffactivity view <player>` | Open another staff member's graphical all-time summary in-game; console receives text output | `staffactivity.command.view` |
| `/staffactivity today [player]` | Show today's stats | `staffactivity.command.today` |
| `/staffactivity week [player]` | Show this week's stats | `staffactivity.command.week` |
| `/staffactivity top [online\|active\|actions]` | Show top staff activity lists | `staffactivity.command.top` |
| `/staffactivity sessions <player>` | Show recent staff sessions | `staffactivity.command.sessions` |
| `/staffactivity gui` | Open the in-game Dashboard GUI | `staffactivity.command.gui` |
| `/staffactivity discord test` | Send a Discord webhook test message | `staffactivity.command.debug` |
| `/staffactivity reload` | Reload config and messages | `staffactivity.command.reload` |
| `/staffactivity debug` | Show safe technical status | `staffactivity.command.debug` |

Alias:

```text
/jpzsa
```

The in-game staff summary opened by `/staffactivity view <player>` includes clickable buttons that run the existing `today`, `week` and `sessions` command paths for the selected player. The teleport counter opens recent teleport history captured by the installed `0.2.6-SNAPSHOT` build or newer.

The in-game Dashboard opened by `/staffactivity gui` is the main StaffActivity menu. It shows plugin version, database status, pending writes, Discord status, online staff, tracking permission, timezone and active session count, and links to existing online staff, top, today, week, reload and Discord test flows.

## Permissions

| Permission | Purpose |
| --- | --- |
| `staffactivity.track` | Marks a player as tracked staff |
| `staffactivity.command.self` | Allows viewing own summary |
| `staffactivity.command.view` | Allows viewing another staff member |
| `staffactivity.command.today` | Allows daily stats command |
| `staffactivity.command.week` | Allows weekly stats command |
| `staffactivity.command.top` | Allows top list command |
| `staffactivity.command.sessions` | Allows recent sessions command |
| `staffactivity.command.gui` | Allows opening the in-game management GUI |
| `staffactivity.command.reload` | Allows config reload |
| `staffactivity.command.debug` | Allows safe debug and Discord test command |
| `staffactivity.admin` | Grants all StaffActivity command permissions |

Example LuckPerms setup:

```text
/lp group helper permission set staffactivity.track true
/lp group admin permission set staffactivity.admin true
```

## Configuration

Default config:

```yaml
plugin:
  language: fi
  timezone: Europe/Helsinki

tracking:
  permission: staffactivity.track
  track-operators-automatically: false

activity:
  afk-timeout-seconds: 300
  movement-threshold: 0.25
  rotation-threshold-degrees: 10.0

storage:
  type: SQLITE
  autosave-interval-seconds: 60

command-tracking:
  enabled: true
  store-command-root: true
  excluded-commands:
    - login
    - register
    - changepassword
    - password
    - pin
    - 2fa

discord:
  enabled: false
  webhook-url: ""
```

## Discord Webhook

Discord is disabled by default. Enable it in `plugins/StaffActivity/config.yml` after installing the plugin:

```yaml
discord:
  enabled: true
  webhook-url: "https://discord.com/api/webhooks/..."

  reports:
    daily:
      enabled: true
      time: "23:55"

    weekly:
      enabled: true
      day: SUNDAY
      time: "20:00"

  events:
    staff-join: false
    staff-quit: false
    plugin-errors: true
```

The webhook URL is a secret. Do not commit a live server `config.yml` containing it.

After changing the config, reload and send a safe test message:

```text
/staffactivity reload
/staffactivity discord test
```

## Data Storage

StaffActivity stores runtime data in:

```text
plugins/StaffActivity/staffactivity.db
```

Main tables:

- `staff_members`
- `staff_sessions`
- `staff_daily_stats`
- `staff_actions`
- `staff_active_sessions`
- `staff_teleport_events`

The active session table is used for autosave and crash recovery.
The teleport event table stores recent staff teleport details for GUI drill-downs.

## Privacy And Safety

StaffActivity intentionally avoids collecting sensitive data:

- No IP addresses are stored.
- Full command arguments are not stored by default.
- Sensitive command roots can be excluded in config.
- Discord webhook URLs are never shown by `/staffactivity debug`.
- Database work runs on a dedicated background executor.

## Building

Windows:

```powershell
.\gradlew.bat build --no-daemon
```

Linux/macOS:

```bash
./gradlew build --no-daemon
```

The built plugin jar is written to:

```text
build/libs/StaffActivity-0.2.6-SNAPSHOT.jar
```

## Testing

Run the full test suite:

```powershell
.\gradlew.bat test --no-daemon
```

Run build and tests:

```powershell
.\gradlew.bat build --no-daemon
```

## Current Status

Current build: `0.2.6-SNAPSHOT`.

This project is still a snapshot-stage implementation. The core architecture, SQLite persistence, session tracking, commands, autosave, Discord webhook foundation, in-game dashboard, staff summary GUI and teleport history tracking are in place.

Known limitations:

- No public release artifacts yet.
- No web dashboard.
- No MySQL/MariaDB/PostgreSQL support.
- Plan, LuckPerms group mode and LiteBans integrations are planned but not implemented.
- Discord reports are plain text webhook messages, not rich embeds yet.

## Roadmap

- Complete production README and release notes.
- Add first GitHub release.
- Add more config fallback tests.
- Add richer Discord report formatting.
- Add optional LiteBans integration for reliable moderation action tracking.
- Add optional Plan integration.
- Add LuckPerms group-based tracking mode.
