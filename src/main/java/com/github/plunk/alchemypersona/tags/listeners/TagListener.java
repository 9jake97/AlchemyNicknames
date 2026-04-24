package com.github.plunk.alchemypersona.tags.listeners;

import com.github.plunk.alchemypersona.AlchemyPersona;
import com.github.plunk.alchemypersona.tags.managers.TagManager;
import com.github.plunk.alchemypersona.tags.utils.ColorUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TagListener implements Listener {

    private final AlchemyPersona plugin;
    private final TagManager tagManager;

    public TagListener(AlchemyPersona plugin, TagManager tagManager) {
        this.plugin = plugin;
        this.tagManager = tagManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        tagManager.loadPlayerTag(event.getPlayer().getUniqueId());
    }
}
