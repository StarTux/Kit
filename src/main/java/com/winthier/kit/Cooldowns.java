package com.winthier.kit;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

@RequiredArgsConstructor
public final class Cooldowns {
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

    void setCooldown(UUID uuid, String kit, int seconds) {
        if (seconds == 0) return;
        long cooldown;
        if (seconds < 0) {
            cooldown = Long.MAX_VALUE;
        } else {
            cooldown = Instant.now().getEpochSecond() + (long) seconds;
        }
        ConfigurationSection kitSection = config.getConfigurationSection(kit);
        if (kitSection == null) kitSection = config.createSection(kit);
        kitSection.set(uuid.toString(), cooldown);
    }
}
