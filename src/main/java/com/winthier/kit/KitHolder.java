package com.winthier.kit;

import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.Inventory;

public final class KitHolder implements InventoryHolder {
    Kit kit;
    Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
