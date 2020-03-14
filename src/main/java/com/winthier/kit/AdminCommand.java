package com.winthier.kit;

import com.winthier.generic_events.GenericEvents;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class AdminCommand implements CommandExecutor {
    final KitPlugin plugin;

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        switch (args[0]) {
        case "reload": {
            if (args.length != 1) return false;
            plugin.reload();
            sender.sendMessage("Kit configs reloaded");
            return true;
        }
        case "give": {
            if (args.length != 3) return false;
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
        case "cooldowns": {
            if (args.length > 2) return false;
            if (args.length == 1) {
                sender.sendMessage(ChatColor.YELLOW + "Available cooldowns: "
                                   + ChatColor.WHITE
                                   + plugin.cooldowns.listCooldowns().stream()
                                   .collect(Collectors.joining(", ")));
            } else if (args.length == 2) {
                String kitName = args[1];
                sender.sendMessage(ChatColor.YELLOW + "Cooldowns for kit '" + kitName + "':");
                long now = Instant.now().getEpochSecond();
                for (UUID uuid : plugin.cooldowns.listCooldowns(kitName)) {
                    String name = GenericEvents.cachedPlayerName(uuid);
                    long cooldown = plugin.cooldowns.getCooldown(uuid, kitName);
                    long dist = cooldown - now;
                    if (dist <= 0) continue;
                    String time = cooldown == Long.MAX_VALUE
                        ? "Forever"
                        : formatTime(dist);
                    sender.sendMessage(ChatColor.GRAY + "- "
                                       + ChatColor.YELLOW + name + ": "
                                       + ChatColor.AQUA + time);
                }
            }
            return true;
        }
        case "player": {
            if (args.length != 2) return false;
            String name = args[1];
            UUID uuid = GenericEvents.cachedPlayerUuid(name);
            if (uuid == null) {
                sender.sendMessage(ChatColor.RED + "Unknown player: "
                                   + name);
                return true;
            }
            name = GenericEvents.cachedPlayerName(uuid);
            long now = Instant.now().getEpochSecond();
            List<String> ls = new ArrayList<>();
            for (Kit kit : plugin.kits) {
                long cd = plugin.cooldowns.getCooldown(uuid, kit.name);
                if (cd == 0) continue;
                long dist = cd - now;
                String time;
                if (cd == Long.MAX_VALUE) {
                    time = "Forever";
                } else if (cd < now) {
                    time = "Expired";
                } else {
                    time = formatTime(dist);
                }
                ls.add(ChatColor.GRAY + "- "
                       + ChatColor.YELLOW + kit.name + ": "
                       + ChatColor.AQUA + time);
            }
            if (ls.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "No cooldowns.");
                return true;
            }
            sender.sendMessage("" + ChatColor.YELLOW + ls.size()
                               + (ls.size() == 1 ? " cooldown" : " cooldowns"));
            for (String s : ls) {
                sender.sendMessage(s);
            }
            return true;
        }
        default:
            return false;
        }
    }

    String formatTime(long seconds) {
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("%d:%02d.%02d", hours, minutes % 60L, seconds % 60L);
    }
}
