package com.vibecode.tripticket.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an active trip ticket visit session.
 * The guest is visiting the host's world temporarily.
 */
public class TripSession {

    private final UUID guestUuid;
    private final String guestName;
    private final Location guestOriginalLocation;
    private final UUID hostUuid;
    private final String hostName;
    private final long startTimeMillis;
    private final long durationSeconds;
    private final long warningSeconds;
    private boolean warningSent;

    public TripSession(UUID guestUuid,
                       String guestName,
                       Location guestOriginalLocation,
                       UUID hostUuid,
                       String hostName,
                       long startTimeMillis,
                       long durationSeconds,
                       long warningSeconds) {
        this.guestUuid = guestUuid;
        this.guestName = guestName;
        this.guestOriginalLocation = guestOriginalLocation;
        this.hostUuid = hostUuid;
        this.hostName = hostName;
        this.startTimeMillis = startTimeMillis;
        this.durationSeconds = durationSeconds;
        this.warningSeconds = warningSeconds;
        this.warningSent = false;
    }

    public UUID getGuestUuid() {
        return guestUuid;
    }

    public String getGuestName() {
        return guestName;
    }

    public Location getGuestOriginalLocation() {
        return guestOriginalLocation.clone();
    }

    public UUID getHostUuid() {
        return hostUuid;
    }

    public String getHostName() {
        return hostName;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public long getWarningSeconds() {
        return warningSeconds;
    }

    public boolean isWarningSent() {
        return warningSent;
    }

    public void setWarningSent(boolean warningSent) {
        this.warningSent = warningSent;
    }

    /**
     * Calculates remaining seconds on this trip ticket.
     */
    public long getRemainingSeconds() {
        long elapsedSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000L;
        long remaining = durationSeconds - elapsedSeconds;
        return Math.max(0, remaining);
    }

    public boolean isExpired() {
        return getRemainingSeconds() <= 0;
    }

    public boolean isWarningDue() {
        return getRemainingSeconds() <= warningSeconds;
    }

    /**
     * Formats remaining time nicely (e.g. "2m 45s" or "8s").
     */
    public String getFormattedRemainingTime() {
        long remaining = getRemainingSeconds();
        if (remaining >= 60) {
            long minutes = remaining / 60;
            long seconds = remaining % 60;
            if (seconds == 0) {
                return minutes + "m";
            }
            return String.format("%dm %02ds", minutes, seconds);
        }
        return remaining + "s";
    }

    /**
     * Serializes this session to a map for YAML storage.
     */
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("guestUuid", guestUuid.toString());
        map.put("guestName", guestName);
        map.put("hostUuid", hostUuid != null ? hostUuid.toString() : "");
        map.put("hostName", hostName != null ? hostName : "");
        map.put("startTimeMillis", startTimeMillis);
        map.put("durationSeconds", durationSeconds);
        map.put("warningSeconds", warningSeconds);
        map.put("warningSent", warningSent);

        if (guestOriginalLocation != null && guestOriginalLocation.getWorld() != null) {
            map.put("world", guestOriginalLocation.getWorld().getName());
            map.put("x", guestOriginalLocation.getX());
            map.put("y", guestOriginalLocation.getY());
            map.put("z", guestOriginalLocation.getZ());
            map.put("yaw", (double) guestOriginalLocation.getYaw());
            map.put("pitch", (double) guestOriginalLocation.getPitch());
        }
        return map;
    }

    /**
     * Deserializes a session from a map.
     */
    public static TripSession deserialize(Map<String, Object> map) {
        try {
            UUID guestUuid = UUID.fromString((String) map.get("guestUuid"));
            String guestName = (String) map.getOrDefault("guestName", "Unknown");

            String hostUuidStr = (String) map.getOrDefault("hostUuid", "");
            UUID hostUuid = hostUuidStr.isEmpty() ? null : UUID.fromString(hostUuidStr);
            String hostName = (String) map.getOrDefault("hostName", "Unknown");

            long startTimeMillis = ((Number) map.get("startTimeMillis")).longValue();
            long durationSeconds = ((Number) map.get("durationSeconds")).longValue();
            long warningSeconds = ((Number) map.getOrDefault("warningSeconds", 10)).longValue();

            String worldName = (String) map.get("world");
            World world = worldName != null ? Bukkit.getWorld(worldName) : null;
            if (world == null && !Bukkit.getWorlds().isEmpty()) {
                world = Bukkit.getWorlds().get(0);
            }

            double x = ((Number) map.getOrDefault("x", 0.0)).doubleValue();
            double y = ((Number) map.getOrDefault("y", 64.0)).doubleValue();
            double z = ((Number) map.getOrDefault("z", 0.0)).doubleValue();
            float yaw = ((Number) map.getOrDefault("yaw", 0.0)).floatValue();
            float pitch = ((Number) map.getOrDefault("pitch", 0.0)).floatValue();

            Location location = new Location(world, x, y, z, yaw, pitch);

            TripSession session = new TripSession(
                    guestUuid, guestName, location,
                    hostUuid, hostName, startTimeMillis, durationSeconds, warningSeconds
            );
            session.setWarningSent((Boolean) map.getOrDefault("warningSent", false));
            return session;
        } catch (Exception e) {
            return null;
        }
    }
}
