package com.winthier.kit;

import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.sidebar.Priority;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
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
        PluginPlayerEvent.Name.KIT_OPEN.ultimate(plugin, player)
            .detail(Detail.NAME, holder.kit.getName())
            .call();
    }

    @EventHandler
    public void onPlayerSidebar(PlayerSidebarEvent event) {
        if (!plugin.sidebarList.contains(event.getPlayer().getUniqueId())) return;
        event.add(plugin, Priority.HIGH, TextComponent.ofChildren(new Component[] {
                    Component.text("You have a ", NamedTextColor.AQUA),
                    Component.text("/kit", NamedTextColor.YELLOW),
                }));
    }
}
