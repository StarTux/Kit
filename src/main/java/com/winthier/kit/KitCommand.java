package com.winthier.kit;

import com.cavetale.mytems.MytemsPlugin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
public final class KitCommand implements CommandExecutor {
    final KitPlugin plugin;

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player == null) {
            sender.sendMessage("Player expected");
            return true;
        }
        if (args.length != 0) return false;
        showKitList(player);
        return true;
    }

    void openKit(Player player, Kit kit) {
        if (!kit.playerCanClaim(player)) {
            player.sendMessage(ChatColor.RED + "Kit not found: " + kit.getName());
            return;
        }
        if (kit.playerIsOnCooldown(player)) {
            if (kit.hasInfiniteCooldown()) {
                player.sendMessage(ChatColor.RED + "You already claimed this kit.");
            } else {
                long secs = kit.getRemainingCooldown(player);
                player.sendMessage(ChatColor.RED + "You are on cooldown: "
                                   + ChatColor.GRAY + formatSeconds(secs));
            }
            return;
        }
        kit.setPlayerOnCooldown(player);
        plugin.getLogger().info("Giving kit " + kit.name + " to " + player.getName());
        kit.giveToPlayer(player);
        plugin.updateSidebarList();
    }

    private static int slotDist(int slot, int cx, int cy) {
        int x = slot % 9;
        int y = slot / 9;
        return Math.abs(x - cx) + Math.abs(y - cy);
    }

    void showKitList(Player player) {
        List<Kit> kits = plugin.kits.values().stream()
            .filter(kit -> kit.playerCanSee(player))
            .filter(kit -> !(kit.playerIsOnCooldown(player) && kit.hasInfiniteCooldown()))
            .collect(Collectors.toList());
        if (kits.isEmpty()) {
            player.sendMessage(ChatColor.RED + "There are no kits available for you.");
            return;
        }
        int rows = Math.max(3, Math.min(6, (kits.size() - 1) / 9 + 1));
        Gui gui = new Gui(plugin);
        gui.title("Kits");
        gui.size(rows * 9);
        List<Integer> slots = new ArrayList<>(rows * 9);
        for (int i = 0; i < rows * 9; i += 1) slots.add(i);
        Collections.sort(slots, (a, b) -> Integer.compare(slotDist(a, 4, rows / 2), slotDist(b, 4, rows / 2)));
        if (fillGui(gui, kits, slots, player, false)) {
            new BukkitRunnable() {
                @Override public void run() {
                    if (!gui.isOpened() || gui.isClosed() || !player.isValid()) {
                        cancel();
                        return;
                    }
                    if (!fillGui(gui, kits, slots, player, true)) {
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 20L, 20L);
        }
        gui.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.MASTER, 0.5f, 1.0f);
    }

    /**
     * @return true if further updates are required, false otherwise
     */
    private boolean fillGui(Gui gui, List<Kit> kits, List<Integer> slots, Player player, boolean update) {
        boolean result = false;
        for (int i = 0; i < kits.size(); i += 1) {
            if (i >= slots.size()) break;
            int slot = slots.get(i);
            Kit kit = kits.get(i);
            if (update && kit.hasInfiniteCooldown()) continue;
            final ItemStack icon;
            if (kit.getItems().isEmpty()) {
                icon = new ItemStack(Material.LIME_SHULKER_BOX);
            } else {
                ItemStack original = kit.getItems().get(0).createItemStack();
                ItemStack fixed = MytemsPlugin.getInstance().fixItemStack(original);
                icon = fixed != null ? fixed : original;
            }
            ItemMeta meta = icon.getItemMeta();
            meta.addItemFlags(ItemFlag.values());
            meta.setDisplayNameComponent(new ComponentBuilder(kit.getName()).italic(false).color(ChatColor.GREEN).create());
            List<BaseComponent[]> lore = new ArrayList<>();
            if (kit.playerIsOnCooldown(player)) {
                if (kit.hasInfiniteCooldown()) {
                    lore.add(new ComponentBuilder("You already claimed this kit.").color(ChatColor.RED).create());
                } else {
                    long secs = kit.getRemainingCooldown(player);
                    lore.add(new ComponentBuilder("Cooldown: ").color(ChatColor.RED).italic(false)
                             .append(formatSeconds(secs)).color(ChatColor.GRAY).create());
                    result = true;
                }
            } else {
                lore.add(new ComponentBuilder("Kit").italic(true).color(ChatColor.GRAY).create());
            }
            for (String line : kit.getDescription()) {
                lore.add(new ComponentBuilder(fmt(line)).italic(false).color(ChatColor.WHITE).create());
            }
            meta.setLoreComponents(lore);
            icon.setItemMeta(meta);
            gui.setItem(slot, icon, click -> {
                    if (!click.isLeftClick()) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 0.5f);
                        return;
                    }
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
                    openKit(player, kit);
                });
        }
        return result;
    }

    static String formatSeconds(long seconds) {
        long minutes = seconds / 60;
        if (minutes <= 60) {
            return String.format("%02d:%02d", minutes, seconds % 60);
        }
        long hours = minutes / 60;
        if (hours <= 24) {
            return String.format("%dh %02d:%02d", hours, minutes % 60, seconds % 60);
        }
        long days = hours / 24;
        return String.format("%dd %dh %02d:%02d", days, hours % 24, minutes % 60, seconds % 60);
    }

    static String fmt(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }
}
