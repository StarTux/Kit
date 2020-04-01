package com.winthier.kit;

import lombok.RequiredArgsConstructor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    final KitPlugin plugin;

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof KitHolder)) return;
        KitHolder holder = (KitHolder) event.getInventory().getHolder();
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        for (ItemStack item : holder.inventory.getContents()) {
            if (item == null || item.getAmount() == 0) continue;
            for (ItemStack drop : player.getInventory().addItem(item).values()) {
                player.getWorld()
                    .dropItem(player.getEyeLocation(), drop)
                    .setPickupDelay(0);
            }
        }
        plugin.command.showKitList(player);
        for (String msg : holder.kit.messages) {
            player.sendMessage(KitCommand.fmt(msg));
        }
        for (String cmd : holder.kit.commands) {
            cmd = cmd.replace("{player}", player.getName());
            plugin.getLogger().info("Issuing command: " + cmd);
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                                               cmd);
        }
        player.playSound(player.getEyeLocation(),
                         Sound.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }
}
