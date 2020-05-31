package com.winthier.kit;

import java.util.Base64;
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
    String serialized = null;

    public KitItem() { }

    public KitItem(final ItemStack item) {
        material = item.getType();
        amount = item.getAmount();
        if (!item.isSimilar(new ItemStack(material, amount))) {
            byte[] bytes = item.serializeAsBytes();
            serialized = Base64.getEncoder().encodeToString(bytes);
        }
    }

    public ItemStack createItemStack() {
        if (serialized != null) {
            byte[] bytes = Base64.getDecoder().decode(serialized);
            return ItemStack.deserializeBytes(bytes);
        }
        ItemStack item = new ItemStack(material, amount);
        if (tag != null && !tag.isEmpty()) {
            item = Bukkit.getServer().getUnsafe().modifyItemStack(item, tag);
        }
        return item;
    }

    @Override
    public String toString() {
        if (material == null) return "INVALID";
        return
            (amount != 1
             ? "" + amount + "*"
             : "")
            + material.name().toLowerCase()
            + (tag != null || serialized != null
               ? "{...}"
               : "");
    }
}
