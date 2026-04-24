package com.github.plunk.alchemypersona.joinmessages.utils;

import net.md_5.bungee.api.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HexUtils {

    // Combined Hex Pattern handling (&?# | <# | #) RRGGBB (>?)
    private static final Pattern COMBINED_HEX_PATTERN = Pattern.compile("(&?#|<#)([a-fA-F0-9]{6})(>?)");

    public static String colorify(String message) {
        if (message == null || message.isEmpty())
            return message;

        // 1. Convert all Hex formats robustly ( #RRGGBB, &#RRGGBB, <#RRGGBB> )
        Matcher matcher = COMBINED_HEX_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(2);
            try {
                ChatColor color = ChatColor.of("#" + hex);
                matcher.appendReplacement(sb, color.toString());
            } catch (Exception e) {
                matcher.appendReplacement(sb, matcher.group());
            }
        }
        matcher.appendTail(sb);
        message = sb.toString();

        // 2. Convert unnamed colors e.g. <white>
        message = convertTag(message, "white", ChatColor.WHITE);
        message = convertTag(message, "black", ChatColor.BLACK);
        message = convertTag(message, "dark_blue", ChatColor.DARK_BLUE);
        message = convertTag(message, "dark_green", ChatColor.DARK_GREEN);
        message = convertTag(message, "dark_aqua", ChatColor.DARK_AQUA);
        message = convertTag(message, "dark_red", ChatColor.DARK_RED);
        message = convertTag(message, "dark_purple", ChatColor.DARK_PURPLE);
        message = convertTag(message, "gold", ChatColor.GOLD);
        message = convertTag(message, "gray", ChatColor.GRAY);
        message = convertTag(message, "dark_gray", ChatColor.DARK_GRAY);
        message = convertTag(message, "blue", ChatColor.BLUE);
        message = convertTag(message, "green", ChatColor.GREEN);
        message = convertTag(message, "aqua", ChatColor.AQUA);
        message = convertTag(message, "red", ChatColor.RED);
        message = convertTag(message, "light_purple", ChatColor.LIGHT_PURPLE);
        message = convertTag(message, "yellow", ChatColor.YELLOW);

        // 3. Translate legacy & codes
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String convertTag(String msg, String tagName, ChatColor color) {
        return msg.replace("<" + tagName + ">", color.toString())
                .replace("<" + tagName.toUpperCase() + ">", color.toString());
    }
}
