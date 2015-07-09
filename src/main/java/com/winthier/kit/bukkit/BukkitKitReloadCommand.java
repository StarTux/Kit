package com.winthier.kit.bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;;
import org.bukkit.command.CommandSender;

public class BukkitKitReloadCommand implements CommandExecutor
{
    final BukkitKitPlugin plugin;

    BukkitKitReloadCommand(BukkitKitPlugin plugin)
    {
        this.plugin = plugin;
    }
    
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        plugin.reload();
        sender.sendMessage("Kit configs reloaded");
        return true;
    }
}
