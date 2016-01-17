package com.winthier.kit.bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.simple.JSONValue;

public class BukkitKitCommand implements CommandExecutor
{
    final BukkitKitPlugin plugin;

    BukkitKitCommand(BukkitKitPlugin plugin)
    {
        this.plugin = plugin;
    }
    
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
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
        Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) {
            sender.sendMessage("Player expected");
            return true;
        }
        if (args.length == 0) {
            List<BukkitKit> kits = new ArrayList<>();
            UUID uuid = player.getUniqueId();
            for (BukkitKit kit : plugin.getAllKits()) if (kit.playerHasPermission(uuid)) kits.add(kit);
            if (kits.isEmpty()) {
                player.sendMessage(ChatColor.RED + "There are no kits available for you.");
                return true;
            }
            List<Object> message = new ArrayList<>();
            message.add(format("&3&lKits"));
            for (BukkitKit kit : kits) {
                message.add(" ");
                if (kit.playerIsOnCooldown(uuid)) {
                    message.add(button("&r[&8"+kit.getName()+"&r]",
                                       "&8/kit "+kit.getName().toLowerCase()+"\n&oKit\nYou are on cooldown.",
                                       "/kit "+kit.getName().toLowerCase()));
                } else {
                    message.add(button("&r[&a"+kit.getName()+"&r]",
                                       "&a/kit "+kit.getName().toLowerCase()+"\n&oKit\nGet the "+kit.getName()+" kit.",
                                       "/kit "+kit.getName().toLowerCase()));
                }
            }
            String json = JSONValue.toJSONString(message);
            String cmd = "minecraft:tellraw " + player.getName() + " " + json;
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd);
        } else if (args.length == 1) {
            UUID uuid = player.getUniqueId();
            String kitName = args[0];
            BukkitKit kit = plugin.getKitNamed(kitName);
            if (kit == null || !kit.playerHasPermission(uuid)) {
                player.sendMessage(ChatColor.RED + "Kit not found: " + kitName);
                return true;
            }
            plugin.getLogger().info("Giving kit " + kit.getName() + " to " + player.getName());
            kit.giveToPlayer(uuid);
        } else {
            return false;
        }
        return true;
    }

    Object button(String chat, String tooltip, String command)
    {
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
