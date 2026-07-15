package fi.juhpaza.staffactivity;

import fi.juhpaza.staffactivity.command.StaffActivityCommand;
import fi.juhpaza.staffactivity.config.ConfigService;
import fi.juhpaza.staffactivity.message.MessageService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main Paper plugin entrypoint for StaffActivity.
 */
public final class StaffActivity extends JavaPlugin {
    private ConfigService configService;
    private MessageService messageService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.configService = new ConfigService(this);
        this.messageService = new MessageService(this);

        registerCommands();
        getLogger().info("StaffActivity enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("StaffActivity disabled.");
    }

    public ConfigService configService() {
        return configService;
    }

    public MessageService messageService() {
        return messageService;
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
