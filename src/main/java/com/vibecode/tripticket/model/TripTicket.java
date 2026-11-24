package com.vibecode.tripticket.model;

import java.util.UUID;

/**
 * Represents an invitation ticket issued by a host to a guest player.
 */
public class TripTicket {

    private final UUID hostUuid;
    private final String hostName;
    private final String hostWorldName;
    private final UUID guestUuid;
    private final String guestName;
    private final long issuedAtMillis;
    private final long durationSeconds;

    public TripTicket(UUID hostUuid,
                      String hostName,
                      String hostWorldName,
                      UUID guestUuid,
                      String guestName,
                      long durationSeconds) {
        this.hostUuid = hostUuid;
        this.hostName = hostName;
        this.hostWorldName = hostWorldName;
        this.guestUuid = guestUuid;
        this.guestName = guestName;
        this.issuedAtMillis = System.currentTimeMillis();
        this.durationSeconds = durationSeconds;
    }

    public UUID getHostUuid() {
        return hostUuid;
    }

    public String getHostName() {
        return hostName;
    }

    public String getHostWorldName() {
        return hostWorldName;
    }

    public UUID getGuestUuid() {
        return guestUuid;
    }

    public String getGuestName() {
        return guestName;
    }

    public long getIssuedAtMillis() {
        return issuedAtMillis;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public boolean isExpired(long timeoutSeconds) {
        long elapsedSeconds = (System.currentTimeMillis() - issuedAtMillis) / 1000L;
        return elapsedSeconds > timeoutSeconds;
    }
}
