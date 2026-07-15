package fi.juhpaza.staffactivity.gui;

import fi.juhpaza.staffactivity.StaffActivity;
import fi.juhpaza.staffactivity.model.SessionSnapshot;
import fi.juhpaza.staffactivity.model.StaffSummary;
import fi.juhpaza.staffactivity.util.DurationFormatter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Builds the in-game StaffActivity management inventory.
 */
public final class StaffActivityGui {
    public static final int SIZE = 45;

    private final StaffActivity plugin;

    public StaffActivityGui(StaffActivity plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(
                new StaffActivityGuiHolder(StaffActivityGuiView.ADMIN),
                SIZE,
                Component.text("StaffActivity", NamedTextColor.GOLD)
        );
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openSummary(Player player, StaffSummary summary) {
        Inventory inventory = Bukkit.createInventory(
                new StaffActivityGuiHolder(StaffActivityGuiView.STAFF_SUMMARY, summary.latestName()),
                SIZE,
                Component.text("StaffActivity: " + summary.latestName(), NamedTextColor.GOLD)
        );
        fillSummary(inventory, summary);
        player.openInventory(inventory);
    }

    private void fill(Inventory inventory) {
        List<SessionSnapshot> snapshots = plugin.sessionService().snapshots(Instant.now());
        inventory.setItem(10, item(
                Material.CLOCK,
                "Aktiiviset sessiot",
                NamedTextColor.GREEN,
                List.of(
                        "Seurattuja staff-jäseniä nyt: " + snapshots.size(),
                        "Klikkaa refresh päivittääksesi näkymän."
                )
        ));
        inventory.setItem(11, item(
                Material.WRITABLE_BOOK,
                "Tietokanta",
                NamedTextColor.AQUA,
                List.of(
                        "Tila: " + plugin.databaseService().status().name().toLowerCase(),
                        "Odottavat kirjoitukset: " + plugin.databaseService().pendingOperations()
                )
        ));
        inventory.setItem(12, item(
                Material.REDSTONE,
                "Discord",
                plugin.configService().discordConfigured() ? NamedTextColor.GREEN : NamedTextColor.YELLOW,
                List.of(
                        "Käytössä: " + yesNo(plugin.configService().discordEnabled()),
                        "Webhook asetettu: " + yesNo(plugin.configService().discordConfigured()),
                        "Klikkaa testataksesi webhookin."
                )
        ));
        inventory.setItem(14, actionItem(Material.LIME_DYE, "Päivitä näkymä", NamedTextColor.GREEN, "Avaa GUI uudelleen tuoreilla tiedoilla."));
        inventory.setItem(15, actionItem(Material.COMPARATOR, "Reload", NamedTextColor.YELLOW, "Lataa config ja viestit uudelleen."));
        inventory.setItem(16, actionItem(Material.BELL, "Discord-testiviesti", NamedTextColor.LIGHT_PURPLE, "Lähettää testiviestin webhookiin."));
        inventory.setItem(19, actionItem(Material.GOLD_INGOT, "Top online", NamedTextColor.GOLD, "Avaa top online -lista chattiin."));
        inventory.setItem(20, actionItem(Material.EMERALD, "Top active", NamedTextColor.GREEN, "Avaa top active -lista chattiin."));
        inventory.setItem(21, actionItem(Material.NETHER_STAR, "Top actions", NamedTextColor.AQUA, "Avaa top actions -lista chattiin."));

        int slot = 27;
        for (SessionSnapshot snapshot : snapshots.stream().limit(9).toList()) {
            inventory.setItem(slot++, item(
                    Material.PLAYER_HEAD,
                    snapshot.latestName(),
                    NamedTextColor.WHITE,
                    List.of(
                            "Online: " + DurationFormatter.seconds(snapshot.onlineTime().toSeconds()),
                            "Active: " + DurationFormatter.seconds(snapshot.activeTime().toSeconds()),
                            "AFK: " + DurationFormatter.seconds(snapshot.afkTime().toSeconds()),
                            "Komennot: " + snapshot.commandCount()
                    )
            ));
        }

        inventory.setItem(40, actionItem(Material.BARRIER, "Sulje", NamedTextColor.RED, "Sulkee hallintapaneelin."));
    }

    private void fillSummary(Inventory inventory, StaffSummary summary) {
        long activityPercent = activityPercent(summary.totalOnlineSeconds(), summary.totalActiveSeconds());
        inventory.setItem(4, item(
                Material.PLAYER_HEAD,
                summary.latestName(),
                NamedTextColor.GOLD,
                List.of(
                        "UUID: " + summary.uuid(),
                        "Ensimmäinen havainto: " + summary.firstSeen(),
                        "Viimeksi nähty: " + summary.lastSeen()
                )
        ));
        inventory.setItem(10, item(
                Material.CLOCK,
                "Online-aika",
                NamedTextColor.GREEN,
                List.of(
                        DurationFormatter.seconds(summary.totalOnlineSeconds()),
                        "Sessiot: " + summary.totalSessions()
                )
        ));
        inventory.setItem(11, item(
                Material.LIME_DYE,
                "Aktiivinen aika",
                NamedTextColor.GREEN,
                List.of(
                        DurationFormatter.seconds(summary.totalActiveSeconds()),
                        "Aktiivisuus: " + activityPercent + "%"
                )
        ));
        inventory.setItem(12, item(
                Material.GRAY_DYE,
                "AFK-aika",
                NamedTextColor.GRAY,
                List.of(DurationFormatter.seconds(summary.totalAfkSeconds()))
        ));
        inventory.setItem(14, item(
                Material.COMMAND_BLOCK,
                "Komennot",
                NamedTextColor.AQUA,
                List.of(Integer.toString(summary.totalCommands()))
        ));
        inventory.setItem(15, item(
                Material.ENDER_PEARL,
                "Teleportit",
                NamedTextColor.LIGHT_PURPLE,
                List.of(Integer.toString(summary.totalTeleports()))
        ));
        inventory.setItem(16, item(
                Material.GRASS_BLOCK,
                "Gamemode-vaihdot",
                NamedTextColor.YELLOW,
                List.of(Integer.toString(summary.totalGamemodeChanges()))
        ));
        inventory.setItem(22, item(
                Material.NETHER_STAR,
                "Staff-toimet",
                NamedTextColor.RED,
                List.of(Integer.toString(summary.totalStaffActions()))
        ));
        inventory.setItem(29, actionItem(Material.MAP, "Tämän päivän tilasto", NamedTextColor.YELLOW, "Klikkaa avataksesi päivän yhteenvedon chattiin."));
        inventory.setItem(30, actionItem(Material.BOOK, "Viimeisimmät sessiot", NamedTextColor.AQUA, "Klikkaa avataksesi viimeisimmät sessiot chattiin."));
        inventory.setItem(32, actionItem(Material.PAPER, "Tämän viikon tilasto", NamedTextColor.GREEN, "Klikkaa avataksesi viikon yhteenvedon chattiin."));
        inventory.setItem(40, actionItem(Material.BARRIER, "Sulje", NamedTextColor.RED, "Sulkee tilastonäkymän."));
    }

    private ItemStack actionItem(Material material, String name, NamedTextColor color, String lore) {
        return item(material, name, color, List.of(lore));
    }

    private ItemStack item(Material material, String name, NamedTextColor color, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }

    private String yesNo(boolean value) {
        return value ? "kyllä" : "ei";
    }

    private long activityPercent(long onlineSeconds, long activeSeconds) {
        if (onlineSeconds <= 0) {
            return 0;
        }
        return Math.round((activeSeconds * 100.0) / onlineSeconds);
    }
}
