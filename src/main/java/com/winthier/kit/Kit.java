package com.winthier.kit;

import com.winthier.kit.sql.SQLKit;
import java.util.Date;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * A cache of data retrieved from the database of one kit for one
 * player.
 */
@RequiredArgsConstructor
public final class Kit {
    @NonNull private final SQLKit row;
    private final Date cooldown;
    private Inventory inventory;
    private Component displayName;
    private SQLKit.Tag tag;

    public boolean hasCooldown() {
        return row.getType() == KitType.PERMISSION_COOLDOWN
            && cooldown != null;
    }

    public ItemStack findFirstItem() {
        if (inventory().getSize() >= 14) {
            ItemStack center = inventory().getItem(13);
            if (center != null && !center.getType().isAir()) return center;
        }
        for (ItemStack itemStack : inventory()) {
            if (itemStack != null && !itemStack.getType().isAir()) {
                return itemStack;
            }
        }
        return null;
    }

    public SQLKit row() {
        return row;
    }

    public Date cooldown() {
        return cooldown;
    }

    public Inventory inventory() {
        if (inventory == null) {
            inventory = row.parseInventory();
        }
        return inventory;
    }

    public Component displayName() {
        if (displayName == null) {
            displayName = row.parseDisplayComponent();
        }
        return displayName;
    }

    public SQLKit.Tag tag() {
        if (tag == null) {
            tag = row.parseTag();
        }
        return tag;
    }
}
