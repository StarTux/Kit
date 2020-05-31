package com.winthier.kit;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * (De)serializable with Json.
 */
public final class KitItem {
    @Getter Material material = Material.AIR;
    @Getter int amount = 1;
    @Getter String tag = null;

    public KitItem() { }

    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(material, amount);
        if (tag != null && !tag.isEmpty()) {
            item = Bukkit.getServer().getUnsafe().modifyItemStack(item, tag);
        }
        return item;
    }
}
