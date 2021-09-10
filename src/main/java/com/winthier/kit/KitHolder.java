package com.winthier.kit;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class KitHolder implements InventoryHolder {
    protected final Kit kit;
    protected Inventory inventory;
    protected Runnable onClose;

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
