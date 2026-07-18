package com.winthier.kit;

import com.winthier.kit.sql.SQLKit;
import com.winthier.kit.sql.SQLMember;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.suggestCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class Kits {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd");

    public static Kit createKit(String baseKitName, Iterable<UUID> uuids) {
        final String templateKitName = "template_" + baseKitName;
        final SQLKit kit = KitPlugin.getInstance().getDatabase().find(SQLKit.class).eq("name", templateKitName).findUnique();
        if (kit == null) {
            throw new IllegalStateException("Could not find template kit: " + templateKitName);
        }
        final Date now = new Date();
        final String newKitName = baseKitName + "_" + DATE_FORMAT.format(now);
        kit.setId(null);
        kit.setName(newKitName);
        kit.setEnabled(false);
        kit.setCreatedTime(now);
        if (KitPlugin.getInstance().getDatabase().insert(kit) == 0) {
            throw new IllegalStateException("Could not clone kit: " + templateKitName + " => " + newKitName);
        }
        final List<SQLMember> memberRows = new ArrayList<>();
        for (UUID uuid : uuids) {
            memberRows.add(new SQLMember(kit, uuid));
        }
        KitPlugin.getInstance().getDatabase().insertIgnore(memberRows);
        final Kit result = new Kit(kit, (Date) null);
        result.members().addAll(memberRows);
        return result;
    }

    public static void autoCreateKit(CommandSender sender, String baseKitName, Iterable<UUID> uuids) {
        KitPlugin.getInstance().getDatabase().scheduleAsyncTask(() -> {
                final Kit kit;
                try {
                    kit = createKit(baseKitName, uuids);
                } catch (IllegalStateException ise) {
                    sender.sendMessage(text(ise.getMessage(), RED));
                    return;
                }
                final String command = "/kite info " + kit.row().getName();
                sender.sendMessage(
                    textOfChildren(
                        text("Created new kit "),
                        text(kit.row().getName(), WHITE),
                        text(" with "),
                        text(kit.members().size(), WHITE),
                        text(" members")
                    )
                    .color(YELLOW)
                    .hoverEvent(showText(text(command, GRAY)))
                    .clickEvent(suggestCommand(command))
                );
            });
    }

    private Kits() { }
}
