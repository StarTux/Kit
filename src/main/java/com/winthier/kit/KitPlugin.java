package com.winthier.kit;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public final class KitPlugin extends JavaPlugin {
    final Cooldowns cooldowns = new Cooldowns(this);
    final List<Kit> kits = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reload();
        getCommand("kit").setExecutor(new KitCommand(this));
    }

    @Override
    public void onDisable() {
        if (cooldowns != null) cooldowns.save();
    }

    void reload() {
        reloadConfig();
        cooldowns.load();
        loadKits();
    }

    void loadKits() {
        kits.clear();
        ConfigurationSection kitsSection = getConfig().getConfigurationSection("kits");
        for (String key : kitsSection.getKeys(false)) {
            getLogger().info("Loading kit " + key);
            ConfigurationSection kitSection = kitsSection.getConfigurationSection(key);
            Kit kit = new Kit(this, key);
            kit.load(kitSection);
            kits.add(kit);
        }
    }

    Kit getKitNamed(String name) {
        for (Kit kit : kits) {
            if (name.equalsIgnoreCase(kit.name)) return kit;
        }
        return null;
    }
}
