package com.winthier.kit;

import com.cavetale.mytems.Mytems;
import java.util.Base64;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * (De)serializable with Json.
 */
public final class KitItem {
    @Getter protected Material material = Material.AIR;
    @Getter protected int amount = 1;
    protected String serialized = null;
    private transient ItemStack itemStack; // cache

    public KitItem() { }

    public KitItem(final ItemStack item) {
        this.itemStack = item;
        material = item.getType();
        amount = item.getAmount();
        if (!item.isSimilar(new ItemStack(material, amount))) {
            byte[] bytes = item.serializeAsBytes();
            serialized = Base64.getEncoder().encodeToString(bytes);
        }
    }

    public ItemStack createItemStack() {
        if (itemStack == null) {
            if (serialized != null) {
                byte[] bytes = Base64.getDecoder().decode(serialized);
                itemStack = ItemStack.deserializeBytes(bytes);
            } else {
                itemStack = new ItemStack(material, amount);
            }
        }
        return itemStack.clone();
    }

    @Override
    public String toString() {
        createItemStack(); // set itemStack field
        Mytems mytems = Mytems.forItem(itemStack);
        if (mytems != null) {
            return mytems.serializeItem(itemStack);
        }
        if (material == null) return "INVALID";
        return
            (amount != 1
             ? "" + amount + "*"
             : "")
            + material.name().toLowerCase()
            + (serialized != null
               ? "{...}"
               : "");
    }

    public String toSingleString() {
        ItemStack theItem = createItemStack().clone();
        theItem.setAmount(1);
        Mytems mytems = Mytems.forItem(theItem);
        if (mytems != null) {
            return mytems.serializeItem(theItem);
        }
        return material.name().toLowerCase();
    }
}
