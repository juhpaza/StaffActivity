package fi.juhpaza.staffactivity.gui;

import fi.juhpaza.staffactivity.StaffActivity;
import fi.juhpaza.staffactivity.model.StaffSummary;
import fi.juhpaza.staffactivity.util.DurationFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Builds the in-game StaffActivity management inventory.
 */
public final class StaffActivityGui {
    public static final int SUMMARY_SIZE = 45;

    private final StaffActivity plugin;
    private final StaffActivityDashboardGui dashboardGui;
    private final StaffActivitySessionsGui sessionsGui;
    private final StaffActivityStatsMenuGui statsMenuGui;

    public StaffActivityGui(StaffActivity plugin) {
        this.plugin = plugin;
        this.dashboardGui = new StaffActivityDashboardGui(plugin);
        this.sessionsGui = new StaffActivitySessionsGui(plugin);
        this.statsMenuGui = new StaffActivityStatsMenuGui();
    }

    public void open(Player player) {
        dashboardGui.open(player);
    }

    public void openOnlineStaff(Player player) {
        sessionsGui.open(player);
    }

    public void openTopStatistics(Player player) {
        statsMenuGui.openTop(player);
    }

    public void openTodayStatistics(Player player) {
        statsMenuGui.openToday(player);
    }

    public void openWeeklyStatistics(Player player) {
        statsMenuGui.openWeek(player);
    }

    public void openSummary(Player player, StaffSummary summary) {
        plugin.databaseService().countTeleports(summary.uuid()).whenComplete((teleportEvents, throwable) ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    int displayedTeleports = summary.totalTeleports();
                    if (throwable != null) {
                        plugin.getLogger().warning("Failed to count teleport events for " + summary.latestName() + ": " + throwable.getMessage());
                    } else {
                        displayedTeleports = Math.max(summary.totalTeleports(), teleportEvents);
                    }
                    openSummary(player, summary, displayedTeleports);
                }));
    }

    private void openSummary(Player player, StaffSummary summary, int displayedTeleports) {
        Inventory inventory = Bukkit.createInventory(
                new StaffActivityGuiHolder(StaffActivityGuiView.STAFF_SUMMARY, summary.latestName()),
                SUMMARY_SIZE,
                StaffActivityGuiItems.title(summary.latestName(), NamedTextColor.GOLD)
        );
        fillSummary(inventory, summary, displayedTeleports);
        player.openInventory(inventory);
    }

    private void fillSummary(Inventory inventory, StaffSummary summary, int displayedTeleports) {
        UUID uuid = UUID.fromString(summary.uuid());
        long activityPercent = activityPercent(summary.totalOnlineSeconds(), summary.totalActiveSeconds());
        inventory.setItem(4, StaffActivityGuiItems.playerHead(
                uuid,
                summary.latestName(),
                NamedTextColor.GOLD,
                List.of(
                        "UUID: " + summary.uuid(),
                        "Ensimmäinen havainto: " + summary.firstSeen(),
                        "Viimeksi nähty: " + summary.lastSeen()
                )
        ));
        inventory.setItem(10, StaffActivityGuiItems.item(
                Material.CLOCK,
                "Online-aika",
                NamedTextColor.GREEN,
                List.of(
                        DurationFormatter.seconds(summary.totalOnlineSeconds()),
                        "Sessiot: " + summary.totalSessions()
                )
        ));
        inventory.setItem(11, StaffActivityGuiItems.item(
                Material.LIME_DYE,
                "Aktiivinen aika",
                NamedTextColor.GREEN,
                List.of(
                        DurationFormatter.seconds(summary.totalActiveSeconds()),
                        "Aktiivisuus: " + activityPercent + "%"
                )
        ));
        inventory.setItem(12, StaffActivityGuiItems.item(
                Material.GRAY_DYE,
                "AFK-aika",
                NamedTextColor.GRAY,
                List.of(DurationFormatter.seconds(summary.totalAfkSeconds()))
        ));
        inventory.setItem(14, StaffActivityGuiItems.item(
                Material.COMMAND_BLOCK,
                "Kirjatut komennot",
                NamedTextColor.AQUA,
                List.of(
                        "Yhteensä: " + summary.totalCommands(),
                        "Laskee staffin käyttämät sallitut komennot.",
                        "Tallentaa vain komentojen määrän.",
                        "Argumentteja tai salasanoja ei tallenneta.",
                        "Rajatut komennot ohitetaan configista."
                )
        ));
        inventory.setItem(15, StaffActivityGuiItems.item(
                Material.ENDER_PEARL,
                "Teleportit",
                NamedTextColor.LIGHT_PURPLE,
                List.of(
                        "Yhteensä: " + displayedTeleports,
                        "Sessioihin kirjattu: " + summary.totalTeleports(),
                        "Klikkaa nähdäksesi lisätiedot."
                )
        ));
        inventory.setItem(16, StaffActivityGuiItems.item(
                Material.GRASS_BLOCK,
                "Gamemode",
                NamedTextColor.YELLOW,
                List.of(
                        "Nykyinen: " + currentGamemode(uuid),
                        "Vaihdot yhteensä: " + summary.totalGamemodeChanges()
                )
        ));
        inventory.setItem(22, StaffActivityGuiItems.item(
                Material.NETHER_STAR,
                "Staff-toimet",
                NamedTextColor.RED,
                List.of(Integer.toString(summary.totalStaffActions()))
        ));
        inventory.setItem(29, StaffActivityGuiItems.actionItem(Material.MAP, "Tämän päivän tilasto", NamedTextColor.YELLOW, "Klikkaa avataksesi päivän yhteenvedon chattiin."));
        inventory.setItem(30, StaffActivityGuiItems.actionItem(Material.BOOK, "Viimeisimmät jaksot", NamedTextColor.AQUA, "Klikkaa avataksesi viimeisimmät aktiivisuusjaksot chattiin."));
        inventory.setItem(32, StaffActivityGuiItems.actionItem(Material.PAPER, "Tämän viikon tilasto", NamedTextColor.GREEN, "Klikkaa avataksesi viikon yhteenvedon chattiin."));
        inventory.setItem(40, StaffActivityGuiItems.actionItem(Material.BARRIER, "Sulje", NamedTextColor.RED, "Sulkee tilastonäkymän."));
    }

    private long activityPercent(long onlineSeconds, long activeSeconds) {
        if (onlineSeconds <= 0) {
            return 0;
        }
        return Math.round((activeSeconds * 100.0) / onlineSeconds);
    }

    private String currentGamemode(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return "ei online";
        }
        return player.getGameMode().name().toLowerCase(Locale.ROOT);
    }
}
