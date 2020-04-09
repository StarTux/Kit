package com.winthier.kit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class KitItem {
    final KitPlugin plugin;
    @Getter Material material = Material.AIR;
    @Getter int amount = 1;
    @Getter String tag = null;

    void load(ConfigurationSection config) {
        String str = config.getString("Material", "");
        try {
            material = Material.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException iae) {
            plugin.getLogger().warning("Invalid material: " + str);
        }
        amount = config.getInt("Amount", amount);
        tag = config.getString("Tag");
    }

    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(material, amount);
        if (tag != null && !tag.isEmpty()) {
            item = plugin.getServer().getUnsafe().modifyItemStack(item, tag);
        }
        return item;
    }
}
