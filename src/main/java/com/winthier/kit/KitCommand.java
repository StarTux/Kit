package com.winthier.kit;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.connect.ServerCategory;
import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.fam.Fam;
import com.cavetale.mytems.util.Text;
import com.winthier.kit.sql.SQLMember;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import static com.cavetale.core.event.item.PlayerReceiveItemsEvent.receiveInventory;
import static com.cavetale.mytems.util.Items.tooltip;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class KitCommand extends AbstractCommand<KitPlugin> {
    protected final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd yyyy");

    protected KitCommand(final KitPlugin plugin) {
        super(plugin, "kit");
    }

    @Override
    protected void onEnable() {
        rootNode.denyTabCompletion()
            .description("Open your kits")
            .playerCaller(this::kit);
    }

    private void kit(Player player) {
        if (!ServerCategory.current().isSurvival()) {
            throw new CommandWarn("Kits are only available in survival");
        }
        plugin.findAvailableKits(player.getUniqueId(), kits -> showKitList(player, kits));
    }

    private void showKitList(Player player, List<Kit> kits) {
        if (kits.isEmpty()) {
            player.sendMessage(text("There are no kits available for you.", RED));
            return;
        }
        int rows = Math.max(3, Math.min(6, (kits.size() - 1) / 9 + 1));
        Gui gui = new Gui(plugin);
        gui.size(rows * 9);
        gui.title(GuiOverlay.BLANK.builder(rows * 9, GREEN)
                  .layer(GuiOverlay.TITLE_BAR, DARK_GREEN)
                  .title(text("Kits", GREEN))
                  .build());
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
            if (update && !kit.hasCooldown()) continue;
            ItemStack firstItem = kit.findFirstItem();
            final ItemStack icon = firstItem != null
                ? firstItem.clone()
                : new ItemStack(Material.LIME_SHULKER_BOX);
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(kit.displayName());
            if (kit.cooldown() != null) {
                Duration time = Duration.between(Instant.now(), kit.cooldown().toInstant());
                if (!time.isNegative()) {
                    tooltip.add(join(noSeparators(), text("Unlocks in ", RED), formatSeconds(time).color(GRAY)));
                    result = true;
                }
            } else {
                tooltip.add(text("Kit", GRAY));
            }
            if (kit.row().getCreatedTime().getTime() != 0L) {
                String dateText = dateFormat.format(kit.row().getCreatedTime());
                tooltip.add(text(dateText, YELLOW));
            }
            if (kit.row().getDescription() != null) {
                tooltip.addAll(Text.wrapLore(kit.row().getDescription(), c -> c.color(WHITE)));
            }
            icon.editMeta(meta -> {
                    tooltip(meta, tooltip);
                    meta.addItemFlags(ItemFlag.values());
                });
            gui.setItem(slot, icon, click -> {
                    clickKit(player, kit, click);
                });
        }
        return result;
    }

    private void clickKit(Player player, Kit kit, InventoryClickEvent click) {
        if (!click.isLeftClick()) return;
        plugin.lockKitAsync(kit.row(), player.getUniqueId(), locked -> {
                if (locked) {
                    plugin.updateSidebarList();
                    openKit(player, kit);
                }
            });
    }

    private void openKit(Player player, Kit kit) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
        final int size = kit.inventory().getSize();
        Gui gui = new Gui(plugin)
            .size(size)
            .title(GuiOverlay.HOLES.builder(size, WHITE)
                   .layer(GuiOverlay.TITLE_BAR, DARK_GREEN)
                   .title(kit.displayName())
                   .build());
        for (int i = 0; i < size; i += 1) {
            gui.getInventory().setItem(i, kit.inventory().getItem(i));
        }
        gui.onClose(evt -> {
                receiveInventory(player, gui.getInventory());
                if (kit.tag().getCommands() != null && !kit.tag().getCommands().isEmpty()) {
                    for (String command : kit.tag().getCommands()) {
                        String cmd = command
                            .replace("{player}", player.getName())
                            .replace("{uuid}", player.getUniqueId().toString());
                        plugin.getLogger().info("Issuing command: " + cmd);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    }
                }
                if (kit.tag().getTitles() != null && !kit.tag().getTitles().isEmpty()) {
                    String cmd = "titles unlockset " + player.getName() + " " + String.join(" ", kit.tag().getTitles());
                    plugin.getLogger().info("Issuing command: " + cmd);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
                if (kit.row().getDescription() != null) {
                    player.sendMessage(text(kit.row().getDescription(), GREEN));
                }
                if (kit.row().getFriendship() > 0) {
                    plugin.database.find(SQLMember.class)
                        .eq("kitId", kit.row().getId())
                        .findValuesAsync("member", UUID.class, list -> {
                                Set<UUID> uuids = new HashSet<>(list);
                                uuids.remove(player.getUniqueId());
                                if (uuids.isEmpty()) return;
                                Fam.increaseSingleFriendship(kit.row().getFriendship(), player.getUniqueId(), uuids);
                                List<String> names = new ArrayList<>();
                                for (UUID uuid : uuids) {
                                    names.add(PlayerCache.nameForUuid(uuid));
                                }
                                names.sort(String.CASE_INSENSITIVE_ORDER);
                                List<Component> tooltip = new ArrayList<>();
                                tooltip.add(text("/friends", GREEN));
                                tooltip.add(text(String.join(" ", names), GRAY)); // Chat tooltips are wrapped automatically
                                player.sendMessage(text("Your friendship with " + names.size() + " player" + (names.size() == 1 ? "" : "s")
                                                        + " has grown!", GREEN)
                                                   .hoverEvent(showText(join(separator(newline()), tooltip)))
                                                   .clickEvent(runCommand("/friends")));
                            });
                }
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.5f, 2.0f);
                PluginPlayerEvent.Name.KIT_OPEN.make(plugin, player)
                    .detail(Detail.NAME, kit.row().getName())
                    .callEvent();
            });
        gui.setEditable(true);
        gui.onOpen(evt -> player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.MASTER, 1.0f, 1.0f));
        gui.open(player);
    }

    private static int slotDist(int slot, int cx, int cy) {
        int x = slot % 9;
        int y = slot / 9;
        return Math.abs(x - cx) + Math.abs(y - cy);
    }

    private static Component formatSeconds(Duration time) {
        final long seconds = time.toSeconds() % 60L;
        final long minutes = time.toMinutes() % 60L;
        final long hours = time.toHours() % 24L;
        final long days = time.toDays() % 24;
        List<Component> list = new ArrayList<>(11);
        if (days > 0) {
            list.add(text(days));
            list.add(text("d", DARK_GRAY));
            list.add(space());
        }
        list.add(text(hours));
        list.add(text("h", DARK_GRAY));
        list.add(space());
        list.add(text(minutes));
        list.add(text("m", DARK_GRAY));
        list.add(space());
        list.add(text(seconds));
        list.add(text("s", DARK_GRAY));
        return join(noSeparators(), list);
    }
}
