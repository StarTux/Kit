package com.winthier.kit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class Kit {
    final KitPlugin plugin;
    @Getter final String name;
    @Getter List<String> messages;
    @Getter String permission;
    @Getter List<KitItem> items = new ArrayList<>();;
    @Getter int cooldown;
    @Getter boolean hidden;
    List<String> commands;

    public boolean playerIsOnCooldown(UUID player) {
        long cd = plugin.cooldowns.getCooldown(player, name);
        if (cd == 0) return false;
        return cd > Instant.now().getEpochSecond();
    }

    public boolean playerIsOnCooldown(Player player) {
        return playerIsOnCooldown(player.getUniqueId());
    }

    public boolean playerHasPermission(Player player) {
        if (permission == null || permission.isEmpty()) return true;
        if (player.isOp()) return true;
        return player.hasPermission(permission);
    }

    public void setPlayerOnCooldown(UUID player) {
        plugin.cooldowns.setCooldown(player, name, cooldown);
        plugin.cooldowns.save();
    }

    public void setPlayerOnCooldown(Player player) {
        setPlayerOnCooldown(player.getUniqueId());
    }

    public void sendCooldownMessage(Player player) {
        player.sendMessage(ChatColor.RED + "You are still on cooldown");
    }

    public void sendMessage(Player player) {
        for (String msg : messages) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }
    }

    void load(ConfigurationSection config) {
        cooldown = config.getInt("Cooldown");
        messages = config.getStringList("Messages");
        permission = config.getString("Permission");
        hidden = config.getBoolean("Hidden");
        MemoryConfiguration tmpSection = new MemoryConfiguration();
        for (Map<?, ?> map : config.getMapList("Items")) {
            ConfigurationSection section = tmpSection.createSection("tmp", map);
            KitItem item = new KitItem(plugin);
            item.load(section);
            items.add(item);
        }
        commands = config.getStringList("Commands");
    }

    public void giveToPlayer(Player player) {
        for (KitItem item : getItems()) item.giveToPlayer(player);
        for (String cmd : commands) {
            cmd = cmd.replace("{player}", player.getName());
            plugin.getLogger().info("Issuing command: " + cmd);
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                                               cmd);
        }
        sendMessage(player);
    }
}
