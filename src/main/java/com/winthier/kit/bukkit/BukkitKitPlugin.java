package com.winthier.kit.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitKitPlugin extends JavaPlugin
{
    private BukkitCooldowns cooldowns = null;
    private List<BukkitKit> kits = null;
    
    @Override
    public void onEnable()
    {
        saveDefaultConfig();
        reloadConfig();
        getCommand("kit").setExecutor(new BukkitKitCommand(this));
    }

    @Override
    public void onDisable()
    {
        if (cooldowns != null) cooldowns.save();
    }

    void reload()
    {
        cooldowns = null;
        kits = null;
        reloadConfig();
    }

    BukkitCooldowns getCooldowns()
    {
        if (cooldowns == null) {
            cooldowns = new BukkitCooldowns(this);
        }
        return cooldowns;
    }

    List<BukkitKit> getAllKits()
    {
        if (kits == null) {
            kits = new ArrayList<>();
            ConfigurationSection kitsSection = getConfig().getConfigurationSection("kits");
            for (String key : kitsSection.getKeys(false)) {
                getLogger().info("Loading kit " + key);
                ConfigurationSection kitSection = kitsSection.getConfigurationSection(key);
                BukkitKit kit = new BukkitKit(this, key);
                kit.readConfig(kitSection);
                kits.add(kit);
            }
        }
        return kits;
    }

    BukkitKit getKitNamed(String name)
    {
        for (BukkitKit kit : getAllKits()) {
            if (kit.getName().equalsIgnoreCase(name)) return kit;
        }
        return null;
    }
}
