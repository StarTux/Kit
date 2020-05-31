package com.winthier.kit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
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
