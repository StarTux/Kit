package com.winthier.kit;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
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
    private static final Component NOTIFICATION = join(noSeparators(), text("You have a ", AQUA), text("/kit", YELLOW));

    protected void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerHud(PlayerHudEvent event) {
        if (!plugin.sidebarList.contains(event.getPlayer().getUniqueId())) return;
        if (!event.getPlayer().hasPermission("kit.kit")) return;
        event.sidebar(PlayerHudPriority.HIGH, List.of(NOTIFICATION));
        event.bossbar(PlayerHudPriority.LOW, NOTIFICATION, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, 1.0f);
    }
}
