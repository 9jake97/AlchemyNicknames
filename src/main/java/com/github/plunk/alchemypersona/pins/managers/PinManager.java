package com.github.plunk.alchemypersona.pins.managers;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.SuffixNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.github.plunk.alchemypersona.AlchemyPersona;

public class PinManager {

    private final LuckPerms luckPerms;
    private final AlchemyPersona plugin;

    public PinManager(AlchemyPersona plugin) {
        this.plugin = plugin;
        this.luckPerms = LuckPermsProvider.get();
    }

    private int getPriority() {
        return plugin.getPinsConfig().getInt("luckperms_priority", 200);
    }

    private String getContextString() {
        return plugin.getPinsConfig().getString("luckperms_context", "");
    }

    public void setPin(Player player, String pinUnicode) {
        Bukkit.getLogger()
                .info("[AlchemyPersona DEBUG] setPin called for " + player.getName() + " with unicode: " + pinUnicode);
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            Bukkit.getLogger().warning("[AlchemyPersona DEBUG] User not found in LuckPerms!");
            return;
        }

        clearPin(player);

        int priority = getPriority();
        String contextStr = getContextString();

        net.luckperms.api.node.NodeBuilder<?, ?> builder = SuffixNode.builder(pinUnicode + " ", priority);

        if (!contextStr.isEmpty()) {
            String[] parts = contextStr.split("=");
            if (parts.length == 2) {
                builder.withContext(parts[0], parts[1]);
                Bukkit.getLogger().info("[AlchemyPersona DEBUG] Applying context: " + parts[0] + "=" + parts[1]);
            }
        }

        Node node = builder.build();
        user.data().add(node);
        Bukkit.getLogger()
                .info("[AlchemyPersona DEBUG] Added suffix node: " + node.getKey() + " with priority " + priority);

        luckPerms.getUserManager().saveUser(user);
        Bukkit.getLogger().info("[AlchemyPersona DEBUG] User saved.");
    }

    /**
     * Get the player's current pin suffix (if any).
     * 
     * @return The current pin unicode string, or null if no pin is set.
     */
    public String getCurrentPin(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null)
            return null;

        int targetPriority = getPriority();

        for (Node node : user.getNodes()) {
            if (node.getType() == net.luckperms.api.node.NodeType.SUFFIX) {
                SuffixNode suffixNode = (SuffixNode) node;
                if (suffixNode.getPriority() == targetPriority) {
                    // Return the suffix without trailing space
                    String suffix = suffixNode.getMetaValue();
                    return suffix != null ? suffix.trim() : null;
                }
            }
        }
        return null;
    }

    public void clearPin(Player player) {
        Bukkit.getLogger().info("[AlchemyPersona DEBUG] clearPin called for " + player.getName());
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null)
            return;

        int targetPriority = getPriority();

        user.data().clear(node -> {
            if (node.getType() == net.luckperms.api.node.NodeType.SUFFIX) {
                SuffixNode suffixNode = (SuffixNode) node;
                if (suffixNode.getPriority() == targetPriority) {
                    Bukkit.getLogger().info("[AlchemyPersona DEBUG] Removing old suffix node: " + node.getKey());
                    return true;
                }
            }
            return false;
        });

        luckPerms.getUserManager().saveUser(user);
    }

    /**
     * Grant pin permission to a player (allows them to use the pin in the GUI).
     * 
     * @param uuid    The player's UUID
     * @param pinName The pin name (used to build permission LPP.pin.pinName)
     * @return true if successful, false if user not found
     */
    public boolean grantPinPermission(java.util.UUID uuid, String pinName) {
        String permission = "LPP.pin." + pinName;
        Bukkit.getLogger().info("[AlchemyPersona DEBUG] Granting permission " + permission + " to " + uuid);

        return luckPerms.getUserManager().loadUser(uuid).thenApply(user -> {
            if (user == null) {
                Bukkit.getLogger().warning("[AlchemyPersona DEBUG] User not found: " + uuid);
                return false;
            }

            Node node = Node.builder(permission).build();
            user.data().add(node);
            luckPerms.getUserManager().saveUser(user);
            Bukkit.getLogger().info("[AlchemyPersona DEBUG] Permission granted successfully.");
            return true;
        }).join();
    }

    /**
     * Revoke pin permission from a player.
     * 
     * @param uuid    The player's UUID
     * @param pinName The pin name
     * @return true if successful
     */
    public boolean revokePinPermission(java.util.UUID uuid, String pinName) {
        String permission = "LPP.pin." + pinName;
        Bukkit.getLogger().info("[AlchemyPersona DEBUG] Revoking permission " + permission + " from " + uuid);

        return luckPerms.getUserManager().loadUser(uuid).thenApply(user -> {
            if (user == null) {
                return false;
            }

            Node node = Node.builder(permission).build();
            user.data().remove(node);
            luckPerms.getUserManager().saveUser(user);
            Bukkit.getLogger().info("[AlchemyPersona DEBUG] Permission revoked successfully.");
            return true;
        }).join();
    }
}
