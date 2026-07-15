package fi.juhpaza.staffactivity.gui;

import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Builds small navigation menus that launch existing stats commands.
 */
final class StaffActivityStatsMenuGui {
    static final int SIZE = 54;

    void openTop(Player player) {
        Inventory inventory = Bukkit.createInventory(
                new StaffActivityGuiHolder(StaffActivityGuiView.TOP_STATISTICS),
                SIZE,
                StaffActivityGuiItems.title("Top", NamedTextColor.GOLD)
        );
        fillTop(inventory);
        player.openInventory(inventory);
    }

    void openToday(Player player) {
        Inventory inventory = Bukkit.createInventory(
                new StaffActivityGuiHolder(StaffActivityGuiView.TODAY_STATISTICS),
                SIZE,
                StaffActivityGuiItems.title("Today", NamedTextColor.YELLOW)
        );
        fillPeriod(inventory, "Today's Statistics", "Tämän päivän tilastot", Material.MAP, NamedTextColor.YELLOW, "today");
        player.openInventory(inventory);
    }

    void openWeek(Player player) {
        Inventory inventory = Bukkit.createInventory(
                new StaffActivityGuiHolder(StaffActivityGuiView.WEEKLY_STATISTICS),
                SIZE,
                StaffActivityGuiItems.title("Week", NamedTextColor.AQUA)
        );
        fillPeriod(inventory, "Weekly Statistics", "Tämän viikon tilastot", Material.PAPER, NamedTextColor.AQUA, "week");
        player.openInventory(inventory);
    }

    private void fillTop(Inventory inventory) {
        StaffActivityGuiItems.frame(inventory, Material.GRAY_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE);
        inventory.setItem(4, StaffActivityGuiItems.item(
                Material.GOLD_INGOT,
                "Top Statistics",
                NamedTextColor.GOLD,
                List.of("Avaa olemassa olevat top-listat chattiin.")
        ));
        inventory.setItem(20, StaffActivityGuiItems.actionItem(Material.GOLD_INGOT, "Top Online", NamedTextColor.GOLD, "Näyttää online-ajan top-listan."));
        inventory.setItem(22, StaffActivityGuiItems.actionItem(Material.EMERALD, "Top Active", NamedTextColor.GREEN, "Näyttää aktiivisen ajan top-listan."));
        inventory.setItem(24, StaffActivityGuiItems.actionItem(Material.NETHER_STAR, "Top Actions", NamedTextColor.AQUA, "Näyttää staff-toimien top-listan."));
        addFooter(inventory);
    }

    private void fillPeriod(Inventory inventory, String title, String description, Material icon, NamedTextColor color, String command) {
        StaffActivityGuiItems.frame(inventory, Material.GRAY_STAINED_GLASS_PANE, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        inventory.setItem(4, StaffActivityGuiItems.item(
                icon,
                title,
                color,
                List.of(
                        description,
                        "Käyttää olemassa olevaa /staffactivity " + command + " -komentoa."
                )
        ));
        inventory.setItem(21, StaffActivityGuiItems.actionItem(
                icon,
                "Oma tilasto",
                color,
                "Näyttää oman " + commandLabel(command) + " yhteenvedon chattiin."
        ));
        inventory.setItem(23, StaffActivityGuiItems.actionItem(
                Material.SPYGLASS,
                "Player Lookup",
                NamedTextColor.LIGHT_PURPLE,
                "Tulossa myöhemmin."
        ));
        addFooter(inventory);
    }

    private void addFooter(Inventory inventory) {
        inventory.setItem(45, StaffActivityGuiItems.actionItem(Material.ARROW, "Takaisin", NamedTextColor.YELLOW, "Palaa Dashboardiin."));
        inventory.setItem(53, StaffActivityGuiItems.actionItem(Material.BARRIER, "Close", NamedTextColor.RED, "Sulkee StaffActivity-valikon."));
    }

    private String commandLabel(String command) {
        return "today".equals(command) ? "päivän" : "viikon";
    }
}
