package com.winthier.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class KitCommand implements CommandExecutor {
    final KitPlugin plugin;

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player == null) {
            sender.sendMessage("Player expected");
            return true;
        }
        if (args.length == 0) {
            showKitList(player);
            return true;
        }
        if (args.length != 1) return false;
        String kitName = args[0];
        Kit kit = plugin.getKitNamed(kitName);
        if (kit == null || !kit.playerCanClaim(player)) {
            player.sendMessage(ChatColor.RED + "Kit not found: " + kitName);
            return true;
        }
        if (kit.playerIsOnCooldown(player)) {
            if (kit.hasInfiniteCooldown()) {
                player.sendMessage(ChatColor.RED + "You already claimed this kit.");
            } else {
                long secs = kit.getRemainingCooldown(player);
                player.sendMessage(ChatColor.RED + "You are on cooldown: "
                                   + ChatColor.GRAY + formatSeconds(secs));
            }
            return true;
        }
        kit.setPlayerOnCooldown(player);
        plugin.getLogger().info("Giving kit " + kit.name + " to " + player.getName());
        kit.giveToPlayer(player);
        return true;
    }

    void showKitList(Player player) {
        List<Kit> kits = plugin.kits.stream()
            .filter(kit -> kit.playerCanSee(player))
            .collect(Collectors.toList());
        if (kits.isEmpty()) {
            player.sendMessage(ChatColor.RED + "There are no kits available for you.");
            return;
        }
        ComponentBuilder cb = new ComponentBuilder();
        cb.append("Kits:").color(ChatColor.GRAY);
        for (Kit kit : kits) {
            cb.append(" ").reset();
            String cmd = "/kit " + kit.name.toLowerCase();
            cb.append("[" + kit.name + "]");
            cb.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
            if (kit.playerIsOnCooldown(player)) {
                cb.color(ChatColor.DARK_GRAY);
                String remain;
                if (kit.hasInfiniteCooldown()) {
                    remain = ChatColor.RED + "Already claimed!";
                } else {
                    long secs = kit.getRemainingCooldown(player);
                    remain = ChatColor.DARK_GRAY + "You are on cooldown: "
                        + ChatColor.GRAY + formatSeconds(secs);
                }
                List<BaseComponent> tooltip = new ArrayList<>();
                tooltip.add(new TextComponent("" + ChatColor.DARK_GRAY + cmd));
                tooltip.add(new TextComponent("\n" + remain));
                for (String line : kit.description) {
                    line = "\n" + ChatColor.stripColor(fmt(line));
                    tooltip.add(new TextComponent("" + ChatColor.DARK_GRAY + line));
                }
                cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        tooltip.toArray(new TextComponent[0])));
            } else {
                cb.color(ChatColor.GREEN);
                List<BaseComponent> tooltip = new ArrayList<>();
                tooltip.add(new TextComponent("" + ChatColor.GREEN + cmd));
                for (String line : kit.description) {
                    tooltip.add(new TextComponent("\n" + fmt(line)));
                }
                cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        tooltip.toArray(new TextComponent[0])));
            }
        }
        player.spigot().sendMessage(cb.create());
    }

    String formatSeconds(long seconds) {
        long minutes = seconds / 60;
        if (minutes <= 60) {
            return String.format("%02d:%02d", minutes, seconds % 60);
        }
        long hours = minutes / 60;
        if (hours <= 24) {
            return String.format("%02dh%02d:%02d", hours, minutes % 60, seconds % 60);
        }
        long days = hours / 24;
        return String.format("%d%02dh%02d:%02d", days, hours % 24, minutes % 60, seconds % 60);
    }

    static String fmt(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }
}
