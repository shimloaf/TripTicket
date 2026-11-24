package com.vibecode.tripticket.command;

import com.vibecode.tripticket.TripTicketPlugin;
import com.vibecode.tripticket.model.TripSession;
import com.vibecode.tripticket.service.ConfigManager;
import com.vibecode.tripticket.service.TicketManager;
import com.vibecode.tripticket.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Handles all /tripticket commands and tab completions.
 */
public class TripTicketCommand implements CommandExecutor, TabCompleter {

    private final TripTicketPlugin plugin;
    private final ConfigManager configManager;
    private final TicketManager ticketManager;

    private static final List<String> SUBCOMMANDS = List.of(
            "invite", "accept", "deny", "decline", "return", "leave", "back", "remaining", "time", "cancel", "rip", "setrip", "help", "reload"
    );

    public TripTicketCommand(TripTicketPlugin plugin, ConfigManager configManager, TicketManager ticketManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.ticketManager = ticketManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help":
                sendHelp(sender, label);
                return true;

            case "reload":
                handleReload(sender);
                return true;

            case "rip":
                handleRip(sender);
                return true;

            case "setrip":
                handleSetRip(sender);
                return true;

            case "accept":
                handleAccept(sender);
                return true;

            case "deny":
            case "decline":
                handleDeny(sender);
                return true;

            case "return":
            case "leave":
            case "back":
                handleReturn(sender);
                return true;

            case "remaining":
            case "time":
            case "status":
                handleRemaining(sender);
                return true;

            case "cancel":
                handleCancel(sender, args);
                return true;

            case "invite":
                handleExplicitInvite(sender, args);
                return true;

            default:
                // Direct invite by player name: /ticket <player> [duration]
                handleDirectPlayerInvite(sender, args);
                return true;
        }
    }

    private void handleExplicitInvite(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "&cUsage: /ticket invite <player> [duration]");
            return;
        }

        String[] shiftedArgs = new String[args.length - 1];
        System.arraycopy(args, 1, shiftedArgs, 0, shiftedArgs.length);
        handleDirectPlayerInvite(sender, shiftedArgs);
    }

    private void handleDirectPlayerInvite(CommandSender sender, String[] args) {
        if (!(sender instanceof Player host)) {
            MessageUtil.sendMessage(sender, "&cOnly players in-game can invite others to their world.");
            return;
        }

        if (!configManager.canInvite(host)) {
            MessageUtil.sendMessage(host, configManager.getMessage("no-permission"));
            return;
        }

        String targetName = args[0];
        Player guest = Bukkit.getPlayer(targetName);

        if (guest == null || !guest.isOnline()) {
            MessageUtil.sendMessage(host, configManager.getMessage("player-not-found", Map.of("player", targetName)));
            return;
        }

        if (guest.getUniqueId().equals(host.getUniqueId())) {
            ticketManager.requestSelfTrip(host);
            return;
        }

        long duration = configManager.getDefaultDurationSeconds();
        if (args.length >= 2) {
            if (configManager.canUseCustomDuration(host)) {
                long parsed = ConfigManager.parseDuration(args[1]);
                if (parsed <= 0) {
                    MessageUtil.sendMessage(host, "&cInvalid duration format. Example: &e3m&c, &e90s&c, or &e180&c.");
                    return;
                }
                duration = parsed;
            } else {
                Map<String, String> ph = Map.of("duration", ConfigManager.formatDuration(duration));
                MessageUtil.sendMessage(host, configManager.getMessage("custom-duration-denied", ph));
            }
        }

        ticketManager.issueTicket(host, guest, duration);
    }

    private void handleAccept(CommandSender sender) {
        if (!(sender instanceof Player guest)) {
            MessageUtil.sendMessage(sender, "&cOnly players can accept trip tickets.");
            return;
        }

        if (!configManager.canAccept(guest)) {
            MessageUtil.sendMessage(guest, configManager.getMessage("no-permission"));
            return;
        }

        ticketManager.acceptTicket(guest);
    }

    private void handleDeny(CommandSender sender) {
        if (!(sender instanceof Player guest)) {
            MessageUtil.sendMessage(sender, "&cOnly players can decline trip tickets.");
            return;
        }

        ticketManager.denyTicket(guest);
    }

    private void handleReturn(CommandSender sender) {
        if (!(sender instanceof Player guest)) {
            MessageUtil.sendMessage(sender, "&cOnly players can return from a trip.");
            return;
        }

        if (!configManager.canReturn(guest)) {
            MessageUtil.sendMessage(guest, configManager.getMessage("no-permission"));
            return;
        }

        TripSession session = ticketManager.getSession(guest.getUniqueId());
        if (session == null) {
            MessageUtil.sendMessage(guest, configManager.getMessage("not-on-trip"));
            return;
        }

        ticketManager.whiskBack(session, TicketManager.ReturnReason.RETURN_EARLY);
    }

    private void handleRemaining(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "&cOnly players can check remaining ticket time.");
            return;
        }

        if (!configManager.canCheckTime(player)) {
            MessageUtil.sendMessage(player, configManager.getMessage("no-permission"));
            return;
        }

        TripSession session = ticketManager.getSession(player.getUniqueId());
        if (session == null) {
            MessageUtil.sendMessage(player, configManager.getMessage("not-on-trip"));
            return;
        }

        Map<String, String> ph = Map.of("remaining", session.getFormattedRemainingTime());
        MessageUtil.sendMessage(player, configManager.getMessage("time-remaining", ph));
    }

    private void handleCancel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player issuer)) {
            MessageUtil.sendMessage(sender, "&cOnly players can cancel trip tickets.");
            return;
        }

        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                MessageUtil.sendMessage(issuer, configManager.getMessage("player-not-found", Map.of("player", args[1])));
                return;
            }

            boolean cancelled = ticketManager.cancelTicketOrVisit(issuer, target);
            if (!cancelled) {
                MessageUtil.sendMessage(issuer, "&cNo active or pending ticket found between you and &e" + target.getName() + "&c.");
            }
            return;
        }

        // Check if caller has an active warmup to cancel
        if (ticketManager.hasActiveWarmup(issuer.getUniqueId())) {
            ticketManager.cancelWarmup(issuer.getUniqueId());
            MessageUtil.sendMessage(issuer, "&cWarmup cancelled.");
            return;
        }

        MessageUtil.sendMessage(issuer, "&cUsage: /ticket cancel <player>");
    }

    private void handleReload(CommandSender sender) {
        if (!configManager.canReload(sender)) {
            MessageUtil.sendMessage(sender, configManager.getMessage("no-permission"));
            return;
        }

        configManager.loadConfig();
        MessageUtil.sendMessage(sender, configManager.getMessage("reloaded"));
    }

    private void handleRip(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "&cOnly players can use /ticket rip.");
            return;
        }

        if (!configManager.canRip(player)) {
            MessageUtil.sendMessage(player, configManager.getMessage("no-permission"));
            return;
        }

        ticketManager.startRipCountdown(player);
    }

    private void handleSetRip(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "&cOnly in-game players can set the RIP anchor location.");
            return;
        }

        if (!configManager.canSetRip(player)) {
            MessageUtil.sendMessage(player, configManager.getMessage("no-permission"));
            return;
        }

        org.bukkit.Location loc = player.getLocation();
        configManager.setRipAnchor(loc);

        Map<String, String> ph = Map.of(
                "world", loc.getWorld().getName(),
                "x", String.format(Locale.ROOT, "%.1f", loc.getX()),
                "y", String.format(Locale.ROOT, "%.1f", loc.getY()),
                "z", String.format(Locale.ROOT, "%.1f", loc.getZ())
        );
        MessageUtil.sendMessage(player, configManager.getMessage("rip-anchor-set", ph));
    }

    private void sendHelp(CommandSender sender, String label) {
        MessageUtil.sendMessage(sender, "&8===== &6TripTicket &8- &eCross-World Invites &8=====");
        MessageUtil.sendMessage(sender, "&e/" + label + " <player> [duration] &7- Invite a player to visit your world");
        MessageUtil.sendMessage(sender, "&e/" + label + " <your-name> &7- Initiate a permanent random world trip");
        MessageUtil.sendMessage(sender, "&e/" + label + " accept &7- Accept a ticket invitation and embark");
        MessageUtil.sendMessage(sender, "&e/" + label + " deny &7- Decline a ticket invitation");
        MessageUtil.sendMessage(sender, "&e/" + label + " return &7- Return early to your original world");
        MessageUtil.sendMessage(sender, "&e/" + label + " remaining &7- Check time remaining on your ticket");
        MessageUtil.sendMessage(sender, "&e/" + label + " rip &7- Perish and respawn at the anchor world");
        if (configManager.canSetRip(sender)) {
            MessageUtil.sendMessage(sender, "&e/" + label + " setrip &7- Set the anchor location for /ticket rip");
        }
        MessageUtil.sendMessage(sender, "&e/" + label + " cancel <player> &7- Cancel an invitation or visitor");
        if (configManager.canReload(sender)) {
            MessageUtil.sendMessage(sender, "&e/" + label + " reload &7- Reload configuration");
        }
        MessageUtil.sendMessage(sender, "&8=================================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String current = args[0].toLowerCase();

            for (String sub : List.of("invite", "accept", "deny", "return", "remaining", "rip", "setrip", "cancel", "help", "reload")) {
                if (sub.startsWith(current)) {
                    if (sub.equals("reload") && !configManager.canReload(sender)) {
                        continue;
                    }
                    if (sub.equals("setrip") && !configManager.canSetRip(sender)) {
                        continue;
                    }
                    completions.add(sub);
                }
            }

            // Also suggest online player names directly (including self for self-trip)
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(current)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String current = args[1].toLowerCase();

            if (sub.equals("invite") || sub.equals("cancel")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(current)) {
                        completions.add(player.getName());
                    }
                }
            } else if (!SUBCOMMANDS.contains(sub)) {
                // First arg was player name, only suggest durations if sender can use custom duration
                if (sender instanceof Player p && configManager.canUseCustomDuration(p)) {
                    for (String d : List.of("1m", "2m", "3m", "5m", "10m", "30s", "60s", "180s")) {
                        if (d.startsWith(current)) {
                            completions.add(d);
                        }
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("invite")) {
            if (sender instanceof Player p && configManager.canUseCustomDuration(p)) {
                String current = args[2].toLowerCase();
                for (String d : List.of("1m", "2m", "3m", "5m", "10m", "30s", "60s", "180s")) {
                    if (d.startsWith(current)) {
                        completions.add(d);
                    }
                }
            }
        }

        Collections.sort(completions);
        return completions;
    }
}
