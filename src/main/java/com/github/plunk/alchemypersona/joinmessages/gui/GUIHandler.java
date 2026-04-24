package com.github.plunk.alchemypersona.joinmessages.gui;

import com.github.plunk.alchemypersona.AlchemyPersona;
import com.github.plunk.alchemypersona.joinmessages.Data;
import com.github.plunk.alchemypersona.joinmessages.objects.JoinMessage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUIHandler implements Listener {

    private final AlchemyPersona plugin;

    public GUIHandler(AlchemyPersona plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player))
            return;
        Player p = (Player) e.getWhoClicked();

        if (!MessageGUI.hasGUI(p))
            return;

        MessageGUI gui = MessageGUI.getGUI(p);
        if (gui == null)
            return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        int slot = e.getRawSlot();
        Map<Integer, String> messages = gui.getMessages();

        if (messages != null && messages.containsKey(slot)) {
            // Player clicked a message
            String identifier = messages.get(slot);
            JoinMessage msg = plugin.getMessageManager().getMessage(identifier);
            if (msg == null)
                return;

            if (!p.hasPermission(msg.getPermission())) {
                p.sendMessage(ChatColor.RED + "You do not have permission for this message.");
                p.closeInventory();
                return;
            }

            // Save selection
            Data.get().set("players." + p.getUniqueId().toString(), identifier);
            Data.save();

            p.sendMessage(ChatColor.GREEN + "Selected join message: " + ChatColor.YELLOW + msg.getDisplayName());
            p.closeInventory();
        } else if (slot == 53) {
            // Next Page
            openMenu(p, gui.getPage() + 1);
        } else if (slot == 45) {
            // Prev Page
            openMenu(p, gui.getPage() - 1);
        } else if (slot == 49) {
            // Close / Info
            p.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player) {
            MessageGUI.close((Player) e.getPlayer());
        }
    }

    public void openMenu(Player p, int page) {
        List<String> available = plugin.getMessageManager().getAvailableMessageIdentifiers(p);

        GUIOptions options = plugin.getJoinMessagesGuiOptions();
        int pageSize = options.getMenuSize() - 18; // Reserve bottom rows for nav? DeluxeTags used 36 items/page in 54
                                                   // slot inv.
        // Assuming 54 slot inventory, using first 45 for items?
        // DeluxeTags uses 36 items.

        int totalPages = (int) Math.ceil((double) available.size() / 36.0);
        if (page < 1)
            page = 1;

        String title = options.getMenuTitle().replace("%page%", String.valueOf(page));
        MessageGUI gui = new MessageGUI(color(title), page).setSlots(options.getMenuSize());

        Map<Integer, String> slotMap = new HashMap<>();

        // Paging logic
        int startIndex = (page - 1) * 36;
        int endIndex = Math.min(startIndex + 36, available.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            String id = available.get(i);
            JoinMessage msg = plugin.getMessageManager().getMessage(id);
            if (msg != null) {
                gui.setItem(slot, MessageGUI.createItem(
                        msg.getIconMaterial(),
                        msg.getIconData(),
                        1,
                        color(msg.getDisplayName()),
                        colorList(msg.getDescription(), p)));
                slotMap.put(slot, id);
                slot++;
            }
        }
        gui.setMessages(slotMap);

        // Navigation items (simplified for now)
        if (page > 1) {
            gui.setItem(45, MessageGUI.createItem(Material.ARROW, (short) 0, 1, color("&ePrevious Page"), null));
        }
        if (page < totalPages) {
            gui.setItem(53, MessageGUI.createItem(Material.ARROW, (short) 0, 1, color("&eNext Page"), null));
        }

        gui.setItem(49, MessageGUI.createItem(Material.BARRIER, (short) 0, 1, color("&cClose"), null));

        gui.openInventory(p);
    }

    private String color(String s) {
        return com.github.plunk.alchemypersona.joinmessages.utils.HexUtils.colorify(s);
    }

    private List<String> colorList(List<String> list, Player p) {
        if (list == null)
            return null;
        java.util.List<String> colored = new java.util.ArrayList<>();
        for (String s : list) {
            String replaced = s.replace("%player%", p.getName());
            colored.add(color(replaced));
        }
        return colored;
    }
}


