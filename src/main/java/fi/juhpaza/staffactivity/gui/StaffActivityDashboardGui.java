package fi.juhpaza.staffactivity.gui;

import fi.juhpaza.staffactivity.StaffActivity;
import fi.juhpaza.staffactivity.database.DatabaseStatus;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Builds the main StaffActivity dashboard inventory.
 */
final class StaffActivityDashboardGui {
    static final int SIZE = 54;

    private final StaffActivity plugin;

    StaffActivityDashboardGui(StaffActivity plugin) {
        this.plugin = plugin;
    }

    void open(Player player) {
        Inventory inventory = Bukkit.createInventory(
                new StaffActivityGuiHolder(StaffActivityGuiView.DASHBOARD),
                SIZE,
                Component.text("StaffActivity Dashboard", NamedTextColor.GOLD)
        );
        fill(inventory);
        player.openInventory(inventory);
    }

    private void fill(Inventory inventory) {
        StaffActivityGuiItems.frame(inventory, Material.GRAY_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE);
        inventory.setItem(4, StaffActivityGuiItems.item(
                Material.NETHER_STAR,
                "StaffActivity Dashboard",
                NamedTextColor.GOLD,
                List.of(
                        "Versio: " + plugin.getPluginMeta().getVersion(),
                        "Päävalikko staff-aktiivisuuden hallintaan."
                )
        ));

        inventory.setItem(10, statusItem());
        inventory.setItem(11, databaseItem());
        inventory.setItem(12, discordItem());
        inventory.setItem(13, trackingItem());
        inventory.setItem(14, timezoneItem());
        inventory.setItem(15, activeSessionsItem());
        inventory.setItem(16, pendingWritesItem());

        inventory.setItem(19, StaffActivityGuiItems.actionItem(
                Material.PLAYER_HEAD,
                "Online Staff",
                NamedTextColor.GREEN,
                "Avaa aktiivisten staff-sessioiden listan."
        ));
        inventory.setItem(20, StaffActivityGuiItems.actionItem(
                Material.GOLD_INGOT,
                "Top Statistics",
                NamedTextColor.GOLD,
                "Avaa top-listojen valikon."
        ));
        inventory.setItem(21, StaffActivityGuiItems.actionItem(
                Material.MAP,
                "Today's Statistics",
                NamedTextColor.YELLOW,
                "Avaa tämän päivän tilastovalikon."
        ));
        inventory.setItem(22, StaffActivityGuiItems.actionItem(
                Material.PAPER,
                "Weekly Statistics",
                NamedTextColor.AQUA,
                "Avaa tämän viikon tilastovalikon."
        ));
        inventory.setItem(23, StaffActivityGuiItems.actionItem(
                Material.SPYGLASS,
                "Player Lookup",
                NamedTextColor.LIGHT_PURPLE,
                "Tulossa myöhemmin."
        ));

        inventory.setItem(28, placeholderItem(Material.BOOK, "Sessions"));
        inventory.setItem(29, placeholderItem(Material.IRON_SWORD, "Staff Actions"));
        inventory.setItem(30, placeholderItem(Material.BELL, "Discord Reports"));
        inventory.setItem(31, placeholderItem(Material.COMPASS, "Activity Charts"));
        inventory.setItem(32, placeholderItem(Material.WRITABLE_BOOK, "Database Tools"));
        inventory.setItem(33, placeholderItem(Material.COMMAND_BLOCK, "Debug Tools"));
        inventory.setItem(34, placeholderItem(Material.COMPARATOR, "Settings"));

        inventory.setItem(46, StaffActivityGuiItems.actionItem(
                Material.LIME_DYE,
                "Päivitä Dashboard",
                NamedTextColor.GREEN,
                "Avaa päävalikon uudelleen tuoreilla tiedoilla."
        ));
        inventory.setItem(48, StaffActivityGuiItems.actionItem(
                Material.REPEATER,
                "Reload",
                NamedTextColor.YELLOW,
                "Suorittaa saman kuin /staffactivity reload."
        ));
        inventory.setItem(50, StaffActivityGuiItems.actionItem(
                Material.REDSTONE_TORCH,
                "Discord Test",
                NamedTextColor.LIGHT_PURPLE,
                "Suorittaa saman kuin /staffactivity discord test."
        ));
        inventory.setItem(52, StaffActivityGuiItems.actionItem(
                Material.BARRIER,
                "Close",
                NamedTextColor.RED,
                "Sulkee StaffActivity-valikon."
        ));
    }

    private org.bukkit.inventory.ItemStack statusItem() {
        return StaffActivityGuiItems.item(
                Material.BEACON,
                "Plugin",
                NamedTextColor.GREEN,
                List.of(
                        "Versio: " + plugin.getPluginMeta().getVersion(),
                        "Tila: ladattu"
                )
        );
    }

    private org.bukkit.inventory.ItemStack databaseItem() {
        DatabaseStatus status = plugin.databaseService().status();
        NamedTextColor color = status == DatabaseStatus.READY ? NamedTextColor.GREEN : NamedTextColor.RED;
        return StaffActivityGuiItems.item(
                Material.WRITABLE_BOOK,
                "Database",
                color,
                List.of(
                        "Status: " + databaseStatusLabel(status),
                        "Tietokanta käyttää nykyistä StaffActivity-skeemaa."
                )
        );
    }

    private org.bukkit.inventory.ItemStack discordItem() {
        boolean enabled = plugin.configService().discordEnabled();
        return StaffActivityGuiItems.item(
                Material.REDSTONE,
                "Discord",
                enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY,
                List.of(
                        "Status: " + enabledDisabled(enabled),
                        "Webhook asetettu: " + yesNo(plugin.configService().discordConfigured())
                )
        );
    }

    private org.bukkit.inventory.ItemStack trackingItem() {
        return StaffActivityGuiItems.item(
                Material.NAME_TAG,
                "Tracking Permission",
                NamedTextColor.AQUA,
                List.of(plugin.configService().trackingPermission())
        );
    }

    private org.bukkit.inventory.ItemStack timezoneItem() {
        return StaffActivityGuiItems.item(
                Material.CLOCK,
                "Timezone",
                NamedTextColor.YELLOW,
                List.of(plugin.configService().timezoneId())
        );
    }

    private org.bukkit.inventory.ItemStack activeSessionsItem() {
        int activeSessions = plugin.sessionService().activeSessionCount();
        return StaffActivityGuiItems.item(
                Material.LIME_DYE,
                "Active Sessions",
                NamedTextColor.GREEN,
                List.of(
                        "Aktiiviset sessiot: " + activeSessions,
                        "Online staff: " + activeSessions
                )
        );
    }

    private org.bukkit.inventory.ItemStack pendingWritesItem() {
        return StaffActivityGuiItems.item(
                Material.HOPPER,
                "Pending Writes",
                NamedTextColor.AQUA,
                List.of(Integer.toString(plugin.databaseService().pendingOperations()))
        );
    }

    private org.bukkit.inventory.ItemStack placeholderItem(Material material, String name) {
        return StaffActivityGuiItems.item(
                material,
                name,
                NamedTextColor.DARK_GRAY,
                List.of("Varattu tulevaa Dashboard-laajennusta varten.")
        );
    }

    private String databaseStatusLabel(DatabaseStatus status) {
        return status == DatabaseStatus.READY ? "Ready" : "Error";
    }

    private String enabledDisabled(boolean value) {
        return value ? "Enabled" : "Disabled";
    }

    private String yesNo(boolean value) {
        return value ? "kyllä" : "ei";
    }
}
