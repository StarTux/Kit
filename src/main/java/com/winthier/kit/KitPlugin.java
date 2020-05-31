package com.winthier.kit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class KitPlugin extends JavaPlugin {
    final Map<String, Kit> kits = new HashMap<>();
    final EventListener listener = new EventListener(this);
    final KitCommand command = new KitCommand(this);
    private File kitsFolder;
    private File usersFolder;

    @Override
    public void onEnable() {
        kitsFolder = new File(getDataFolder(), "kits");
        kitsFolder.mkdirs();
        usersFolder = new File(getDataFolder(), "users");
        usersFolder.mkdirs();
        getCommand("kit").setExecutor(command);
        getCommand("kitadmin").setExecutor(new AdminCommand(this));
        getServer().getPluginManager().registerEvents(listener, this);
        reload();
    }

    public void reload() {
        kits.clear();
        loadKitsFolder();
    }

    int loadLegacyKits() {
        int count = 0;
        final LegacyCooldowns cooldowns = new LegacyCooldowns(this);
        cooldowns.load();
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
            for (UUID uuid : cooldowns.listCooldowns(kit.name)) {
                kit.users.cooldowns.put(uuid, cooldowns.getCooldown(uuid, kit.name));
            }
            saveKit(kit);
            saveUsers(kit);
            kits.put(key, kit);
            count += 1;
        }
        return count;
    }

    private void loadKitsFolder() {
        int count = 0;
        for (File file : kitsFolder.listFiles()) {
            String name = file.getName();
            if (!name.endsWith(".json")) continue;
            name = name.substring(0, name.length() - 5);
            Kit kit;
            try {
                kit = Json.load(file, Kit.class);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "loadKitsFolder, Json.load(Kit)", e);
                getLogger().warning("" + file);
                continue;
            }
            if (kit == null) {
                getLogger().warning("Invalid kit: " + file);
                continue;
            }
            File usersFile = new File(usersFolder, name + ".json");
            try {
                kit.users = Json.load(usersFile, Kit.Users.class, Kit.Users::new);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "loadKitsFolder, Json.load(Kit.User)", e);
                getLogger().warning("" + usersFile);
                continue;
            }
            kit.plugin = this;
            kit.name = name;
            kits.put(name, kit);
            count += 1;
        }
        getLogger().info(count + " kit(s) loaded");
    }

    private void saveKit(Kit kit) {
        File file = new File(kitsFolder, kit.name + ".json");
        Json.save(file, kit, true);
    }

    void saveUsers(Kit kit) {
        File file = new File(usersFolder, kit.name + ".json");
        Json.save(file, kit.users, true);
    }

    public Kit getKitNamed(String name) {
        return kits.get(name);
    }
}
