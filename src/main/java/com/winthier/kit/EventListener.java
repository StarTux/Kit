package com.winthier.kit;

import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.sidebar.Priority;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    final KitPlugin plugin;

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof KitHolder)) return;
        KitHolder holder = (KitHolder) event.getInventory().getHolder();
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Runnable onClose = holder.onClose;
        if (onClose == null) return;
        holder.onClose = null;
        onClose.run();
    }

    @EventHandler
    public void onPlayerSidebar(PlayerSidebarEvent event) {
        if (!plugin.sidebarList.contains(event.getPlayer().getUniqueId())) return;
        event.addLines(plugin, Priority.HIGH, ChatColor.AQUA + "You have a " + ChatColor.YELLOW + "/kit");
    }
}
