package com.winthier.kit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class KitPlugin extends JavaPlugin {
    final Cooldowns cooldowns = new Cooldowns(this);
    final List<Kit> kits = new ArrayList<>();
    final EventListener listener = new EventListener(this);
    final KitCommand command = new KitCommand(this);

    @Override
    public void onEnable() {
        saveResource("kits.yml", false);
        getCommand("kit").setExecutor(command);
        getCommand("kitadmin").setExecutor(new AdminCommand(this));
        getServer().getPluginManager().registerEvents(listener, this);
        reload();
    }

    void reload() {
        cooldowns.load();
        loadKits();
    }

    void loadKits() {
        kits.clear();
        File file = new File(getDataFolder(), "kits.yml");
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        for (String key : config.getKeys(false)) {
            getLogger().info("Loading kit " + key);
            ConfigurationSection section = config.getConfigurationSection(key);
            Kit kit = new Kit(this, key);
            kit.load(section);
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
