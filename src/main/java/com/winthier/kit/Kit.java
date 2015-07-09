package com.winthier.kit;

import java.util.Collection;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

public abstract class Kit
{
    protected abstract String getName();

    protected abstract String getMessage();

    protected abstract Collection<? extends KitItem> getItems();

    protected abstract int getCooldownInSeconds();

    protected abstract boolean playerIsOnCooldown(UUID player);

    protected abstract void setPlayerOnCooldown(UUID player);

    protected abstract void sendCooldownMessage(UUID uuid);

    protected abstract void sendMessage(UUID uuid);

    protected abstract boolean playerHasPermission(UUID player);

    public void giveToPlayer(UUID player)
    {
        if (playerIsOnCooldown(player)) {
            sendCooldownMessage(player);
            return;
        }
        for (KitItem item : getItems()) item.giveToPlayer(player);
        sendMessage(player);
        setPlayerOnCooldown(player);
    }
}
