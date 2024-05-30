package com.winthier.kit.sql;

import com.cavetale.core.editor.EditMenuAdapter;
import com.cavetale.core.editor.EditMenuButton;
import com.cavetale.core.editor.EditMenuItem;
import com.cavetale.core.editor.EditMenuNode;
import com.cavetale.core.util.Json;
import com.cavetale.inventory.storage.InventoryStorage;
import com.cavetale.mytems.Mytems;
import com.winthier.kit.KitAdminCommand;
import com.winthier.kit.KitType;
import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Key;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.title.Title;
import com.winthier.title.TitlePlugin;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@Data @NotNull @Name("kits")
@Key({"enabled", "type"})
public final class SQLKit implements SQLRow, EditMenuAdapter {
    @EditMenuItem(settable = false)
    @Id private Integer id;

    @EditMenuItem(settable = false)
    @VarChar(63) @Unique
    private String name = "";

    private KitType type;

    @EditMenuItem(deletable = true)
    @Text @Nullable
    private String displayName; // Gson-serialized component

    @EditMenuItem(deletable = true)
    @VarChar(255) @Nullable
    private String description;

    @EditMenuItem(deletable = true)
    @VarChar(255) @Nullable
    private String permission;

    private boolean enabled;

    private int friendship;

    private long cooldown;

    @Default("NOW()")
    private Date createdTime;

    @EditMenuItem(hidden = true)
    @MediumText @Nullable
    private String inventory;

    @EditMenuItem(hidden = true)
    @Text @Nullable
    private String tag;

    public SQLKit() { }

    public SQLKit(final String name, final KitType type) {
        this.name = name;
        this.type = type;
        this.createdTime = new Date();
    }

    @Data
    public static final class Tag implements EditMenuAdapter {
        @EditMenuItem(deletable = true)
        protected List<String> titles;
        @EditMenuItem(deletable = true)
        protected List<String> commands;

        public boolean isEmpty() {
            return (titles == null || titles.isEmpty())
                && (commands == null || titles.isEmpty());
        }

        @Override
        public List<EditMenuButton> getEditMenuButtons(EditMenuNode node) {
            List<EditMenuButton> result = new ArrayList<>();
            if (titles != null) {
                result.add(new EditMenuButton() {
                        @Override public ItemStack getMenuIcon() {
                            return Mytems.QUESTION_MARK.createItemStack();
                        }

                        @Override public List<Component> getTooltip() {
                            List<Component> tooltip = new ArrayList<>();
                            tooltip.add(text("Titles", GRAY));
                            int index = 0;
                            for (String titleName : titles) {
                                Title title = TitlePlugin.getInstance().getTitle(titleName);
                                tooltip.add(join(noSeparators(),
                                                 text("[" + (index++) + "] ", GRAY),
                                                 (title != null
                                                  ? title.getTitleComponent()
                                                  : text(titleName, DARK_GRAY, ITALIC))));
                            }
                            return tooltip;
                        }

                        @Override public void onClick(Player player, ClickType clickType) { }
                    });
            }
            if (commands != null) {
                result.add(new EditMenuButton() {
                        @Override public ItemStack getMenuIcon() {
                            return Mytems.EXCLAMATION_MARK.createItemStack();
                        }

                        @Override public List<Component> getTooltip() {
                            List<Component> tooltip = new ArrayList<>();
                            tooltip.add(text("Commands", GRAY));
                            int index = 0;
                            for (String command : commands) {
                                tooltip.add(join(noSeparators(),
                                                 text("[" + (index++) + "] ", GRAY),
                                                 text(command, WHITE)));
                            }
                            return tooltip;
                        }

                        @Override public void onClick(Player player, ClickType clickType) { }
                    });
            }
            return result;
        }
    }

    public Component parseDisplayComponent() {
        if (displayName != null) {
            try {
                return GsonComponentSerializer.gson().deserialize(displayName);
            } catch (Exception e) {
                return Component.text(displayName);
            }
        } else {
            return Component.text(name);
        }
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
            : Bukkit.createInventory(null, 3 * 9);
    }

    @Override
    public List<EditMenuButton> getEditMenuButtons(EditMenuNode node) {
        return List.of(new EditMenuButton() {
                @Override public ItemStack getMenuIcon() {
                    return Mytems.QUESTION_MARK.createItemStack();
                }

                @Override public List<Component> getTooltip() {
                    return KitAdminCommand.getKitInfo(SQLKit.this);
                }

                @Override public void onClick(Player player, ClickType clickType) { }
            });
    }
}
