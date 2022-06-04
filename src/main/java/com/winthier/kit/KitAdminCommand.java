package com.winthier.kit;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
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
import com.winthier.playercache.PlayerCache;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class KitAdminCommand extends AbstractCommand<KitPlugin> {
    protected KitAdminCommand(final KitPlugin plugin) {
        super(plugin, "kitadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("copy").arguments("<kit> <name>")
            .description("Clone a kit")
            .completers(CommandArgCompleter.supplyIgnoreCaseList(this::getKitNames),
                        CommandArgCompleter.supplyIgnoreCaseList(this::getKitNames))
            .senderCaller(this::copy);
        rootNode.addChild("check").denyTabCompletion()
            .description("Run a sidebar check")
            .senderCaller(this::check);
        rootNode.addChild("migrate").denyTabCompletion()
            .description("Import legacy files")
            .senderCaller(this::migrate);
    }

    private List<String> getKitNames() {
        return plugin.database.find(SQLKit.class).select("name").findValues(String.class);
    }

    private SQLKit findKit(String name) {
        return plugin.database.find(SQLKit.class).eq("name", name).findUnique();
    }

    private SQLKit requireKit(String name) {
        SQLKit kit = findKit(name);
        if (kit == null) throw new CommandWarn("Kit not found: " + name);
        return kit;
    }

    private boolean copy(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        String oldKitName = args[0];
        String newKitName = args[1];
        SQLKit kit = requireKit(oldKitName);
        if (findKit(newKitName) != null) {
            throw new CommandWarn("Kit already exists: " + newKitName);
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
        sender.sendMessage(text("Added " + added + ", skipped " + skipped + " members from file " + path, YELLOW));
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
        List<SQLMember> newRows = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : MemberList.get(listName).entrySet()) {
            newRows.add(new SQLMember(kit, entry.getKey()));
        }
        final int added = plugin.database.insertIgnore(newRows);
        final int skipped = newRows.size() - added;
        sender.sendMessage(text("Added " + added + ", skipped " + skipped + " members from MemberList " + listName,
                                YELLOW));
        return true;
    }

    private void check(CommandSender sender) {
        plugin.updateSidebarList();
        sender.sendMessage(text("Sidebar list updated", AQUA));
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
            legacyKit.setName(name.substring(0, name.length() - 5));
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
                    member.setEnabled(kit.isEnabled());
                    newMembers.add(member);
                }
            } else if (type == KitType.PERMISSION) {
                legacyUsers.getCooldowns().forEach((UUID uuid, Long time) -> {
                        SQLClaimed claimed = new SQLClaimed(kit, uuid);
                        claimed.setClaimedTime(now);
                        newClaimeds.add(claimed);
                    });
            } else if (type == KitType.PERMISSION_COOLDOWN) {
                legacyUsers.getCooldowns().forEach((UUID uuid, Long epochSecond) -> {
                        SQLCooldown cooldown = new SQLCooldown(kit, uuid, Date.from(Instant.ofEpochSecond(epochSecond)));
                        cooldown.setClaimedTime(now);
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
