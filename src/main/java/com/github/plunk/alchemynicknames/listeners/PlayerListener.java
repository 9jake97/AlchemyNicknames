package com.github.plunk.alchemynicknames.listeners;

import com.github.plunk.alchemynicknames.AlchemyNicknames;
import com.github.plunk.alchemynicknames.managers.NicknameManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    private final AlchemyNicknames plugin;
    private final NicknameManager nicknameManager;

    public PlayerListener(AlchemyNicknames plugin, NicknameManager nicknameManager) {
        this.plugin = plugin;
        this.nicknameManager = nicknameManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        // We no longer set the display name directly to avoid overwriting prefixes/suffixes.
        // Nicknames should be displayed using the %alchemynicknames_displayname% placeholder.
    }

    // Removed onChat renderer to prevent interference with dedicated chat plugins.
    // The nickname will still show up if the chat plugin uses player.getDisplayName() 
    // or the %alchemynicknames_displayname% placeholder.
}
