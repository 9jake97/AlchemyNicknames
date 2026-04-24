package com.github.plunk.alchemypersona.joinmessages.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MessageHolder implements InventoryHolder {
    private final int page;
    private final String category;

    public MessageHolder(int page, String category) {
        this.page = page;
        this.category = category;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public int getPage() {
        return page;
    }

    public String getCategory() {
        return category;
    }
}
