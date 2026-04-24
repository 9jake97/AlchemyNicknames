package com.github.plunk.alchemypersona.joinmessages.objects;

import org.bukkit.Material;
import java.util.List;

public class JoinMessage {
    private String identifier;
    private int priority;
    private String message; // The actual chat message
    private String permission;

    // Icon properties for GUI
    private Material iconMaterial;
    private short iconData;
    private String displayName;
    private List<String> description;

    // Theme color for auto-formatting
    private String themeColor;

    // Support for Nexo Glyphs
    private String nexoGlyphId;

    public JoinMessage(String identifier, int priority, String message, String permission) {
        this.identifier = identifier;
        this.priority = priority;
        this.message = message;
        this.permission = permission;
    }

    public String getNexoGlyphId() {
        return nexoGlyphId;
    }

    public void setNexoGlyphId(String nexoGlyphId) {
        this.nexoGlyphId = nexoGlyphId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    // Icon methods
    public Material getIconMaterial() {
        return iconMaterial != null ? iconMaterial : Material.PAPER;
    }

    public void setIconMaterial(Material mat) {
        this.iconMaterial = mat;
    }

    public short getIconData() {
        return iconData;
    }

    public void setIconData(short data) {
        this.iconData = data;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : identifier;
    }

    public void setDisplayName(String name) {
        this.displayName = name;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> desc) {
        this.description = desc;
    }

    public String getThemeColor() {
        return themeColor;
    }

    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }

    public String applyTheme(String input, String playerName) {
        if (themeColor == null || themeColor.isEmpty()) {
            return input.replace("%player%", playerName);
        }

        String colorTag = themeColor;
        // If it's a raw hex without #, add it
        if (colorTag.length() == 6 && colorTag.matches("[a-fA-F0-9]{6}")) {
            colorTag = "#" + colorTag;
        }
        // If it's a named color without brackets, add them
        if (!colorTag.startsWith("<") && !colorTag.startsWith("&") && !colorTag.startsWith("#")) {
            colorTag = "<" + colorTag + ">";
        }

        // Apply theme color as base, override specifics to white
        return colorTag + input
                .replace("%player%", "<white>" + playerName + colorTag)
                .replace("{", "<white>{" + colorTag)
                .replace("}", "<white>}" + colorTag)
                .replace("[", "<white>[" + colorTag)
                .replace("]", "<white>]" + colorTag);
    }

    public String getFormattedMessage(String playerName) {
        String base = message != null ? message : "";
        if (base.isEmpty() && themeColor != null) {
            base = "%player%";
        }

        if (themeColor != null && !themeColor.isEmpty()) {
            String colorTag = themeColor;
            if (colorTag.length() == 6 && colorTag.matches("[a-fA-F0-9]{6}")) {
                colorTag = "#" + colorTag;
            }
            if (!colorTag.startsWith("<") && !colorTag.startsWith("&") && !colorTag.startsWith("#")) {
                colorTag = "<" + colorTag + ">";
            }

            // Standard prefix: [Theme+][NextCharacter]
            String prefix = "<white>[" + colorTag + "+" + "<white>] ";
            return prefix + applyTheme(base, playerName);
        }

        return base.replace("%player%", playerName);
    }
}
