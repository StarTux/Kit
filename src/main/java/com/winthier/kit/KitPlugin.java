package com.winthier.kit;

import com.cavetale.core.connect.ServerCategory;
import com.cavetale.core.perm.Perm;
import com.winthier.kit.sql.SQLClaimed;
import com.winthier.kit.sql.SQLCooldown;
import com.winthier.kit.sql.SQLKit;
import com.winthier.kit.sql.SQLMember;
import com.winthier.sql.SQLDatabase;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class KitPlugin extends JavaPlugin {
    protected final SQLDatabase database = new SQLDatabase(this);
    protected final EventListener eventListener = new EventListener(this);
    protected final KitCommand kitCommand = new KitCommand(this);
    protected final Set<UUID> sidebarList = new HashSet<>();
    protected final KitAdminCommand kitAdminCommand = new KitAdminCommand(this);

    @Override
    public void onEnable() {
        database.registerTables(Set.of(SQLKit.class,
                                       SQLMember.class,
                                       SQLCooldown.class,
                                       SQLClaimed.class));
        if (!database.createAllTables()) {
            throw new IllegalStateException("Database creation failed");
        }
        kitCommand.enable();
        kitAdminCommand.enable();
        Gui.enable(this);
        if (ServerCategory.current().isSurvival()) {
            eventListener.enable();
            getServer().getScheduler().runTaskTimer(this, this::updateSidebarList, 0L, 200L);
        }
    }

    @Override
    public void onDisable() {
        Gui.disable(this);
    }

    public List<Kit> findAvailableKits(UUID uuid) {
        final List<Integer> unclaimed = database.find(SQLMember.class)
            .select("kitId")
            .eq("enabled", true)
            .eq("claimed", false)
            .eq("member", uuid)
            .findValues(Integer.class);
        Map<Integer, Date> cooldowns = new HashMap<>();
        for (SQLCooldown row : database.find(SQLCooldown.class)
                 .select("kitId", "expiryTime")
                 .eq("member", uuid)
                 .findList()) {
            cooldowns.put(row.getKitId(), row.getExpiryTime());
        }
        List<Integer> claimed = database.find(SQLClaimed.class)
            .select("kitId")
            .eq("member", uuid)
            .findValues(Integer.class);
        List<Kit> result = new ArrayList<>();
        for (SQLKit row : database.find(SQLKit.class)
                 .eq("enabled", true)
                 .eq("type", KitType.MEMBER)
                 .in("id", unclaimed)
                 .findList()) {
            result.add(new Kit(row, null));
        }
        for (SQLKit row : database.find(SQLKit.class)
                 .eq("enabled", true)
                 .in("type", List.of(KitType.PERMISSION, KitType.PERMISSION_COOLDOWN))
                 .findList()) {
            if (claimed.contains(row.getId())) {
                continue;
            }
            if (row.getPermission() == null || !Perm.get().has(uuid, row.getPermission())) {
                continue;
            }
            result.add(new Kit(row, cooldowns.get(row.getId())));
        }
        result.sort((a, b) -> Long.compare(a.row().getCreatedTime().getTime(),
                                           b.row().getCreatedTime().getTime()));
        return result;
    }

    public void findAvailableKits(UUID uuid, Consumer<List<Kit>> callback) {
        database.scheduleAsyncTask(() -> {
                List<Kit> kits = findAvailableKits(uuid);
                Bukkit.getScheduler().runTask(this, () -> callback.accept(kits));
            });
    }

    public boolean lockKit(SQLKit row, UUID uuid) {
        return switch (row.getType()) {
        case MEMBER -> database.update(SQLMember.class)
            .where(c -> c
                   .eq("kitId", row.getId())
                   .eq("member", uuid)
                   .eq("enabled", true)
                   .eq("claimed", false))
            .set("claimed", true)
            .sync() != 0;
        case PERMISSION -> database.insertIgnore(new SQLClaimed(row, uuid)) != 0;
        case PERMISSION_COOLDOWN -> {
            Date now = new Date();
            Date expiry = Date.from(Instant.now().plusSeconds(row.getCooldown()));
            yield database.update(SQLCooldown.class)
                .where(c -> c
                       .eq("kitId", row.getId())
                       .eq("member", uuid)
                       .lt("expiryTime", now))
                .set("claimedTime", now)
                .set("expiryTime", expiry)
                .sync() != 0
                || database.insertIgnore(new SQLCooldown(row, uuid, expiry)) != 0;
        }
        };
    }

    public void lockKitAsync(SQLKit row, UUID uuid, Consumer<Boolean> callback) {
        database.scheduleAsyncTask(() -> {
                boolean result = lockKit(row, uuid);
                Bukkit.getScheduler().runTask(this, () -> callback.accept(result));
            });
    }

    protected void updateSidebarList() {
        Set<UUID> uuids = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            uuids.add(player.getUniqueId());
        }
        if (uuids.isEmpty()) {
            sidebarList.clear();
            return;
        }
        database.scheduleAsyncTask(() -> updateSidebarListNow(uuids));
    }

    protected void updateSidebarListNow(Set<UUID> uuids) {
        final Set<UUID> unclaimedMembers = Set.copyOf(database.find(SQLMember.class)
                                                      .select("member")
                                                      .eq("enabled", true)
                                                      .eq("claimed", false)
                                                      .in("member", uuids)
                                                      .findValues(UUID.class));
        Date now = new Date();
        Map<Integer, Set<UUID>> lockedKitMap = new TreeMap<>();
        for (SQLCooldown row : database.find(SQLCooldown.class)
                 .select("kitId", "member")
                 .gt("expiryTime", now)
                 .in("member", uuids)
                 .findList()) {
            lockedKitMap.computeIfAbsent(row.getKitId(), i -> new HashSet<>()).add(row.getMember());
        }
        for (SQLClaimed row : database.find(SQLClaimed.class)
                 .select("kitId", "member")
                 .in("member", uuids)
                 .findList()) {
            lockedKitMap.computeIfAbsent(row.getKitId(), i -> new HashSet<>()).add(row.getMember());
        }
        List<SQLKit> permissionKits = database.find(SQLKit.class)
            .select("id", "permission")
            .eq("enabled", true)
            .in("type", List.of(KitType.PERMISSION, KitType.PERMISSION_COOLDOWN))
            .findList();
        Bukkit.getScheduler().runTask(this, () -> {
                sidebarList.clear();
                PLAYERS: for (Player player : Bukkit.getOnlinePlayers()) {
                    final UUID uuid = player.getUniqueId();
                    if (unclaimedMembers.contains(uuid)) {
                        sidebarList.add(uuid);
                        continue;
                    }
                    for (SQLKit kit : permissionKits) {
                        if (kit.getPermission() != null
                            && player.hasPermission(kit.getPermission())
                            && player.isPermissionSet(kit.getPermission())) {
                            Set<UUID> set = lockedKitMap.get(kit.getId());
                            if (set == null || !set.contains(uuid)) {
                                sidebarList.add(uuid);
                                continue PLAYERS;
                            }
                        }
                    }
                }
            });
    }
}
