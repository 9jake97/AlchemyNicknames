package com.github.plunk.alchemypersona.placeholders;

import com.github.plunk.alchemypersona.AlchemyPersona;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PersonaExpansion extends PlaceholderExpansion {

    private final AlchemyPersona plugin;

    public PersonaExpansion(AlchemyPersona plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("PersonaExpansion initialized and ready for registration.");
    }

    @Override
    public @NotNull String getIdentifier() {
        return "persona";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Plunk";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("check")) {
            return "Expansion is alive!";
        }

        if (player == null) return "";

        // Nicknames
        if (params.equalsIgnoreCase("nickname")) {
            if (plugin.getNicknameManager() == null) return player.getName();
            String nick = plugin.getNicknameManager().getNickname(player.getUniqueId());
            return nick != null ? nick : player.getName();
        }

        if (params.equalsIgnoreCase("displayname")) {
            if (plugin.getNicknameManager() == null) return player.getName();
            String nick = plugin.getNicknameManager().getNickname(player.getUniqueId());
            if (nick == null) return player.getName();
            Component component = plugin.getNicknameManager().parseNickname(nick);
            return LegacyComponentSerializer.legacySection().serialize(component);
        }

        // Tags
        if (params.equalsIgnoreCase("tag")) {
            if (plugin.getTagManager() == null) return "";
            return plugin.getTagManager().getPlayerTagDisplay(player.getUniqueId());
        }

        if (params.equalsIgnoreCase("tag_id")) {
            if (plugin.getTagManager() == null) return "";
            String id = plugin.getTagManager().getPlayerTagId(player.getUniqueId());
            return id != null ? id : "";
        }

        // Pins
        if (params.equalsIgnoreCase("pin")) {
            if (plugin.getPinManager() == null) return "";
            String pin = plugin.getPinManager().getCurrentPin(player.isOnline() ? player.getPlayer() : null);
            return pin != null ? pin : "";
        }

        return null;
    }
}
