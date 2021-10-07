package com.winthier.kit;

import com.cavetale.core.font.DefaultFont;
import com.cavetale.mytems.MytemsPlugin;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
public final class KitCommand implements CommandExecutor {
    final KitPlugin plugin;
    protected final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd yyyy");

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
            player.sendMessage(Component.text("Kit not found: " + kit.getName(), NamedTextColor.RED));
            return;
        }
        if (kit.playerIsOnCooldown(player)) {
            if (kit.hasInfiniteCooldown()) {
                player.sendMessage(Component.text("You already claimed this kit.", NamedTextColor.RED));
            } else {
                long secs = kit.getRemainingCooldown(player);
                player.sendMessage(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                            Component.text("You are on cooldown: ", NamedTextColor.RED),
                            Component.text(formatSeconds(secs), NamedTextColor.GRAY),
                        }));
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
            player.sendMessage(Component.text("There are no kits available for you.", NamedTextColor.RED));
            return;
        }
        int rows = Math.max(3, Math.min(6, (kits.size() - 1) / 9 + 1));
        Gui gui = new Gui(plugin);
        gui.size(rows * 9);
        gui.title(DefaultFont.guiBlankOverlay(rows * 9, NamedTextColor.GREEN, Component.text("Kits")));
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
            List<Component> lore = new ArrayList<>();
            if (kit.playerIsOnCooldown(player)) {
                if (kit.hasInfiniteCooldown()) {
                    lore.add(Component.text("You already claimed this kit.", NamedTextColor.RED));
                } else {
                    long secs = kit.getRemainingCooldown(player);
                    lore.add(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                                Component.text("Cooldown: ", NamedTextColor.RED),
                                Component.text(formatSeconds(secs), NamedTextColor.GRAY),
                            }));
                    result = true;
                }
            } else {
                lore.add(Component.text("Kit", NamedTextColor.GRAY));
            }
            if (kit.date != 0) {
                Date date = new Date(kit.date);
                String dateText = dateFormat.format(date);
                lore.add(Component.text(dateText, NamedTextColor.YELLOW));
            }
            for (String line : kit.getDescription()) {
                lore.add(Component.text(fmt(line), NamedTextColor.WHITE));
            }
            icon.editMeta(meta -> {
                    meta.addItemFlags(ItemFlag.values());
                    meta.displayName(kit.parseDisplayName());
                    meta.lore(lore);
                });
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
