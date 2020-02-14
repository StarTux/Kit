package com.winthier.kit;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class KitCommand implements CommandExecutor {
    final KitPlugin plugin;

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Reload
        if (args.length == 1 && args[0].equalsIgnoreCase("-reload")) {
            if (!sender.hasPermission("kit.admin")) {
                sender.sendMessage("No permission");
                return true;
            }
            plugin.reload();
            sender.sendMessage("Kit configs reloaded");
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("-give")) {
            if (!sender.hasPermission("kit.admin")) {
                sender.sendMessage("No permission");
                return true;
            }
            Player target = plugin.getServer().getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
            Kit kit = plugin.getKitNamed(args[2]);
            if (kit == null) {
                sender.sendMessage("Kit not found: " + args[2]);
                return true;
            }
            if (kit.playerIsOnCooldown(target)) {
                return true;
            }
            kit.setPlayerOnCooldown(target);
            plugin.getLogger().info("Giving kit " + kit.name + " to " + target.getName());
            kit.giveToPlayer(target);
            sender.sendMessage("Kit " + kit.name + " given to " + target.getName());
            return true;
        }
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player == null) {
            sender.sendMessage("Player expected");
            return true;
        }
        if (args.length == 0) {
            List<Kit> kits = new ArrayList<>();
            for (Kit kit : plugin.kits) {
                if (!kit.playerHasPermission(player)) continue;
                if (kit.hidden) continue;
                kits.add(kit);
            }
            if (kits.isEmpty()) {
                player.sendMessage(ChatColor.RED + "There are no kits available for you.");
                return true;
            }
            List<Object> message = new ArrayList<>();
            message.add(format("&3&lKits"));
            for (Kit kit : kits) {
                message.add(" ");
                if (kit.playerIsOnCooldown(player)) {
                    message.add(button("&r[&8" + kit.name + "&r]",
                                       "&8/kit " + kit.name.toLowerCase()
                                       + "\n&oKit\nYou are on cooldown.",
                                       "/kit " + kit.name.toLowerCase()));
                } else {
                    message.add(button("&r[&a" + kit.name + "&r]",
                                       "&a/kit " + kit.name.toLowerCase()
                                       + "\n&oKit\nGet the " + kit.name + " kit.",
                                       "/kit " + kit.name.toLowerCase()));
                }
            }
            String json = new Gson().toJson(message);
            String cmd = "minecraft:tellraw " + player.getName() + " " + json;
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        } else if (args.length == 1) {
            String kitName = args[0];
            Kit kit = plugin.getKitNamed(kitName);
            if (kit == null || !kit.playerHasPermission(player) || kit.hidden) {
                player.sendMessage(ChatColor.RED + "Kit not found: " + kitName);
                return true;
            }
            if (kit.playerIsOnCooldown(player)) {
                kit.sendCooldownMessage(player);
                return true;
            }
            kit.setPlayerOnCooldown(player);
            plugin.getLogger().info("Giving kit " + kit.name + " to " + player.getName());
            kit.giveToPlayer(player);
        } else {
            return false;
        }
        return true;
    }

    Object button(String chat, String tooltip, String command) {
        Map<String, Object> map = new HashMap<>();

        map.put("text", "");
        List<Object> extraList = new ArrayList<>();
        for (String token : format(chat).split(" ")) {
            if (!extraList.isEmpty()) extraList.add(" ");
            extraList.add(token);
        }
        map.put("extra", extraList);
        Map<String, Object> map2 = new HashMap<>();
        map.put("clickEvent", map2);
        map2.put("action", "run_command");
        map2.put("value", command);
        map2 = new HashMap<>();
        map.put("hoverEvent", map2);
        map2.put("action", "show_text");
        map2.put("value", format(tooltip));
        return map;
    }

    static String format(String msg, Object... args) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (args.length > 0) msg = String.format(msg, args);
        return msg;
    }
}
