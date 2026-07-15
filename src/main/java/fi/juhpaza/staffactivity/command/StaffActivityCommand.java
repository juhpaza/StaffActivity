package fi.juhpaza.staffactivity.command;

import fi.juhpaza.staffactivity.StaffActivity;
import fi.juhpaza.staffactivity.model.DailyStats;
import fi.juhpaza.staffactivity.model.PeriodStats;
import fi.juhpaza.staffactivity.model.RecentSession;
import fi.juhpaza.staffactivity.model.StaffSummary;
import fi.juhpaza.staffactivity.model.TopEntry;
import fi.juhpaza.staffactivity.util.DurationFormatter;
import java.util.ArrayList;
import java.util.List;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Handles the first-phase StaffActivity command surface.
 */
public final class StaffActivityCommand implements CommandExecutor, TabCompleter {
    private final StaffActivity plugin;

    public StaffActivityCommand(StaffActivity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return handleSelf(sender);
        }

        return switch (args[0].toLowerCase()) {
            case "view" -> handleView(sender, args);
            case "today" -> handleToday(sender, args);
            case "week" -> handleWeek(sender, args);
            case "top" -> handleTop(sender, args);
            case "sessions" -> handleSessions(sender, args);
            case "debug" -> handleDebug(sender);
            case "reload" -> handleReload(sender);
            default -> {
                plugin.messageService().send(sender, "commands.usage");
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        List<String> options = new ArrayList<>();
        if (has(sender, "staffactivity.command.debug")) {
            options.add("debug");
        }
        if (has(sender, "staffactivity.command.self")) {
            options.add("today");
            options.add("week");
        }
        if (has(sender, "staffactivity.command.view")) {
            options.add("view");
        }
        if (has(sender, "staffactivity.command.top")) {
            options.add("top");
        }
        if (has(sender, "staffactivity.command.sessions")) {
            options.add("sessions");
        }
        if (has(sender, "staffactivity.command.reload")) {
            options.add("reload");
        }

        String prefix = args[0].toLowerCase();
        return options.stream()
                .filter(option -> option.startsWith(prefix))
                .toList();
    }

    private boolean handleDebug(CommandSender sender) {
        if (!has(sender, "staffactivity.command.debug")) {
            plugin.messageService().send(sender, "commands.no-permission");
            return true;
        }

        plugin.messageService().send(sender, "commands.debug.header");
        plugin.messageService().send(sender, "commands.debug.version", "version", plugin.getPluginMeta().getVersion());
        plugin.messageService().send(sender, "commands.debug.database", "database", plugin.databaseService().status().name().toLowerCase());
        plugin.messageService().send(sender, "commands.debug.active-sessions", "count", Integer.toString(plugin.sessionService().activeSessionCount()));
        plugin.messageService().send(sender, "commands.debug.pending-writes", "count", Integer.toString(plugin.databaseService().pendingOperations()));
        plugin.messageService().send(sender, "commands.debug.discord", "status", plugin.configService().discordEnabled() ? "enabled" : "disabled");
        plugin.messageService().send(sender, "commands.debug.timezone", "timezone", plugin.configService().timezoneId());
        return true;
    }

    private boolean handleSelf(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messageService().send(sender, "commands.player-only");
            return true;
        }
        if (!has(sender, "staffactivity.command.self")) {
            plugin.messageService().send(sender, "commands.no-permission");
            return true;
        }
        sendSummary(sender, plugin.databaseService().findSummaryByUuid(player.getUniqueId()), player.getName());
        return true;
    }

    private boolean handleView(CommandSender sender, String[] args) {
        if (!has(sender, "staffactivity.command.view")) {
            plugin.messageService().send(sender, "commands.no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.messageService().send(sender, "commands.usage");
            return true;
        }
        sendSummary(sender, plugin.databaseService().findSummaryByName(args[1]), args[1]);
        return true;
    }

    private boolean handleToday(CommandSender sender, String[] args) {
        if (!has(sender, "staffactivity.command.today")) {
            plugin.messageService().send(sender, "commands.no-permission");
            return true;
        }
        CompletableFuture<Optional<StaffSummary>> summaryFuture;
        String requestedName;
        if (args.length >= 2) {
            requestedName = args[1];
            summaryFuture = plugin.databaseService().findSummaryByName(args[1]);
        } else if (sender instanceof Player player) {
            requestedName = player.getName();
            summaryFuture = plugin.databaseService().findSummaryByUuid(player.getUniqueId());
        } else {
            plugin.messageService().send(sender, "commands.player-only");
            return true;
        }

        String today = LocalDate.now(plugin.configService().timezone()).toString();
        summaryFuture.thenCompose(summary -> summary
                        .map(value -> plugin.databaseService().findDailyStats(value.uuid(), today)
                                .thenApply(daily -> new TodayResult(value, daily)))
                        .orElseGet(() -> CompletableFuture.completedFuture(new TodayResult(null, Optional.empty()))))
                .whenComplete((result, throwable) -> runSync(() -> {
                    if (throwable != null) {
                        failQuery(sender, throwable);
                        return;
                    }
                    if (result.summary == null) {
                        plugin.messageService().send(sender, "commands.player-not-found", "player", requestedName);
                        return;
                    }
                    sendToday(sender, result.summary, result.daily, today);
                }));
        return true;
    }

    private boolean handleSessions(CommandSender sender, String[] args) {
        if (!has(sender, "staffactivity.command.sessions")) {
            plugin.messageService().send(sender, "commands.no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.messageService().send(sender, "commands.usage");
            return true;
        }
        String requestedName = args[1];
        plugin.databaseService().findSummaryByName(requestedName)
                .thenCompose(summary -> summary
                        .map(value -> plugin.databaseService().findRecentSessions(value.uuid(), 10)
                                .thenApply(sessions -> new SessionsResult(value, sessions)))
                        .orElseGet(() -> CompletableFuture.completedFuture(new SessionsResult(null, List.of()))))
                .whenComplete((result, throwable) -> runSync(() -> {
                    if (throwable != null) {
                        failQuery(sender, throwable);
                        return;
                    }
                    if (result.summary == null) {
                        plugin.messageService().send(sender, "commands.player-not-found", "player", requestedName);
                        return;
                    }
                    sendSessions(sender, result.summary, result.sessions);
                }));
        return true;
    }

    private boolean handleWeek(CommandSender sender, String[] args) {
        if (!has(sender, "staffactivity.command.week")) {
            plugin.messageService().send(sender, "commands.no-permission");
            return true;
        }
        CompletableFuture<Optional<StaffSummary>> summaryFuture;
        String requestedName;
        if (args.length >= 2) {
            requestedName = args[1];
            summaryFuture = plugin.databaseService().findSummaryByName(args[1]);
        } else if (sender instanceof Player player) {
            requestedName = player.getName();
            summaryFuture = plugin.databaseService().findSummaryByUuid(player.getUniqueId());
        } else {
            plugin.messageService().send(sender, "commands.player-only");
            return true;
        }

        LocalDate today = LocalDate.now(plugin.configService().timezone());
        LocalDate start = today.with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(6);
        summaryFuture.thenCompose(summary -> summary
                        .map(value -> plugin.databaseService().findPeriodStats(value.uuid(), start.toString(), end.toString())
                                .thenApply(period -> new WeekResult(value, period)))
                        .orElseGet(() -> CompletableFuture.completedFuture(new WeekResult(null, null))))
                .whenComplete((result, throwable) -> runSync(() -> {
                    if (throwable != null) {
                        failQuery(sender, throwable);
                        return;
                    }
                    if (result.summary == null) {
                        plugin.messageService().send(sender, "commands.player-not-found", "player", requestedName);
                        return;
                    }
                    sendWeek(sender, result.summary, result.period, start.toString(), end.toString());
                }));
        return true;
    }

    private boolean handleTop(CommandSender sender, String[] args) {
        if (!has(sender, "staffactivity.command.top")) {
            plugin.messageService().send(sender, "commands.no-permission");
            return true;
        }
        String type = args.length >= 2 ? args[1].toLowerCase() : "online";
        String metricColumn = switch (type) {
            case "active" -> "total_active_seconds";
            case "actions" -> "total_staff_actions";
            case "online" -> "total_online_seconds";
            default -> null;
        };
        if (metricColumn == null) {
            plugin.messageService().send(sender, "commands.usage");
            return true;
        }

        plugin.databaseService().findTop(metricColumn, 10)
                .whenComplete((entries, throwable) -> runSync(() -> {
                    if (throwable != null) {
                        failQuery(sender, throwable);
                        return;
                    }
                    sendTop(sender, type, entries);
                }));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!has(sender, "staffactivity.command.reload")) {
            plugin.messageService().send(sender, "commands.no-permission");
            return true;
        }

        plugin.reloadConfig();
        plugin.configService().reload();
        plugin.messageService().reload();
        plugin.messageService().send(sender, "commands.reload");
        return true;
    }

    private boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("staffactivity.admin");
    }

    private void sendSummary(CommandSender sender, CompletableFuture<Optional<StaffSummary>> future, String requestedName) {
        future.whenComplete((summary, throwable) -> runSync(() -> {
            if (throwable != null) {
                failQuery(sender, throwable);
                return;
            }
            if (summary.isEmpty()) {
                plugin.messageService().send(sender, "commands.player-not-found", "player", requestedName);
                return;
            }
            StaffSummary value = summary.orElseThrow();
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
        }));
    }

    private void sendToday(CommandSender sender, StaffSummary summary, Optional<DailyStats> daily, String today) {
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

    private void sendSessions(CommandSender sender, StaffSummary summary, List<RecentSession> sessions) {
        plugin.messageService().send(sender, "commands.sessions.header", "player", summary.latestName());
        if (sessions.isEmpty()) {
            plugin.messageService().send(sender, "commands.sessions.empty");
            return;
        }
        for (RecentSession session : sessions) {
            plugin.messageService().send(sender, "commands.sessions.row",
                    "started", session.startedAt(),
                    "ended", session.endedAt(),
                    "online", DurationFormatter.seconds(session.onlineSeconds()),
                    "reason", session.closeReason());
        }
    }

    private void sendWeek(CommandSender sender, StaffSummary summary, PeriodStats period, String start, String end) {
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

    private void sendTop(CommandSender sender, String type, List<TopEntry> entries) {
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

    private void failQuery(CommandSender sender, Throwable throwable) {
        plugin.getLogger().warning("StaffActivity query failed: " + throwable.getMessage());
        plugin.messageService().send(sender, "commands.query-failed");
    }

    private void runSync(Runnable runnable) {
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    private record TodayResult(StaffSummary summary, Optional<DailyStats> daily) {
    }

    private record SessionsResult(StaffSummary summary, List<RecentSession> sessions) {
    }

    private record WeekResult(StaffSummary summary, PeriodStats period) {
    }
}
