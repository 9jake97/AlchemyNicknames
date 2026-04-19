package com.github.plunk.alchemynicknames.commands;

import com.github.plunk.alchemynicknames.AlchemyNicknames;
import com.github.plunk.alchemynicknames.managers.NicknameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class NicknameCommand implements CommandExecutor, TabCompleter {

    private final AlchemyNicknames plugin;
    private final NicknameManager nicknameManager;

    public NicknameCommand(AlchemyNicknames plugin, NicknameManager nicknameManager) {
        this.plugin = plugin;
        this.nicknameManager = nicknameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (label.equalsIgnoreCase("unnick")) {
            return handleUnnick(sender, args);
        }

        if (args.length == 0) {
            sender.sendMessage(parseMessage("messages.prefix")
                    .append(MiniMessage.miniMessage().deserialize("<red>Usage: /nick <name> or /nick <player> <name>")));
            return true;
        }

        Player target;
        String nick;

        if (args.length >= 2 && sender.hasPermission("alchemynicknames.others")) {
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(parseMessage("messages.prefix").append(parseMessage("messages.player-not-found")));
                return true;
            }
            nick = args[1];
        } else if (sender instanceof Player) {
            target = (Player) sender;
            nick = args[0];
            if (!target.hasPermission("alchemynicknames.nickname")) {
                sender.sendMessage(parseMessage("messages.prefix").append(parseMessage("messages.no-permission")));
                return true;
            }
        } else {
            sender.sendMessage(Component.text("Only players can set their own nicknames. Use /nick <player> <name> instead."));
            return true;
        }

        // Validation
        if (!validateNickname(sender, nick)) {
            return true;
        }

        nicknameManager.setNickname(target.getUniqueId(), nick);
        
        Component nickComp = nicknameManager.parseNickname(nick);
        target.sendMessage(parseMessage("messages.prefix").append(parseMessage("messages.nickname-set").replaceText(builder -> builder.matchLiteral("%nickname%").replacement(nickComp))));
        
        if (target != sender) {
            sender.sendMessage(parseMessage("messages.prefix").append(parseMessage("messages.nickname-others-set")
                    .replaceText(builder -> builder.matchLiteral("%player%").replacement(target.getName()))
                    .replaceText(builder -> builder.matchLiteral("%nickname%").replacement(nickComp))));
        }

        return true;
    }

    private boolean handleUnnick(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 1 && sender.hasPermission("alchemynicknames.others")) {
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(parseMessage("messages.prefix").append(parseMessage("messages.player-not-found")));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(Component.text("Only players can reset their own nicknames."));
            return true;
        }

        nicknameManager.setNickname(target.getUniqueId(), null);
        target.sendMessage(parseMessage("messages.prefix").append(parseMessage("messages.nickname-reset")));
        
        if (target != sender) {
            sender.sendMessage(parseMessage("messages.prefix").append(Component.text("Reset nickname for " + target.getName())));
        }
        return true;
    }

    private boolean validateNickname(CommandSender sender, String nick) {
        // Translate hex first to unify
        String unified = nick.replaceAll("&#([A-Fa-f0-9]{6})", "<#$1>");
        // Strip MiniMessage tags
        String plain = MiniMessage.miniMessage().stripTags(unified);
        // Strip legacy colors
        plain = plain.replaceAll("&[0-9a-fk-orx]", "");
        
        int min = plugin.getConfig().getInt("nickname.min-length", 3);
        int max = plugin.getConfig().getInt("nickname.max-length", 16);

        if (plain.length() < min) {
            sender.sendMessage(parseMessage("messages.prefix").append(parseMessage("messages.too-short").replaceText(b -> b.matchLiteral("%min%").replacement(String.valueOf(min)))));
            return false;
        }
        if (plain.length() > max) {
            sender.sendMessage(parseMessage("messages.prefix").append(parseMessage("messages.too-long").replaceText(b -> b.matchLiteral("%max%").replacement(String.valueOf(max)))));
            return false;
        }

        List<String> blacklist = plugin.getConfig().getStringList("nickname.blacklist");
        for (String word : blacklist) {
            if (Pattern.compile(word, Pattern.CASE_INSENSITIVE).matcher(plain).find()) {
                sender.sendMessage(parseMessage("messages.prefix").append(parseMessage("messages.blacklisted")));
                return false;
            }
        }

        return true;
    }

    private Component parseMessage(String path) {
        String msg = plugin.getConfig().getString(path, "");
        return MiniMessage.miniMessage().deserialize(msg);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("alchemynicknames.others")) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }
}
