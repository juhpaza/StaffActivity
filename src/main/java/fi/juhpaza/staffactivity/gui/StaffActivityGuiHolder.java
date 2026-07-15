package fi.juhpaza.staffactivity.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marker holder for StaffActivity management inventories.
 */
public final class StaffActivityGuiHolder implements InventoryHolder {
    private final StaffActivityGuiView view;

    public StaffActivityGuiHolder(StaffActivityGuiView view) {
        this.view = view;
    }

    public StaffActivityGuiView view() {
        return view;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
