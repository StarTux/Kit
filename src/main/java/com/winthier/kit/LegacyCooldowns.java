package com.winthier.kit;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

@RequiredArgsConstructor
public final class LegacyCooldowns {
    final KitPlugin plugin;
    private YamlConfiguration config = null;
    static final String FILENAME = "cooldowns.yml";

    File saveFile() {
        return new File(plugin.getDataFolder(), FILENAME);
    }

    void load() {
        config = YamlConfiguration.loadConfiguration(saveFile());
        // clean up
        long currentTime = Instant.now().getEpochSecond();
        for (String kitKey : config.getKeys(false)) {
            ConfigurationSection kitSection = config.getConfigurationSection(kitKey);
            for (String userKey : kitSection.getKeys(false)) {
                long time = kitSection.getLong(userKey);
                if (time < currentTime) kitSection.set(userKey, null);
            }
        }
    }

    void save() {
        try {
            config.save(saveFile());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    long getCooldown(UUID uuid, String kit) {
        return config.getLong(kit + "." + uuid.toString());
    }

    long setCooldown(UUID uuid, String kit, int seconds) {
        if (seconds == 0) return 0;
        long cooldown;
        if (seconds < 0) {
            cooldown = Long.MAX_VALUE;
        } else {
            cooldown = Instant.now().getEpochSecond() + (long) seconds;
        }
        ConfigurationSection kitSection = config.getConfigurationSection(kit);
        if (kitSection == null) kitSection = config.createSection(kit);
        kitSection.set(uuid.toString(), cooldown);
        return cooldown;
    }

    boolean resetCooldown(UUID uuid, String kit) {
        ConfigurationSection kitSection = config.getConfigurationSection(kit);
        if (kitSection == null) return false;
        String u = uuid.toString();
        if (!kitSection.isSet(u)) return false;
        kitSection.set(u, null);
        return true;
    }

    List<String> listCooldowns() {
        return new ArrayList<>(config.getKeys(false));
    }

    List<UUID> listCooldowns(String kitName) {
        List<UUID> list = new ArrayList<>();
        ConfigurationSection kitSection = config
            .getConfigurationSection(kitName);
        if (kitSection == null) return list;
        for (String key : kitSection.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException iae) {
                plugin.getLogger().warning("Found invalid UUID: "
                                           + kitName + "." + key);
                kitSection.set(key, null);
                continue;
            }
            list.add(uuid);
        }
        return list;
    }
}
