package com.winthier.kit;

import com.cavetale.memberlist.MemberList;
import com.winthier.playercache.PlayerCache;
import com.winthier.title.Title;
import com.winthier.title.TitlePlugin;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class EditCommand implements TabExecutor {
    final KitPlugin plugin;
    protected final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd");

    final class Wrong extends Exception {
        Wrong(final String msg) {
            super(msg);
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) return false;
        try {
            return onCommand(sender, args[0], args[1], Arrays.copyOfRange(args, 2, args.length));
        } catch (Wrong w) {
            sender.sendMessage(ChatColor.RED + w.getMessage());
            return true;
        }
    }

    boolean onCommand(CommandSender sender, String cmd, String kitName, String[] args) throws Wrong {
        if ("create".equals(cmd)) {
            if (plugin.getKitNamed(kitName) != null) throw new Wrong("Kit already exists: " + kitName);
            Kit kit = new Kit(plugin, kitName);
            kit.date = System.currentTimeMillis();
            kit.hidden = true;
            plugin.kits.put(kitName, kit);
            plugin.saveKit(kit);
            sender.sendMessage("Kit created: " + kitName);
            return true;
        }
        Kit kit = plugin.getKitNamed(kitName);
        if (kit == null) throw new Wrong("Kit not found: " + kitName);
        switch (cmd) {
        case "permission": {
            if (args.length != 1) return false;
            String arg = args[0];
            kit.permission = arg.equals(".") ? "" : arg;
            sender.sendMessage("Permission updated: " + kit.permission);
            break;
        }
        case "items": {
            if (!(sender instanceof Player)) throw new Wrong("player expected");
            Player player = (Player) sender;
            KitHolder holder = new KitHolder(kit);
            holder.inventory = plugin.getServer().createInventory(holder, 9 * 6, Component.text(kit.name + " - Editor"));
            int i = 0;
            for (KitItem kitItem : kit.items) {
                int index = i++;
                holder.inventory.setItem(index, kitItem.createItemStack());
            }
            kit.items.clear();
            holder.onClose = () -> {
                kit.items.clear();
                for (ItemStack item : holder.inventory.getContents()) {
                    if (item == null || item.getType() == Material.AIR) continue;
                    kit.items.add(new KitItem(item));
                }
                plugin.saveKit(kit);
                player.sendMessage("" + ChatColor.YELLOW + kit.items.size() + " item(s) saved");
            };
            player.openInventory(holder.inventory);
            // Don't save just yet
            return true;
        }
        case "cooldown": {
            if (args.length != 1) return false;
            long cd;
            try {
                cd = Long.parseLong(args[0]);
            } catch (NumberFormatException nfe) {
                throw new Wrong("Invalid cooldown: " + args[0]);
            }
            kit.cooldown = cd;
            sender.sendMessage("" + ChatColor.YELLOW + "Cooldown set to " + kit.cooldown);
            break;
        }
        case "hide": {
            kit.hidden = true;
            sender.sendMessage("" + ChatColor.YELLOW + "Kit hidden");
            break;
        }
        case "show": {
            kit.hidden = false;
            sender.sendMessage("" + ChatColor.YELLOW + "Kit is now public");
            break;
        }
        case "msg": appendStringList(sender, kit.messages, args); break;
        case "rmmsg": removeStringList(sender, kit.messages, args); break;
        case "cmd": appendStringList(sender, kit.commands, args); break;
        case "rmcmd": removeStringList(sender, kit.commands, args); break;
        case "desc": appendStringList(sender, kit.description, args); break;
        case "rmdesc": removeStringList(sender, kit.description, args); break;
        case "member": {
            int count = 0;
            for (String arg : args) {
                PlayerCache player = PlayerCache.forArg(arg);
                if (player == null) throw new Wrong("Player not found: " + arg);
                if (kit.members.containsKey(player.uuid)) {
                    sender.sendMessage(ChatColor.RED + "Already added: " + player.name);
                    continue;
                }
                kit.members.put(player.uuid, player.name);
                sender.sendMessage(ChatColor.YELLOW + "Member added: " + player.name);
                count += 1;
            }
            sender.sendMessage("" + ChatColor.YELLOW + count + " members added");
            break;
        }
        case "rmmember": {
            int count = 0;
            for (String arg : args) {
                PlayerCache player = PlayerCache.forArg(arg);
                if (arg == null) throw new Wrong("Player not found: " + arg);
                String oldName = kit.members.remove(player.uuid);
                if (oldName == null) {
                    sender.sendMessage(ChatColor.RED + "Not a member: " + player.name);
                    continue;
                }
                sender.sendMessage(ChatColor.YELLOW + "Member removed: " + oldName);
                count += 1;
            }
            sender.sendMessage("" + ChatColor.YELLOW + count + " members removed");
            break;
        }
        case "memberlist": {
            if (args.length == 0) return false;
            String listName = String.join(" ", args);
            int skipped = 0;
            int added = 0;
            for (Map.Entry<UUID, String> entry : MemberList.get(listName).entrySet()) {
                if (kit.members.containsKey(entry.getKey())) {
                    skipped += 1;
                } else {
                    kit.members.put(entry.getKey(), entry.getValue());
                    added += 1;
                }
            }
            sender.sendMessage(Component.text("Added " + added + ", skipped " + skipped + " members from MemberList " + listName,
                                              NamedTextColor.YELLOW));
            break;
        }
        case "friendship": {
            if (args.length != 1) return false;
            try {
                kit.friendship = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                sender.sendMessage(Component.text("Number expected: " + args[0], NamedTextColor.RED));
                return true;
            }
            plugin.saveKit(kit);
            sender.sendMessage(Component.text("Friendship: " + kit.friendship, NamedTextColor.YELLOW));
            break;
        }
        case "displayname": {
            if (args.length == 0) return false;
            String displayName = String.join(" ", args);
            try {
                Component component = GsonComponentSerializer.gson().deserialize(displayName);
                kit.setDisplayName(component);
            } catch (Exception e) {
                sender.sendMessage(Component.text("Invalid component: " + displayName, NamedTextColor.RED));
                return true;
            }
            sender.sendMessage(Component.text().content("Display name updated: ").color(NamedTextColor.YELLOW)
                               .append(kit.parseDisplayName()));
            break;
        }
        case "copy": {
            if (args.length != 1) return false;
            String newKitName = args[0];
            if (plugin.getKitNamed(newKitName) != null) {
                throw new Wrong("Kit already exists: " + newKitName);
            }
            Kit newKit = kit.clone();
            newKit.plugin = plugin;
            newKit.name = newKitName;
            newKit.hidden = true;
            newKit.members.clear();
            newKit.date = System.currentTimeMillis();
            plugin.kits.put(newKitName, newKit);
            plugin.saveKit(newKit);
            sender.sendMessage(ChatColor.YELLOW + "Kit cloned: " + kit.getName() + " => " + newKit.getName());
            return true;
        }
        case "info": {
            if (args.length != 0) return false;
            plugin.adminCommand.kitInfo(sender, kit);
            return true;
        }
        case "date": {
            if (args.length != 1) return false;
            if ("0".equals(args[0])) {
                kit.date = 0L;
                sender.sendMessage(Component.text("Date reset!", NamedTextColor.YELLOW));
                break;
            }
            Date date;
            try {
                date = dateParser.parse(args[0]);
            } catch (ParseException pe) {
                throw new Wrong("Invalid date, expected yyyy-mm-dd: " + args[0]);
            }
            kit.date = date.getTime();
            sender.sendMessage(Component.text("Date: " + plugin.command.dateFormat.format(date),
                                              NamedTextColor.YELLOW));
            break;
        }
        case "titles": {
            if (args.length == 0) {
                kit.titles = List.of();
                sender.sendMessage(Component.text("Titles reset!", NamedTextColor.YELLOW));
            } else {
                List<Component> titles = new ArrayList<>(args.length);
                for (String arg : args) {
                    Title title = TitlePlugin.getInstance().getTitle(arg);
                    if (title == null) {
                        throw new Wrong("Title not found: " + arg);
                    }
                    titles.add(title.getTitleComponent());
                }
                kit.titles = List.of(args);
                sender.sendMessage(Component.join(JoinConfiguration.builder()
                                                  .prefix(Component.text("Title list updated: ",
                                                                         NamedTextColor.YELLOW))
                                                  .separator(Component.space()).build(),
                                                  titles));
            }
            break;
        }
        default: throw new Wrong("Command not found: " + cmd);
        }
        plugin.saveKit(kit);
        return true;
    }

    void appendStringList(CommandSender sender, List<String> list, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("" + ChatColor.YELLOW + list.size() + " entries");
            int i = 0;
            for (String line : list) {
                sender.sendMessage(" " + ChatColor.YELLOW + (i++) + ") " + ChatColor.RESET + line);
            }
            return;
        }
        String line = Stream.of(args).collect(Collectors.joining(" "));
        list.add(line);
        sender.sendMessage(ChatColor.YELLOW + "Line added: " + ChatColor.RESET + line);
    }

    void removeStringList(CommandSender sender, List<String> list, String[] args) throws Wrong {
        if (args.length != 1) throw new Wrong("Index required");
        int index;
        try {
            index = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            throw new Wrong("Invalid index: " + args[0]);
        }
        if (index < 0 || index >= list.size()) {
            throw new Wrong("Index out of bounds: " + index);
        }
        String line = list.remove(index);
        sender.sendMessage(ChatColor.YELLOW + " Line " + index + " removed: " + ChatColor.RESET + line);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return null;
        String arg = args[args.length - 1];
        if (args.length == 1) {
            return Stream.of("create", "permission", "items", "info",
                             "cooldown", "hide", "show", "msg",
                             "rmmsg", "cmd", "rmcmd", "desc",
                             "rmdesc", "member", "rmmember",
                             "memberlist", "friendship",
                             "displayname", "date", "titles")
                .filter(s -> s.contains(arg))
                .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return plugin.kits.keySet().stream()
                .filter(s -> s.contains(arg))
                .collect(Collectors.toList());
        }
        if (args[0].equals("titles")) {
            return TitlePlugin.getInstance().getTitles().stream()
                .map(Title::getName)
                .filter(s -> s.toLowerCase().contains(arg.toLowerCase()))
                .collect(Collectors.toList());
        }
        return null;
    }
}
