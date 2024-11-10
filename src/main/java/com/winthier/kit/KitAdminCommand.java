package com.winthier.kit;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.editor.EditMenuDelegate;
import com.cavetale.core.editor.EditMenuNode;
import com.cavetale.core.editor.Editor;
import com.cavetale.core.item.ItemKinds;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.core.util.Json;
import com.cavetale.inventory.storage.InventoryStorage;
import com.cavetale.memberlist.MemberList;
import com.winthier.kit.legacy.LegacyKit;
import com.winthier.kit.legacy.LegacyKitItem;
import com.winthier.kit.legacy.LegacyUsers;
import com.winthier.kit.sql.SQLClaimed;
import com.winthier.kit.sql.SQLCooldown;
import com.winthier.kit.sql.SQLKit;
import com.winthier.kit.sql.SQLMember;
import com.winthier.title.Title;
import com.winthier.title.TitlePlugin;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Container;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class KitAdminCommand extends AbstractCommand<KitPlugin> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    protected KitAdminCommand(final KitPlugin plugin) {
        super(plugin, "kitadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("info").arguments("<kit>")
            .description("Print kit info")
            .completers(CommandArgCompleter.supplyIgnoreCaseList(this::getKitNames))
            .senderCaller(this::info);
        rootNode.addChild("create").arguments("<name>")
            .description("Create kit")
            .completers(CommandArgCompleter.supplyIgnoreCaseList(this::getKitNames))
            .senderCaller(this::create);
        rootNode.addChild("copy").arguments("<kit> <name>")
            .description("Clone a kit")
            .completers(CommandArgCompleter.supplyIgnoreCaseList(this::getKitNames),
                        CommandArgCompleter.supplyIgnoreCaseList(this::getKitNames))
            .senderCaller(this::copy);
        rootNode.addChild("edit").arguments("<kit> [what]")
            .description("Open kit editor")
            .completers(CommandArgCompleter.supplyIgnoreCaseList(this::getKitNames),
                        CommandArgCompleter.list(List.of("tag", "items")))
            .playerCaller(this::edit);
        rootNode.addChild("member").arguments("<kit> <players...>")
            .completers(CommandArgCompleter.supplyIgnoreCaseList(this::getKitNames),
                        PlayerCache.NAME_COMPLETER,
                        CommandArgCompleter.REPEAT)
            .senderCaller(this::member);
        rootNode.addChild("memberlist").arguments("<kit> <list>")
            .description("Add members from MemberList")
            .completers(CommandArgCompleter.supplyIgnoreCaseList(this::getKitNames),
                        CommandArgCompleter.NULL)
            .senderCaller(this::memberList);
        rootNode.addChild("fromfile").arguments("<kit> <path>")
            .description("Add members from file with names or UUIDs")
            .completers(CommandArgCompleter.supplyIgnoreCaseList(this::getKitNames),
                        CommandArgCompleter.NULL)
            .senderCaller(this::fromFile);
        rootNode.addChild("tofile").arguments("<kit>")
            .description("Save kit members to a file")
            .completers(CommandArgCompleter.supplyIgnoreCaseList(this::getKitNames))
            .senderCaller(this::toFile);
        rootNode.addChild("delete").arguments("<kit>")
            .description("Delete a kit")
            .completers(CommandArgCompleter.supplyIgnoreCaseList(this::getKitNames))
            .senderCaller(this::delete);
        rootNode.addChild("migrate").denyTabCompletion()
            .description("Import legacy files")
            .senderCaller(this::migrate);
    }

    private List<String> getKitNames() {
        return plugin.database.find(SQLKit.class).findValues("name", String.class);
    }

    private String findKitName(String name) {
        List<String> values = plugin.database.find(SQLKit.class)
            .eq("name", name)
            .findValues("name", String.class);
        return !values.isEmpty() ? values.get(0) : null;
    }

    private SQLKit findKit(String name) {
        return plugin.database.find(SQLKit.class).eq("name", name).findUnique();
    }

    private SQLKit requireKit(String name) {
        SQLKit kit = findKit(name);
        if (kit == null) throw new CommandWarn("Kit not found: " + name);
        return kit;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        SQLKit kit = requireKit(args[0]);
        sender.sendMessage(join(separator(newline()), getKitInfo(kit)));
        SQLKit.Tag tag = kit.parseTag();
        if (tag.getCommands() != null) {
            for (String command : tag.getCommands()) {
                sender.sendMessage(info("command", command));
            }
        }
        if (tag.getTitles() != null) {
            List<Component> names = new ArrayList<>();
            for (String titleName : tag.getTitles()) {
                Title title = TitlePlugin.getInstance().getTitle(titleName);
                names.add(title != null
                          ? title.getTitleComponent()
                          : text(titleName, DARK_GRAY, ITALIC));
            }
            sender.sendMessage(info("titles", names));
        }
        if (kit.getInventory() != null) {
            Inventory inventory = kit.parseInventory();
            Map<String, Integer> counts = new HashMap<>();
            Map<String, ItemStack> prototypes = new HashMap<>();
            itemsHelper(inventory, counts, prototypes);
            List<String> allNames = new ArrayList<>(counts.keySet());
            allNames.sort(String.CASE_INSENSITIVE_ORDER);
            List<Component> components = new ArrayList<>();
            for (String name : allNames) {
                int count = counts.get(name);
                ItemStack item = prototypes.get(name);
                components.add(ItemKinds.chatDescription(item, count));
            }
            sender.sendMessage(info("items", components));
        }
        if (kit.getType() == KitType.MEMBER) {
            Map<String, Component> displayNames = new HashMap<>();
            for (SQLMember row : plugin.database.find(SQLMember.class).eq("kitId", kit.getId()).findList()) {
                String name = PlayerCache.nameForUuid(row.getMember());
                displayNames.put(name, text(name, row.isClaimed() ? GRAY : GREEN)
                                 .hoverEvent(showText(join(separator(newline()),
                                                           text(name, row.isClaimed() ? GRAY : GREEN),
                                                           text(row.getMember().toString(), GRAY),
                                                           info("enabled", row.isEnabled()),
                                                           info("created", row.getCreatedTime()),
                                                           info("claimed", row.isClaimed()),
                                                           info("claimed time", row.getClaimedTime()))))
                                 .insertion(name));
            }
            List<String> names = new ArrayList<>(displayNames.keySet());
            names.sort(String.CASE_INSENSITIVE_ORDER);
            List<Component> components = new ArrayList<>(names.size());
            names.forEach(n -> components.add(displayNames.get(n)));
                                                   sender.sendMessage(info("members", components));
        }
        return true;
    }

    public static List<Component> getKitInfo(SQLKit kit) {
        return List.of(info("name", join(separator(space()), text(kit.getName()), text("#" + kit.getId(), YELLOW))),
                       info("type", kit.getType().name()),
                       info("display name", (kit.getDisplayName() != null
                                             ? kit.parseDisplayComponent().insertion(kit.getDisplayName())
                                             : null)),
                       info("description", kit.getDescription()),
                       info("permission", kit.getPermission()),
                       info("enabled", kit.isEnabled()),
                       info("friendship", "" + kit.getFriendship()),
                       info("cooldown", "" + kit.getCooldown()),
                       info("created", kit.getCreatedTime()));
    }

    private static Component info(String key, Component value) {
        return join(separator(space()), text(tiny(key), GRAY), (value != null
                                                                ? value
                                                                : text(tiny("null"), DARK_GRAY, ITALIC)));
    }

    private static Component info(String key, boolean value) {
        return info(key, join(noSeparators(), value ? text(tiny("true"), GREEN) : text(tiny("false"), RED)));
    }

    private static Component info(String key, String value) {
        return info(key, value != null ? text(value) : text(tiny("null"), DARK_GRAY, ITALIC));
    }

    private static Component info(String key, Date date) {
        return info(key, (date != null
                          ? text(DATE_FORMAT.format(date))
                          : text(tiny("null"), DARK_GRAY, ITALIC)));
    }

    private static Component info(String key, List<Component> list) {
        return info(key + "(" + list.size() + ")", join(separator(space()), list));
    }

    private void itemsHelper(Inventory inventory, Map<String, Integer> counts, Map<String, ItemStack> prototypes) {
        for (ItemStack item : inventory) {
            if (item == null || item.getType().isAir()) continue;
            final String name = ItemKinds.name(item);
            counts.put(name, counts.getOrDefault(name, 0) + item.getAmount());
            prototypes.put(name, item);
            if (item.getItemMeta() instanceof BlockStateMeta meta
                && meta.hasBlockState()
                && meta.getBlockState() instanceof Container container) {
                itemsHelper(container.getInventory(), counts, prototypes);
            }
        }
    }

    private boolean create(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String newKitName = args[0];
        String conflictingKitName = findKitName(newKitName);
        if (conflictingKitName != null) {
            throw new CommandWarn("Kit already exists: " + conflictingKitName);
        }
        SQLKit kit = new SQLKit(newKitName, KitType.MEMBER);
        if (plugin.database.insert(kit) == 0) {
            throw new CommandWarn("Could not create kit: " + newKitName);
        }
        sender.sendMessage(text("Kit created: " + newKitName, AQUA));
        return true;
    }

    private boolean copy(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        String oldKitName = args[0];
        String newKitName = args[1];
        SQLKit kit = requireKit(oldKitName);
        String conflictingKitName = findKitName(newKitName);
        if (conflictingKitName != null) {
            throw new CommandWarn("Kit already exists: " + conflictingKitName);
        }
        kit.setId(null);
        kit.setName(newKitName);
        kit.setEnabled(false);
        kit.setCreatedTime(new Date());
        if (plugin.database.insert(kit) == 0) {
            throw new CommandWarn("Could not clone kit: " + oldKitName + " => " + newKitName);
        }
        sender.sendMessage(text("Kit cloned: " + oldKitName + " => " + newKitName, AQUA));
        return true;
    }

    private boolean edit(Player player, String[] args) {
        if (args.length != 1 && args.length != 2) return false;
        SQLKit kit = requireKit(args[0]);
        if (args.length == 1) {
            Editor.get().open(plugin, player, kit, new EditMenuDelegate() {
                    @Override public Runnable getSaveFunction(EditMenuNode node) {
                        return () -> {
                            plugin.database.updateAsync(kit, res -> {
                                    player.sendMessage(join(noSeparators(),
                                                            text("Kit #" + kit.getId() + " saved: ", AQUA),
                                                            text(res + ": ", YELLOW),
                                                            kit.parseDisplayComponent()));
                                });
                            if (kit.getType() == KitType.MEMBER) {
                                plugin.database.update(SQLMember.class)
                                    .where(c -> c
                                           .eq("kitId", kit.getId())
                                           .eq("enabled", !kit.isEnabled()))
                                    .set("enabled", kit.isEnabled())
                                    .async(res -> {
                                            if (res == 0) return;
                                            plugin.updateSidebarList();
                                            player.sendMessage(text(res + " members updated", AQUA));
                                        });
                            }
                        };
                    }
                });
            return true;
        }
        switch (args[1]) {
        case "tag": {
            SQLKit.Tag tag = kit.parseTag();
            Editor.get().open(plugin, player, tag, new EditMenuDelegate() {
                    @Override public Runnable getSaveFunction(EditMenuNode node) {
                        return () -> {
                            kit.setTag(!tag.isEmpty()
                                       ? Json.serialize(tag)
                                       : null);
                            plugin.database.updateAsync(kit, Set.of("tag"), res -> {
                                    player.sendMessage(join(noSeparators(),
                                                            text("Kit Tag #" + kit.getId() + " saved: ", AQUA),
                                                            text(res + ": ", YELLOW),
                                                            kit.parseDisplayComponent()));
                                });
                        };
                    };
                });
            return true;
        }
        case "items": {
            InventoryStorage storage = kit.parseInventoryStorage();
            Gui gui = new Gui(plugin)
                .size(!storage.isEmpty() ? storage.getSize() : 3 * 9)
                .title(kit.parseDisplayComponent());
            if (!storage.isEmpty()) storage.restore(gui.getInventory(), "kita.edit");
            gui.onClose(evt -> {
                    InventoryStorage storage2 = InventoryStorage.of(gui.getInventory());
                    kit.setInventory(!storage2.isEmpty()
                                     ? Json.serialize(storage2)
                                     : null);
                    plugin.database.updateAsync(kit, Set.of("inventory"), res -> {
                            player.sendMessage(join(noSeparators(),
                                                    text("Kit Items #" + kit.getId() + " saved: ", AQUA),
                                                    text(res + ": ", YELLOW),
                                                    kit.parseDisplayComponent()));
                        });
                });
            gui.setEditable(true);
            gui.open(player);
            return true;
        }
        default: return false;
        }
    }

    private boolean member(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        String kitName = args[0];
        List<String> names = List.of(Arrays.copyOfRange(args, 1, args.length));
        SQLKit kit = requireKit(kitName);
        if (kit.getType() != KitType.MEMBER) {
            throw new CommandWarn("Not a member kit!");
        }
        List<SQLMember> newRows = new ArrayList<>();
        for (String name : names) {
            PlayerCache target = PlayerCache.require(name);
            newRows.add(new SQLMember(kit, target.uuid));
        }
        final int added = plugin.database.insertIgnore(newRows);
        final int skipped = newRows.size() - added;
        sender.sendMessage(text("Added " + added + ", skipped " + skipped + " members", AQUA));
        return true;
    }

    private boolean fromFile(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        String kitName = args[0];
        String fileName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        SQLKit kit = requireKit(kitName);
        if (kit.getType() != KitType.MEMBER) {
            throw new CommandWarn("Not a member kit!");
        }
        Path path = Paths.get(args[1]);
        if (!Files.isReadable(path)) {
            throw new CommandWarn("Path not found: " + path);
        }
        List<String> lines;
        try {
            lines = Files.lines(path).toList();
        } catch (IOException ioe) {
            throw new CommandWarn("Error reading: " + ioe.getMessage());
        }
        Set<PlayerCache> targets = new HashSet<>();
        for (String line : lines) {
            line = line.strip();
            if (line.isEmpty()) continue;
            targets.add(PlayerCache.require(line));
        }
        if (targets.isEmpty()) {
            throw new CommandWarn("File is empty!");
        }
        List<SQLMember> newRows = new ArrayList<>();
        for (PlayerCache target : targets) {
            newRows.add(new SQLMember(kit, target.uuid));
        }
        final int added = plugin.database.insertIgnore(newRows);
        final int skipped = newRows.size() - added;
        sender.sendMessage(text("Added " + added + ", skipped " + skipped + " members from file " + path, AQUA));
        return true;
    }

    private boolean toFile(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        final SQLKit kit = requireKit(args[0]);
        if (kit.getType() != KitType.MEMBER) {
            throw new CommandWarn("Not a member kit: " + kit.getName());
        }
        plugin.getDataFolder().mkdirs();
        final File file = new File(plugin.getDataFolder(), kit.getName() + ".txt");
        final List<SQLMember> members = plugin.database.find(SQLMember.class).eq("kitId", kit.getId()).findList();
        if (members.isEmpty()) {
            throw new CommandWarn("No members: " + kit.getName());
        }
        try (PrintStream printStream = new PrintStream(file)) {
            for (SQLMember row : members) {
                final UUID uuid = row.getMember();
                final String name = PlayerCache.nameForUuid(uuid);
                printStream.println("" + uuid);
            }
        } catch (IOException ioe) {
            plugin.getLogger().log(Level.SEVERE, "" + file, ioe);
            throw new CommandWarn("Error, see console: " + ioe.getMessage());
        }
        sender.sendMessage(text("Wrote " + members.size() + " members to " + file + ": " + kit.getName(), YELLOW));
        return true;
    }

    private boolean memberList(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        String kitName = args[0];
        String listName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        SQLKit kit = requireKit(kitName);
        if (kit.getType() != KitType.MEMBER) {
            throw new CommandWarn("Not a member kit!");
        }
        if (kit.isEnabled()) {
            throw new CommandWarn("Kit already enabled!");
        }
        List<SQLMember> newRows = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : MemberList.get(listName).entrySet()) {
            newRows.add(new SQLMember(kit, entry.getKey()));
        }
        final int added = plugin.database.insertIgnore(newRows);
        final int skipped = newRows.size() - added;
        sender.sendMessage(text("Added " + added + ", skipped " + skipped
                                + " members from MemberList " + listName, AQUA));
        return true;
    }

    private boolean delete(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        SQLKit kit = requireKit(args[0]);
        plugin.database.delete(kit);
        int rowCount = 0;
        rowCount += plugin.database.find(SQLMember.class).eq("kitId", kit.getId()).delete();
        rowCount += plugin.database.find(SQLCooldown.class).eq("kitId", kit.getId()).delete();
        rowCount += plugin.database.find(SQLClaimed.class).eq("kitId", kit.getId()).delete();
        plugin.updateSidebarList();
        sender.sendMessage(join(noSeparators(),
                                text("Deleted kit #" + kit.getId() + ": ", AQUA),
                                kit.parseDisplayComponent(),
                                text(", member rows=" + rowCount, AQUA)));
        return true;
    }

    private void migrate(CommandSender sender) {
        File kitsFolder = new File(plugin.getDataFolder(), "kits");
        if (!kitsFolder.isDirectory()) {
            throw new CommandWarn("Kits folder not found: " + kitsFolder);
        }
        File usersFolder = new File(plugin.getDataFolder(), "users");
        if (!usersFolder.exists()) {
            throw new CommandWarn("Users folder not found: " + usersFolder);
        }
        List<LegacyKit> legacyKits = new ArrayList<>();
        for (File kitFile : kitsFolder.listFiles()) {
            if (!kitFile.isFile() || !kitFile.getName().endsWith(".json")) {
                throw new CommandWarn("Bad file: " + kitFile);
            }
            LegacyKit legacyKit = Json.load(kitFile, LegacyKit.class);
            if (legacyKit == null) {
                throw new CommandWarn("Invalid file: " + kitFile);
            }
            String name = kitFile.getName();
            name = name.substring(0, name.length() - 5);
            String conflictingKitName = findKitName(name);
            if (conflictingKitName != null) {
                throw new CommandWarn("Kit already exists: " + name + "/" + conflictingKitName);
            }
            legacyKit.setName(name);
            legacyKits.add(legacyKit);
        }
        Map<String, LegacyUsers> legacyUsersMap = new HashMap<>();
        for (File usersFile : usersFolder.listFiles()) {
            if (!usersFile.isFile() || !usersFile.getName().endsWith(".json")) {
                throw new CommandWarn("Bad file: " + usersFile);
            }
            String name = usersFile.getName();
            name = name.substring(0, name.length() - 5);
            LegacyUsers legacyUsers = Json.load(usersFile, LegacyUsers.class);
            if (legacyUsers == null) {
                throw new CommandWarn("Invalid file: " + usersFile);
            }
            legacyUsersMap.put(name, legacyUsers);
        }
        Date now = new Date();
        int kitCount = 0;
        int memberCount = 0;
        int cooldownCount = 0;
        int claimedCount = 0;
        for (LegacyKit legacyKit : legacyKits) {
            LegacyUsers legacyUsers = legacyUsersMap.get(legacyKit.getName());
            if (legacyUsers == null) {
                throw new CommandWarn("Kit without users: " + legacyKit.getName());
            }
            final KitType type;
            if (legacyKit.getPermission() != null && !legacyKit.getPermission().isEmpty()) {
                if (legacyKit.getCooldown() > 0) {
                    type = KitType.PERMISSION_COOLDOWN;
                } else {
                    type = KitType.PERMISSION;
                }
            } else {
                type = KitType.MEMBER;
            }
            SQLKit kit = new SQLKit(legacyKit.getName(), type);
            kit.setDisplayName(legacyKit.getDisplayName());
            kit.setDescription(!legacyKit.getDescription().isEmpty()
                               ? ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', legacyKit.getDescription().get(0)))
                               : null);
            kit.setPermission(!legacyKit.getPermission().isEmpty()
                              ? legacyKit.getPermission()
                              : null);
            kit.setEnabled(!legacyKit.isHidden());
            kit.setFriendship(legacyKit.getFriendship());
            kit.setCooldown(Math.max(0L, legacyKit.getCooldown()));
            kit.setCreatedTime(new Date(legacyKit.getDate()));
            SQLKit.Tag tag = new SQLKit.Tag();
            tag.setCommands(legacyKit.getCommands());
            tag.setTitles(legacyKit.getTitles());
            kit.setTag(!tag.isEmpty()
                       ? Json.serialize(tag)
                       : null);
            final int inventorySize = ((legacyKit.getItems().size() - 1) / 9 + 1) * 9;
            Inventory inventory = Bukkit.getServer().createInventory(null, inventorySize);
            for (LegacyKitItem legacyKitItem : legacyKit.getItems()) {
                inventory.addItem(legacyKitItem.createItemStack());
            }
            kit.setInventory(!inventory.isEmpty()
                             ? Json.serialize(InventoryStorage.of(inventory))
                             : null);
            try {
                kitCount += plugin.database.insert(kit);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "KitAdminCommand#migrate", e);
                throw new CommandWarn("Error saving kit: " + kit);
            }
            List<SQLMember> newMembers = new ArrayList<>();
            List<SQLCooldown> newCooldowns = new ArrayList<>();
            List<SQLClaimed> newClaimeds = new ArrayList<>();
            if (type == KitType.MEMBER) {
                for (UUID uuid : legacyKit.getMembers().keySet()) {
                    SQLMember member = new SQLMember(kit, uuid);
                    if (legacyUsers.getCooldowns().get(uuid) != null) {
                        // Will yield Long.MAX_VALUE
                        member.setClaimed(true);
                        member.setClaimedTime(now);
                    }
                    member.setCreatedTime(kit.getCreatedTime());
                    member.setEnabled(kit.isEnabled());
                    newMembers.add(member);
                }
            } else if (type == KitType.PERMISSION) {
                legacyUsers.getCooldowns().forEach((UUID uuid, Long time) -> {
                        SQLClaimed claimed = new SQLClaimed(kit, uuid);
                        claimed.setClaimedTime(kit.getCreatedTime());
                        newClaimeds.add(claimed);
                    });
            } else if (type == KitType.PERMISSION_COOLDOWN) {
                legacyUsers.getCooldowns().forEach((UUID uuid, Long epochSecond) -> {
                        SQLCooldown cooldown = new SQLCooldown(kit, uuid, Date.from(Instant.ofEpochSecond(epochSecond)));
                        cooldown.setClaimedTime(kit.getCreatedTime());
                        newCooldowns.add(cooldown);
                    });
            }
            try {
                memberCount += plugin.database.insert(newMembers);
                cooldownCount += plugin.database.insert(newCooldowns);
                claimedCount += plugin.database.insert(newClaimeds);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "KitAdminCommand#migrate", e);
                throw new CommandWarn("Error saving members/cooldowns: " + kit
                                      + " members=" + newMembers
                                      + " cds=" + newCooldowns
                                      + " claimed=" + newClaimeds);
            }
        }
        sender.sendMessage(text("Migration done"
                                + " kits=" + kitCount
                                + " members=" + memberCount
                                + " cds=" + cooldownCount
                                + " claimed=" + claimedCount,
                                AQUA));
    }
}
