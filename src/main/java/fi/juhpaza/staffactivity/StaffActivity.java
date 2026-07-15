package fi.juhpaza.staffactivity;

import fi.juhpaza.staffactivity.command.StaffActivityCommand;
import fi.juhpaza.staffactivity.config.ConfigService;
import fi.juhpaza.staffactivity.database.DatabaseService;
import fi.juhpaza.staffactivity.listener.StaffActivityListener;
import fi.juhpaza.staffactivity.message.MessageService;
import fi.juhpaza.staffactivity.model.SessionCloseReason;
import fi.juhpaza.staffactivity.model.SessionSnapshot;
import fi.juhpaza.staffactivity.service.SessionService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main Paper plugin entrypoint for StaffActivity.
 */
public final class StaffActivity extends JavaPlugin {
    private ConfigService configService;
    private DatabaseService databaseService;
    private MessageService messageService;
    private SessionService sessionService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.configService = new ConfigService(this);
        this.databaseService = new DatabaseService(this);
        this.messageService = new MessageService(this);
        this.sessionService = new SessionService(configService.afkTimeout());

        registerCommands();
        getServer().getPluginManager().registerEvents(new StaffActivityListener(this), this);
        databaseService.initialize().whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                getLogger().severe("Database initialization failed: " + throwable.getMessage());
            } else {
                getLogger().info("Database initialized.");
            }
        });
        getLogger().info("StaffActivity enabled.");
    }

    @Override
    public void onDisable() {
        if (databaseService != null) {
            if (sessionService != null) {
                sessionService.closeAll(java.time.Instant.now(), SessionCloseReason.PLUGIN_SHUTDOWN)
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
