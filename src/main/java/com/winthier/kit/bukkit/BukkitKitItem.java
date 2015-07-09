package com.winthier.kit.bukkit;

import com.winthier.kit.KitItem;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class BukkitKitItem extends KitItem
{
    final BukkitKitPlugin plugin;
    @Getter String itemID = "minecraft:air";
    @Getter int itemAmount = 1;
    @Getter int itemData = 0;
    @Getter String itemDataTag = "";

    BukkitKitItem(BukkitKitPlugin plugin){
        this.plugin = plugin;
    }

    @Override
    public void giveToPlayer(UUID uuid)
    {
        Player player = Bukkit.getServer().getPlayer(uuid);
        if (player == null) return;
        String command = consoleCommand(player.getName());
        
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
    }

    void readConfig(ConfigurationSection config)
    {
        itemID = config.getString("ID", itemID);
        itemAmount = config.getInt("Amount", itemAmount);
        itemData = config.getInt("Data", itemData);
        itemDataTag = config.getString("DataTag", itemDataTag);
    }
}
