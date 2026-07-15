package fi.juhpaza.staffactivity.gui;

import fi.juhpaza.staffactivity.StaffActivity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles clicks inside the StaffActivity management GUI.
 */
public final class StaffActivityGuiListener implements Listener {
    private final StaffActivity plugin;

    public StaffActivityGuiListener(StaffActivity plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof StaffActivityGuiHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (holder.view() == StaffActivityGuiView.STAFF_SUMMARY) {
            switch (event.getRawSlot()) {
                case 29 -> runSummaryCommand(player, "today", holder.targetName());
                case 30 -> runSummaryCommand(player, "sessions", holder.targetName());
                case 32 -> runSummaryCommand(player, "week", holder.targetName());
                case 40 -> player.closeInventory();
                default -> {
                }
            }
            return;
        }

        switch (event.getRawSlot()) {
            case 12, 16 -> sendDiscordTest(player);
            case 14 -> plugin.staffActivityGui().open(player);
            case 15 -> reload(player);
            case 19 -> runCommand(player, "staffactivity top online");
            case 20 -> runCommand(player, "staffactivity top active");
            case 21 -> runCommand(player, "staffactivity top actions");
            case 40 -> player.closeInventory();
            default -> {
            }
        }
    }

    private void sendDiscordTest(Player player) {
        player.closeInventory();
        plugin.discordReportService().sendTest()
                .whenComplete((ignored, throwable) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (throwable != null) {
                        plugin.getLogger().warning("Discord webhook GUI test failed: " + throwable.getMessage());
                        plugin.messageService().send(player, "commands.discord-test.failed");
                        return;
                    }
                    plugin.messageService().send(player, "commands.discord-test.sent");
                }));
    }

    private void reload(Player player) {
        plugin.reloadConfig();
        plugin.configService().reload();
        plugin.messageService().reload();
        plugin.restartDiscordReports();
        plugin.messageService().send(player, "commands.reload");
        plugin.staffActivityGui().open(player);
    }

    private void runCommand(Player player, String command) {
        player.closeInventory();
        player.performCommand(command);
    }

    private void runSummaryCommand(Player player, String subCommand, String targetName) {
        if (targetName == null || targetName.isBlank()) {
            return;
        }
        runCommand(player, "staffactivity " + subCommand + " " + targetName);
    }
}
