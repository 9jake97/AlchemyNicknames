package com.github.plunk.alchemypersona.joinmessages.listeners;

import com.github.plunk.alchemypersona.AlchemyPersona;
import com.github.plunk.alchemypersona.joinmessages.Data;
import com.github.plunk.alchemypersona.joinmessages.objects.JoinMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class JoinListener implements Listener {

    private final AlchemyPersona plugin;

    public JoinListener(AlchemyPersona plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        // 1. Check for saved selection
        String selectedId = Data.get().getString("players." + p.getUniqueId().toString());

        if (selectedId != null) {
            JoinMessage msg = plugin.getMessageManager().getMessage(selectedId);

            // Validate message exists and permission
            if (msg != null && p.hasPermission(msg.getPermission())) {
                // Use centralized formatting logic
                String text = msg.getFormattedMessage(p.getName());
                text = com.github.plunk.alchemypersona.joinmessages.utils.HexUtils.colorify(text);

                if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, text);
                }

                // Create the base message
                TextComponent message = new TextComponent(
                        TextComponent.fromLegacyText(text));

                // Hover Interaction: Show the exact broadcast message as requested
                message.setHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new Text(
                                        TextComponent.fromLegacyText(text))));

                // Clear default and broadcast
                e.setJoinMessage(null);
                plugin.getServer().spigot().broadcast(message);
                return;
            }
        }
    }
}
