package fi.juhpaza.staffactivity.config;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Reads validated StaffActivity configuration values.
 */
public final class ConfigService {
    private final JavaPlugin plugin;
    private ZoneId timezone;
    private boolean discordEnabled;
    private String discordWebhookUrl;
    private boolean discordDailyReportEnabled;
    private LocalTime discordDailyReportTime;
    private boolean discordWeeklyReportEnabled;
    private DayOfWeek discordWeeklyReportDay;
    private LocalTime discordWeeklyReportTime;
    private boolean discordStaffJoinEnabled;
    private boolean discordStaffQuitEnabled;
    private boolean discordPluginErrorsEnabled;
    private Duration afkTimeout;
    private Duration autosaveInterval;
    private String trackingPermission;
    private boolean trackOperatorsAutomatically;
    private double movementThresholdSquared;
    private double rotationThresholdDegrees;
    private boolean movementTrigger;
    private boolean rotationTrigger;
    private boolean chatTrigger;
    private boolean commandsTrigger;
    private boolean inventoryTrigger;
    private boolean blockInteractionTrigger;
    private boolean entityInteractionTrigger;
    private boolean teleportTrigger;
    private boolean gamemodeChangeTrigger;
    private boolean commandTrackingEnabled;
    private java.util.Set<String> excludedCommands;

    public ConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        this.timezone = parseTimezone(config.getString("plugin.timezone", "Europe/Helsinki"));
        this.discordEnabled = config.getBoolean("discord.enabled", false);
        this.discordWebhookUrl = config.getString("discord.webhook-url", "");
        this.discordDailyReportEnabled = config.getBoolean("discord.reports.daily.enabled", false);
        this.discordDailyReportTime = parseTime(config.getString("discord.reports.daily.time", "23:55"), LocalTime.of(23, 55), "discord.reports.daily.time");
        this.discordWeeklyReportEnabled = config.getBoolean("discord.reports.weekly.enabled", false);
        this.discordWeeklyReportDay = parseDay(config.getString("discord.reports.weekly.day", "SUNDAY"));
        this.discordWeeklyReportTime = parseTime(config.getString("discord.reports.weekly.time", "20:00"), LocalTime.of(20, 0), "discord.reports.weekly.time");
        this.discordStaffJoinEnabled = config.getBoolean("discord.events.staff-join", false);
        this.discordStaffQuitEnabled = config.getBoolean("discord.events.staff-quit", false);
        this.discordPluginErrorsEnabled = config.getBoolean("discord.events.plugin-errors", true);
        this.afkTimeout = Duration.ofSeconds(Math.max(30, config.getLong("activity.afk-timeout-seconds", 300)));
        this.autosaveInterval = Duration.ofSeconds(Math.max(10, config.getLong("storage.autosave-interval-seconds", 60)));
        this.trackingPermission = config.getString("tracking.permission", "staffactivity.track");
        this.trackOperatorsAutomatically = config.getBoolean("tracking.track-operators-automatically", false);
        double movementThreshold = Math.max(0.0, config.getDouble("activity.movement-threshold", 0.25));
        this.movementThresholdSquared = movementThreshold * movementThreshold;
        this.rotationThresholdDegrees = Math.max(0.0, config.getDouble("activity.rotation-threshold-degrees", 10.0));
        this.movementTrigger = config.getBoolean("activity.triggers.movement", true);
        this.rotationTrigger = config.getBoolean("activity.triggers.rotation", true);
        this.chatTrigger = config.getBoolean("activity.triggers.chat", true);
        this.commandsTrigger = config.getBoolean("activity.triggers.commands", true);
        this.inventoryTrigger = config.getBoolean("activity.triggers.inventory", true);
        this.blockInteractionTrigger = config.getBoolean("activity.triggers.block-interaction", true);
        this.entityInteractionTrigger = config.getBoolean("activity.triggers.entity-interaction", true);
        this.teleportTrigger = config.getBoolean("activity.triggers.teleport", true);
        this.gamemodeChangeTrigger = config.getBoolean("activity.triggers.gamemode-change", true);
        this.commandTrackingEnabled = config.getBoolean("command-tracking.enabled", true);
        this.excludedCommands = config.getStringList("command-tracking.excluded-commands").stream()
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public String timezoneId() {
        return timezone.getId();
    }

    public ZoneId timezone() {
        return timezone;
    }

    public boolean discordEnabled() {
        return discordEnabled;
    }

    public boolean discordConfigured() {
        return discordEnabled && discordWebhookUrl != null && !discordWebhookUrl.isBlank();
    }

    public String discordWebhookUrl() {
        return discordWebhookUrl;
    }

    public boolean discordDailyReportEnabled() {
        return discordDailyReportEnabled;
    }

    public LocalTime discordDailyReportTime() {
        return discordDailyReportTime;
    }

    public boolean discordWeeklyReportEnabled() {
        return discordWeeklyReportEnabled;
    }

    public DayOfWeek discordWeeklyReportDay() {
        return discordWeeklyReportDay;
    }

    public LocalTime discordWeeklyReportTime() {
        return discordWeeklyReportTime;
    }

    public boolean discordStaffJoinEnabled() {
        return discordStaffJoinEnabled;
    }

    public boolean discordStaffQuitEnabled() {
        return discordStaffQuitEnabled;
    }

    public boolean discordPluginErrorsEnabled() {
        return discordPluginErrorsEnabled;
    }

    public Duration afkTimeout() {
        return afkTimeout;
    }

    public Duration autosaveInterval() {
        return autosaveInterval;
    }

    public String trackingPermission() {
        return trackingPermission;
    }

    public boolean trackOperatorsAutomatically() {
        return trackOperatorsAutomatically;
    }

    public double movementThresholdSquared() {
        return movementThresholdSquared;
    }

    public double rotationThresholdDegrees() {
        return rotationThresholdDegrees;
    }

    public boolean movementTrigger() {
        return movementTrigger;
    }

    public boolean rotationTrigger() {
        return rotationTrigger;
    }

    public boolean chatTrigger() {
        return chatTrigger;
    }

    public boolean commandsTrigger() {
        return commandsTrigger;
    }

    public boolean inventoryTrigger() {
        return inventoryTrigger;
    }

    public boolean blockInteractionTrigger() {
        return blockInteractionTrigger;
    }

    public boolean entityInteractionTrigger() {
        return entityInteractionTrigger;
    }

    public boolean teleportTrigger() {
        return teleportTrigger;
    }

    public boolean gamemodeChangeTrigger() {
        return gamemodeChangeTrigger;
    }

    public boolean commandTrackingEnabled() {
        return commandTrackingEnabled;
    }

    public java.util.Set<String> excludedCommands() {
        return excludedCommands;
    }

    private ZoneId parseTimezone(String value) {
        try {
            return ZoneId.of(value);
        } catch (DateTimeException ex) {
            plugin.getLogger().warning("Invalid plugin.timezone '" + value + "', falling back to Europe/Helsinki.");
            return ZoneId.of("Europe/Helsinki");
        }
    }

    private LocalTime parseTime(String value, LocalTime fallback, String path) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeException | NullPointerException ex) {
            plugin.getLogger().warning("Invalid " + path + " '" + value + "', falling back to " + fallback + ".");
            return fallback;
        }
    }

    private DayOfWeek parseDay(String value) {
        try {
            return DayOfWeek.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            plugin.getLogger().warning("Invalid discord.reports.weekly.day '" + value + "', falling back to SUNDAY.");
            return DayOfWeek.SUNDAY;
        }
    }
}
