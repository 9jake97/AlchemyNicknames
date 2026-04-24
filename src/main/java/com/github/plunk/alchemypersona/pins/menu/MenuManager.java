package com.github.plunk.alchemypersona.pins.menu;

import com.github.plunk.alchemypersona.AlchemyPersona;
import com.github.plunk.alchemypersona.pins.managers.PinManager;
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
import java.util.Set;

public class MenuManager implements Listener {

    private final AlchemyPersona plugin;
    private final PinManager pinManager;
    private FileConfiguration menuConfig;
    private FileConfiguration pinsConfig;

    public MenuManager(AlchemyPersona plugin, PinManager pinManager) {
        this.plugin = plugin;
        this.pinManager = pinManager;
        loadMenu();
    }

    public AlchemyPersona getPlugin() {
        return plugin;
    }

    public void loadMenu() {
        File file = new File(plugin.getDataFolder(), "pins_menu.yml");
        if (!file.exists()) {
            plugin.saveResource("pins_menu.yml", false);
        }
        menuConfig = YamlConfiguration.loadConfiguration(file);

        File pinsFile = new File(plugin.getDataFolder(), "pins.yml");
        if (!pinsFile.exists()) {
            plugin.saveResource("pins.yml", false);
        }
        pinsConfig = YamlConfiguration.loadConfiguration(pinsFile);
    }

    public void reloadConfig() {
        loadMenu();
    }

    public void openMenu(Player player, int page, String category) {
        if (category == null)
            category = "all";

        ConfigurationSection catsSection = menuConfig.getConfigurationSection("categories");
        ConfigurationSection currentCatSec = (catsSection != null) ? catsSection.getConfigurationSection(category)
                : null;

        String baseTitle = menuConfig.getString("menu_title", "Pins");
        String finalTitle = baseTitle;

        if (currentCatSec != null && currentCatSec.contains("title_unicode")) {
            finalTitle = color(currentCatSec.getString("title_unicode"));
        }

        // Apply Title Offset
        String offset = menuConfig.getString("menu_title_offset", "");
        if (offset != null && !offset.isEmpty()) {
            finalTitle = color(offset) + finalTitle;
        }

        int size = menuConfig.getInt("size", 54);
        List<Integer> pinSlots = menuConfig.getIntegerList("pin_slots");

        if (pinSlots.isEmpty()) {
            for (int i = 0; i < size; i++)
                pinSlots.add(i);
        }

        Inventory inv = Bukkit.createInventory(new PinsHolder(page, category), size, finalTitle);

        // 1. Fixed Items & Category Buttons
        if (catsSection != null) {
            for (String key : catsSection.getKeys(false)) {
                ConfigurationSection catSec = catsSection.getConfigurationSection(key);
                if (catSec == null)
                    continue;
                if (catSec.contains("slot")) {
                    ItemStack item = createItem(catSec, player);
                    if (key.equalsIgnoreCase(category)) {
                        ItemMeta meta = item.getItemMeta();
                        meta.setDisplayName(meta.getDisplayName() + ChatColor.GREEN + " (Selected)");
                        item.setItemMeta(meta);
                    }
                    int slot = catSec.getInt("slot");
                    if (slot < size)
                        inv.setItem(slot, item);
                }
            }
        }

        ConfigurationSection fixedSection = menuConfig.getConfigurationSection("fixed_items");
        if (fixedSection != null) {
            for (String key : fixedSection.getKeys(false)) {
                ConfigurationSection itemSec = fixedSection.getConfigurationSection(key);
                if (itemSec == null)
                    continue;

                ItemStack item = createItem(itemSec, player);
                if (itemSec.contains("slots")) {
                    for (int slot : itemSec.getIntegerList("slots")) {
                        if (slot < size && inv.getItem(slot) == null)
                            inv.setItem(slot, item);
                    }
                } else {
                    int slot = itemSec.getInt("slot");
                    if (slot < size && inv.getItem(slot) == null)
                        inv.setItem(slot, item);
                }
            }
        }

        // 2. Filtered Pins (FROM PINS CONFIG)
        ConfigurationSection pinsSection = pinsConfig.getConfigurationSection("pins");
        if (pinsSection != null) {
            List<String> filteredPins = new ArrayList<>();
            for (String key : pinsSection.getKeys(false)) {
                ConfigurationSection pSec = pinsSection.getConfigurationSection(key);
                if (pSec == null)
                    continue;

                List<String> pinCats = pSec.getStringList("categories");
                if (category.equalsIgnoreCase("all") || pinCats.contains(category)) {
                    // Check Permission (Hide if unowned)
                    String perm = pSec.getString("permission");
                    if (perm == null && pinsConfig.contains("default_permission")) {
                        String defaultPerm = pinsConfig.getString("default_permission");
                        if (defaultPerm != null) {
                            perm = defaultPerm.replace("%name%", key);
                        }
                    }

                    if (perm != null && !player.hasPermission(perm)) {
                        continue;
                    }

                    filteredPins.add(key);
                }
            }

            int pinsPerPage = pinSlots.size();
            int totalPins = filteredPins.size();
            int totalPages = (int) Math.ceil((double) totalPins / pinsPerPage);

            if (page < 0)
                page = 0;
            if (page >= totalPages && totalPages > 0)
                page = totalPages - 1;

            int startIndex = page * pinsPerPage;
            int endIndex = Math.min(startIndex + pinsPerPage, totalPins);

            for (int i = startIndex; i < endIndex; i++) {
                String key = filteredPins.get(i);
                ConfigurationSection pinSec = pinsSection.getConfigurationSection(key);
                if (pinSec != null) {
                    ItemStack item = createItem(pinSec, player);
                    int slotIndex = i - startIndex;
                    if (slotIndex < pinSlots.size()) {
                        inv.setItem(pinSlots.get(slotIndex), item);
                    }
                }
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createItem(ConfigurationSection section, Player player) {
        // Locked Logic
        String perm = null;
        if (section.contains("permission")) {
            perm = section.getString("permission");
        } else if (pinsConfig.contains("default_permission")) {
            // Explicitly missing permission? Use default.
            // Note: If permission key exists but is empty, it might mean 'no perm', but we
            // assume missing key here.
            String defaultPerm = pinsConfig.getString("default_permission");
            // Assuming pin key is the name, but section usually doesn't know its key unless
            // passed.
            // Oh, section is a sub-section. We don't easily know the parent key unless we
            // passed it.
            // Luckily display_name usually contains the name or we can reuse display name.
            // BUT, strictly speaking, the pin key in the map is the unique ID.
            // We need to pass the pin key to createItem or infer it.
            // Checking how createItem is called: createItem(pinSec, player) inside the loop
            // where we have 'key'.
            // For now, let's use display_name stripped of color or pass the key.
            // Passing the key is cleaner. Let's start with a placeholder using display name
            // if we can't change signature easily right now.
            // Wait, I can pass the key.
            // Actually, let's check if we can get the key from section. getName() returns
            // the last path part!
            String key = section.getName();
            if (defaultPerm != null) {
                perm = defaultPerm.replace("%name%", key);
            }
        }

        if (perm != null) {
            String finalPerm = perm; // Effective final for closure if needed, but not here.
            if (!player.hasPermission(perm)) {
                if (section.contains("locked")) {
                    // Use specific locked section
                    section = section.getConfigurationSection("locked");
                } else if (pinsConfig.contains("default_locked_pin")) {
                    // Use default locked section
                    ConfigurationSection defaultLocked = pinsConfig.getConfigurationSection("default_locked_pin");
                    // We need to merge or use the default, but inject the name.
                    // Since createItem assumes a single section, we might need a helper or hack.
                    // Let's create the item from defaultLocked, then manually replace %name% in
                    // meta.
                    ItemStack item = createItem(defaultLocked, player); // Recursive call, but default shouldn't have
                                                                        // perm
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        String originalName = section.getString("display_name", "Pin");
                        if (meta.hasDisplayName()) {
                            meta.setDisplayName(meta.getDisplayName().replace("%name%", color(originalName)));
                        }
                        if (meta.hasLore()) {
                            List<String> lore = meta.getLore();
                            List<String> newLore = new ArrayList<>();
                            for (String line : lore) {
                                newLore.add(line.replace("%name%", color(originalName)));
                            }
                            meta.setLore(newLore);
                        }
                        item.setItemMeta(meta);
                    }
                    return item;
                }
            }
        }

        String materialName = section.getString("material");
        if (materialName == null && pinsConfig.contains("default_material")) {
            materialName = pinsConfig.getString("default_material");
        }
        if (materialName == null) {
            materialName = "STONE"; // Absolute fallback
        }

        Material mat = Material.matchMaterial(materialName);
        if (mat == null)
            mat = Material.STONE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Apply Enchantment Glow if:
            // 1. It is a pin (identified by having 'pin_unicode' or 'default_material'
            // context implies pin section)
            // 2. It is NOT locked (which we know it isn't, because if it was locked, we
            // would have returned early in the 'locked' block above, OR we are in the
            // 'locked' block recursion)
            // Wait, createItem is called for locked items too (recursively or directly).
            // We need to know if this is an "unlocked pin" to add glow.
            // A simple heuristic: If the section has "pin_unicode" AND we are not inside a
            // "locked" section config (which usually doesn't have pin_unicode unless
            // copied).
            // The default_locked_pin doesn't have pin_unicode.
            // The individual pins DO have pin_unicode.
            if (section.contains("pin_unicode")) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            if (section.contains("display_name")) {
                String displayName = section.getString("display_name");

                String suffix = section.getString("pin_unicode", "");

                // Nexo Glyph Integration (Direct API)
                if (section.contains("nexo_glyph_id") && Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
                    String glyphId = section.getString("nexo_glyph_id");
                    try {
                        com.nexomc.nexo.glyphs.Glyph glyph = com.nexomc.nexo.NexoPlugin.instance().fontManager()
                                .glyphFromID(glyphId);
                        if (glyph != null && !glyph.getUnicodes().isEmpty()) {
                            suffix = glyph.getUnicodes().get(0);
                        }
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("[AlchemyPersona] Failed to load Nexo glyph: " + glyphId);
                    }
                }

                if (!suffix.isEmpty()) {
                    displayName += " " + suffix;
                }

                meta.setDisplayName(color(displayName));
            }
            if (section.contains("lore")) {
                List<String> lore = new ArrayList<>();
                for (String line : section.getStringList("lore")) {
                    lore.add(color(line));
                }
                meta.setLore(lore);
            } else if (pinsConfig.contains("default_unlocked_lore")) {
                List<String> lore = new ArrayList<>();
                List<String> defaultLore = pinsConfig.getStringList("default_unlocked_lore");
                String displayName = section.getString("display_name", "");
                // Strip color codes for cleaner name replacement if needed, or keeping it
                // simple
                // We'll just replace %name% with the raw display name (with colors)
                for (String line : defaultLore) {
                    lore.add(color(line.replace("%name%", color(displayName))));
                }
                meta.setLore(lore);
            }
            if (section.contains("model_data")) {
                meta.setCustomModelData(section.getInt("model_data"));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PinsHolder))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        PinsHolder holder = (PinsHolder) event.getInventory().getHolder();
        int currentPage = holder.getPage();
        String currentCat = holder.getCategory();

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

        ConfigurationSection fixedSection = menuConfig.getConfigurationSection("fixed_items");
        if (fixedSection != null) {
            for (String key : fixedSection.getKeys(false)) {
                ConfigurationSection itemSec = fixedSection.getConfigurationSection(key);
                if (itemSec == null)
                    continue;

                boolean hit = false;
                if (itemSec.contains("slots")) {
                    if (itemSec.getIntegerList("slots").contains(slot))
                        hit = true;
                } else if (itemSec.getInt("slot") == slot) {
                    hit = true;
                }

                if (hit) {
                    handleAction(player, itemSec, currentPage, currentCat);
                    return;
                }
            }
        }

        List<Integer> pinSlots = menuConfig.getIntegerList("pin_slots");
        if (pinSlots.contains(slot)) {
            // Read from PINS CONFIG
            ConfigurationSection pinsSection = pinsConfig.getConfigurationSection("pins");
            if (pinsSection != null) {
                List<String> filteredPins = new ArrayList<>();
                for (String key : pinsSection.getKeys(false)) {
                    ConfigurationSection pSec = pinsSection.getConfigurationSection(key);
                    if (pSec == null)
                        continue;
                    List<String> pinCats = pSec.getStringList("categories");
                    if (currentCat.equalsIgnoreCase("all") || pinCats.contains(currentCat)) {
                        filteredPins.add(key);
                    }
                }

                int pinsPerPage = pinSlots.size();
                int startIndex = currentPage * pinsPerPage;
                int slotIndex = pinSlots.indexOf(slot);
                int realIndex = startIndex + slotIndex;

                if (realIndex < filteredPins.size()) {
                    String pinKey = filteredPins.get(realIndex);
                    ConfigurationSection pinSec = pinsSection.getConfigurationSection(pinKey);
                    if (pinSec != null) {
                        handleAction(player, pinSec, currentPage, currentCat);
                    }
                }
            }
        }
    }

    private void handleAction(Player player, ConfigurationSection section, int currentPage, String currentCat) {
        String perm = section.getString("permission");
        if (perm == null && pinsConfig.contains("default_permission")) {
            String defaultPerm = pinsConfig.getString("default_permission");
            String key = section.getName();
            if (defaultPerm != null) {
                perm = defaultPerm.replace("%name%", key);
            }
        }

        // Permission check moved to individual action logic or pre-checks since
        // 'locked' overrides visuals.
        // But for actions like page turn, no perm needed.
        // For PINS, we check perm.

        // If it's a category or fixed item with action, perm might be needed.
        if (perm != null && !player.hasPermission(perm) && !section.contains("locked")) {
            player.sendMessage(ChatColor.RED + "You don't have permission!");
            return;
        }

        // Check if locked (Permission check)
        if (perm != null && !player.hasPermission(perm)) {
            // If valid pin (has permission defined), and user lacks it, it's locked.
            // We block access regardless of whether 'locked' section exists.
            player.sendMessage(ChatColor.RED + "You do not own this pin!");
            return;
        }

        String action = section.getString("action");
        String unicode = section.getString("pin_unicode");

        // Nexo Override for Suffix
        if (section.contains("nexo_glyph_id") && Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
            String glyphId = section.getString("nexo_glyph_id");
            Bukkit.getLogger().info("[AlchemyPersona DEBUG] Attempting to resolve Nexo glyph with ID: " + glyphId);
            try {
                com.nexomc.nexo.glyphs.Glyph glyph = com.nexomc.nexo.NexoPlugin.instance().fontManager()
                        .glyphFromID(glyphId);
                if (glyph != null) {
                    if (!glyph.getUnicodes().isEmpty()) {
                        unicode = glyph.getUnicodes().get(0); // Use the actual glyph character
                        Bukkit.getLogger().info("[AlchemyPersona DEBUG] Resolved Nexo glyph unicode: " + unicode);
                    } else {
                        Bukkit.getLogger().warning("[AlchemyPersona DEBUG] Nexo glyph found but has no unicodes!");
                    }
                } else {
                    Bukkit.getLogger().warning("[AlchemyPersona DEBUG] Nexo glyph is NULL for ID: " + glyphId);
                }
            } catch (Exception e) {
                Bukkit.getLogger().severe("[AlchemyPersona DEBUG] Exception resolving Nexo glyph: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Bukkit.getLogger().info("[AlchemyPersona DEBUG] No nexo_glyph_id found or Nexo not enabled.");
        }

        if ("close".equalsIgnoreCase(action)) {
            player.closeInventory();
        } else if ("reset".equalsIgnoreCase(action)) {
            pinManager.clearPin(player);
            player.sendMessage(ChatColor.GREEN + "Pin reset!");
            player.closeInventory();
        } else if ("next_page".equalsIgnoreCase(action)) {
            openMenu(player, currentPage + 1, currentCat);
        } else if ("previous_page".equalsIgnoreCase(action)) {
            if (currentPage > 0)
                openMenu(player, currentPage - 1, currentCat);
        }

        if (unicode != null) {
            // Toggle behavior: if clicking the same pin, remove it
            String currentPin = pinManager.getCurrentPin(player);
            String newPin = color(unicode);

            if (currentPin != null && currentPin.equals(newPin)) {
                // Already have this pin - remove it
                pinManager.clearPin(player);
                player.sendMessage(ChatColor.YELLOW + "Pin removed!");
            } else {
                // Set the new pin
                pinManager.setPin(player, newPin);
                player.sendMessage(ChatColor.GREEN + "Pin set!");
            }
            player.closeInventory();
        }
    }

    private String color(String s) {
        if (s == null)
            return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
