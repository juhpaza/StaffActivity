package fi.juhpaza.staffactivity.gui;

import fi.juhpaza.staffactivity.StaffActivity;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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

        switch (holder.view()) {
            case DASHBOARD -> handleDashboardClick(player, event.getRawSlot());
            case ONLINE_STAFF -> handleOnlineStaffClick(player, event.getRawSlot());
            case TOP_STATISTICS -> handleTopStatisticsClick(player, event.getRawSlot());
            case TODAY_STATISTICS -> handlePeriodStatisticsClick(player, event.getRawSlot(), "today");
            case WEEKLY_STATISTICS -> handlePeriodStatisticsClick(player, event.getRawSlot(), "week");
            case STAFF_SUMMARY -> handleSummaryClick(player, event.getRawSlot(), holder.targetName());
        }
    }

    private void handleDashboardClick(Player player, int slot) {
        switch (slot) {
            case 19 -> plugin.staffActivityGui().openOnlineStaff(player);
            case 20 -> plugin.staffActivityGui().openTopStatistics(player);
            case 21 -> plugin.staffActivityGui().openTodayStatistics(player);
            case 22 -> plugin.staffActivityGui().openWeeklyStatistics(player);
            case 23 -> comingSoon(player);
            case 46 -> plugin.staffActivityGui().open(player);
            case 48 -> reload(player);
            case 50 -> sendDiscordTest(player);
            case 52 -> player.closeInventory();
            default -> {
            }
        }
    }

    private void handleOnlineStaffClick(Player player, int slot) {
        switch (slot) {
            case 45 -> plugin.staffActivityGui().open(player);
            case 49 -> plugin.staffActivityGui().openOnlineStaff(player);
            case 53 -> player.closeInventory();
            default -> {
            }
        }
    }

    private void handleTopStatisticsClick(Player player, int slot) {
        switch (slot) {
            case 20 -> runCommand(player, "staffactivity top online");
            case 22 -> runCommand(player, "staffactivity top active");
            case 24 -> runCommand(player, "staffactivity top actions");
            case 45 -> plugin.staffActivityGui().open(player);
            case 53 -> player.closeInventory();
            default -> {
            }
        }
    }

    private void handlePeriodStatisticsClick(Player player, int slot, String periodCommand) {
        switch (slot) {
            case 21 -> runCommand(player, "staffactivity " + periodCommand);
            case 23 -> comingSoon(player);
            case 45 -> plugin.staffActivityGui().open(player);
            case 53 -> player.closeInventory();
            default -> {
            }
        }
    }

    private void handleSummaryClick(Player player, int slot, String targetName) {
        switch (slot) {
            case 15 -> showTeleportDetailsUnavailable(player, targetName);
            case 16 -> showCurrentGamemode(player, targetName);
            case 29 -> runSummaryCommand(player, "today", targetName);
            case 30 -> runSummaryCommand(player, "sessions", targetName);
            case 32 -> runSummaryCommand(player, "week", targetName);
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

    private void comingSoon(Player player) {
        player.sendMessage("Tulossa myöhemmin");
    }

    private void showTeleportDetailsUnavailable(Player player, String targetName) {
        player.closeInventory();
        player.sendMessage(Component.text("StaffActivity", NamedTextColor.GOLD)
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Teleportit: " + targetName, NamedTextColor.LIGHT_PURPLE)));
        player.sendMessage(Component.text("Nykyinen versio tallentaa teleporttien kokonaismäärän.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("Mistä-mihin, koordinaatit ja vanish-tila vaativat teleporttihistorian tallennuksen.", NamedTextColor.GRAY));
    }

    private void showCurrentGamemode(Player player, String targetName) {
        player.closeInventory();
        Player target = Bukkit.getPlayerExact(targetName);
        String gamemode = target == null ? "ei online" : target.getGameMode().name().toLowerCase(Locale.ROOT);
        player.sendMessage(Component.text("StaffActivity", NamedTextColor.GOLD)
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Gamemode: " + targetName, NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Nykyinen: " + gamemode, NamedTextColor.GRAY));
    }

    private void runSummaryCommand(Player player, String subCommand, String targetName) {
        if (targetName == null || targetName.isBlank()) {
            return;
        }
        runCommand(player, "staffactivity " + subCommand + " " + targetName);
    }
}
