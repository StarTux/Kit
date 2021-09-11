package com.winthier.kit;

import com.winthier.playercache.PlayerCache;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
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
        case "list": {
            if (args.length != 1) return false;
            sender.sendMessage(plugin.kits.size() + " kits loaded: "
                               + plugin.kits.values().stream()
                               .map(k -> k.name).collect(Collectors.joining(", ")));
            return true;
        }
        case "info": {
            if (args.length != 2) return false;
            Kit kit = plugin.getKitNamed(args[1]);
            if (kit == null) {
                sender.sendMessage("Kit not found: " + args[1]);
                return true;
            }
            kitInfo(sender, kit);
            return true;
        }
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
            if (args.length != 2) return false;
            String kitName = args[1];
            Kit kit = plugin.getKitNamed(kitName);
            if (kit == null) {
                sender.sendMessage(ChatColor.RED + "Kit not found: " + kitName);
                return true;
            }
            Set<UUID> uuids = kit.users.cooldowns.keySet();
            sender.sendMessage("" + ChatColor.YELLOW + uuids.size() + " cooldowns for kit '" + kit.name + "':");
            long now = Instant.now().getEpochSecond();
            for (UUID uuid : uuids) {
                String name = PlayerCache.nameForUuid(uuid);
                long cooldown = kit.getPlayerCooldown(uuid);
                long dist = cooldown - now;
                String time;
                if (cooldown == Long.MAX_VALUE) {
                    time = "Forever";
                } else if (dist <= 0) {
                    time = "Expired";
                } else {
                    time = formatTime(dist);
                }
                sender.sendMessage(" " + ChatColor.YELLOW + name + ": " + ChatColor.AQUA + time);
            }
            return true;
        }
        case "player": {
            if (args.length != 2) return false;
            String name = args[1];
            UUID uuid = PlayerCache.uuidForName(name);
            if (uuid == null) {
                sender.sendMessage(ChatColor.RED + "Unknown player: "
                                   + name);
                return true;
            }
            name = PlayerCache.nameForUuid(uuid);
            long now = Instant.now().getEpochSecond();
            List<String> ls = new ArrayList<>();
            for (Kit kit : plugin.kits.values()) {
                long cd = kit.getPlayerCooldown(uuid);
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
                sender.sendMessage(ChatColor.RED + name + " has no cooldowns.");
                return true;
            }
            sender.sendMessage(ChatColor.YELLOW + name + " has " + ls.size()
                               + (ls.size() == 1 ? "cooldown:" : "cooldowns:"));
            for (String s : ls) {
                sender.sendMessage(s);
            }
            return true;
        }
        case "set": {
            if (args.length != 3 && args.length != 4) return false;
            String kitName = args[1];
            String playerName = args[2];
            Kit kit = plugin.getKitNamed(kitName);
            if (kit == null) {
                sender.sendMessage(ChatColor.RED + "Kit not found: " + kitName);
                return true;
            }
            UUID uuid = PlayerCache.uuidForName(playerName);
            if (uuid == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
                return true;
            }
            playerName = PlayerCache.nameForUuid(uuid);
            if (args.length == 3) {
                if (!kit.resetPlayerCooldown(uuid)) {
                    sender.sendMessage(ChatColor.RED + "Player " + playerName
                                       + " was not on cooldown: " + kit.name);
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Cooldown cleared: "
                                       + playerName + ", " + kit.name);
                }
            } else if (args.length == 4) {
                long seconds;
                try {
                    seconds = Long.parseLong(args[3]);
                } catch (NumberFormatException nfe) {
                    sender.sendMessage(ChatColor.RED + "Seconds expected: "
                                       + args[3]);
                    return true;
                }
                long cd = kit.setPlayerOnCooldown(uuid, seconds);
                sender.sendMessage(ChatColor.YELLOW + "Player " + playerName
                                   + " now has cooldown for " + kit.name + ": "
                                   + new Date(cd * 1000L));
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

    void kitInfo(CommandSender sender, Kit kit) {
        info(sender, "name", kit.name);
        info(sender, "displayName", kit.parseDisplayName());
        info(sender, "hidden", "" + kit.hidden);
        info(sender, "cooldown", "" + kit.cooldown);
        info(sender, "permission", kit.permission);
        info(sender, "items", kit.getAllItemStrings());
        info(sender, "commands", kit.commands);
        info(sender, "friendship", "" + kit.friendship);
        info(sender, "description", kit.description);
        info(sender, "messages", kit.messages);
        info(sender, "members", kit.members.values());
    }

    void info(CommandSender sender, String key, Component value) {
        sender.sendMessage(TextComponent.ofChildren(new Component[] {
                    Component.text(key + " ", NamedTextColor.GRAY),
                    value,
                }));
    }

    void info(CommandSender sender, String key, String value) {
        info(sender, key, Component.text(value));
    }

    void info(CommandSender sender, String key, Collection<String> list) {
        info(sender, key, String.join(" ", list));
    }
}
