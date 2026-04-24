package com.github.plunk.alchemypersona.tags.utils;

import net.md_5.bungee.api.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    /**
     * Translates color codes, supporting both legacy (&) and modern hex (#RRGGBB).
     */
    public static String color(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        // Handle Hex
        Matcher matcher = HEX_PATTERN.matcher(message);
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, ChatColor.of(color) + "");
            matcher = HEX_PATTERN.matcher(message);
        }

        // Handle Legacy
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
