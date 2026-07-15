package fi.juhpaza.staffactivity.command;

import fi.juhpaza.staffactivity.StaffActivity;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

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
            plugin.messageService().send(sender, "commands.usage");
            return true;
        }

        return switch (args[0].toLowerCase()) {
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
        plugin.messageService().send(sender, "commands.debug.database", "database", "not-initialized");
        plugin.messageService().send(sender, "commands.debug.active-sessions", "count", "0");
        plugin.messageService().send(sender, "commands.debug.pending-writes", "count", "0");
        plugin.messageService().send(sender, "commands.debug.discord", "status", plugin.configService().discordEnabled() ? "enabled" : "disabled");
        plugin.messageService().send(sender, "commands.debug.timezone", "timezone", plugin.configService().timezoneId());
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
}
