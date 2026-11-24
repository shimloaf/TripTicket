package com.vibecode.tripticket.listener;

import com.vibecode.tripticket.TripTicketPlugin;
import com.vibecode.tripticket.model.TripSession;
import com.vibecode.tripticket.service.ConfigManager;
import com.vibecode.tripticket.service.LocationSafety;
import com.vibecode.tripticket.service.TicketManager;
import com.vibecode.tripticket.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for player movement, damage, death, disconnects, and reconnections.
 */
public class TripListener implements Listener {

    private final TripTicketPlugin plugin;
    private final ConfigManager configManager;
    private final TicketManager ticketManager;

    private final Map<UUID, Location> pendingRespawnReturns = new ConcurrentHashMap<>();

    public TripListener(TripTicketPlugin plugin, ConfigManager configManager, TicketManager ticketManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.ticketManager = ticketManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        ticketManager.cancelWarmup(uuid);

        TripSession session = ticketManager.getSession(uuid);
        if (session != null) {
            if (configManager.isReturnOnDisconnect()) {
                Location safeLoc = LocationSafety.getSafeLocation(session.getGuestOriginalLocation(), configManager.isSafeTeleportCheck());
                player.teleport(safeLoc);
                ticketManager.whiskBack(session, TicketManager.ReturnReason.CANCELLED_BY_HOST);
            } else {
                ticketManager.whiskBack(session, TicketManager.ReturnReason.CANCELLED_BY_HOST);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Location returnLoc = ticketManager.getOfflineReturns().remove(uuid);
        if (returnLoc != null) {
            Location safeLoc = LocationSafety.getSafeLocation(returnLoc, configManager.isSafeTeleportCheck());
            player.teleport(safeLoc);
            ticketManager.saveSessions();

            MessageUtil.sendMessage(player, configManager.getMessage("offline-return-notice"));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        ticketManager.cancelWarmup(uuid);

        TripSession session = ticketManager.getSession(uuid);
        if (session != null) {
            String onDeath = configManager.getOnDeath().toLowerCase();
            if ("return".equals(onDeath)) {
                pendingRespawnReturns.put(uuid, session.getGuestOriginalLocation());
            }

            ticketManager.whiskBack(session, TicketManager.ReturnReason.DEATH);
            MessageUtil.sendMessage(player, "&cYour trip ticket ended due to death.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location returnLoc = pendingRespawnReturns.remove(player.getUniqueId());
        if (returnLoc != null) {
            Location safeLoc = LocationSafety.getSafeLocation(returnLoc, configManager.isSafeTeleportCheck());
            event.setRespawnLocation(safeLoc);
            MessageUtil.sendMessage(player, configManager.getMessage("ticket-expired-guest"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!configManager.isCancelWarmupOnMove()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (ticketManager.hasActiveWarmup(uuid)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to == null) return;

            if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
                ticketManager.cancelWarmup(uuid);
                MessageUtil.sendMessage(player, configManager.getMessage("warmup-cancelled-move"));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!configManager.isCancelWarmupOnDamage()) {
            return;
        }

        if (event.getEntity() instanceof Player player) {
            UUID uuid = player.getUniqueId();
            if (ticketManager.hasActiveWarmup(uuid)) {
                ticketManager.cancelWarmup(uuid);
                MessageUtil.sendMessage(player, configManager.getMessage("warmup-cancelled-damage"));
            }
        }
    }
}
