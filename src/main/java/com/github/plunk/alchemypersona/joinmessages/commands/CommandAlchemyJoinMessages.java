package com.github.plunk.alchemypersona.joinmessages.commands;

import com.github.plunk.alchemypersona.AlchemyPersona;
import com.github.plunk.alchemypersona.joinmessages.Data;
import com.github.plunk.alchemypersona.joinmessages.objects.JoinMessage;
import com.github.plunk.alchemypersona.joinmessages.utils.HexUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandAlchemyJoinMessages implements CommandExecutor, TabCompleter {

    private final AlchemyPersona plugin;

    public CommandAlchemyJoinMessages(AlchemyPersona plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("alchemypersona.admin")) {
                    sender.sendMessage(ChatColor.RED + "No Permission.");
                    return true;
                }
                plugin.reloadConfig();
                Data.reload();
                plugin.getMessageManager().loadMessages();
                plugin.getJoinMessagesGuiOptions().loadOptions();
                plugin.getJoinMessagesMenuManager().loadMenu();
                sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
                return true;
            }
            if (args[0].equalsIgnoreCase("gui")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can use GUI.");
                    return true;
                }
                plugin.getJoinMessagesMenuManager().openMenu((Player) sender, 0, "all");
                return true;
            }
            if (args[0].equalsIgnoreCase("check")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                Player p = (Player) sender;
                String selectedId = Data.get().getString("players." + p.getUniqueId().toString());

                if (selectedId == null) {
                    p.sendMessage(ChatColor.RED + "You have no join message selected.");
                    return true;
                }

                JoinMessage msg = plugin.getMessageManager().getMessage(selectedId);
                if (msg == null) {
                    p.sendMessage(ChatColor.RED + "Error: Your selected message '" + selectedId
                            + "' no longer exists in the config.");
                    return true;
                }

                p.sendMessage(ChatColor.GRAY + "Current Selection: " + ChatColor.YELLOW + msg.getDisplayName());
                p.sendMessage(ChatColor.GRAY + "Permission Node: " + ChatColor.YELLOW + msg.getPermission());

                if (p.hasPermission(msg.getPermission())) {
                    p.sendMessage(ChatColor.GREEN + "[Valid] You have permission for this message.");
                } else {
                    p.sendMessage(ChatColor.RED + "[Invalid] You do NOT have permission for this message.");
                }

                String preview = msg.getFormattedMessage(p.getName());
                preview = HexUtils.colorify(preview);
                p.sendMessage(ChatColor.GRAY + "Preview: " + preview);
                return true;
            }
            if (args[0].equalsIgnoreCase("give")) {
                if (!sender.hasPermission("alchemypersona.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /jm give <player> <messageId>");
                    return true;
                }

                String targetName = args[1];
                String messageId = args[2];

                org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(targetName);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + targetName);
                    return true;
                }

                JoinMessage msg = plugin.getMessageManager().getMessage(messageId);
                if (msg == null) {
                    sender.sendMessage(ChatColor.RED + "Join message '" + messageId + "' not found.");
                    return true;
                }

                // Grant the permission via LuckPerms command
                org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(),
                        "lp user " + target.getName() + " permission set " + msg.getPermission());

                // Save the selection so the message is active
                Data.get().set("players." + target.getUniqueId().toString(), messageId);
                Data.save();

                sender.sendMessage(ChatColor.GREEN + "Gave join message '" + messageId + "' to " + target.getName()
                        + " and set as active.");
                return true;
            }
        }

        // Default behavior (open GUI if player, help if console?)
        if (sender instanceof Player) {
            plugin.getJoinMessagesMenuManager().openMenu((Player) sender, 0, "all");
        } else {
            sender.sendMessage("Usage: /alchemypersona.admin");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("gui", "check"));
            if (sender.hasPermission("alchemypersona.admin"))
                subs.add("reload");
            if (sender.hasPermission("alchemypersona.admin"))
                subs.add("give");
            StringUtil.copyPartialMatches(args[0], subs, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            if (sender.hasPermission("alchemypersona.admin")) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    names.add(p.getName());
                }
                StringUtil.copyPartialMatches(args[1], names, completions);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            if (sender.hasPermission("alchemypersona.admin")) {
                StringUtil.copyPartialMatches(args[2], new ArrayList<>(plugin.getMessageManager().getMessageIds()),
                        completions);
            }
        }

        return completions;
    }
}
