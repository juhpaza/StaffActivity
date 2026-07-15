package fi.juhpaza.staffactivity.gui;

import fi.juhpaza.staffactivity.StaffActivity;
import fi.juhpaza.staffactivity.model.SessionSnapshot;
import fi.juhpaza.staffactivity.util.DurationFormatter;
import java.time.Instant;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Shows currently active in-memory staff sessions.
 */
final class StaffActivitySessionsGui {
    static final int SIZE = 54;

    private final StaffActivity plugin;

    StaffActivitySessionsGui(StaffActivity plugin) {
        this.plugin = plugin;
    }

    void open(Player player) {
        Inventory inventory = Bukkit.createInventory(
                new StaffActivityGuiHolder(StaffActivityGuiView.ONLINE_STAFF),
                SIZE,
                Component.text("StaffActivity: Online Staff", NamedTextColor.GREEN)
        );
        fill(inventory);
        player.openInventory(inventory);
    }

    private void fill(Inventory inventory) {
        StaffActivityGuiItems.frame(inventory, Material.GRAY_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE);
        inventory.setItem(4, StaffActivityGuiItems.item(
                Material.PLAYER_HEAD,
                "Online Staff",
                NamedTextColor.GREEN,
                List.of("Aktiiviset seuratut staff-sessiot juuri nyt.")
        ));

        List<SessionSnapshot> snapshots = plugin.sessionService().snapshots(Instant.now());
        if (snapshots.isEmpty()) {
            inventory.setItem(22, StaffActivityGuiItems.item(
                    Material.GRAY_DYE,
                    "Ei aktiivisia staff-sessioita",
                    NamedTextColor.GRAY,
                    List.of("Dashboard päivittyy, kun seurattu staff-jäsen on palvelimella.")
            ));
        } else {
            int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
            int index = 0;
            for (SessionSnapshot snapshot : snapshots.stream().limit(slots.length).toList()) {
                inventory.setItem(slots[index++], sessionItem(snapshot));
            }
        }

        inventory.setItem(45, StaffActivityGuiItems.actionItem(Material.ARROW, "Takaisin", NamedTextColor.YELLOW, "Palaa Dashboardiin."));
        inventory.setItem(49, StaffActivityGuiItems.actionItem(Material.LIME_DYE, "Päivitä", NamedTextColor.GREEN, "Päivittää aktiiviset sessiot."));
        inventory.setItem(53, StaffActivityGuiItems.actionItem(Material.BARRIER, "Close", NamedTextColor.RED, "Sulkee StaffActivity-valikon."));
    }

    private org.bukkit.inventory.ItemStack sessionItem(SessionSnapshot snapshot) {
        return StaffActivityGuiItems.playerHead(
                snapshot.uuid(),
                snapshot.latestName(),
                NamedTextColor.WHITE,
                List.of(
                        "Online: " + DurationFormatter.seconds(snapshot.onlineTime().toSeconds()),
                        "Active: " + DurationFormatter.seconds(snapshot.activeTime().toSeconds()),
                        "AFK: " + DurationFormatter.seconds(snapshot.afkTime().toSeconds()),
                        "Komennot: " + snapshot.commandCount()
                )
        );
    }
}
