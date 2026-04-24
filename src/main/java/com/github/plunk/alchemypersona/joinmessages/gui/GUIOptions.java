package com.github.plunk.alchemypersona.joinmessages.gui;

import com.github.plunk.alchemypersona.AlchemyPersona;
import org.bukkit.configuration.file.FileConfiguration;

public class GUIOptions {

    private final AlchemyPersona plugin;
    private String menuTitle;
    private int menuSize;
    // Add other options as needed from DeluxeTags

    public GUIOptions(AlchemyPersona plugin) {
        this.plugin = plugin;
        loadOptions();
    }

    public void loadOptions() {
        FileConfiguration config = plugin.getJoinMessagesConfig();
        this.menuTitle = config.getString("gui.title", "&8Select Join Message");
        this.menuSize = config.getInt("gui.size", 54);
    }

    public String getMenuTitle() {
        return menuTitle;
    }

    public int getMenuSize() {
        return menuSize;
    }
}


