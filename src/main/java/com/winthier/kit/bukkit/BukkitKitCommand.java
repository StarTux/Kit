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
            message.add(jsonItem(ChatColor.DARK_AQUA, "Your kits:"));
            for (BukkitKit kit : kits) {
                message.add(" ");
                if (kit.playerIsOnCooldown(uuid)) {
                    message.add(jsonItem(ChatColor.DARK_GRAY, kit.getName(),
                                         ChatColor.DARK_GRAY, "You are on cooldown for the " + kit.getName() + " kit.",
                                         null));
                } else {
                    message.add(jsonItem(ChatColor.AQUA, "[" + kit.getName() + "]",
                                         ChatColor.DARK_AQUA, "Click to get the " + kit.getName() + " kit.",
                                         "/kit " + kit.getName()));
                }
            }
            String json = JSONValue.toJSONString(message);
            System.out.println("json=" + json);
            String cmd = "minecraft:tellraw " + player.getName() + " " + json;
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd);
        } else if (args.length == 1) {
            UUID uuid = player.getUniqueId();
            String questName = args[0];
            BukkitKit kit = plugin.getKitNamed(questName);
            if (kit == null || !kit.playerHasPermission(uuid)) {
                player.sendMessage(ChatColor.RED + "Quest not found: " + questName);
                return true;
            }
            plugin.getLogger().info("Giving kit " + kit.getName() + " to " + player.getName());
            kit.giveToPlayer(uuid);
        } else {
            return false;
        }
        return true;
    }

    private Object jsonItem(ChatColor color, String text, ChatColor tooltipColor, String tooltip, String command)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("color", jsonColor(color));
        result.put("text", text);
        if (tooltip != null) {
            Map<String, Object> hover = new HashMap<>();
            result.put("hoverEvent", hover);
            hover.put("action", "show_text");
            Map<String, Object> hoverValue = new HashMap<>();
            hover.put("value", hoverValue);
            hoverValue.put("color", jsonColor(tooltipColor));
            hoverValue.put("text", tooltip);
        }
        if (command != null) {
            Map<String, Object> click = new HashMap<>();
            result.put("clickEvent", click);
            click.put("action", "run_command");
            click.put("value", command);
        }
        return result;
    }

    private Object jsonItem(ChatColor color, String text)
    {
        return jsonItem(color, text, null, null, null);
    }

    private String jsonColor(ChatColor color)
    {
        switch (color) {
        default: return color.name().toLowerCase();
        }
    }
}
