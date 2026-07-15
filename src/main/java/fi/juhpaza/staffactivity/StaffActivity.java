package fi.juhpaza.staffactivity;

import fi.juhpaza.staffactivity.command.StaffActivityCommand;
import fi.juhpaza.staffactivity.config.ConfigService;
import fi.juhpaza.staffactivity.database.DatabaseMigrator;
import fi.juhpaza.staffactivity.database.DatabaseService;
import fi.juhpaza.staffactivity.discord.DiscordReportService;
import fi.juhpaza.staffactivity.gui.StaffActivityGui;
import fi.juhpaza.staffactivity.gui.StaffActivityGuiListener;
import fi.juhpaza.staffactivity.listener.StaffActivityListener;
import fi.juhpaza.staffactivity.message.MessageService;
import fi.juhpaza.staffactivity.model.SessionCloseReason;
import fi.juhpaza.staffactivity.model.SessionSnapshot;
import fi.juhpaza.staffactivity.service.SessionService;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import org.bukkit.command.PluginCommand;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main Paper plugin entrypoint for StaffActivity.
 */
public final class StaffActivity extends JavaPlugin {
    private static final List<String> REQUIRED_TABLES = List.of(
            "schema_version",
            "staff_members",
            "staff_sessions",
            "staff_daily_stats",
            "staff_actions",
            "staff_active_sessions",
            "staff_teleport_events"
    );

    private ConfigService configService;
    private DatabaseService databaseService;
    private DiscordReportService discordReportService;
    private StaffActivityGui staffActivityGui;
    private MessageService messageService;
    private SessionService sessionService;
    private BukkitTask autosaveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveDefaultMessages();

        this.configService = new ConfigService(this);
        this.databaseService = new DatabaseService(this);
        this.discordReportService = new DiscordReportService(this);
        this.staffActivityGui = new StaffActivityGui(this);
        this.messageService = new MessageService(this);
        this.sessionService = new SessionService(configService.afkTimeout());

        registerCommands();
        getServer().getPluginManager().registerEvents(new StaffActivityListener(this), this);
        getServer().getPluginManager().registerEvents(new StaffActivityGuiListener(this), this);
        logStartupConfiguration();
        databaseService.initialize(configService.timezone()).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                getLogger().severe("Database initialization failed: " + throwable.getMessage());
            } else {
                getServer().getScheduler().runTask(this, () -> {
                    startAutosave();
                    discordReportService.start();
                    getLogger().info("Database: SQLite connection established.");
                    logDatabaseStartupDiagnostics();
                });
            }
        });
        getLogger().info("StaffActivity ready.");
    }

    @Override
    public void onDisable() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
        }
        if (discordReportService != null) {
            discordReportService.stop();
        }
        if (databaseService != null) {
            if (sessionService != null) {
                sessionService.closeAll(Instant.now(), SessionCloseReason.PLUGIN_SHUTDOWN)
                        .forEach(this::persistClosedSession);
            }
            databaseService.close();
        }
        getLogger().info("StaffActivity disabled.");
    }

    public ConfigService configService() {
        return configService;
    }

    public DatabaseService databaseService() {
        return databaseService;
    }

    public DiscordReportService discordReportService() {
        return discordReportService;
    }

    public StaffActivityGui staffActivityGui() {
        return staffActivityGui;
    }

    public MessageService messageService() {
        return messageService;
    }

    public SessionService sessionService() {
        return sessionService;
    }

    public void persistClosedSession(SessionSnapshot snapshot) {
        databaseService.saveClosedSession(snapshot, configService.timezone())
                .exceptionally(throwable -> {
                    getLogger().warning("Failed to persist staff session for " + snapshot.uuid() + ": " + throwable.getMessage());
                    if (discordReportService != null) {
                        discordReportService.pluginError("Staff-session tallennus epäonnistui. Katso konsoliloki.");
                    }
                    return null;
                });
    }

    public void restartDiscordReports() {
        discordReportService.start();
    }

    private void startAutosave() {
        long intervalTicks = Math.max(20L, configService.autosaveInterval().toSeconds() * 20L);
        autosaveTask = getServer().getScheduler().runTaskTimer(this, this::autosaveActiveSessions, intervalTicks, intervalTicks);
    }

    private void autosaveActiveSessions() {
        List<SessionSnapshot> snapshots = sessionService.snapshots(Instant.now());
        if (snapshots.isEmpty()) {
            return;
        }
        databaseService.saveActiveSessions(snapshots)
                .exceptionally(throwable -> {
                    getLogger().warning("Failed to autosave active staff sessions: " + throwable.getMessage());
                    return null;
                });
    }

    private void registerCommands() {
        PluginCommand command = getCommand("staffactivity");
        if (command == null) {
            throw new IllegalStateException("staffactivity command is missing from plugin.yml");
        }

        StaffActivityCommand executor = new StaffActivityCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void saveDefaultMessages() {
        if (!Files.exists(getDataFolder().toPath().resolve("messages.yml"))) {
            saveResource("messages.yml", false);
        }
    }

    private void logStartupConfiguration() {
        getLogger().info("Loading StaffActivity " + getPluginMeta().getVersion());
        getLogger().info("Runtime: Java " + System.getProperty("java.version"));
        getLogger().info("Server: " + getServer().getVersion());
        getLogger().info("Database: preparing SQLite schema target v" + DatabaseMigrator.CURRENT_SCHEMA_VERSION + ".");
        getLogger().info("Config: timezone " + configService.timezoneId() + ".");
        getLogger().info("Tracking: permission '" + configService.trackingPermission() + "'.");
        getLogger().info("Tracking: OP auto-track " + enabledDisabled(configService.trackOperatorsAutomatically()) + ".");
        getLogger().info("Autosave: active session snapshots every " + configService.autosaveInterval().toSeconds() + " seconds.");
        getLogger().info("Features: command tracking " + enabledDisabled(configService.commandTrackingEnabled()) + ".");
        getLogger().info("Features: teleport history " + enabledDisabled(configService.teleportTrigger()) + ".");
        getLogger().info("Discord: reports " + enabledDisabled(configService.discordEnabled()) + ".");
        getLogger().info("Discord: webhook configured " + yesNo(configService.discordConfigured()) + ".");
    }

    private void logDatabaseStartupDiagnostics() {
        databaseService.findMissingTables(REQUIRED_TABLES).whenComplete((missingTables, throwable) ->
                getServer().getScheduler().runTask(this, () -> {
                    if (throwable != null) {
                        getLogger().warning("Database startup verification failed: " + throwable.getMessage());
                    } else if (missingTables.isEmpty()) {
                        getLogger().info("Database: all required tables are present.");
                    } else {
                        getLogger().severe("Database: missing required tables: " + String.join(", ", missingTables));
                    }
                }));
    }

    private String enabledDisabled(boolean value) {
        return value ? "enabled" : "disabled";
    }

    private String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
