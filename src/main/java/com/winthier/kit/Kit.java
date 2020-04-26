package com.winthier.kit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

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
    Map<UUID, String> members;
    List<String> description;

    public boolean playerIsOnCooldown(UUID player) {
        long cd = plugin.cooldowns.getCooldown(player, name);
        if (cd == 0) return false;
        return cd > Instant.now().getEpochSecond();
    }

    public boolean playerIsOnCooldown(Player player) {
        return playerIsOnCooldown(player.getUniqueId());
    }

    public long getRemainingCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        long cd = plugin.cooldowns.getCooldown(uuid, name);
        return Math.max(0, cd - Instant.now().getEpochSecond());
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

    public boolean playerCanSee(Player player) {
        if (!playerHasPermission(player)) return false;
        if (hidden) return false;
        if (members != null && !members.containsKey(player.getUniqueId())) return false;
        return true;
    }

    public boolean playerCanClaim(Player player) {
        if (!playerHasPermission(player)) return false;
        if (members != null && !members.containsKey(player.getUniqueId())) return false;
        return true;
    }

    public boolean hasInfiniteCooldown() {
        return cooldown < 0;
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
        if (config.isConfigurationSection("Members")) {
            members = new HashMap<>();
            ConfigurationSection section = config.getConfigurationSection("Members");
            for (String key : section.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(key);
                } catch (IllegalArgumentException iae) {
                    plugin.getLogger().warning("Members: Invalid UUID: " + key);
                    continue;
                }
                members.put(uuid, section.getString(key));
            }
        }
        description = config.getStringList("Description");
    }

    /**
     * Give kit to player.  This will not put them on cooldown.
     * Continued in EventListener.
     */
    public void giveToPlayer(Player player) {
        int size = ((items.size() - 1) / 9 + 1) * 9;
        KitHolder holder = new KitHolder();
        holder.kit = this;
        holder.inventory = plugin.getServer().createInventory(holder, size, name);
        int[] slots = new int[size];
        slots[0] = size / 2;
        for (int i = 0; i < size / 2; i += 1) {
            slots[i + i + 1] = slots[0] - i;
            slots[i + i + 2] = slots[0] + i;
        }
        int i = 0;
        for (KitItem item : items) {
            holder.inventory.setItem(slots[i++], item.createItemStack());
        }
        player.openInventory(holder.inventory);
        player.playSound(player.getEyeLocation(),
                         Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isValid()) return;
                player.playSound(player.getEyeLocation(),
                                 Sound.ENTITY_PLAYER_LEVELUP,
                                 SoundCategory.PLAYERS, 0.25f, 2.0f);
            }, 20L);
    }
}
