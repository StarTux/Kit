package com.winthier.kit;

import com.cavetale.fam.Fam;
import com.cavetale.mytems.Mytems;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

/**
 * (De)serializable with Json.
 */
@Getter
public final class Kit {
    protected transient KitPlugin plugin;
    protected transient String name = "";
    protected String displayName; // Gson-serialized component
    protected List<String> messages = new ArrayList<>();
    protected String permission = "";
    protected List<KitItem> items = new ArrayList<>();
    protected long cooldown = -1;
    protected boolean hidden = false;
    protected List<String> commands = new ArrayList<>();
    protected List<String> titles = List.of();
    protected Map<UUID, String> members = new HashMap<>();
    protected List<String> description = new ArrayList<>();
    protected int friendship = 0;
    protected long date = 0L;
    protected transient Users users = new Users();
    protected transient Component displayNameComponent; // cache

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
        KitHolder holder = new KitHolder(this);
        holder.inventory = plugin.getServer().createInventory(holder, size, parseDisplayName());
        holder.onClose = () -> {
            for (ItemStack item : holder.inventory.getContents()) {
                if (item == null || item.getAmount() == 0) continue;
                for (ItemStack drop : player.getInventory().addItem(item).values()) {
                    player.getWorld()
                        .dropItem(player.getEyeLocation(), drop)
                        .setPickupDelay(0);
                }
            }
            for (String msg : messages) {
                player.sendMessage(KitCommand.fmt(msg));
            }
            for (String cmd : commands) {
                cmd = cmd.replace("{player}", player.getName());
                plugin.getLogger().info("Issuing command: " + cmd);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
            if (!titles.isEmpty()) {
                String cmd = "titles unlockset " + player.getName() + " " + String.join(" ", titles);
                plugin.getLogger().info("Issuing command: " + cmd);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
            if (friendship > 0 && members.size() > 1) {
                Set<UUID> set = new HashSet<>(members.keySet());
                set.remove(player.getUniqueId());
                Fam.increaseSingleFriendship(friendship, player.getUniqueId(), set);
                player.sendMessage(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                            Component.text("Your friendship with "),
                            Component.join(JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)),
                                           set.stream()
                                           .map(members::get)
                                           .map(theName -> Component.text(theName, NamedTextColor.WHITE))
                                           .collect(Collectors.toList())),
                            Component.text(" has grown!"),
                        }).color(NamedTextColor.GREEN));
            }
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        };
        int offset = count < 9
            ? (9 - count) / 2
            : 0;
        int i = 0;
        for (KitItem item : items) {
            int index = offset + i++;
            ItemStack itemStack = item.createItemStack();
            Mytems mytems = Mytems.forItem(itemStack);
            if (mytems != null) {
                String serialized = mytems.serializeItem(itemStack);
                itemStack = Mytems.deserializeItem(serialized, player);
            }
            holder.inventory.setItem(index, itemStack);
        }
        player.openInventory(holder.inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isValid()) return;
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 2.0f);
            }, 20L);
    }

    public List<KitItem> getAllItems() {
        List<KitItem> allItems = new ArrayList<>();
        for (KitItem it : items) {
            allItems.add(it);
            if (it.createItemStack().getItemMeta() instanceof BlockStateMeta) {
                BlockStateMeta blockStateMeta = (BlockStateMeta) it.createItemStack().getItemMeta();
                if (blockStateMeta.getBlockState() instanceof Container) {
                    Container container = (Container) blockStateMeta.getBlockState();
                    for (ItemStack itemStack : container.getInventory()) {
                        if (itemStack == null || itemStack.getType() == Material.AIR) continue;
                        allItems.add(new KitItem(itemStack));
                    }
                }
            }
        }
        return allItems;
    }

    public List<String> getAllItemStrings() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (KitItem kitItem : getAllItems()) {
            map.compute(kitItem.toSingleString(), (k, i) -> (i != null ? i : 0) + kitItem.amount);
        }
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String itemName = entry.getKey();
            int amount = entry.getValue();
            result.add(amount == 1 ? itemName : amount + "x" + itemName);
        }
        return result;
    }

    public Kit clone() {
        return Json.deserialize(Json.serialize(this), Kit.class);
    }

    public void setDisplayName(Component component) {
        this.displayName = GsonComponentSerializer.gson().serialize(component);
        this.displayNameComponent = component;
    }

    public Component parseDisplayName() {
        if (displayNameComponent == null) {
            displayNameComponent = displayName != null
                ? GsonComponentSerializer.gson().deserialize(displayName)
                : Component.text(name, NamedTextColor.GREEN);
        }
        return displayNameComponent;
    }
}
