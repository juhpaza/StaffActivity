package fi.juhpaza.staffactivity.config;

import java.time.DateTimeException;
import java.time.Duration;
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
    private Duration afkTimeout;

    public ConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        this.timezone = parseTimezone(config.getString("plugin.timezone", "Europe/Helsinki"));
        this.discordEnabled = config.getBoolean("discord.enabled", false);
        this.afkTimeout = Duration.ofSeconds(Math.max(30, config.getLong("activity.afk-timeout-seconds", 300)));
    }

    public String timezoneId() {
        return timezone.getId();
    }

    public boolean discordEnabled() {
        return discordEnabled;
    }

    public Duration afkTimeout() {
        return afkTimeout;
    }

    private ZoneId parseTimezone(String value) {
        try {
            return ZoneId.of(value);
        } catch (DateTimeException ex) {
            plugin.getLogger().warning("Invalid plugin.timezone '" + value + "', falling back to Europe/Helsinki.");
            return ZoneId.of("Europe/Helsinki");
        }
    }
}
