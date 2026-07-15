package fi.juhpaza.staffactivity.discord;

import fi.juhpaza.staffactivity.StaffActivity;
import fi.juhpaza.staffactivity.config.ConfigService;
import fi.juhpaza.staffactivity.model.StaffReportEntry;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Schedules and sends StaffActivity Discord webhook notifications.
 */
public final class DiscordReportService {
    private static final long CHECK_INTERVAL_TICKS = 20L * 60L;

    private final StaffActivity plugin;
    private final DiscordWebhookClient webhookClient;
    private final DiscordMessageFormatter formatter;
    private BukkitTask task;
    private LocalDate lastDailyReport;
    private LocalDate lastWeeklyReport;

    public DiscordReportService(StaffActivity plugin) {
        this(plugin, new DiscordWebhookClient(), new DiscordMessageFormatter());
    }

    DiscordReportService(StaffActivity plugin, DiscordWebhookClient webhookClient, DiscordMessageFormatter formatter) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.webhookClient = Objects.requireNonNull(webhookClient, "webhookClient");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
    }

    public void start() {
        stop();
        if (!plugin.configService().discordConfigured()) {
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkScheduledReports, 20L * 30L, CHECK_INTERVAL_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public CompletableFuture<Void> sendTest() {
        return send(formatter.testMessage(plugin.getServer().getName()));
    }

    public void staffJoin(Player player) {
        ConfigService config = plugin.configService();
        if (config.discordStaffJoinEnabled()) {
            sendSilently(formatter.staffJoin(player.getName()));
        }
    }

    public void staffQuit(Player player) {
        ConfigService config = plugin.configService();
        if (config.discordStaffQuitEnabled()) {
            sendSilently(formatter.staffQuit(player.getName()));
        }
    }

    public void pluginError(String message) {
        if (plugin.configService().discordPluginErrorsEnabled()) {
            sendSilently(formatter.pluginError(message));
        }
    }

    private void checkScheduledReports() {
        ConfigService config = plugin.configService();
        if (!config.discordConfigured()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(config.timezone());
        if (config.discordDailyReportEnabled() && shouldSendDaily(now.toLocalDate(), now.toLocalTime(), config.discordDailyReportTime())) {
            sendDailyReport(now.toLocalDate());
        }
        if (config.discordWeeklyReportEnabled() && shouldSendWeekly(now.toLocalDate(), now.toLocalTime(), config.discordWeeklyReportDay(), config.discordWeeklyReportTime())) {
            sendWeeklyReport(now.toLocalDate());
        }
    }

    private boolean shouldSendDaily(LocalDate date, LocalTime now, LocalTime configuredTime) {
        return !date.equals(lastDailyReport) && !now.isBefore(configuredTime);
    }

    private boolean shouldSendWeekly(LocalDate date, LocalTime now, DayOfWeek configuredDay, LocalTime configuredTime) {
        return date.getDayOfWeek() == configuredDay
                && !date.equals(lastWeeklyReport)
                && !now.isBefore(configuredTime);
    }

    private void sendDailyReport(LocalDate date) {
        lastDailyReport = date;
        plugin.databaseService().findDailyReport(date.toString())
                .thenCompose(entries -> send(formatter.report("StaffActivity paivaraportti", date.toString(), entries)))
                .exceptionally(throwable -> {
                    lastDailyReport = null;
                    plugin.getLogger().warning("Failed to send Discord daily report: " + throwable.getMessage());
                    return null;
                });
    }

    private void sendWeeklyReport(LocalDate date) {
        lastWeeklyReport = date;
        LocalDate start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = start.plusDays(6);
        plugin.databaseService().findPeriodReport(start.toString(), end.toString())
                .thenCompose(entries -> send(formatter.report("StaffActivity viikkoraportti", start + " - " + end, entries)))
                .exceptionally(throwable -> {
                    lastWeeklyReport = null;
                    plugin.getLogger().warning("Failed to send Discord weekly report: " + throwable.getMessage());
                    return null;
                });
    }

    private CompletableFuture<Void> send(String content) {
        ConfigService config = plugin.configService();
        if (!config.discordConfigured()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Discord webhook is disabled or not configured"));
        }
        return webhookClient.send(config.discordWebhookUrl(), content);
    }

    private void sendSilently(String content) {
        if (!plugin.configService().discordConfigured()) {
            return;
        }
        send(content).exceptionally(throwable -> {
            plugin.getLogger().warning("Failed to send Discord webhook message: " + throwable.getMessage());
            return null;
        });
    }
}
