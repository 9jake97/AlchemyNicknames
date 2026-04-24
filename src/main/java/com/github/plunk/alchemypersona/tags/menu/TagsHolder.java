package com.github.plunk.alchemypersona.tags.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class TagsHolder implements InventoryHolder {
    private final int page;
    private final String category;

    public TagsHolder(int page, String category) {
        this.page = page;
        this.category = category;
    }

    @Override
    public Inventory getInventory() {
        return null; // Not needed, we create inventory in manager
    }

    public int getPage() {
        return page;
    }

    public String getCategory() {
        return category;
    }
}
