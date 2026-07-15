package fi.juhpaza.staffactivity.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marker holder for StaffActivity management inventories.
 */
public final class StaffActivityGuiHolder implements InventoryHolder {
    private final StaffActivityGuiView view;
    private final String targetName;

    public StaffActivityGuiHolder(StaffActivityGuiView view) {
        this(view, null);
    }

    public StaffActivityGuiHolder(StaffActivityGuiView view, String targetName) {
        this.view = view;
        this.targetName = targetName;
    }

    public StaffActivityGuiView view() {
        return view;
    }

    public String targetName() {
        return targetName;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
