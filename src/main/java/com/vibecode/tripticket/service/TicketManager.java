package com.vibecode.tripticket.service;

import com.vibecode.tripticket.TripTicketPlugin;
import com.vibecode.tripticket.model.TripSession;
import com.vibecode.tripticket.model.TripTicket;
import com.vibecode.tripticket.util.MessageUtil;
import com.vibecode.tripticket.util.SoundParticleUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates trip tickets, guest visit sessions, countdown warnings, and persistent returns.
 */
public class TicketManager {

    public enum ReturnReason {
        TICKET_EXPIRED,
        RETURN_EARLY,
        CANCELLED_BY_HOST,
        ADMIN,
        DEATH,
        SERVER_SHUTDOWN
    }

    private final TripTicketPlugin plugin;
    private final ConfigManager configManager;

    private final Map<UUID, TripSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, TripTicket> pendingTickets = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> activeWarmups = new ConcurrentHashMap<>();
    private final Map<UUID, Location> offlineReturns = new ConcurrentHashMap<>();

    private BukkitTask tickerTask;
    private final File dataFile;

    public TicketManager(TripTicketPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataFile = new File(plugin.getDataFolder(), "sessions.yml");
    }

    /**
     * Starts the periodic session update timer.
     */
    public void startTicker() {
        if (tickerTask != null && !tickerTask.isCancelled()) {
            tickerTask.cancel();
        }

        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    /**
     * Ticks every second.
     */
    private void tick() {
        // 1. Process active trip sessions
        for (TripSession session : new ArrayList<>(activeSessions.values())) {
            Player guest = Bukkit.getPlayer(session.getGuestUuid());
            long remaining = session.getRemainingSeconds();

            if (remaining <= 0) {
                // Ticket time is up! Whisk guest back to their original world
                whiskBack(session, ReturnReason.TICKET_EXPIRED);
                continue;
            }

            if (guest != null && guest.isOnline()) {
                // Warning countdown reminders (e.g. 10s, 5s, 4s, 3s, 2s, 1s)
                if (configManager.getCountdownSeconds().contains((int) remaining) || remaining == session.getWarningSeconds()) {
                    sendCountdownWarning(guest, remaining);
                }

                // Action Bar ticker
                if (configManager.isShowActionBar()) {
                    String formatted = session.getFormattedRemainingTime();
                    String barMessage = "&6Trip Ticket: &e" + formatted + " &6remaining";
                    MessageUtil.sendActionBar(guest, barMessage);
                }
            }
        }

        // 2. Expire pending unaccepted tickets
        long timeout = configManager.getInviteTimeoutSeconds();
        for (Map.Entry<UUID, TripTicket> entry : new ArrayList<>(pendingTickets.entrySet())) {
            TripTicket ticket = entry.getValue();
            if (ticket.isExpired(timeout)) {
                pendingTickets.remove(entry.getKey());

                Player host = Bukkit.getPlayer(ticket.getHostUuid());
                if (host != null && host.isOnline()) {
                    MessageUtil.sendMessage(host, configManager.getMessage("ticket-expired-host-pending", Map.of("guest", ticket.getGuestName())));
                }

                Player guest = Bukkit.getPlayer(ticket.getGuestUuid());
                if (guest != null && guest.isOnline()) {
                    MessageUtil.sendMessage(guest, configManager.getMessage("ticket-expired-guest-pending", Map.of("host", ticket.getHostName())));
                }
            }
        }
    }

    private void sendCountdownWarning(Player guest, long secondsRemaining) {
        Map<String, String> placeholders = Map.of("seconds", String.valueOf(secondsRemaining));

        // Chat message
        String msg = configManager.getMessage("warning-countdown", placeholders);
        MessageUtil.sendMessage(guest, msg);

        // Screen title
        if (configManager.isShowTitleOnWarning()) {
            String title = "&c&lTICKET EXPIRING!";
            String subtitle = "&eReturning to your world in &c" + secondsRemaining + "s&e...";
            MessageUtil.sendTitle(guest, title, subtitle, 5, 20, 5);
        }

        // Sound effect
        if (configManager.isPlaySounds()) {
            float pitch = secondsRemaining <= 3 ? 1.6f : 1.0f;
            SoundParticleUtil.playSound(guest, configManager.getSoundWarning(), 1.0f, pitch, plugin.getLogger());
        }
    }

    /**
     * Host issues an invitation ticket to a guest player.
     */
    public void issueTicket(Player host, Player guest, long durationSeconds) {
        if (host.getUniqueId().equals(guest.getUniqueId())) {
            MessageUtil.sendMessage(host, configManager.getMessage("cannot-invite-self"));
            return;
        }

        if (activeSessions.containsKey(guest.getUniqueId())) {
            MessageUtil.sendMessage(host, configManager.getMessage("already-on-trip", Map.of("guest", guest.getName())));
            return;
        }

        if (hasCooldown(host)) {
            long remainingCooldown = getRemainingCooldownSeconds(host);
            MessageUtil.sendMessage(host, configManager.getMessage("cooldown", Map.of("seconds", String.valueOf(remainingCooldown))));
            return;
        }

        TripTicket ticket = new TripTicket(
                host.getUniqueId(), host.getName(),
                host.getWorld().getName(),
                guest.getUniqueId(), guest.getName(),
                durationSeconds
        );
        pendingTickets.put(guest.getUniqueId(), ticket);

        // Notify host
        Map<String, String> hostPh = Map.of(
                "guest", guest.getName(),
                "duration", ConfigManager.formatDuration(durationSeconds)
        );
        MessageUtil.sendMessage(host, configManager.getMessage("ticket-sent", hostPh));

        // Send clickable invitation ticket to guest
        MessageUtil.sendTicketInvitePrompt(guest, host.getName(), host.getWorld().getName(), ConfigManager.formatDuration(durationSeconds));
    }

    /**
     * Guest accepts their pending trip ticket.
     */
    public boolean acceptTicket(Player guest) {
        TripTicket ticket = pendingTickets.remove(guest.getUniqueId());
        if (ticket == null || ticket.isExpired(configManager.getInviteTimeoutSeconds())) {
            MessageUtil.sendMessage(guest, configManager.getMessage("no-pending-ticket"));
            return false;
        }

        Player host = Bukkit.getPlayer(ticket.getHostUuid());
        if (host == null || !host.isOnline()) {
            MessageUtil.sendMessage(guest, configManager.getMessage("player-not-found", Map.of("player", ticket.getHostName())));
            return false;
        }

        if (activeSessions.containsKey(guest.getUniqueId())) {
            MessageUtil.sendMessage(guest, configManager.getMessage("already-on-trip-self"));
            return false;
        }

        long warmup = configManager.getWarmupSeconds();
        if (warmup > 0 && !configManager.bypassesWarmup(guest)) {
            cancelWarmup(guest.getUniqueId());

            MessageUtil.sendMessage(guest, configManager.getMessage("warmup-start", Map.of("seconds", String.valueOf(warmup))));

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                activeWarmups.remove(guest.getUniqueId());
                if (guest.isOnline() && host.isOnline()) {
                    embark(guest, host, ticket.getDurationSeconds());
                } else if (guest.isOnline()) {
                    MessageUtil.sendMessage(guest, configManager.getMessage("player-not-found", Map.of("player", ticket.getHostName())));
                }
            }, warmup * 20L);

            activeWarmups.put(guest.getUniqueId(), task);
        } else {
            embark(guest, host, ticket.getDurationSeconds());
        }

        return true;
    }

    /**
     * Guest declines their pending ticket.
     */
    public boolean denyTicket(Player guest) {
        TripTicket ticket = pendingTickets.remove(guest.getUniqueId());
        if (ticket == null) {
            MessageUtil.sendMessage(guest, configManager.getMessage("no-pending-ticket"));
            return false;
        }

        MessageUtil.sendMessage(guest, configManager.getMessage("ticket-declined-guest"));

        Player host = Bukkit.getPlayer(ticket.getHostUuid());
        if (host != null && host.isOnline()) {
            MessageUtil.sendMessage(host, configManager.getMessage("ticket-declined-host", Map.of("guest", guest.getName())));
        }
        return true;
    }

    /**
     * Guest embarks on the trip: saves original location & world, teleports to host, begins session.
     */
    private void embark(Player guest, Player host, long durationSeconds) {
        Location guestOriginalLocation = guest.getLocation().clone();
        Location hostLocation = host.getLocation().clone();

        // Departure effects at guest's original spot
        if (configManager.isPlayParticles()) {
            SoundParticleUtil.spawnParticles(guestOriginalLocation, configManager.getParticleEmbark(), 30, 0.5, 1.0, 0.5, plugin.getLogger());
        }

        // Teleport guest to host
        guest.teleport(hostLocation);

        // Arrival effects at host's spot
        if (configManager.isPlayParticles()) {
            SoundParticleUtil.spawnParticles(hostLocation, configManager.getParticleEmbark(), 30, 0.5, 1.0, 0.5, plugin.getLogger());
        }
        if (configManager.isPlaySounds()) {
            SoundParticleUtil.playSound(guest, configManager.getSoundEmbark(), 1.0f, 1.0f, plugin.getLogger());
            SoundParticleUtil.playSound(host, configManager.getSoundEmbark(), 1.0f, 1.0f, plugin.getLogger());
        }

        applyCooldown(host);

        // Create active session
        TripSession session = new TripSession(
                guest.getUniqueId(),
                guest.getName(),
                guestOriginalLocation,
                host.getUniqueId(),
                host.getName(),
                System.currentTimeMillis(),
                durationSeconds,
                configManager.getWarningSeconds()
        );
        activeSessions.put(guest.getUniqueId(), session);
        saveSessions();

        // Notify guest
        Map<String, String> guestPh = Map.of(
                "host", host.getName(),
                "world", host.getWorld().getName(),
                "duration", ConfigManager.formatDuration(durationSeconds)
        );
        MessageUtil.sendMessage(guest, configManager.getMessage("ticket-accepted-guest", guestPh));

        // Notify host
        Map<String, String> hostPh = Map.of(
                "guest", guest.getName(),
                "duration", ConfigManager.formatDuration(durationSeconds)
        );
        MessageUtil.sendMessage(host, configManager.getMessage("ticket-accepted-host", hostPh));
    }

    /**
     * Whisks guest back to their original position and world.
     */
    public void whiskBack(TripSession session, ReturnReason reason) {
        activeSessions.remove(session.getGuestUuid());
        saveSessions();

        Player guest = Bukkit.getPlayer(session.getGuestUuid());
        Player host = Bukkit.getPlayer(session.getHostUuid());
        Location originalLoc = session.getGuestOriginalLocation();

        if (guest != null && guest.isOnline()) {
            Location safeLoc = LocationSafety.getSafeLocation(originalLoc, configManager.isSafeTeleportCheck());

            // Departure effects at current location
            if (configManager.isPlayParticles()) {
                SoundParticleUtil.spawnParticles(guest.getLocation(), configManager.getParticleReturn(), 30, 0.5, 1.0, 0.5, plugin.getLogger());
            }

            guest.teleport(safeLoc);

            // Arrival effects at original position
            if (configManager.isPlayParticles()) {
                SoundParticleUtil.spawnParticles(safeLoc, configManager.getParticleReturn(), 30, 0.5, 1.0, 0.5, plugin.getLogger());
            }
            if (configManager.isPlaySounds()) {
                SoundParticleUtil.playSound(guest, configManager.getSoundReturn(), 1.0f, 1.0f, plugin.getLogger());
            }

            // Guest messaging
            switch (reason) {
                case TICKET_EXPIRED:
                    MessageUtil.sendMessage(guest, configManager.getMessage("ticket-expired-guest"));
                    break;
                case RETURN_EARLY:
                    MessageUtil.sendMessage(guest, configManager.getMessage("return-early-guest"));
                    break;
                case CANCELLED_BY_HOST:
                    MessageUtil.sendMessage(guest, configManager.getMessage("cancelled-by-host", Map.of("host", session.getHostName())));
                    break;
                case ADMIN:
                    MessageUtil.sendMessage(guest, "&cYour trip ticket was cancelled by an administrator. Returned home.");
                    break;
                case SERVER_SHUTDOWN:
                    MessageUtil.sendMessage(guest, "&eServer restarting/reloading. Safely returned to your original world.");
                    break;
                case DEATH:
                    break;
            }

            // Host messaging
            if (host != null && host.isOnline()) {
                Map<String, String> hostPh = Map.of("guest", session.getGuestName());
                if (reason == ReturnReason.TICKET_EXPIRED) {
                    MessageUtil.sendMessage(host, configManager.getMessage("ticket-expired-host", hostPh));
                } else if (reason == ReturnReason.RETURN_EARLY) {
                    MessageUtil.sendMessage(host, configManager.getMessage("return-early-host", hostPh));
                }
            }
        } else {
            // Guest is offline: queue return for when they reconnect
            offlineReturns.put(session.getGuestUuid(), originalLoc);
            saveSessions();
        }
    }

    /**
     * Cancels an active or pending ticket.
     */
    public boolean cancelTicketOrVisit(Player issuer, Player target) {
        UUID targetUuid = target.getUniqueId();

        // 1. Check if target is visiting issuer in an active session
        TripSession session = activeSessions.get(targetUuid);
        if (session != null) {
            if (session.getHostUuid().equals(issuer.getUniqueId()) || configManager.isAdmin(issuer)) {
                whiskBack(session, configManager.isAdmin(issuer) && !session.getHostUuid().equals(issuer.getUniqueId())
                        ? ReturnReason.ADMIN : ReturnReason.CANCELLED_BY_HOST);
                MessageUtil.sendMessage(issuer, "&aCancelled trip ticket for &e" + target.getName() + "&a. They have been whisked back.");
                return true;
            }
        }

        // 2. Check if target has an unaccepted pending ticket from issuer
        TripTicket pending = pendingTickets.get(targetUuid);
        if (pending != null && (pending.getHostUuid().equals(issuer.getUniqueId()) || configManager.isAdmin(issuer))) {
            pendingTickets.remove(targetUuid);
            MessageUtil.sendMessage(issuer, "&aCancelled pending ticket invitation sent to &e" + target.getName() + "&a.");
            MessageUtil.sendMessage(target, "&cYour ticket invitation from &e" + issuer.getName() + " &cwas cancelled.");
            return true;
        }

        return false;
    }

    public void cancelWarmup(UUID playerId) {
        BukkitTask task = activeWarmups.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public boolean hasActiveWarmup(UUID playerId) {
        return activeWarmups.containsKey(playerId);
    }

    public TripSession getSession(UUID guestId) {
        return activeSessions.get(guestId);
    }

    public boolean isOnTrip(UUID guestId) {
        return activeSessions.containsKey(guestId);
    }

    public Map<UUID, TripSession> getActiveSessions() {
        return Collections.unmodifiableMap(activeSessions);
    }

    public Map<UUID, Location> getOfflineReturns() {
        return offlineReturns;
    }

    private void applyCooldown(Player player) {
        if (configManager.bypassesCooldown(player)) {
            return;
        }
        long cd = configManager.getCooldownSeconds();
        if (cd > 0) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cd * 1000L));
        }
    }

    public boolean hasCooldown(Player player) {
        if (configManager.bypassesCooldown(player)) {
            return false;
        }
        Long exp = cooldowns.get(player.getUniqueId());
        return exp != null && exp > System.currentTimeMillis();
    }

    public long getRemainingCooldownSeconds(Player player) {
        Long exp = cooldowns.get(player.getUniqueId());
        if (exp == null) return 0;
        return Math.max(0, (exp - System.currentTimeMillis()) / 1000L);
    }

    public void saveSessions() {
        YamlConfiguration config = new YamlConfiguration();

        ConfigurationSection activeSection = config.createSection("active");
        for (Map.Entry<UUID, TripSession> entry : activeSessions.entrySet()) {
            activeSection.createSection(entry.getKey().toString(), entry.getValue().serialize());
        }

        ConfigurationSection offlineSection = config.createSection("offline-returns");
        for (Map.Entry<UUID, Location> entry : offlineReturns.entrySet()) {
            Location loc = entry.getValue();
            if (loc != null && loc.getWorld() != null) {
                ConfigurationSection locSection = offlineSection.createSection(entry.getKey().toString());
                locSection.set("world", loc.getWorld().getName());
                locSection.set("x", loc.getX());
                locSection.set("y", loc.getY());
                locSection.set("z", loc.getZ());
                locSection.set("yaw", (double) loc.getYaw());
                locSection.set("pitch", (double) loc.getPitch());
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save sessions.yml: " + e.getMessage());
        }
    }

    public void loadSessions() {
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        activeSessions.clear();
        offlineReturns.clear();

        ConfigurationSection activeSection = config.getConfigurationSection("active");
        if (activeSection != null) {
            for (String key : activeSection.getKeys(false)) {
                ConfigurationSection sec = activeSection.getConfigurationSection(key);
                if (sec != null) {
                    TripSession session = TripSession.deserialize(sec.getValues(false));
                    if (session != null) {
                        activeSessions.put(session.getGuestUuid(), session);
                    }
                }
            }
        }

        ConfigurationSection offlineSection = config.getConfigurationSection("offline-returns");
        if (offlineSection != null) {
            for (String key : offlineSection.getKeys(false)) {
                ConfigurationSection sec = offlineSection.getConfigurationSection(key);
                if (sec != null) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        String worldName = sec.getString("world");
                        World world = worldName != null ? Bukkit.getWorld(worldName) : null;
                        if (world == null && !Bukkit.getWorlds().isEmpty()) {
                            world = Bukkit.getWorlds().get(0);
                        }
                        double x = sec.getDouble("x");
                        double y = sec.getDouble("y");
                        double z = sec.getDouble("z");
                        float yaw = (float) sec.getDouble("yaw");
                        float pitch = (float) sec.getDouble("pitch");
                        offlineReturns.put(uuid, new Location(world, x, y, z, yaw, pitch));
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    public void onDisable() {
        if (tickerTask != null) {
            tickerTask.cancel();
        }

        for (BukkitTask warmup : activeWarmups.values()) {
            warmup.cancel();
        }
        activeWarmups.clear();

        if (configManager.isReturnOnShutdown()) {
            for (TripSession session : new ArrayList<>(activeSessions.values())) {
                whiskBack(session, ReturnReason.SERVER_SHUTDOWN);
            }
        }

        saveSessions();
    }
}
