package com.github.plunk.alchemypersona.joinmessages.gui;

import com.github.plunk.alchemypersona.joinmessages.objects.JoinMessage;
import com.github.plunk.alchemypersona.AlchemyPersona;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class MessageGUI {

    private static HashMap<String, MessageGUI> inGUI = new HashMap<>();
    private Inventory inventory;
    private final Map<Integer, ItemStack> items;
    private final String displayName;
    private int slots;
    private int page;

    // Maps slot to JoinMessage identifier
    private Map<Integer, String> messages = new HashMap<>();

    public MessageGUI(String displayName, int page) {
        this.displayName = displayName;
        this.items = new HashMap<>();
        this.page = page;
    }

    public MessageGUI setSlots(int slots) {
        this.slots = slots;
        return this;
    }

    public MessageGUI setItem(int slot, ItemStack item) {
        this.items.put(slot, item);
        return this;
    }

    public void setMessages(Map<Integer, String> messages) {
        this.messages = messages;
    }

    public Map<Integer, String> getMessages() {
        return this.messages;
    }

    public void openInventory(Player player) {
        this.inventory = Bukkit.createInventory(null, this.slots, this.displayName);
        for (Integer slot : this.items.keySet()) {
            this.inventory.setItem(slot, this.items.get(slot));
        }
        player.openInventory(this.inventory);
        inGUI.put(player.getName(), this);
    }

    public int getPage() {
        return page;
    }

    public static boolean hasGUI(Player p) {
        return inGUI.containsKey(p.getName());
    }

    public static MessageGUI getGUI(Player p) {
        return inGUI.get(p.getName());
    }

    public static void close(Player p) {
        inGUI.remove(p.getName());
    }

    // Helper for creating items
    public static ItemStack createItem(Material mat, short data, int amount, String name, List<String> lore) {
        if (mat == null)
            return null;
        ItemStack item = new ItemStack(mat, amount, data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null)
                meta.setDisplayName(name);
            if (lore != null)
                meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}


