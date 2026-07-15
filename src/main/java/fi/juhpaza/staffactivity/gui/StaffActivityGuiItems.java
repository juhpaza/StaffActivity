package fi.juhpaza.staffactivity.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Small item factory shared by StaffActivity inventory views.
 */
final class StaffActivityGuiItems {
    private StaffActivityGuiItems() {
    }

    static ItemStack item(Material material, String name, NamedTextColor color, List<String> lore) {
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

    static ItemStack actionItem(Material material, String name, NamedTextColor color, String lore) {
        return item(material, name, color, List.of(lore));
    }

    static ItemStack filler(Material material) {
        return item(material, " ", NamedTextColor.DARK_GRAY, List.of());
    }

    static void frame(Inventory inventory, Material border, Material accent) {
        ItemStack borderItem = filler(border);
        ItemStack accentItem = filler(accent);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            int row = slot / 9;
            int column = slot % 9;
            if (row == 0 || row == (inventory.getSize() / 9) - 1 || column == 0 || column == 8) {
                inventory.setItem(slot, borderItem);
            }
        }
        inventory.setItem(1, accentItem);
        inventory.setItem(7, accentItem);
        inventory.setItem(inventory.getSize() - 8, accentItem);
        inventory.setItem(inventory.getSize() - 2, accentItem);
    }
}
