package com.winthier.kit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * (De)serializable with Json.
 */
public final class Kit {
    transient KitPlugin plugin;
    @Getter transient String name = "";
    @Getter List<String> messages = new ArrayList<>();
    @Getter String permission = "";
    @Getter List<KitItem> items = new ArrayList<>();
    @Getter long cooldown = -1;
    @Getter boolean hidden = false;
    List<String> commands = new ArrayList<>();
    Map<UUID, String> members = new HashMap<>();
    List<String> description = new ArrayList<>();
    transient Users users = new Users();

    public static final class Users {
        Map<UUID, Long> cooldowns = new HashMap<>();
    }

    public Kit() { }

    Kit(final KitPlugin plugin, final String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public long getPlayerCooldown(UUID uuid) {
        Long cd = users.cooldowns.get(uuid);
        return cd != null ? cd : 0;
    }

    public boolean playerIsOnCooldown(UUID uuid) {
        long cd = getPlayerCooldown(uuid);
        if (cd == 0) return false;
        return cd > Instant.now().getEpochSecond();
    }

    public boolean playerIsOnCooldown(Player player) {
        return playerIsOnCooldown(player.getUniqueId());
    }

    public long getRemainingCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        long cd = getPlayerCooldown(uuid);
        return Math.max(0, cd - Instant.now().getEpochSecond());
    }

    public boolean playerHasPermission(Player player) {
        if (permission == null || permission.isEmpty()) return true;
        if (player.isOp()) return true;
        return player.hasPermission(permission);
    }

    public boolean playerIsMember(Player player) {
        if (members == null || members.isEmpty()) return true;
        return members.containsKey(player.getUniqueId());
    }

    public boolean resetPlayerCooldown(UUID uuid) {
        Long oldValue = users.cooldowns.remove(uuid);
        if (oldValue == null) return false;
        plugin.saveUsers(this);
        return true;
    }

    public long setPlayerOnCooldown(UUID uuid) {
        return setPlayerOnCooldown(uuid, cooldown);
    }

    public long setPlayerOnCooldown(UUID uuid, long seconds) {
        long result;
        if (seconds >= 0) {
            result = Instant.now().getEpochSecond() + seconds;
            users.cooldowns.put(uuid, result);
        } else {
            result = Long.MAX_VALUE;
            users.cooldowns.put(uuid, result);
        }
        plugin.saveUsers(this);
        return result;
    }

    public void setPlayerOnCooldown(Player player) {
        setPlayerOnCooldown(player.getUniqueId());
    }

    public boolean playerCanSee(Player player) {
        return !hidden && playerIsMember(player) && playerHasPermission(player);
    }

    public boolean playerCanClaim(Player player) {
        return playerIsMember(player) && playerHasPermission(player);
    }

    public boolean hasInfiniteCooldown() {
        return cooldown < 0;
    }

    /**
     * Give kit to player.  This will not put them on cooldown.
     * Continued in EventListener.
     */
    public void giveToPlayer(Player player) {
        int count = items.size();
        int size = ((count - 1) / 9 + 1) * 9;
        KitHolder holder = new KitHolder();
        holder.inventory = plugin.getServer().createInventory(holder, size, name);
        holder.onClose = () -> {
            for (ItemStack item : holder.inventory.getContents()) {
                if (item == null || item.getAmount() == 0) continue;
                for (ItemStack drop : player.getInventory().addItem(item).values()) {
                    player.getWorld()
                        .dropItem(player.getEyeLocation(), drop)
                        .setPickupDelay(0);
                }
            }
            plugin.command.showKitList(player);
            for (String msg : messages) {
                player.sendMessage(KitCommand.fmt(msg));
            }
            for (String cmd : commands) {
                cmd = cmd.replace("{player}", player.getName());
                plugin.getLogger().info("Issuing command: " + cmd);
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                                                   cmd);
            }
            player.playSound(player.getEyeLocation(),
                             Sound.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        };
        int offset = count < 9
            ? (9 - count) / 2
            : 0;
        int i = 0;
        for (KitItem item : items) {
            int index = offset + i++;
            holder.inventory.setItem(index, item.createItemStack());
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
