package com.vibecode.tripticket.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for formatting and sending colored messages, titles, action bars, and clickable prompts.
 */
public final class MessageUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private MessageUtil() {
    }

    /**
     * Translates standard color codes (&a) and hex color codes (&#ffffff).
     */
    public static String colorize(String text) {
        if (text == null) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hexCode.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);

        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    /**
     * Sends a formatted message to a command sender.
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isEmpty()) {
            sender.sendMessage(colorize(message));
        }
    }

    /**
     * Sends an action bar message to a player.
     */
    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(colorize(message)));
        } catch (Throwable ignored) {
        }
    }

    /**
     * Sends a title and subtitle to a player.
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return;
        }
        String formattedTitle = title != null ? colorize(title) : "";
        String formattedSubtitle = subtitle != null ? colorize(subtitle) : "";
        player.sendTitle(formattedTitle, formattedSubtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Sends an interactive clickable ticket invite with Accept and Decline buttons.
     */
    public static void sendTicketInvitePrompt(Player guest, String hostName, String worldName, String durationFormatted) {
        if (guest == null) return;

        sendMessage(guest, "&8--------------------------------------------------");
        sendMessage(guest, "&6&l🎫 TRIP TICKET INVITATION");
        sendMessage(guest, "&e" + hostName + " &7has invited you to visit their world &b(" + worldName + ")&7!");
        sendMessage(guest, "&7Ticket duration: &a" + durationFormatted);

        try {
            TextComponent acceptBtn = new TextComponent(colorize("&a&l[✔ ACCEPT TICKET] "));
            acceptBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ticket accept"));
            acceptBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(colorize("&aClick to embark on the trip!"))));

            TextComponent space = new TextComponent("   ");

            TextComponent denyBtn = new TextComponent(colorize("&c&l[✖ DECLINE]"));
            denyBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ticket deny"));
            denyBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(colorize("&cClick to decline this ticket."))));

            TextComponent actionRow = new TextComponent("");
            actionRow.addExtra(acceptBtn);
            actionRow.addExtra(space);
            actionRow.addExtra(denyBtn);

            guest.spigot().sendMessage(actionRow);
        } catch (Throwable t) {
            // Fallback for environments where Bungee Component API has differences
            sendMessage(guest, "&aType &e/ticket accept &ato accept or &c/ticket deny &ato decline.");
        }

        sendMessage(guest, "&8--------------------------------------------------");
    }
}
