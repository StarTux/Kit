package com.winthier.kit;

import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.sidebar.Priority;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final KitPlugin plugin;

    protected void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerSidebar(PlayerSidebarEvent event) {
        if (!plugin.sidebarList.contains(event.getPlayer().getUniqueId())) return;
        event.add(plugin, Priority.HIGH,
                  Component.text("You have a ", NamedTextColor.AQUA)
                  .append(Component.text("/kit", NamedTextColor.YELLOW)));
    }
}
