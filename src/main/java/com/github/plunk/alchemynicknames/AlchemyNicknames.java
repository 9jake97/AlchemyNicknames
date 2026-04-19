package com.github.plunk.alchemynicknames;

import com.github.plunk.alchemynicknames.commands.NicknameCommand;
import com.github.plunk.alchemynicknames.commands.NicknameImportCommand;
import com.github.plunk.alchemynicknames.listeners.PlayerListener;
import com.github.plunk.alchemynicknames.managers.NicknameManager;
import com.github.plunk.alchemynicknames.placeholders.NicknameExpansion;
import org.bukkit.plugin.java.JavaPlugin;

public class AlchemyNicknames extends JavaPlugin {

    private static AlchemyNicknames instance;
    private NicknameManager nicknameManager;
    private io.javalin.Javalin server;
    private record Session(String uuid, long expiresAt) {}
    private static final long SESSION_TTL_MS = 10 * 60 * 1000L; // 10 minutes
    private final java.util.Map<String, Session> sessions = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Random random = new java.util.Random();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        nicknameManager = new NicknameManager(this);
        nicknameManager.loadNicknames();

        // Register commands
        NicknameCommand nicknameCommand = new NicknameCommand(this, nicknameManager);
        getCommand("nickname").setExecutor(nicknameCommand);
        getCommand("nickname").setTabCompleter(nicknameCommand);
        getCommand("unnick").setExecutor(nicknameCommand);
        getCommand("unnick").setTabCompleter(nicknameCommand);

        NicknameImportCommand importCommand = new NicknameImportCommand(this, nicknameManager);
        getCommand("nickimport").setExecutor(importCommand);
        getCommand("nickimport").setTabCompleter(importCommand);

        getCommand("nicknameeditor").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("§cOnly players can use this command.");
                return true;
            }

            String token;
            do { token = String.format("%05d", random.nextInt(100000)); } while (sessions.containsKey(token));
            sessions.put(token, new Session(player.getUniqueId().toString(), System.currentTimeMillis() + SESSION_TTL_MS));

            String editorUrl = getConfig().getString("web.editor-url", "https://plunk.github.io/AlchemyNicknames");
            String apiBase = getConfig().getString("web.base-url", "https://stats.bloc.kz");
            if (editorUrl.endsWith("/")) editorUrl = editorUrl.substring(0, editorUrl.length() - 1);
            if (apiBase.endsWith("/")) apiBase = apiBase.substring(0, apiBase.length() - 1);
            String link = editorUrl + "/?player=" + player.getName() + "&token=" + token + "&api=" + apiBase;

            var mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

            player.sendMessage(mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"));
            player.sendMessage(mm.deserialize("      <gradient:#FF0080:#8000FF><bold>✦ Nickname Designer ✦</bold></gradient>"));
            player.sendMessage(mm.deserialize(" <dark_gray>»</dark_gray> <gray>Design your own <gradient:#FF0080:#8000FF>RGB gradient</gradient> nickname</gray>"));
            player.sendMessage(mm.deserialize(" <dark_gray>»</dark_gray> <gray>Your session expires in <white>10 minutes</white></gray>"));
            player.sendMessage(net.kyori.adventure.text.Component.empty());
            player.sendMessage(mm.deserialize("      <click:open_url:'" + link + "'><hover:show_text:'<gray>Opens in your browser'><gradient:#00c6ff:#8000FF><bold>[  ✦ Open Designer  ]</bold></gradient></hover></click>"));
            player.sendMessage(net.kyori.adventure.text.Component.empty());
            player.sendMessage(mm.deserialize(" <dark_gray>»</dark_gray> <gray>Bedrock / can't click? Visit the site and enter:</gray>"));
            player.sendMessage(mm.deserialize("   <dark_gray>Name: </dark_gray><white>" + player.getName() + "</white>   <dark_gray>Code: </dark_gray><gradient:#00c6ff:#8000FF><bold>" + token + "</bold></gradient>"));
            player.sendMessage(mm.deserialize("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"));
            
            return true;
        });

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this, nicknameManager), this);

        // Register PlaceholderAPI expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NicknameExpansion(this, nicknameManager).register();
        }

        // Start Web Server
        startWebServer();

        // Purge expired sessions every 5 minutes
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> sessions.entrySet().removeIf(e -> System.currentTimeMillis() > e.getValue().expiresAt()),
                6000L, 6000L);

        getLogger().info("AlchemyNicknames has been enabled!");
    }

    private void startWebServer() {
        int port = getConfig().getInt("web.port", 8085);
        server = io.javalin.Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        });

        server.get("/health", ctx -> ctx.result("AlchemyNicknames API is UP"));

        io.javalin.http.Handler saveHandler = ctx -> {
            try {
                getLogger().info("Received nickname save request...");
                SaveRequest req = ctx.bodyAsClass(SaveRequest.class);

                if (req == null || req.token == null || req.nickname == null) {
                    ctx.status(400).result("Bad Request: Missing token or nickname");
                    return;
                }

                Session session = sessions.get(req.token);
                if (session == null || System.currentTimeMillis() > session.expiresAt()) {
                    sessions.remove(req.token);
                    getLogger().warning("Save failed: Invalid or expired token: " + req.token);
                    ctx.status(401).result("Invalid or expired session token");
                    return;
                }

                java.util.UUID uuid = java.util.UUID.fromString(session.uuid());
                nicknameManager.setNickname(uuid, req.nickname);
                sessions.remove(req.token);
                getLogger().info("Successfully saved nickname for " + uuid + ": " + req.nickname);
                ctx.status(200).result("OK");
            } catch (Exception e) {
                getLogger().severe("Error processing save request: " + e.getMessage());
                e.printStackTrace();
                ctx.status(500).result("Internal Server Error: " + e.getMessage());
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

        server.start(port);
        
        getLogger().info("Web server started on port " + port);
    }

    @Override
    public void onDisable() {
        if (server != null) {
            server.stop();
        }
        if (nicknameManager != null) {
            nicknameManager.saveNicknames();
        }
        getLogger().info("AlchemyNicknames has been disabled!");
    }

    public static AlchemyNicknames getInstance() {
        return instance;
    }

    public NicknameManager getNicknameManager() {
        return nicknameManager;
    }

    public static class SaveRequest {
        public String token;
        public String nickname;
        public String plainText;
    }
}
