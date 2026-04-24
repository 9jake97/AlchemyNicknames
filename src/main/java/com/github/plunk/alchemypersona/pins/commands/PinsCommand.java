package com.github.plunk.alchemypersona.pins.commands;

import com.github.plunk.alchemypersona.AlchemyPersona;
import com.github.plunk.alchemypersona.pins.managers.PinManager;
import com.github.plunk.alchemypersona.pins.menu.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PinsCommand implements CommandExecutor, TabCompleter {

    private final MenuManager menuManager;
    private final AlchemyPersona plugin;
    private final PinManager pinManager;

    public PinsCommand(MenuManager menuManager, PinManager pinManager) {
        this.menuManager = menuManager;
        this.plugin = menuManager.getPlugin();
        this.pinManager = pinManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Bukkit.getLogger().info("[AlchemyPersona DEBUG] /pins command execution started!");

        if (args.length > 0) {
            String subCommand = args[0].toLowerCase();

            // /pins reload
            if (subCommand.equals("reload")) {
                if (!sender.hasPermission("alchemypersona.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to reload.");
                    return true;
                }
                plugin.reload();
                sender.sendMessage(ChatColor.GREEN + "AlchemyPersona configuration reloaded.");
                return true;
            }

            // /pins give <player> <pin>
            if (subCommand.equals("give")) {
                if (!sender.hasPermission("alchemypersona.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to give pins.");
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /pins give <player> <pin>");
                    return true;
                }

                String targetName = args[1];
                String pinName = args[2];

                // Validate pin exists
                if (!isPinValid(pinName)) {
                    sender.sendMessage(ChatColor.RED + "Pin '" + pinName + "' not found!");
                    return true;
                }

                // Get target player (online or offline)
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                if (target == null || !target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage(ChatColor.RED + "Player '" + targetName + "' not found!");
                    return true;
                }

                // Grant permission
                boolean success = pinManager.grantPinPermission(target.getUniqueId(), pinName);
                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "Granted pin '" + pinName + "' to " + target.getName() + "!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to grant pin. Check console for errors.");
                }
                return true;
            }

            // /pins take <player> <pin>
            if (subCommand.equals("take")) {
                if (!sender.hasPermission("alchemypersona.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to take pins.");
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /pins take <player> <pin>");
                    return true;
                }

                String targetName = args[1];
                String pinName = args[2];

                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                if (target == null || !target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage(ChatColor.RED + "Player '" + targetName + "' not found!");
                    return true;
                }

                boolean success = pinManager.revokePinPermission(target.getUniqueId(), pinName);
                if (success) {
                    sender.sendMessage(
                            ChatColor.GREEN + "Revoked pin '" + pinName + "' from " + target.getName() + "!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to revoke pin. Check console for errors.");
                }
                return true;
            }

            // /pins clear - Remove player's current pin
            if (subCommand.equals("clear") || subCommand.equals("off") || subCommand.equals("remove")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Players only!");
                    return true;
                }
                Player player = (Player) sender;
                pinManager.clearPin(player);
                player.sendMessage(ChatColor.GREEN + "Your pin has been removed!");
                return true;
            }

            // /pins page <number>
            if (subCommand.equals("page") && args.length > 1) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Players only!");
                    return true;
                }
                Player player = (Player) sender;
                try {
                    int page = Integer.parseInt(args[1]) - 1;
                    if (page < 0)
                        page = 0;
                    menuManager.openMenu(player, page, "all");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid page number.");
                }
                return true;
            }

            // /pins <category> - open menu with category filter
            if (!(sender instanceof Player)) {
                sender.sendMessage("Players only!");
                return true;
            }
            Player player = (Player) sender;
            menuManager.openMenu(player, 0, subCommand);
            return true;
        }

        // /pins - open default menu
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        Player player = (Player) sender;
        player.sendMessage("Opening pins menu...");
        try {
            menuManager.openMenu(player, 0, "all");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[AlchemyPersona DEBUG] Error opening menu: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("Error opening menu: " + e.getMessage());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument: subcommands
            List<String> subCommands = new ArrayList<>();
            subCommands.add("reload");
            subCommands.add("page");
            subCommands.add("clear");
            if (sender.hasPermission("alchemypersona.admin")) {
                subCommands.add("give");
            }
            if (sender.hasPermission("alchemypersona.admin")) {
                subCommands.add("take");
            }
            String input = args[0].toLowerCase();
            completions = subCommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("give") || subCommand.equals("take")) {
                // Second argument for give/take: player names
                String input = args[1].toLowerCase();
                completions = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("give") || subCommand.equals("take")) {
                // Third argument: pin names
                String input = args[2].toLowerCase();
                completions = getPinNames().stream()
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }

    private List<String> getPinNames() {
        List<String> pinNames = new ArrayList<>();
        File pinsFile = new File(plugin.getDataFolder(), "pins.yml");
        Bukkit.getLogger().info("[AlchemyPersona TAB] Looking for pins.yml at: " + pinsFile.getAbsolutePath());
        Bukkit.getLogger().info("[AlchemyPersona TAB] File exists: " + pinsFile.exists());
        if (pinsFile.exists()) {
            FileConfiguration pinsConfig = YamlConfiguration.loadConfiguration(pinsFile);
            ConfigurationSection pinsSection = pinsConfig.getConfigurationSection("pins");
            if (pinsSection != null) {
                pinNames.addAll(pinsSection.getKeys(false));
                Bukkit.getLogger().info("[AlchemyPersona TAB] Found " + pinNames.size() + " pins");
            } else {
                Bukkit.getLogger().warning("[AlchemyPersona TAB] 'pins' section not found in pins.yml!");
            }
        } else {
            Bukkit.getLogger().warning("[AlchemyPersona TAB] pins.yml does not exist!");
        }
        return pinNames;
    }

    private boolean isPinValid(String pinName) {
        return getPinNames().contains(pinName);
    }
}
