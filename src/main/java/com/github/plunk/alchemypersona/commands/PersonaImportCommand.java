package com.github.plunk.alchemypersona.commands;

import com.github.plunk.alchemypersona.AlchemyPersona;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PersonaImportCommand implements CommandExecutor, TabCompleter {

    private final AlchemyPersona plugin;

    public PersonaImportCommand(AlchemyPersona plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("alchemypersona.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /personaimport <legacy|joinmessages|tags|essentials|cmi|all>", NamedTextColor.RED));
            return true;
        }

        String source = args[0].toLowerCase();
        switch (source) {
            case "legacy":
                importLegacyNicknames(sender);
                break;
            case "joinmessages":
                importJoinMessages(sender);
                break;
            case "tags":
                importTags(sender);
                break;
            case "essentials":
                importEssentials(sender);
                break;
            case "cmi":
                importCMI(sender);
                break;
            case "all":
                importLegacyNicknames(sender);
                importJoinMessages(sender);
                importTags(sender);
                sender.sendMessage(Component.text("Bulk import complete!", NamedTextColor.GREEN));
                break;
            default:
                sender.sendMessage(Component.text("Unknown source.", NamedTextColor.RED));
                break;
        }

        return true;
    }

    private void importLegacyNicknames(CommandSender sender) {
        File oldFile = new File(plugin.getDataFolder().getParentFile(), "AlchemyNicknames/player_tags.yml");
        if (!oldFile.exists()) {
            sender.sendMessage(Component.text("Legacy nicknames file not found at " + oldFile.getPath(), NamedTextColor.YELLOW));
            return;
        }

        FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldFile);
        int count = 0;
        for (String key : oldConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String nick = oldConfig.getString(key);
                plugin.getNicknameManager().setNickname(uuid, nick);
                count++;
            } catch (Exception ignored) {}
        }
        sender.sendMessage(Component.text("Imported " + count + " nicknames from legacy AlchemyNicknames.", NamedTextColor.GREEN));
    }

    private void importJoinMessages(CommandSender sender) {
        File oldFile = new File(plugin.getDataFolder().getParentFile(), "AlchemyJoinMessages/data.yml");
        if (!oldFile.exists()) {
            sender.sendMessage(Component.text("Legacy JoinMessages file not found.", NamedTextColor.YELLOW));
            return;
        }

        FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldFile);
        if (!oldConfig.contains("players")) return;

        int count = 0;
        FileConfiguration newConfig = com.github.plunk.alchemypersona.joinmessages.Data.get();
        for (String uuidStr : oldConfig.getConfigurationSection("players").getKeys(false)) {
            String val = oldConfig.getString("players." + uuidStr);
            newConfig.set("players." + uuidStr, val);
            count++;
        }
        com.github.plunk.alchemypersona.joinmessages.Data.save();
        sender.sendMessage(Component.text("Imported " + count + " join message selections.", NamedTextColor.GREEN));
    }

    private void importTags(CommandSender sender) {
        File oldFile = new File(plugin.getDataFolder().getParentFile(), "AlchemyTags/player_tags.yml");
        if (!oldFile.exists()) {
            sender.sendMessage(Component.text("Legacy Tags file not found.", NamedTextColor.YELLOW));
            return;
        }

        FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldFile);
        int count = 0;
        for (String key : oldConfig.getKeys(false)) {
            try {
                // AlchemyTags format: UUID: tagId
                String tagId = oldConfig.getString(key);
                UUID uuid = UUID.fromString(key);
                org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(uuid);
                plugin.getTagManager().setTag(op.getPlayer(), tagId);
                count++;
            } catch (Exception ignored) {}
        }
        sender.sendMessage(Component.text("Imported " + count + " tag selections.", NamedTextColor.GREEN));
    }

    private void importEssentials(CommandSender sender) {
        File essentialsFolder = new File(plugin.getDataFolder().getParentFile(), "Essentials/userdata");
        if (!essentialsFolder.exists()) return;
        File[] files = essentialsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        int count = 0;
        for (File file : files) {
            try {
                UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                String nick = YamlConfiguration.loadConfiguration(file).getString("nickname");
                if (nick != null && !nick.isEmpty()) {
                    plugin.getNicknameManager().setNickname(uuid, nick);
                    count++;
                }
            } catch (Exception ignored) {}
        }
        sender.sendMessage(Component.text("Imported " + count + " nicknames from Essentials.", NamedTextColor.GREEN));
    }

    private void importCMI(CommandSender sender) {
        File cmiFolder = new File(plugin.getDataFolder().getParentFile(), "CMI/Users");
        if (!cmiFolder.exists()) return;
        File[] files = cmiFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        int count = 0;
        for (File file : files) {
            try {
                UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                String nick = YamlConfiguration.loadConfiguration(file).getString("Nickname");
                if (nick != null && !nick.isEmpty()) {
                    plugin.getNicknameManager().setNickname(uuid, nick);
                    count++;
                }
            } catch (Exception ignored) {}
        }
        sender.sendMessage(Component.text("Imported " + count + " nicknames from CMI.", NamedTextColor.GREEN));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("legacy", "joinmessages", "tags", "essentials", "cmi", "all");
        }
        return List.of();
    }
}
