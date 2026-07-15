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

    private final StaffActivityDashboardGui dashboardGui;
    private final StaffActivitySessionsGui sessionsGui;
    private final StaffActivityStatsMenuGui statsMenuGui;

    public StaffActivityGui(StaffActivity plugin) {
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
        Inventory inventory = Bukkit.createInventory(
                new StaffActivityGuiHolder(StaffActivityGuiView.STAFF_SUMMARY, summary.latestName()),
                SUMMARY_SIZE,
                StaffActivityGuiItems.title(summary.latestName(), NamedTextColor.GOLD)
        );
        fillSummary(inventory, summary);
        player.openInventory(inventory);
    }

    private void fillSummary(Inventory inventory, StaffSummary summary) {
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
                "Komennot",
                NamedTextColor.AQUA,
                List.of(Integer.toString(summary.totalCommands()))
        ));
        inventory.setItem(15, StaffActivityGuiItems.item(
                Material.ENDER_PEARL,
                "Teleportit",
                NamedTextColor.LIGHT_PURPLE,
                List.of(
                        "Yhteensä: " + summary.totalTeleports(),
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
        inventory.setItem(30, StaffActivityGuiItems.actionItem(Material.BOOK, "Viimeisimmät sessiot", NamedTextColor.AQUA, "Klikkaa avataksesi viimeisimmät sessiot chattiin."));
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
