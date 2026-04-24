package com.github.plunk.alchemypersona;

import com.github.plunk.alchemypersona.nicknames.commands.NicknameCommand;
import com.github.plunk.alchemypersona.commands.PersonaImportCommand;
import com.github.plunk.alchemypersona.nicknames.listeners.PlayerListener;
import com.github.plunk.alchemypersona.nicknames.managers.NicknameManager;
import com.github.plunk.alchemypersona.nicknames.placeholders.NicknameExpansion;

import com.github.plunk.alchemypersona.pins.commands.PinsCommand;
import com.github.plunk.alchemypersona.pins.managers.PinManager;
import com.github.plunk.alchemypersona.pins.menu.MenuManager;

import com.github.plunk.alchemypersona.tags.managers.TagManager;
import com.github.plunk.alchemypersona.tags.placeholders.TagsExpansion;

import com.github.plunk.alchemypersona.joinmessages.managers.MessageManager;
import com.github.plunk.alchemypersona.joinmessages.gui.MessageMenuManager;
import com.github.plunk.alchemypersona.joinmessages.gui.GUIOptions;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

public class AlchemyPersona extends JavaPlugin {

    private static AlchemyPersona instance;
    
    // Nickname Module
    private NicknameManager nicknameManager;
    private io.javalin.Javalin server;
    private record Session(String uuid, long expiresAt) {}
    private static final long SESSION_TTL_MS = 10 * 60 * 1000L;
    private final java.util.Map<String, Session> sessions = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Random random = new java.util.Random();

    // Pins Module
    private PinManager pinManager;
    private MenuManager pinsMenuManager;

    // Tags Module
    private TagManager tagManager;
    private com.github.plunk.alchemypersona.tags.menu.MenuManager tagsMenuManager;

    // Join Messages Module
    private MessageManager messageManager;
    private MessageMenuManager joinMessagesMenuManager;
    private GUIOptions joinMessagesGuiOptions;
    private java.util.Map<String, String> nexoMapping = new java.util.HashMap<>();

    // Configs
    private FileConfiguration nicknamesConfig;
    private FileConfiguration pinsConfig;
    private FileConfiguration tagsConfig;
    private FileConfiguration joinMessagesConfig;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("nicknames.yml", false);
        saveResource("pins.yml", false);
        saveResource("tags.yml", false);
        saveResource("join_messages.yml", false);
        saveResource("pins_menu.yml", false);
        saveResource("tags_menu.yml", false);
        saveResource("join_messages_menu.yml", false);

        loadModuleConfigs();

        // 1. Initialize Nicknames
        nicknameManager = new NicknameManager(this);
        nicknameManager.loadNicknames();
        registerNicknameCommands();
        registerImportCommand();
        getServer().getPluginManager().registerEvents(new PlayerListener(this, nicknameManager), this);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NicknameExpansion(this, nicknameManager).register();
        }
        startWebServer();
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> sessions.entrySet().removeIf(e -> System.currentTimeMillis() > e.getValue().expiresAt()),
                6000L, 6000L);

        // 2. Initialize Pins
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            this.pinManager = new PinManager(this);
            this.pinsMenuManager = new MenuManager(this, pinManager);
            PinsCommand pinsExecutor = new PinsCommand(pinsMenuManager, pinManager);
            getCommand("pins").setExecutor(pinsExecutor);
            getCommand("pins").setTabCompleter(pinsExecutor);
            getServer().getPluginManager().registerEvents(pinsMenuManager, this);
        }

        // 3. Initialize Tags
        this.tagManager = new TagManager(this);
        this.tagsMenuManager = new com.github.plunk.alchemypersona.tags.menu.MenuManager(this, tagManager);
        com.github.plunk.alchemypersona.tags.commands.TagsCommand tagsCommand = 
            new com.github.plunk.alchemypersona.tags.commands.TagsCommand(this, tagsMenuManager, tagManager);
        getCommand("tags").setExecutor(tagsCommand);
        getCommand("tags").setTabCompleter(tagsCommand);
        getServer().getPluginManager().registerEvents(new com.github.plunk.alchemypersona.tags.listeners.TagListener(this, tagManager), this);
        getServer().getPluginManager().registerEvents(tagsMenuManager, this);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TagsExpansion(this, tagManager).register();
        }

        // 4. Initialize Join Messages
        com.github.plunk.alchemypersona.joinmessages.Data.setup(this);
        this.messageManager = new MessageManager(this);
        this.messageManager.loadMessages();
        this.joinMessagesGuiOptions = new GUIOptions(this);
        this.joinMessagesMenuManager = new MessageMenuManager(this);
        getServer().getPluginManager().registerEvents(this.joinMessagesMenuManager, this);
        getServer().getPluginManager().registerEvents(new com.github.plunk.alchemypersona.joinmessages.listeners.JoinListener(this), this);
        com.github.plunk.alchemypersona.joinmessages.commands.CommandAlchemyJoinMessages ajmHandler = 
            new com.github.plunk.alchemypersona.joinmessages.commands.CommandAlchemyJoinMessages(this);
        getCommand("alchemyjoinmessages").setExecutor(ajmHandler);
        getCommand("alchemyjoinmessages").setTabCompleter(ajmHandler);

        loadNexoGlyphs();

        getLogger().info("AlchemyPersona v" + getDescription().getVersion() + " has been enabled!");
    }

    private void registerNicknameCommands() {
        NicknameCommand nicknameExecutor = new NicknameCommand(this, nicknameManager);
        getCommand("nickname").setExecutor(nicknameExecutor);
        getCommand("nickname").setTabCompleter(nicknameExecutor);
        getCommand("unnick").setExecutor(nicknameExecutor);
        getCommand("unnick").setTabCompleter(nicknameExecutor);
        getCommand("nicknameeditor").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("§cOnly players can use this command.");
                return true;
            }

            String token;
            do { token = String.format("%05d", random.nextInt(100000)); } while (sessions.containsKey(token));
            sessions.put(token, new Session(player.getUniqueId().toString(), System.currentTimeMillis() + SESSION_TTL_MS));

            String editorUrl = getConfig().getString("web.editor-url", "https://plunk.github.io/AlchemyPersona");
            String apiBase = getConfig().getString("web.base-url", "https://stats.bloc.kz");
            if (editorUrl.endsWith("/")) editorUrl = editorUrl.substring(0, editorUrl.length() - 1);
            if (apiBase.endsWith("/")) apiBase = apiBase.substring(0, apiBase.length() - 1);
            String link = editorUrl + "/?player=" + player.getName() + "&token=" + token + "&api=" + apiBase;

            var mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

            player.sendMessage(mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"));
            player.sendMessage(mm.deserialize("      <gradient:#FF0080:#8000FF><bold>✦ Persona Designer ✦</bold></gradient>"));
            player.sendMessage(mm.deserialize(" <dark_gray>»</dark_gray> <gray>Design your own <gradient:#FF0080:#8000FF>RGB gradient</gradient> identity</gray>"));
            player.sendMessage(mm.deserialize(" <dark_gray>»</dark_gray> <gray>Your session expires in <white>10 minutes</white></gray>"));
            player.sendMessage(net.kyori.adventure.text.Component.empty());
            player.sendMessage(mm.deserialize("      <click:open_url:'" + link + "'><hover:show_text:'<gray>Opens in your browser'><gradient:#00c6ff:#8000FF><bold>[  ✦ Open Designer  ]</bold></gradient></hover></click>"));
            player.sendMessage(net.kyori.adventure.text.Component.empty());
            player.sendMessage(mm.deserialize(" <dark_gray>»</dark_gray> <gray>Bedrock / can't click? Visit the site and enter:</gray>"));
            player.sendMessage(mm.deserialize("   <dark_gray>Name: </dark_gray><white>" + player.getName() + "</white>   <dark_gray>Code: </dark_gray><gradient:#00c6ff:#8000FF><bold>" + token + "</bold></gradient>"));
            player.sendMessage(mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"));
            
            return true;
        });
    }

    private void registerImportCommand() {
        PersonaImportCommand importExecutor = new PersonaImportCommand(this);
        getCommand("personaimport").setExecutor(importExecutor);
        getCommand("personaimport").setTabCompleter(importExecutor);
    }

    public void reload() {
        reloadConfig();
        loadModuleConfigs();
        
        nicknameManager.loadSettings();
        nicknameManager.loadNicknames();
        
        if (pinManager != null) pinsMenuManager.loadMenu();
        if (tagManager != null) {
            tagManager.reload();
            tagsMenuManager.reload();
        }
        if (messageManager != null) {
            com.github.plunk.alchemypersona.joinmessages.Data.reload();
            messageManager.loadMessages();
            joinMessagesMenuManager.loadMenu();
        }
    }

    private void loadModuleConfigs() {
        nicknamesConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "nicknames.yml"));
        pinsConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "pins.yml"));
        tagsConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "tags.yml"));
        joinMessagesConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "join_messages.yml"));
    }

    public FileConfiguration getNicknamesConfig() { return nicknamesConfig; }
    public FileConfiguration getPinsConfig() { return pinsConfig; }
    public FileConfiguration getTagsConfig() { return tagsConfig; }
    public FileConfiguration getJoinMessagesConfig() { return joinMessagesConfig; }

    private void startWebServer() {
        int port = getConfig().getInt("web.port", 8085);
        server = io.javalin.Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        });

        server.get("/health", ctx -> ctx.result("AlchemyPersona API is UP"));

        server.get("/data", ctx -> {
            String token = ctx.queryParam("token");
            if (token == null) { ctx.status(400).result("Missing token"); return; }
            Session session = sessions.get(token);
            if (session == null || System.currentTimeMillis() > session.expiresAt()) {
                ctx.status(401).result("Invalid or expired token"); return;
            }
            java.util.UUID uuid = java.util.UUID.fromString(session.uuid());
            org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            
            // Get LuckPerms user for permission checks (even if offline)
            net.luckperms.api.model.user.User lpUser = null;
            var lp = getLuckPerms();
            if (lp != null) {
                lpUser = lp.getUserManager().getUser(uuid);
                if (lpUser == null) {
                    // Try to load from storage if not in memory
                    lpUser = lp.getUserManager().loadUser(uuid).join();
                }
            }

            var data = new java.util.HashMap<String, Object>();
            data.put("playerName", offlinePlayer.getName());
            data.put("nickname", nicknameManager.getNickname(uuid));
            
            final net.luckperms.api.model.user.User finalLpUser = lpUser;
            java.util.function.Predicate<String> hasPerm = (perm) -> {
                if (finalLpUser != null) {
                    return finalLpUser.getCachedData().getPermissionData().checkPermission(perm).asBoolean();
                }
                return false;
            };

            // Pins
            var pins = new java.util.ArrayList<java.util.Map<String, Object>>();
            if (pinManager != null && getPinsConfig() != null) {
                // For current pin, we might need online player or LP metadata
                String currentPin = null;
                if (offlinePlayer.isOnline()) {
                    currentPin = pinManager.getCurrentPin(offlinePlayer.getPlayer());
                } else if (finalLpUser != null) {
                    currentPin = finalLpUser.getCachedData().getMetaData().getSuffix();
                }

                var section = getPinsConfig().getConfigurationSection("pins");
                if (section != null) {
                    for (String pinId : section.getKeys(false)) {
                        var pinData = new java.util.HashMap<String, Object>();
                        pinData.put("id", pinId);
                        pinData.put("displayName", getPinsConfig().getString("pins." + pinId + ".display_name"));
                        String unicode = getPinsConfig().getString("pins." + pinId + ".pin_unicode");
                        pinData.put("unicode", unicode);
                        
                        // Nexo Mapping Lookup
                        if (unicode != null) {
                            String stripped = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', unicode)).trim();
                            if (nexoMapping.containsKey(stripped)) {
                                pinData.put("imageUrl", nexoMapping.get(stripped));
                            }
                        }
                        
                        pinData.put("owned", hasPerm.test("LPP.pin." + pinId));
                        pinData.put("selected", pinId.equals(currentPin) || (currentPin != null && currentPin.equals(unicode)));
                        pins.add(pinData);
                    }
                }
            }
            data.put("pins", pins);

            // Tags
            var tags = new java.util.ArrayList<java.util.Map<String, Object>>();
            if (tagManager != null && getTagsConfig() != null) {
                String currentTagId = tagManager.getPlayerTagId(uuid);
                var tagSection = getTagsConfig().getConfigurationSection("tags");
                if (tagSection != null) {
                    for (String tagId : tagSection.getKeys(false)) {
                        var tagData = new java.util.HashMap<String, Object>();
                        tagData.put("id", tagId);
                        tagData.put("displayName", getTagsConfig().getString("tags." + tagId + ".display_name"));
                        tagData.put("tag", getTagsConfig().getString("tags." + tagId + ".tag"));
                        String perm = getTagsConfig().getString("tags." + tagId + ".permission", "deluxetags.tag." + tagId);
                        tagData.put("owned", hasPerm.test(perm));
                        tagData.put("selected", tagId.equals(currentTagId));
                        tags.add(tagData);
                    }
                }
            }
            data.put("tags", tags);

            // Join Messages
            var jms = new java.util.ArrayList<java.util.Map<String, Object>>();
            if (messageManager != null) {
                String currentJm = com.github.plunk.alchemypersona.joinmessages.Data.get().getString("players." + uuid);
                String pName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Player";
                for (var jm : messageManager.getLoadedMessages()) {
                    var jmData = new java.util.HashMap<String, Object>();
                    jmData.put("id", jm.getIdentifier());
                    jmData.put("text", jm.getFormattedMessage(pName));
                    jmData.put("owned", hasPerm.test(jm.getPermission()));
                    jmData.put("selected", jm.getIdentifier().equals(currentJm));
                    jms.add(jmData);
                }
            }
            data.put("joinMessages", jms);

            ctx.json(data);
        });

        // Serve Nexo assets
        server.get("/api/nickname/assets/{namespace}/textures/{path}", ctx -> {
            String namespace = ctx.pathParam("namespace");
            String path = ctx.pathParam("path");
            java.io.File assetsFolder = new java.io.File(getDataFolder().getParentFile(), "Nexo/pack/assets");
            java.io.File textureFile = new java.io.File(assetsFolder, namespace + "/textures/" + path);
            
            if (textureFile.exists()) {
                ctx.contentType("image/png");
                ctx.result(new java.io.FileInputStream(textureFile));
            } else {
                ctx.status(404);
            }
        });

        io.javalin.http.Handler saveHandler = ctx -> {
            try {
                SaveRequest req = ctx.bodyAsClass(SaveRequest.class);
                if (req == null || req.token == null) {
                    ctx.status(400).result("Bad Request: Missing token");
                    return;
                }

                Session session = sessions.get(req.token);
                if (session == null || System.currentTimeMillis() > session.expiresAt()) {
                    ctx.status(401).result("Invalid or expired session token");
                    return;
                }

                java.util.UUID uuid = java.util.UUID.fromString(session.uuid());
                org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
                
                // Save Nickname
                if (req.nickname != null) {
                    nicknameManager.setNickname(uuid, req.nickname);
                }
                
                // Save Pin
                if (req.selectedPin != null && player != null) {
                    if (req.selectedPin.isEmpty()) {
                        pinManager.clearPin(player);
                    } else {
                        var unicode = getPinsConfig().getString("pins." + req.selectedPin + ".pin_unicode");
                        if (unicode != null) pinManager.setPin(player, unicode);
                    }
                }

                // Save Tag
                if (req.selectedTag != null && player != null) {
                    if (req.selectedTag.isEmpty()) {
                        tagManager.clearTag(player);
                    } else {
                        tagManager.setTag(player, req.selectedTag);
                    }
                }

                // Save Join Message
                if (req.selectedJoinMessage != null) {
                    if (req.selectedJoinMessage.isEmpty()) {
                        com.github.plunk.alchemypersona.joinmessages.Data.get().set("players." + uuid, null);
                    } else {
                        com.github.plunk.alchemypersona.joinmessages.Data.get().set("players." + uuid, req.selectedJoinMessage);
                    }
                    com.github.plunk.alchemypersona.joinmessages.Data.save();
                }

                ctx.status(200).result("OK");
            } catch (Exception e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        };

        server.post("/save", saveHandler);

        server.get("/current", ctx -> {
            String token = ctx.queryParam("token");
            if (token == null) { ctx.status(400).result("Missing token"); return; }
            Session session = sessions.get(token);
            if (session == null || System.currentTimeMillis() > session.expiresAt()) {
                ctx.status(401).result("Invalid or expired token"); return;
            }
            java.util.UUID uuid = java.util.UUID.fromString(session.uuid());
            String nick = nicknameManager.getNickname(uuid);
            ctx.contentType("application/json");
            ctx.result("{\"nickname\":" + (nick != null ? "\"" + nick.replace("\\", "\\\\").replace("\"", "\\\"") + "\"" : "null") + "}");
        });

        try {
            server.start(port);
            getLogger().info("Web server started on port " + port);
        } catch (Exception e) {
            getLogger().severe("Could not start web server! Port " + port + " is already in use.");
            getLogger().severe("Identity Designer will be unavailable until this is fixed.");
            server = null; // Set to null so onDisable doesn't try to stop it
        }
    }

    @Override
    public void onDisable() {
        if (server != null) server.stop();
        if (nicknameManager != null) nicknameManager.saveNicknames();
        instance = null;
        getLogger().info("AlchemyPersona has been enabled!");
    }

    public static AlchemyPersona getInstance() {
        return instance;
    }

    public NicknameManager getNicknameManager() { return nicknameManager; }
    public PinManager getPinManager() { return pinManager; }
    public TagManager getTagManager() { return tagManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public MessageMenuManager getJoinMessagesMenuManager() { return joinMessagesMenuManager; }
    public GUIOptions getJoinMessagesGuiOptions() { return joinMessagesGuiOptions; }

    public net.luckperms.api.LuckPerms getLuckPerms() {
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            return net.luckperms.api.LuckPermsProvider.get();
        }
        return null;
    }

    private void loadNexoGlyphs() {
        java.io.File nexoFolder = new java.io.File(getDataFolder().getParentFile(), "Nexo/glyphs");
        if (!nexoFolder.exists()) return;

        java.io.File[] files = nexoFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (java.io.File file : files) {
            org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            for (String key : config.getKeys(false)) {
                String charStr = config.getString(key + ".char");
                String texture = config.getString(key + ".texture");
                if (charStr != null && texture != null) {
                    // texture format: namespace:path
                    String[] parts = texture.split(":");
                    if (parts.length == 2) {
                        String imageUrl = "/api/nickname/assets/" + parts[0] + "/textures/" + parts[1] + ".png";
                        nexoMapping.put(charStr, imageUrl);
                    }
                }
            }
        }
        getLogger().info("Loaded " + nexoMapping.size() + " Nexo glyph mappings for Pins.");
    }

    public static class SaveRequest {
        public String token;
        public String nickname;
        public String selectedPin;
        public String selectedTag;
        public String selectedJoinMessage;
    }
}
