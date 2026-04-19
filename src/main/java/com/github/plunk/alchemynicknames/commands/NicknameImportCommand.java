package com.github.plunk.alchemynicknames.commands;

import com.github.plunk.alchemynicknames.AlchemyNicknames;
import com.github.plunk.alchemynicknames.managers.NicknameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NicknameImportCommand implements CommandExecutor, TabCompleter {

    private final AlchemyNicknames plugin;
    private final NicknameManager nicknameManager;

    public NicknameImportCommand(AlchemyNicknames plugin, NicknameManager nicknameManager) {
        this.plugin = plugin;
        this.nicknameManager = nicknameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("alchemynicknames.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /nickimport <essentials|cmi>", NamedTextColor.RED));
            return true;
        }

        String source = args[0].toLowerCase();
        switch (source) {
            case "essentials":
                importEssentials(sender);
                break;
            case "cmi":
                importCMI(sender);
                break;
            default:
                sender.sendMessage(Component.text("Unknown source. Use 'essentials' or 'cmi'.", NamedTextColor.RED));
                break;
        }

        return true;
    }

    private void importEssentials(CommandSender sender) {
        File essentialsFolder = new File(plugin.getDataFolder().getParentFile(), "Essentials/userdata");
        if (!essentialsFolder.exists() || !essentialsFolder.isDirectory()) {
            sender.sendMessage(Component.text("Essentials userdata folder not found at " + essentialsFolder.getPath(), NamedTextColor.RED));
            return;
        }

        File[] files = essentialsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            sender.sendMessage(Component.text("No Essentials userdata files found.", NamedTextColor.RED));
            return;
        }

        int count = 0;
        sender.sendMessage(Component.text("Starting Essentials nickname import...", NamedTextColor.YELLOW));

        for (File file : files) {
            try {
                String fileName = file.getName().replace(".yml", "");
                UUID uuid = UUID.fromString(fileName);
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String nick = config.getString("nickname");

                if (nick != null && !nick.isEmpty()) {
                    // Essentials often prefixes nicknames with § or other characters, 
                    // and uses & for colors. AlchemyNicknames handles & via LegacyComponentSerializer.
                    nicknameManager.setNickname(uuid, nick);
                    count++;
                }
            } catch (Exception ignored) {
                // Skip invalid files or UUIDs
            }
        }

        nicknameManager.saveNicknames();
        sender.sendMessage(Component.text("Successfully imported " + count + " nicknames from Essentials!", NamedTextColor.GREEN));
    }

    private void importCMI(CommandSender sender) {
        File cmiFolder = new File(plugin.getDataFolder().getParentFile(), "CMI/Users");
        if (!cmiFolder.exists() || !cmiFolder.isDirectory()) {
            sender.sendMessage(Component.text("CMI Users folder not found at " + cmiFolder.getPath(), NamedTextColor.RED));
            return;
        }

        File[] files = cmiFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            sender.sendMessage(Component.text("No CMI user files found.", NamedTextColor.RED));
            return;
        }

        int count = 0;
        sender.sendMessage(Component.text("Starting CMI nickname import...", NamedTextColor.YELLOW));

        for (File file : files) {
            try {
                String fileName = file.getName().replace(".yml", "");
                UUID uuid = UUID.fromString(fileName);
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String nick = config.getString("Nickname"); // CMI uses capital N

                if (nick != null && !nick.isEmpty()) {
                    nicknameManager.setNickname(uuid, nick);
                    count++;
                }
            } catch (Exception ignored) {
            }
        }

        nicknameManager.saveNicknames();
        sender.sendMessage(Component.text("Successfully imported " + count + " nicknames from CMI!", NamedTextColor.GREEN));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> sources = new ArrayList<>();
            sources.add("essentials");
            sources.add("cmi");
            return sources;
        }
        return new ArrayList<>();
    }
}
