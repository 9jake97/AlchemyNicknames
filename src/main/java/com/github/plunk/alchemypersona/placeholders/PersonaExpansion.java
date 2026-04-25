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
        if (player == null) return "";

        // Nicknames
        if (params.equalsIgnoreCase("nickname")) {
            String nick = plugin.getNicknameManager().getNickname(player.getUniqueId());
            return nick != null ? nick : player.getName();
        }

        if (params.equalsIgnoreCase("displayname")) {
            String nick = plugin.getNicknameManager().getNickname(player.getUniqueId());
            if (nick == null) return player.getName();
            Component component = plugin.getNicknameManager().parseNickname(nick);
            return LegacyComponentSerializer.legacySection().serialize(component);
        }

        // Tags
        if (params.equalsIgnoreCase("tag")) {
            return plugin.getTagManager().getPlayerTagDisplay(player.getUniqueId());
        }

        if (params.equalsIgnoreCase("tag_id")) {
            String id = plugin.getTagManager().getPlayerTagId(player.getUniqueId());
            return id != null ? id : "";
        }

        // Pins
        if (params.equalsIgnoreCase("pin")) {
            String pin = plugin.getPinManager().getCurrentPin(player.isOnline() ? player.getPlayer() : null);
            return pin != null ? pin : "";
        }

        return null;
    }
}
