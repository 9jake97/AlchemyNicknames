package com.github.plunk.alchemypersona.tags.menu;

import com.github.plunk.alchemypersona.AlchemyPersona;
import com.github.plunk.alchemypersona.tags.managers.TagManager;
import com.github.plunk.alchemypersona.tags.utils.ColorUtils;
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

public class MenuManager implements Listener {

    private final AlchemyPersona plugin;
    private final TagManager tagManager;
    private FileConfiguration menuConfig;
    private FileConfiguration tagsConfig;

    public MenuManager(AlchemyPersona plugin, TagManager tagManager) {
        this.plugin = plugin;
        this.tagManager = tagManager;
        loadMenu();
    }

    public AlchemyPersona getPlugin() {
        return plugin;
    }

    public void loadMenu() {
        File file = new File(plugin.getDataFolder(), "tags_menu.yml");
        if (!file.exists()) {
            plugin.saveResource("tags_menu.yml", false);
        }
        menuConfig = YamlConfiguration.loadConfiguration(file);

        File tagsFile = new File(plugin.getDataFolder(), "tags.yml");
        if (!tagsFile.exists()) {
            plugin.saveResource("tags.yml", false);
        }
        tagsConfig = YamlConfiguration.loadConfiguration(tagsFile);
    }

    public void reload() {
        loadMenu();
    }

    public void openMenu(Player player, int page, String category) {
        if (category == null)
            category = "all";
        plugin.getLogger().info("Debug: openMenu called for " + player.getName());

        ConfigurationSection catsSection = menuConfig.getConfigurationSection("categories");
        if (catsSection == null) {
            plugin.getLogger().warning("Debug: 'categories' section is NULL in pins_menu.yml");
        }
        ConfigurationSection currentCatSec = (catsSection != null) ? catsSection.getConfigurationSection(category)
                : null;

        String baseTitle = menuConfig.getString("menu_title", "Tags");
        String finalTitle = baseTitle;

        if (currentCatSec != null && currentCatSec.contains("title_unicode")) {
            finalTitle = color(currentCatSec.getString("title_unicode"));
        }

        // Offset
        String offset = menuConfig.getString("menu_title_offset", "");
        if (offset != null && !offset.isEmpty()) {
            finalTitle = color(offset) + finalTitle;
        }

        int size = menuConfig.getInt("size", 54);
        List<Integer> tagSlots = menuConfig.getIntegerList("tag_slots");
        if (tagSlots.isEmpty()) {
            for (int i = 0; i < size; i++)
                tagSlots.add(i);
        }

        Inventory inv = Bukkit.createInventory(new TagsHolder(page, category), size, finalTitle);

        // 1. Fixed Items & Categories
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

        // 2. Filtered Tags
        ConfigurationSection tagsSection = tagsConfig.getConfigurationSection("tags");
        if (tagsSection != null) {
            List<String> filteredTags = new ArrayList<>();
            for (String key : tagsSection.getKeys(false)) {
                ConfigurationSection tSec = tagsSection.getConfigurationSection(key);
                if (tSec == null)
                    continue;

                // Check permission
                String perm = tSec.getString("permission");
                if (perm == null || perm.isEmpty()) {
                    perm = tagsConfig.getString("default_permission", "deluxetags.tag.%name%")
                            .replace("%name%", key);
                }

                boolean hasPerm = player.hasPermission(perm);
                plugin.getLogger().info("[Debug] Checking " + key + " for " + player.getName() + ": perm=" + perm
                        + " result=" + hasPerm);

                if (hasPerm) {
                    List<String> cats = tSec.getStringList("categories");
                    if (category.equalsIgnoreCase("all") || cats.contains(category)) {
                        filteredTags.add(key);
                    }
                }
            }

            // Alphabetize tags by ID
            filteredTags.sort(String::compareToIgnoreCase);

            int tagsPerPage = tagSlots.size();
            int totalTags = filteredTags.size();
            int totalPages = (int) Math.ceil((double) totalTags / tagsPerPage);

            if (page < 0)
                page = 0;
            if (page >= totalPages && totalPages > 0)
                page = totalPages - 1;

            int startIndex = page * tagsPerPage;
            int endIndex = Math.min(startIndex + tagsPerPage, totalTags);

            for (int i = startIndex; i < endIndex; i++) {
                String key = filteredTags.get(i);
                ConfigurationSection tagSec = tagsSection.getConfigurationSection(key);
                if (tagSec != null) {
                    ItemStack item = createItem(tagSec, player);
                    int slotIndex = i - startIndex;
                    if (slotIndex < tagSlots.size()) {
                        inv.setItem(tagSlots.get(slotIndex), item);
                    }
                }
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createItem(ConfigurationSection section, Player player) {
        if (section == null)
            return null;

        // Determine section type for logging
        boolean aTag = section.contains("tag") || section.contains("tag_unicode");
        String sectionName = section.getName();

        // 1. Resolve Material
        String matName = section.getString("material");
        if (matName == null || matName.isEmpty()) {
            matName = tagsConfig.getString("default_material", "NAME_TAG");
        }

        Material mat = Material.matchMaterial(matName.toUpperCase());
        if (mat == null) {
            mat = aTag ? Material.NAME_TAG : Material.PAPER;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        // 2. Resolve Display Name (Suffix)
        String suffix = "";
        if (section.contains("nexo_glyph_id") && Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
            try {
                com.nexomc.nexo.glyphs.Glyph glyph = com.nexomc.nexo.NexoPlugin.instance().fontManager()
                        .glyphFromID(section.getString("nexo_glyph_id"));
                if (glyph != null && !glyph.getUnicodes().isEmpty())
                    suffix = glyph.getUnicodes().get(0);
            } catch (Exception ignored) {
            }
        }
        if (suffix.isEmpty())
            suffix = section.getString("tag", section.getString("tag_unicode", ""));

        String dn = suffix.isEmpty() ? section.getString("display_name", sectionName) : suffix;

        // Debug: Append material info for non-ops if they see something wrong
        // if (!player.isOp()) { dn += " (" + mat.name() + ")"; }

        meta.setDisplayName(color(dn));

        // 3. Resolve Lore
        List<String> lore = new ArrayList<>();
        if (!aTag) {
            if (section.contains("lore")) {
                for (String l : section.getStringList("lore"))
                    lore.add(color(l));
            }
        }
        meta.setLore(lore);

        // 4. Misc
        if (aTag) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (section.contains("model_data"))
            meta.setCustomModelData(section.getInt("model_data"));
        if (section.contains("custom_model_data"))
            meta.setCustomModelData(section.getInt("custom_model_data"));

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TagsHolder))
            return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        TagsHolder holder = (TagsHolder) event.getInventory().getHolder();
        int slot = event.getSlot();

        // Logic for Fixed Items / Categories (Copy from LP Pins essentially)
        // ... (Checking Categories)
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

        // ... (Checking Fixed Items)
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

        // ... (Checking Tags)
        List<Integer> tagSlots = menuConfig.getIntegerList("tag_slots");
        if (tagSlots.contains(slot)) {
            ConfigurationSection tagsSection = tagsConfig.getConfigurationSection("tags");
            if (tagsSection != null) {
                List<String> filtered = new ArrayList<>();
                for (String key : tagsSection.getKeys(false)) {
                    ConfigurationSection tSec = tagsSection.getConfigurationSection(key);
                    if (tSec == null)
                        continue;

                    // Check permission
                    String perm = tSec.getString("permission");
                    if (perm == null || perm.isEmpty()) {
                        perm = tagsConfig.getString("default_permission", "deluxetags.tag.%name%")
                                .replace("%name%", key);
                    }

                    if (player.hasPermission(perm)) {
                        List<String> cats = tSec.getStringList("categories");
                        if (holder.getCategory().equalsIgnoreCase("all") || cats.contains(holder.getCategory())) {
                            filtered.add(key);
                        }
                    }
                }

                // Alphabetize tags to match openMenu
                filtered.sort(String::compareToIgnoreCase);

                int tagsPerPage = tagSlots.size();
                int realIndex = (holder.getPage() * tagsPerPage) + tagSlots.indexOf(slot);
                if (realIndex < filtered.size()) {
                    String key = filtered.get(realIndex);
                    handleAction(player, tagsSection.getConfigurationSection(key), holder.getPage(),
                            holder.getCategory());
                }
            }
        }
    }

    private void handleAction(Player player, ConfigurationSection section, int page, String cat) {
        // Perm check
        String perm = section.getString("permission");
        if (perm == null && tagsConfig.contains("default_permission")) {
            perm = tagsConfig.getString("default_permission").replace("%name%", section.getName());
        }

        if (perm != null && !player.hasPermission(perm) && !section.contains("locked")) {
            player.sendMessage(ChatColor.RED + "No Permission!");
            return;
        }

        // If locked (checked effectively above for visuals, but verify here)
        if (perm != null && !player.hasPermission(perm)) {
            player.sendMessage(ChatColor.RED + "Locked!");
            return;
        }

        String action = section.getString("action");
        if ("close".equalsIgnoreCase(action))
            player.closeInventory();
        else if ("reset".equalsIgnoreCase(action)) {
            tagManager.clearTag(player);
            player.sendMessage(ChatColor.GREEN + "Tags cleared!");
            player.closeInventory();
        } else if ("next_page".equalsIgnoreCase(action))
            openMenu(player, page + 1, cat);
        else if ("previous_page".equalsIgnoreCase(action)) {
            if (page > 0)
                openMenu(player, page - 1, cat);
        } else {
            // Implicitly set tag if no action defined but section is a Tag (has categories
            // or name)
            // The key of the section is the Tag ID.
            tagManager.setTag(player, section.getName());
            player.sendMessage(ChatColor.GREEN + "Tag Selected!");
            player.closeInventory();
        }
    }

    private String color(String s) {
        return ColorUtils.color(s);
    }
}
