package com.winthier.kit.bukkit;

import com.winthier.kit.Kit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

public class BukkitKit extends Kit
{
    final BukkitKitPlugin plugin;
    @Getter
    String name;
    @Getter
    String message;
    @Getter
    List<BukkitKitItem> items = new ArrayList<>();;
    @Getter
    int cooldownInSeconds;

    BukkitKit(BukkitKitPlugin plugin, String name) {
        this.name = name;
        this.plugin = plugin;
    }
    
    @Override
    protected boolean playerIsOnCooldown(UUID player)
    {
        Long cooldown = plugin.getCooldowns().getCooldown(player, name);
        if (cooldown == null) return false;
        return cooldown > System.currentTimeMillis();
    }
    
    @Override
    protected boolean playerHasPermission(UUID uuid)
    {
        Player player = Bukkit.getServer().getPlayer(uuid);
        if (player == null) return false;
        if (player.isOp()) return true;
        return player.hasPermission("kit." + getName().toLowerCase());
    }

    @Override
    protected void setPlayerOnCooldown(UUID player)
    {
        plugin.getCooldowns().setCooldownInSeconds(player, name, cooldownInSeconds);
        plugin.getCooldowns().save();
    }
    
    @Override
    protected void sendCooldownMessage(UUID uuid)
    {
        Player player = Bukkit.getServer().getPlayer(uuid);
        if (player == null) return;
        player.sendMessage(ChatColor.RED + "You are still on cooldown");
    }

    @Override
    protected void sendMessage(UUID uuid)
    {
        Player player = Bukkit.getServer().getPlayer(uuid);
        if (player == null) return;
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    void readConfig(ConfigurationSection config)
    {
        cooldownInSeconds = config.getInt("CooldownInSeconds");
        message = config.getString("Message");
        MemoryConfiguration tmpSection = new MemoryConfiguration();
        for (Map<?, ?> map : config.getMapList("items")) {
            ConfigurationSection section = tmpSection.createSection("tmp", map);
            BukkitKitItem item = new BukkitKitItem(plugin);
            item.readConfig(section);
            items.add(item);
        }
    }
}
