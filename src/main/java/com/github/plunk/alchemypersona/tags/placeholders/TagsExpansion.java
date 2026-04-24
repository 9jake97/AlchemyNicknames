package com.github.plunk.alchemypersona.tags.placeholders;

import com.github.plunk.alchemypersona.AlchemyPersona;
import com.github.plunk.alchemypersona.tags.managers.TagManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class TagsExpansion extends PlaceholderExpansion {

    private final TagManager tagManager;

    public TagsExpansion(AlchemyPersona plugin, TagManager tagManager) {
        this.tagManager = tagManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "tags";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Plunk";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null)
            return "";

        if (params.equalsIgnoreCase("tag")) {
            return tagManager.getPlayerTagDisplay(player.getUniqueId());
        }

        if (params.equalsIgnoreCase("identifier")) {
            String id = tagManager.getPlayerTagId(player.getUniqueId());
            return id != null ? id : "";
        }

        return null;
    }
}
