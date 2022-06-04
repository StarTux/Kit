package com.winthier.kit.sql;

import com.cavetale.core.editor.EditMenuItem;
import com.cavetale.core.util.Json;
import com.cavetale.inventory.storage.InventoryStorage;
import com.winthier.kit.KitType;
import com.winthier.sql.SQLRow.*;
import com.winthier.sql.SQLRow;
import java.util.Date;
import java.util.List;
import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

@Data @NotNull @Name("kits")
@Key({"enabled", "type"})
public final class SQLKit implements SQLRow {
    @Id private Integer id;

    @VarChar(63) @Unique
    private String name = "";

    private KitType type;

    @Text @Nullable
    private String displayName; // Gson-serialized component

    @VarChar(255) @Nullable
    private String description;

    @VarChar(255) @Nullable
    private String permission;

    private boolean enabled;

    private int friendship;

    private long cooldown;

    @Default("NOW()")
    private Date createdTime;

    @MediumText @Nullable
    private String inventory;

    @Text @Nullable
    private String tag;

    public SQLKit() { }

    public SQLKit(final String name, final KitType type) {
        this.name = name;
        this.type = type;
        this.createdTime = new Date();
    }

    @Data
    public static final class Tag {
        @EditMenuItem(deletable = true)
        protected List<String> titles;
        @EditMenuItem(deletable = true)
        protected List<String> commands;

        public boolean isEmpty() {
            return (titles == null || titles.isEmpty())
                && (commands == null || titles.isEmpty());
        }
    }

    public Component parseDisplayComponent() {
        return displayName != null
            ? GsonComponentSerializer.gson().deserialize(displayName)
            : Component.text(name);
    }

    public void setDisplayComponent(Component component) {
        this.displayName = component != null
            ? GsonComponentSerializer.gson().serialize(component)
            : null;
    }

    public InventoryStorage parseInventoryStorage() {
        return Json.deserialize(inventory, InventoryStorage.class, InventoryStorage::new);
    }

    public Tag parseTag() {
        return Json.deserialize(tag, Tag.class, Tag::new);
    }

    public Inventory parseInventory() {
        InventoryStorage storage = parseInventoryStorage();
        return !storage.isEmpty()
            ? storage.toInventory()
            : Bukkit.createInventory(null, 9);
    }
}
