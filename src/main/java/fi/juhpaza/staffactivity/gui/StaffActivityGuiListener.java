package fi.juhpaza.staffactivity.gui;

import fi.juhpaza.staffactivity.StaffActivity;
import fi.juhpaza.staffactivity.model.RecentTeleport;
import fi.juhpaza.staffactivity.model.StaffSummary;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
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
        if (targetName == null || targetName.isBlank()) {
            return;
        }
        plugin.databaseService().findSummaryByName(targetName)
                .thenCompose(summary -> summary
                        .map(value -> plugin.databaseService().findRecentTeleports(value.uuid(), 5)
                                .thenApply(teleports -> new TeleportResult(value, teleports)))
                        .orElseGet(() -> CompletableFuture.completedFuture(new TeleportResult(null, List.of()))))
                .whenComplete((result, throwable) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (throwable != null) {
                        plugin.getLogger().warning("Teleport history query failed: " + throwable.getMessage());
                        plugin.messageService().send(player, "commands.query-failed");
                        return;
                    }
                    if (result.summary() == null) {
                        plugin.messageService().send(player, "commands.player-not-found", "player", targetName);
                        return;
                    }
                    renderTeleports(player, result.summary(), result.teleports());
                }));
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

    private void renderTeleports(Player player, StaffSummary summary, List<RecentTeleport> teleports) {
        player.sendMessage(Component.text("StaffActivity", NamedTextColor.GOLD)
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Viimeisimmät teleportit: ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(summary.latestName(), NamedTextColor.WHITE)));
        if (teleports.isEmpty()) {
            player.sendMessage(Component.text("Tallennettuja teleportteja ei vielä löytynyt.", NamedTextColor.GRAY));
            return;
        }
        for (RecentTeleport teleport : teleports) {
            player.sendMessage(Component.text(teleport.createdAt(), NamedTextColor.DARK_GRAY)
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(teleport.cause(), NamedTextColor.YELLOW))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(location(teleport.fromWorld(), teleport.fromX(), teleport.fromY(), teleport.fromZ()), NamedTextColor.GRAY))
                    .append(Component.text(" -> ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(location(teleport.toWorld(), teleport.toX(), teleport.toY(), teleport.toZ()), NamedTextColor.WHITE))
                    .append(Component.text(" | vanish: ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(vanishLabel(teleport.vanished()), vanishColor(teleport.vanished()))));
        }
    }

    private String location(String world, double x, double y, double z) {
        return world + " " + coordinate(x) + " " + coordinate(y) + " " + coordinate(z);
    }

    private String coordinate(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String vanishLabel(Boolean vanished) {
        if (vanished == null) {
            return "tuntematon";
        }
        return vanished ? "kyllä" : "ei";
    }

    private NamedTextColor vanishColor(Boolean vanished) {
        if (vanished == null) {
            return NamedTextColor.GRAY;
        }
        return vanished ? NamedTextColor.GREEN : NamedTextColor.RED;
    }

    private record TeleportResult(StaffSummary summary, List<RecentTeleport> teleports) {
    }
}
