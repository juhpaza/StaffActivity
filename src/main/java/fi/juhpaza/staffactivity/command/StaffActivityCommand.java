package fi.juhpaza.staffactivity.command;

import fi.juhpaza.staffactivity.StaffActivity;
import fi.juhpaza.staffactivity.model.DailyStats;
import fi.juhpaza.staffactivity.model.PeriodStats;
import fi.juhpaza.staffactivity.model.RecentSession;
import fi.juhpaza.staffactivity.model.StaffSummary;
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
    private final CommandResponseRenderer renderer;

    public StaffActivityCommand(StaffActivity plugin) {
        this.plugin = plugin;
        this.renderer = new CommandResponseRenderer(plugin);
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
            case "discord" -> handleDiscord(sender, args);
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
        if (has(sender, "staffactivity.command.debug")) {
            options.add("discord");
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
                    renderer.today(sender, result.summary, result.daily, today);
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
                    renderer.sessions(sender, result.summary, result.sessions);
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
                    renderer.week(sender, result.summary, result.period, start.toString(), end.toString());
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
                    renderer.top(sender, type, entries);
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
        plugin.restartDiscordReports();
        return true;
    }

    private boolean handleDiscord(CommandSender sender, String[] args) {
        if (!has(sender, "staffactivity.command.debug")) {
            plugin.messageService().send(sender, "commands.no-permission");
            return true;
        }
        if (args.length < 2 || !"test".equalsIgnoreCase(args[1])) {
            plugin.messageService().send(sender, "commands.usage");
            return true;
        }
        plugin.discordReportService().sendTest()
                .whenComplete((ignored, throwable) -> runSync(() -> {
                    if (throwable != null) {
                        plugin.getLogger().warning("Discord webhook test failed: " + throwable.getMessage());
                        plugin.messageService().send(sender, "commands.discord-test.failed");
                        return;
                    }
                    plugin.messageService().send(sender, "commands.discord-test.sent");
                }));
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
            renderer.summary(sender, summary.orElseThrow());
        }));
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
