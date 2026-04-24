package com.github.plunk.alchemypersona.joinmessages.managers;

import com.github.plunk.alchemypersona.AlchemyPersona;
import com.github.plunk.alchemypersona.joinmessages.objects.JoinMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class MessageManager {

    private final AlchemyPersona plugin;
    private final Map<String, JoinMessage> validMessages = new HashMap<>();

    public MessageManager(AlchemyPersona plugin) {
        this.plugin = plugin;
    }

    public void loadMessages() {
        validMessages.clear();
        FileConfiguration config = plugin.getJoinMessagesConfig();
        if (!config.isConfigurationSection("messages")) {
            return; // No messages configured
        }

        ConfigurationSection section = config.getConfigurationSection("messages");
        for (String key : section.getKeys(false)) {
            String path = "messages." + key + ".";
            int priority = config.getInt(path + "order", 0);
            String text = config.getString(path + "message");
            if (text == null)
                text = config.getString(path + "text");
            if (text == null)
                text = config.getString(path + "join_message");
            if (text == null)
                text = config.getString(path + "join-message");
            if (text == null)
                text = config.getString(path + "kjoin_message");
            if (text == null)
                text = config.getString(path + "kjoin-message");
            if (text == null)
                text = "";

            String permission = config.getString(path + "permission", "cjm.message." + key);

            JoinMessage msg = new JoinMessage(key, priority, text, permission);

            // GUI visual config
            String materialName = section.getString("gui.material", "NAME_TAG");
            Material mat = Material.getMaterial(materialName);
            msg.setIconMaterial(mat != null ? mat : Material.NAME_TAG);
            msg.setIconData((short) config.getInt(path + "icon_data", 0));

            // Load theme color with multiple field candidates
            String color = config.getString(path + "color");
            if (color == null)
                color = config.getString(path + "theme_color");
            if (color == null)
                color = config.getString(path + "theme-color");
            if (color == null)
                color = config.getString(path + "theme");
            msg.setThemeColor(color);

            List<String> desc = config.getStringList(path + "description");
            if (desc == null || desc.isEmpty()) {
                // Check for hover candidates: 'text', 'join_message', 'join-message'
                String hoverText = config.getString(path + "text");
                if (hoverText == null)
                    hoverText = config.getString(path + "join_message");
                if (hoverText == null)
                    hoverText = config.getString(path + "join-message");
                if (hoverText == null)
                    hoverText = config.getString(path + "kjoin_message");
                if (hoverText == null)
                    hoverText = config.getString(path + "kjoin-message");

                if (hoverText != null) {
                    desc = Collections.singletonList(hoverText);
                } else {
                    // Fallback to formatted message preview
                    desc = Collections.singletonList(msg.getFormattedMessage("%player%"));
                }
            }
            msg.setDescription(desc);

            validMessages.put(key, msg);
        }
    }

    public Collection<JoinMessage> getLoadedMessages() {
        return validMessages.values();
    }

    public JoinMessage getMessage(String identifier) {
        return validMessages.get(identifier);
    }

    public List<String> getAvailableMessageIdentifiers(org.bukkit.entity.Player player) {
        List<String> available = new ArrayList<>();
        for (JoinMessage msg : validMessages.values()) {
            if (player.hasPermission(msg.getPermission())) {
                available.add(msg.getIdentifier());
            }
        }
        return available;
    }

    public Set<String> getMessageIds() {
        return validMessages.keySet();
    }
}
