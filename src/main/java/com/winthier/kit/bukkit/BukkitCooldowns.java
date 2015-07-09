package com.winthier.kit.bukkit;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class BukkitCooldowns
{
    final BukkitKitPlugin plugin;
    private YamlConfiguration config = null;
    final static String FILENAME = "cooldowns.yml";

    File saveFile()
    {
        return new File(plugin.getDataFolder(), FILENAME);
    }

    BukkitCooldowns(BukkitKitPlugin plugin)
    {
        this.plugin = plugin;
        config = YamlConfiguration.loadConfiguration(saveFile());
        // clean up
        long currentTime = System.currentTimeMillis();
        for (String kitKey : config.getKeys(false)) {
            ConfigurationSection kitSection = config.getConfigurationSection(kitKey);
            for (String userKey : kitSection.getKeys(false)) {
                long time = kitSection.getLong(userKey);
                if (time < currentTime) kitSection.set(userKey, null);
            }
        }
    }

    void save()
    {
        try {
            config.save(saveFile());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    Long getCooldown(UUID uuid, String kit)
    {
        return config.getLong(kit + "." + uuid.toString());
    }

    void setCooldownInSeconds(UUID uuid, String kit, int seconds)
    {
        long cooldown = System.currentTimeMillis() + (long)seconds * 1000L;
        ConfigurationSection kitSection = config.getConfigurationSection(kit);
        if (kitSection == null) kitSection = config.createSection(kit);
        kitSection.set(uuid.toString(), cooldown);
    }
}
