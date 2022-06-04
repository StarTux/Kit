package com.winthier.kit.legacy;

import java.util.Base64;
import lombok.Data;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Data
public final class LegacyKitItem {
    protected Material material = Material.AIR;
    protected int amount = 1;
    protected String serialized = null;

    public LegacyKitItem() { }

    public ItemStack createItemStack() {
        if (serialized != null) {
            byte[] bytes = Base64.getDecoder().decode(serialized);
            return ItemStack.deserializeBytes(bytes);
        } else {
            return new ItemStack(material, amount);
        }
    }
}
