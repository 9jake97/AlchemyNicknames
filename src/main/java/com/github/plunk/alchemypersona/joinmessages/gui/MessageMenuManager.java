package com.github.plunk.alchemypersona.joinmessages.gui;

import com.github.plunk.alchemypersona.AlchemyPersona;
import com.github.plunk.alchemypersona.joinmessages.objects.JoinMessage;
import com.github.plunk.alchemypersona.joinmessages.utils.HexUtils;
import com.github.plunk.alchemypersona.joinmessages.Data;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MessageMenuManager implements Listener {

    private final AlchemyPersona plugin;
    private FileConfiguration menuConfig;

    public MessageMenuManager(AlchemyPersona plugin) {
        this.plugin = plugin;
        loadMenu();
    }

    public void loadMenu() {
        File file = new File(plugin.getDataFolder(), "join_messages_menu.yml");
        if (!file.exists()) {
            plugin.saveResource("join_messages_menu.yml", false);
        }
        menuConfig = YamlConfiguration.loadConfiguration(file);

        // Add missing keys for migration
        if (!menuConfig.contains("menu_title_offset_pixels")) {
            menuConfig.set("menu_title_offset_pixels", 0);
            try {
                menuConfig.save(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void openMenu(Player player, int page, String category) {
        if (category == null)
            category = "all";

        ConfigurationSection catsSection = menuConfig.getConfigurationSection("categories");
        ConfigurationSection currentCatSec = (catsSection != null) ? catsSection.getConfigurationSection(category)
                : null;

        String baseTitle = menuConfig.getString("menu_title", "Join Messages");
        String finalTitle = baseTitle;

        if (currentCatSec != null && currentCatSec.getString("title_unicode") != null
                && !currentCatSec.getString("title_unicode").isEmpty()) {
            finalTitle = color(currentCatSec.getString("title_unicode"));
        }

        String offset = menuConfig.getString("menu_title_offset", "");
        if (offset != null && !offset.isEmpty()) {
            finalTitle = color(offset) + finalTitle;
        }

        int pixelOffset = menuConfig.getInt("menu_title_offset_pixels", 0);
        if (pixelOffset != 0) {
            finalTitle = getOffsetString(pixelOffset) + finalTitle;
        }

        // Diagnostic log for the developer
        plugin.getLogger().info("Opening GUI for " + player.getName() + " with title: '" + finalTitle
                + "' (Pixel Offset: " + pixelOffset + ")");

        int size = menuConfig.getInt("size", 54);
        List<Integer> tagSlots = menuConfig.getIntegerList("tag_slots");
        if (tagSlots.isEmpty()) {
            for (int i = 0; i < size; i++)
                tagSlots.add(i);
        }

        Inventory inv = Bukkit.createInventory(new MessageHolder(page, category), size, finalTitle);

        // 1. Categories
        if (catsSection != null) {
            for (String key : catsSection.getKeys(false)) {
                ConfigurationSection catSec = catsSection.getConfigurationSection(key);
                if (catSec == null || !catSec.contains("slot"))
                    continue;

                ItemStack item = createConfigItem(catSec, player);
                if (key.equalsIgnoreCase(category)) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(meta.getDisplayName() + ChatColor.GREEN + " (Selected)");
                        item.setItemMeta(meta);
                    }
                }
                int slot = catSec.getInt("slot");
                if (slot < size)
                    inv.setItem(slot, item);
            }
        }

        // 2. Fixed Items
        ConfigurationSection fixedSection = menuConfig.getConfigurationSection("fixed_items");
        if (fixedSection != null) {
            for (String key : fixedSection.getKeys(false)) {
                ConfigurationSection itemSec = fixedSection.getConfigurationSection(key);
                if (itemSec == null)
                    continue;
                ItemStack item = createConfigItem(itemSec, player);
                if (itemSec.contains("slots")) {
                    for (int slot : itemSec.getIntegerList("slots")) {
                        if (slot < size && inv.getItem(slot) == null)
                            inv.setItem(slot, item);
                    }
                } else if (itemSec.contains("slot")) {
                    int slot = itemSec.getInt("slot");
                    if (slot < size && inv.getItem(slot) == null)
                        inv.setItem(slot, item);
                }
            }
        }

        // 3. Messages
        List<JoinMessage> availableMessages = new ArrayList<>();
        for (JoinMessage msg : plugin.getMessageManager().getLoadedMessages()) {
            if (player.hasPermission(msg.getPermission())) {
                availableMessages.add(msg);
            }
        }
        // Alphabetize? AlchemyPersona does it.
        availableMessages.sort((a, b) -> a.getIdentifier().compareToIgnoreCase(b.getIdentifier()));

        int tagsPerPage = tagSlots.size();
        int totalTags = availableMessages.size();
        int totalPages = (int) Math.ceil((double) totalTags / tagsPerPage);

        if (page < 0)
            page = 0;
        if (page >= totalPages && totalPages > 0)
            page = totalPages - 1;

        int startIndex = page * tagsPerPage;
        int endIndex = Math.min(startIndex + tagsPerPage, totalTags);

        for (int i = startIndex; i < endIndex; i++) {
            JoinMessage msg = availableMessages.get(i);
            ItemStack item = createMessageItem(msg, player);
            int slotIndex = i - startIndex;
            if (slotIndex < tagSlots.size()) {
                inv.setItem(tagSlots.get(slotIndex), item);
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createConfigItem(ConfigurationSection section, Player player) {
        String matName = section.getString("material", "PAPER");
        Material mat = Material.matchMaterial(matName.toUpperCase());
        if (mat == null)
            mat = Material.PAPER;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(section.getString("display_name", section.getName())));
            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                lore.add(color(line));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMessageItem(JoinMessage msg, Player player) {
        Material mat = msg.getIconMaterial();
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String dn = msg.getDisplayName();
            // AlchemyPersona uses nexo glyphs if present
            if (msg.getNexoGlyphId() != null && Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
                try {
                    com.nexomc.nexo.glyphs.Glyph glyph = com.nexomc.nexo.NexoPlugin.instance().fontManager()
                            .glyphFromID(msg.getNexoGlyphId());
                    if (glyph != null && !glyph.getUnicodes().isEmpty()) {
                        dn = glyph.getUnicodes().get(0);
                    }
                } catch (Exception ignored) {
                }
            }
            // If display name is same as key (default), use message content for a better
            // look
            if (dn.equals(msg.getIdentifier())) {
                dn = msg.getMessage().replace("%player%", player.getName());
                // Strip possible starting/ending quotes if they exist from config loading
                if (dn.startsWith("'") && dn.endsWith("'"))
                    dn = dn.substring(1, dn.length() - 1);
            }

            meta.setDisplayName(color(dn));

            List<String> lore = new ArrayList<>();
            if (msg.getDescription() != null) {
                for (String l : msg.getDescription()) {
                    // Apply theme rules to tooltip lines
                    String themedLine = msg.applyTheme(l, player.getName());
                    String coloredLine = color(themedLine);

                    // Skip if identical to display name to avoid double lines
                    if (coloredLine.equals(meta.getDisplayName()))
                        continue;
                    lore.add(coloredLine);
                }
            }
            meta.setLore(lore);

            // Selected state handling
            String selectedId = Data.get().getString("players." + player.getUniqueId().toString());
            boolean isSelected = msg.getIdentifier().equals(selectedId);

            if (isSelected) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            if (msg.getIconData() > 0) {
                // For 1.16, we can use MaterialData or just the legacy constructor
                // but since it's deprecated, let's at least make it cleaner if possible.
                // In 1.16, this is the most common way for legacy data.
                item = new ItemStack(mat, 1, (short) msg.getIconData());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MessageHolder))
            return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        MessageHolder holder = (MessageHolder) event.getInventory().getHolder();
        int slot = event.getSlot();

        // Check Categories
        ConfigurationSection categoriesSec = menuConfig.getConfigurationSection("categories");
        if (categoriesSec != null) {
            for (String key : categoriesSec.getKeys(false)) {
                ConfigurationSection catSec = categoriesSec.getConfigurationSection(key);
                if (catSec != null && catSec.getInt("slot") == slot) {
                    openMenu(player, 0, key);
                    return;
                }
            }
        }

        // Check Fixed Items
        ConfigurationSection fixedSection = menuConfig.getConfigurationSection("fixed_items");
        if (fixedSection != null) {
            for (String key : fixedSection.getKeys(false)) {
                ConfigurationSection itemSec = fixedSection.getConfigurationSection(key);
                if (itemSec == null)
                    continue;
                boolean hit = false;
                if (itemSec.contains("slots") && itemSec.getIntegerList("slots").contains(slot))
                    hit = true;
                else if (itemSec.getInt("slot") == slot)
                    hit = true;

                if (hit) {
                    handleAction(player, itemSec, holder.getPage(), holder.getCategory());
                    return;
                }
            }
        }

        // Check Messages
        List<Integer> tagSlots = menuConfig.getIntegerList("tag_slots");
        if (tagSlots.contains(slot)) {
            List<JoinMessage> available = new ArrayList<>();
            for (JoinMessage msg : plugin.getMessageManager().getLoadedMessages()) {
                if (player.hasPermission(msg.getPermission())) {
                    available.add(msg);
                }
            }
            available.sort((a, b) -> a.getIdentifier().compareToIgnoreCase(b.getIdentifier()));

            int tagsPerPage = tagSlots.size();
            int realIndex = (holder.getPage() * tagsPerPage) + tagSlots.indexOf(slot);
            if (realIndex < available.size()) {
                JoinMessage selected = available.get(realIndex);
                Data.get().set("players." + player.getUniqueId().toString(), selected.getIdentifier());
                Data.save();
                player.sendMessage(ChatColor.GREEN + "Selected join message: " + color(selected.getDisplayName()));
                player.closeInventory();
            }
        }
    }

    private void handleAction(Player player, ConfigurationSection section, int page, String cat) {
        String action = section.getString("action");
        if ("close".equalsIgnoreCase(action))
            player.closeInventory();
        else if ("reset".equalsIgnoreCase(action)) {
            Data.get().set("players." + player.getUniqueId().toString(), null);
            Data.save();
            player.sendMessage(ChatColor.GREEN + "Join message cleared!");
            player.closeInventory();
        } else if ("next_page".equalsIgnoreCase(action))
            openMenu(player, page + 1, cat);
        else if ("previous_page".equalsIgnoreCase(action)) {
            if (page > 0)
                openMenu(player, page - 1, cat);
        }
    }

    private String color(String s) {
        return HexUtils.colorify(s);
    }

    private String getOffsetString(int pixels) {
        if (pixels == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        int abs = Math.abs(pixels);
        boolean neg = pixels < 0;

        // Use standard negative space glyphs: 128, 64, 32, 16, 8, 7...1
        int[] values = { 128, 64, 32, 16, 8, 7, 6, 5, 4, 3, 2, 1 };
        char[] negChars = { '\uF80C', '\uF80B', '\uF80A', '\uF809', '\uF808', '\uF807', '\uF806', '\uF805', '\uF804',
                '\uF803',
                '\uF802', '\uF801' };
        char[] posChars = { '\uF82C', '\uF82B', '\uF82A', '\uF829', '\uF828', '\uF827', '\uF826', '\uF825', '\uF824',
                '\uF823',
                '\uF822', '\uF821' };

        char[] chars = neg ? negChars : posChars;

        for (int i = 0; i < values.length; i++) {
            while (abs >= values[i]) {
                sb.append(chars[i]);
                abs -= values[i];
            }
        }
        return sb.toString();
    }
}
