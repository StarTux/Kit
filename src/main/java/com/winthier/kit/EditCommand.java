package com.winthier.kit;

import com.winthier.generic_events.GenericEvents;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class EditCommand implements TabExecutor {
    final KitPlugin plugin;

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
            kit.hidden = true;
            plugin.kits.put(kitName, kit);
            plugin.saveKit(kit);
            sender.sendMessage("Kit created: " + kitName);
            return true;
        }
        Kit kit = plugin.getKitNamed(kitName);
        if (kit == null) throw new Wrong("Kit not found: " + kitName);
        switch (cmd) {
        case "permission":
            if (args.length != 1) return false;
            String arg = args[0];
            kit.permission = arg.equals(".") ? "" : arg;
            sender.sendMessage("Permission updated: " + kit.permission);
            break;
        case "items":
            if (!(sender instanceof Player)) throw new Wrong("player expected");
            Player player = (Player) sender;
            KitHolder holder = new KitHolder();
            holder.inventory = plugin.getServer().createInventory(holder, 9 * 6, kit.name + " - Editor");
            int i = 0;
            for (KitItem kitItem : kit.items) {
                int index = i++;
                holder.inventory.setItem(index, kitItem.createItemStack());
            }
            kit.items.clear();
            holder.onClose = () -> {
                kit.items.clear();
                for (ItemStack item : holder.inventory.getContents()) {
                    if (item == null || item.getAmount() == 0) continue;
                    kit.items.add(new KitItem(item));
                }
                plugin.saveKit(kit);
                player.sendMessage("" + ChatColor.YELLOW + kit.items.size() + " item(s) saved");
            };
            player.openInventory(holder.inventory);
            // Don't save just yet
            return true;
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
            for (String name : args) {
                UUID uuid = GenericEvents.cachedPlayerUuid(name);
                if (uuid == null) throw new Wrong("Player not found: " + name);
                if (kit.members.containsKey(uuid)) {
                    sender.sendMessage(ChatColor.RED + "Already added: " + name);
                    continue;
                }
                kit.members.put(uuid, name);
                sender.sendMessage(ChatColor.YELLOW + "Member added: " + name);
                count += 1;
            }
            sender.sendMessage("" + ChatColor.YELLOW + count + " members added");
            break;
        }
        case "rmmember": {
            int count = 0;
            for (String name : args) {
                UUID uuid = GenericEvents.cachedPlayerUuid(name);
                if (uuid == null) throw new Wrong("Player not found: " + name);
                String oldName = kit.members.remove(uuid);
                if (oldName == null) {
                    sender.sendMessage(ChatColor.RED + "Not a member: " + name);
                    continue;
                }
                sender.sendMessage(ChatColor.YELLOW + "Member removed: " + oldName);
                count += 1;
            }
            sender.sendMessage("" + ChatColor.YELLOW + count + " members removed");
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
                             "cooldown", "hide", "show", "msg", "rmmsg", "cmd",
                             "rmcmd", "desc", "rmdesc", "member", "rmmember")
                .filter(s -> s.contains(arg))
                .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return plugin.kits.keySet().stream()
                .filter(s -> s.contains(arg))
                .collect(Collectors.toList());
        }
        return null;
    }
}
