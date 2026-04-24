package com.github.plunk.alchemypersona.pins.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class PinsHolder implements InventoryHolder {

    private final int page;
    private final String category;

    public PinsHolder(int page, String category) {
        this.page = page;
        this.category = category;
    }

    public int getPage() {
        return page;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
