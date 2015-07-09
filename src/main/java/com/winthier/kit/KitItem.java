package com.winthier.kit;

import java.util.UUID;

public abstract class KitItem
{
    public abstract void giveToPlayer(UUID player);

    protected abstract String getItemID();
    protected abstract int getItemAmount();
    protected abstract int getItemData();
    protected abstract String getItemDataTag();

    protected String consoleCommand(String playerName)
    {
        return String.format("minecraft:give %s %s %d %d %s",
                             playerName,
                             getItemID(),
                             getItemAmount(),
                             getItemData(),
                             getItemDataTag());
    }
}
