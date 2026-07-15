package fi.juhpaza.staffactivity.command;

import fi.juhpaza.staffactivity.StaffActivity;
import fi.juhpaza.staffactivity.model.DailyStats;
import fi.juhpaza.staffactivity.model.PeriodStats;
import fi.juhpaza.staffactivity.model.RecentSession;
import fi.juhpaza.staffactivity.model.StaffSummary;
import fi.juhpaza.staffactivity.model.TopEntry;
import fi.juhpaza.staffactivity.util.DurationFormatter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.command.CommandSender;

/**
 * Renders StaffActivity command responses.
 */
public final class CommandResponseRenderer {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
    private static final DateTimeFormatter FULL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final StaffActivity plugin;

    public CommandResponseRenderer(StaffActivity plugin) {
        this.plugin = plugin;
    }

    public void summary(CommandSender sender, StaffSummary value) {
        plugin.messageService().send(sender, "commands.summary.header", "player", value.latestName());
        plugin.messageService().send(sender, "commands.summary.totals",
                "sessions", Integer.toString(value.totalSessions()),
                "online", DurationFormatter.seconds(value.totalOnlineSeconds()),
                "active", DurationFormatter.seconds(value.totalActiveSeconds()),
                "afk", DurationFormatter.seconds(value.totalAfkSeconds()));
        plugin.messageService().send(sender, "commands.summary.counters",
                "commands", Integer.toString(value.totalCommands()),
                "teleports", Integer.toString(value.totalTeleports()),
                "gamemodes", Integer.toString(value.totalGamemodeChanges()),
                "actions", Integer.toString(value.totalStaffActions()));
        plugin.messageService().send(sender, "commands.summary.seen",
                "first_seen", value.firstSeen(),
                "last_seen", value.lastSeen());
    }

    public void today(CommandSender sender, StaffSummary summary, Optional<DailyStats> daily, String today) {
        plugin.messageService().send(sender, "commands.today.header", "player", summary.latestName(), "date", today);
        if (daily.isEmpty()) {
            plugin.messageService().send(sender, "commands.today.empty");
            return;
        }
        DailyStats value = daily.orElseThrow();
        plugin.messageService().send(sender, "commands.today.totals",
                "sessions", Integer.toString(value.sessionCount()),
                "online", DurationFormatter.seconds(value.onlineSeconds()),
                "active", DurationFormatter.seconds(value.activeSeconds()),
                "afk", DurationFormatter.seconds(value.afkSeconds()));
        plugin.messageService().send(sender, "commands.today.counters",
                "commands", Integer.toString(value.commandCount()),
                "teleports", Integer.toString(value.teleportCount()),
                "gamemodes", Integer.toString(value.gamemodeChangeCount()),
                "actions", Integer.toString(value.staffActionCount()));
    }

    public void week(CommandSender sender, StaffSummary summary, PeriodStats period, String start, String end) {
        plugin.messageService().send(sender, "commands.week.header", "player", summary.latestName(), "start", start, "end", end);
        if (period == null || period.empty()) {
            plugin.messageService().send(sender, "commands.week.empty");
            return;
        }
        plugin.messageService().send(sender, "commands.week.totals",
                "sessions", Integer.toString(period.sessionCount()),
                "online", DurationFormatter.seconds(period.onlineSeconds()),
                "active", DurationFormatter.seconds(period.activeSeconds()),
                "afk", DurationFormatter.seconds(period.afkSeconds()));
        plugin.messageService().send(sender, "commands.week.counters",
                "commands", Integer.toString(period.commandCount()),
                "teleports", Integer.toString(period.teleportCount()),
                "gamemodes", Integer.toString(period.gamemodeChangeCount()),
                "actions", Integer.toString(period.staffActionCount()));
    }

    public void sessions(CommandSender sender, StaffSummary summary, List<RecentSession> sessions) {
        plugin.messageService().send(sender, "commands.sessions.header", "player", summary.latestName());
        if (sessions.isEmpty()) {
            plugin.messageService().send(sender, "commands.sessions.empty");
            return;
        }
        int rank = 1;
        for (RecentSession session : sessions) {
            plugin.messageService().send(sender, "commands.sessions.row",
                    "rank", Integer.toString(rank++),
                    "period", period(session),
                    "online", DurationFormatter.seconds(session.onlineSeconds()),
                    "reason", closeReason(session.closeReason()));
        }
    }

    public void top(CommandSender sender, String type, List<TopEntry> entries) {
        plugin.messageService().send(sender, "commands.top.header", "type", type);
        if (entries.isEmpty()) {
            plugin.messageService().send(sender, "commands.top.empty");
            return;
        }
        int rank = 1;
        for (TopEntry entry : entries) {
            String value = type.equals("actions") ? Long.toString(entry.value()) : DurationFormatter.seconds(entry.value());
            plugin.messageService().send(sender, "commands.top.row",
                    "rank", Integer.toString(rank++),
                    "player", entry.latestName(),
                    "value", value);
        }
    }

    private String period(RecentSession session) {
        ZoneId timezone = plugin.configService().timezone();
        ZonedDateTime started = Instant.parse(session.startedAt()).atZone(timezone);
        ZonedDateTime ended = Instant.parse(session.endedAt()).atZone(timezone);
        if (started.toLocalDate().equals(ended.toLocalDate())) {
            return dateLabel(started) + " " + TIME_FORMATTER.format(started) + "-" + TIME_FORMATTER.format(ended);
        }
        return dateTimeLabel(started) + " - " + dateTimeLabel(ended);
    }

    private String dateLabel(ZonedDateTime time) {
        LocalDate date = time.toLocalDate();
        LocalDate today = LocalDate.now(plugin.configService().timezone());
        if (date.equals(today)) {
            return "Tänään";
        }
        if (date.equals(today.minusDays(1))) {
            return "Eilen";
        }
        if (date.getYear() == today.getYear()) {
            return DATE_FORMATTER.format(time);
        }
        return DateTimeFormatter.ISO_LOCAL_DATE.format(date);
    }

    private String dateTimeLabel(ZonedDateTime time) {
        LocalDate today = LocalDate.now(plugin.configService().timezone());
        if (time.toLocalDate().getYear() == today.getYear()) {
            return DATE_TIME_FORMATTER.format(time);
        }
        return FULL_DATE_TIME_FORMATTER.format(time);
    }

    private String closeReason(String rawReason) {
        return switch (rawReason.toUpperCase(Locale.ROOT)) {
            case "NORMAL" -> "Pelaaja poistui";
            case "PERMISSION_REMOVED" -> "Seuranta päättyi";
            case "PLUGIN_SHUTDOWN" -> "Plugin sammutettu";
            case "SERVER_SHUTDOWN" -> "Palvelin sammutettu";
            default -> rawReason.toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }
}
