package com.winthier.kit;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class KitHolder implements InventoryHolder {
    Inventory inventory;
    Runnable onClose;

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
