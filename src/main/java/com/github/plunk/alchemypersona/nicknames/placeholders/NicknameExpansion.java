package com.github.plunk.alchemypersona.nicknames.placeholders;

import com.github.plunk.alchemypersona.AlchemyPersona;
import com.github.plunk.alchemypersona.nicknames.managers.NicknameManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class NicknameExpansion extends PlaceholderExpansion {

    private final AlchemyPersona plugin;
    private final NicknameManager nicknameManager;

    public NicknameExpansion(AlchemyPersona plugin, NicknameManager nicknameManager) {
        this.plugin = plugin;
        this.nicknameManager = nicknameManager;
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

        if (params.equalsIgnoreCase("nickname")) {
            String nick = nicknameManager.getNickname(player.getUniqueId());
            return nick != null ? nick : player.getName();
        }

        if (params.equalsIgnoreCase("displayname")) {
            String nick = nicknameManager.getNickname(player.getUniqueId());
            if (nick == null) return player.getName();
            
            Component component = nicknameManager.parseNickname(nick);
            // Convert to legacy section (§) for PAPI/other plugins
            return LegacyComponentSerializer.legacySection().serialize(component);
        }

        return null;
    }
}
