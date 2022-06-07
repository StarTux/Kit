package com.winthier.kit;

import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.sidebar.Priority;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final KitPlugin plugin;

    protected void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerSidebar(PlayerSidebarEvent event) {
        if (!plugin.sidebarList.contains(event.getPlayer().getUniqueId())) return;
        if (!event.getPlayer().hasPermission("kit.kit")) return;
        event.add(plugin, Priority.HIGH, join(noSeparators(), text("You have a ", AQUA), text("/kit", YELLOW)));
    }
}
