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
        this.afkTimeout = Duration.ofSeconds(Math.max(30, config.getLong("activity.afk-timeout-seconds", 300)));
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

    public Duration afkTimeout() {
        return afkTimeout;
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
}
