package com.winthier.kit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class KitItem {
    final KitPlugin plugin;
    @Getter Material material = Material.AIR;
    @Getter int amount = 1;
    @Getter String tag = "";

    public void giveToPlayer(Player player) {
        String command = consoleCommand(player.getName());
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
    }

    void load(ConfigurationSection config) {
        String str = config.getString("Material", "");
        try {
            material = Material.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException iae) {
            plugin.getLogger().warning("Invalid material: " + str);
        }
        amount = config.getInt("Amount", amount);
        tag = config.getString("Tag", tag);
    }

    private String consoleCommand(String playerName) {
        return String.format("minecraft:give %s %s%s %d",
                             playerName,
                             material.name().toLowerCase(), tag,
                             amount);
    }
}
