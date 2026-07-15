package fi.juhpaza.staffactivity.message;

import java.io.File;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Sends MiniMessage-based StaffActivity messages.
 */
public final class MessageService {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private FileConfiguration messages;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        this.messages = YamlConfiguration.loadConfiguration(file);
    }

    public void send(CommandSender sender, String path, String... placeholders) {
        sender.sendMessage(resolve(path, placeholders));
    }

    private Component resolve(String path, String... placeholders) {
        String prefix = messages.getString("prefix", "<gold>StaffActivity</gold>");
        String raw = messages.getString(path, "<red>Missing message: " + path + "</red>");
        String rendered = prefix + " <dark_gray>|</dark_gray> " + applyPlaceholders(raw, placeholders);
        return miniMessage.deserialize(rendered);
    }

    private String applyPlaceholders(String input, String... placeholders) {
        String output = input;
        for (int index = 0; index + 1 < placeholders.length; index += 2) {
            output = output.replace("<" + placeholders[index] + ">", placeholders[index + 1]);
        }
        return output;
    }
}
