package com.github.plunk.alchemypersona.discord;

import com.github.plunk.alchemypersona.AlchemyPersona;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LinkManager {

    private final AlchemyPersona plugin;
    private final File dataFile;
    private final Map<String, List<UUID>> discordToUuids = new ConcurrentHashMap<>();
    private final Map<UUID, String> uuidToDiscord = new ConcurrentHashMap<>();

    public LinkManager(AlchemyPersona plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "links.yml");
        load();
    }

    public synchronized void link(String discordId, UUID uuid) {
        String old = uuidToDiscord.remove(uuid);
        if (old != null) {
            List<UUID> list = discordToUuids.get(old);
            if (list != null) {
                list.remove(uuid);
                if (list.isEmpty()) discordToUuids.remove(old);
            }
        }
        discordToUuids.computeIfAbsent(discordId, k -> new ArrayList<>()).add(uuid);
        uuidToDiscord.put(uuid, discordId);
        save();
    }

    public synchronized void unlink(UUID uuid) {
        String discordId = uuidToDiscord.remove(uuid);
        if (discordId == null) return;
        List<UUID> list = discordToUuids.get(discordId);
        if (list != null) {
            list.remove(uuid);
            if (list.isEmpty()) discordToUuids.remove(discordId);
        }
        save();
    }

    public List<UUID> getLinkedAccounts(String discordId) {
        return new ArrayList<>(discordToUuids.getOrDefault(discordId, List.of()));
    }

    public String getDiscordId(UUID uuid) {
        return uuidToDiscord.get(uuid);
    }

    public boolean isLinked(UUID uuid) {
        return uuidToDiscord.containsKey(uuid);
    }

    private void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        var section = cfg.getConfigurationSection("links");
        if (section == null) return;
        for (String discordId : section.getKeys(false)) {
            List<String> raw = cfg.getStringList("links." + discordId);
            List<UUID> uuids = new ArrayList<>();
            for (String s : raw) {
                try {
                    UUID u = UUID.fromString(s);
                    uuids.add(u);
                    uuidToDiscord.put(u, discordId);
                } catch (Exception ignored) {}
            }
            if (!uuids.isEmpty()) discordToUuids.put(discordId, uuids);
        }
        plugin.getLogger().info("Loaded " + uuidToDiscord.size() + " Discord account link(s).");
    }

    public synchronized void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (var e : discordToUuids.entrySet()) {
            List<String> raw = new ArrayList<>();
            for (UUID u : e.getValue()) raw.add(u.toString());
            cfg.set("links." + e.getKey(), raw);
        }
        try {
            cfg.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save links.yml: " + ex.getMessage());
        }
    }
}
