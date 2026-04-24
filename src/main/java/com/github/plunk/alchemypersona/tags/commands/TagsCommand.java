package com.github.plunk.alchemypersona.tags.commands;

import com.github.plunk.alchemypersona.AlchemyPersona;
import com.github.plunk.alchemypersona.tags.managers.TagManager;
import com.github.plunk.alchemypersona.tags.menu.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TagsCommand implements TabExecutor {

    private final AlchemyPersona plugin;
    private final MenuManager menuManager;
    private final TagManager tagManager;

    public TagsCommand(AlchemyPersona plugin, MenuManager menuManager, TagManager tagManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
        this.tagManager = tagManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("alchemypersona.admin")) {
                plugin.reload();
                sender.sendMessage(ChatColor.GREEN + "AlchemyPersona configuration reloaded.");
            } else {
                sender.sendMessage(ChatColor.RED + "No permission.");
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("alchemypersona.admin")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /tags give <player> <tagId>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            String tagId = args[2];
            tagManager.grantTagPermission(target, tagId).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "Granted tag " + tagId + " to " + target.getName());
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to grant tag. Check if tag ID exists.");
                }
            });
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        Player player = (Player) sender;
        int page = 0;
        String category = "all";

        if (args.length > 0) {
            if (args[0].matches("\\d+")) {
                try {
                    page = Integer.parseInt(args[0]) - 1;
                } catch (Exception ignored) {
                }
            } else {
                category = args[0];
            }
        }

        menuManager.openMenu(player, page, category);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>(Arrays.asList("reload", "give"));
            // Add categories if it's a player
            if (sender instanceof Player) {
                // You could add categories here if helpful
            }
            return StringUtil.copyPartialMatches(args[0], subcommands, new ArrayList<>());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return null; // Standard player name completion
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> tags = new ArrayList<>(
                    tagManager.getTagsConfig().getConfigurationSection("tags").getKeys(false));
            return StringUtil.copyPartialMatches(args[2], tags, new ArrayList<>());
        }

        return Collections.emptyList();
    }
}
