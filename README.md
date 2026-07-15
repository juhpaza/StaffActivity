# StaffActivity

StaffActivity is a Paper plugin for tracking staff presence and activity on a Minecraft server.

This repository is in the first implementation phase. The current scope includes session tracking, SQLite persistence, query commands, autosave and the first Discord webhook integration.

## Current Requirements

- Java 21 target
- Paper API
- Gradle Kotlin DSL

## Build

```powershell
.\gradlew.bat build --no-daemon
```

The built plugin jar is written to:

```text
build/libs/StaffActivity-0.1.0-SNAPSHOT.jar
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
