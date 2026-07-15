package fi.juhpaza.staffactivity;

import fi.juhpaza.staffactivity.command.StaffActivityCommand;
import fi.juhpaza.staffactivity.config.ConfigService;
import fi.juhpaza.staffactivity.database.DatabaseService;
import fi.juhpaza.staffactivity.discord.DiscordReportService;
import fi.juhpaza.staffactivity.listener.StaffActivityListener;
import fi.juhpaza.staffactivity.message.MessageService;
import fi.juhpaza.staffactivity.model.SessionCloseReason;
import fi.juhpaza.staffactivity.model.SessionSnapshot;
import fi.juhpaza.staffactivity.service.SessionService;
import java.time.Instant;
import java.util.List;
import org.bukkit.command.PluginCommand;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main Paper plugin entrypoint for StaffActivity.
 */
public final class StaffActivity extends JavaPlugin {
    private ConfigService configService;
    private DatabaseService databaseService;
    private DiscordReportService discordReportService;
    private MessageService messageService;
    private SessionService sessionService;
    private BukkitTask autosaveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.configService = new ConfigService(this);
        this.databaseService = new DatabaseService(this);
        this.discordReportService = new DiscordReportService(this);
        this.messageService = new MessageService(this);
        this.sessionService = new SessionService(configService.afkTimeout());

        registerCommands();
        getServer().getPluginManager().registerEvents(new StaffActivityListener(this), this);
        databaseService.initialize(configService.timezone()).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                getLogger().severe("Database initialization failed: " + throwable.getMessage());
            } else {
                getServer().getScheduler().runTask(this, () -> {
                    startAutosave();
                    discordReportService.start();
                    getLogger().info("Database initialized.");
                });
            }
        });
        getLogger().info("StaffActivity enabled.");
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
}
