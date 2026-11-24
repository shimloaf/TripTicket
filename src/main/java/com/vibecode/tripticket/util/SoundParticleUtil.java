package com.vibecode.tripticket.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Utility for playing sounds and particle effects safely.
 */
public final class SoundParticleUtil {

    private SoundParticleUtil() {
    }

    public static void playSound(Player player, String soundName, float volume, float pitch, Logger logger) {
        playSound(player, soundName, volume, pitch, null, logger);
    }

    public static void playSound(Player player, String soundName, float volume, float pitch, String fallbackSoundName, Logger logger) {
        if (player == null || soundName == null || soundName.trim().isEmpty() || soundName.equalsIgnoreCase("none")) {
            return;
        }

        Location loc = player.getLocation();
        if (loc.getWorld() == null) {
            return;
        }

        Sound sound = resolveSound(soundName, fallbackSoundName, logger);
        if (sound != null) {
            try {
                loc.getWorld().playSound(loc, sound, volume, pitch);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void playSound(Location location, String soundName, float volume, float pitch, String fallbackSoundName, Logger logger) {
        if (location == null || location.getWorld() == null || soundName == null || soundName.trim().isEmpty() || soundName.equalsIgnoreCase("none")) {
            return;
        }

        Sound sound = resolveSound(soundName, fallbackSoundName, logger);
        if (sound != null) {
            try {
                location.getWorld().playSound(location, sound, volume, pitch);
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Plays a layered acoustic ticket ripping sound: crisp snip/cut (shears) combined with fluttering paper tear (book page turn) and fibrous rip (wool break).
     */
    public static void playRipTicketSound(Player player, String tearSound, String paperSound, Logger logger) {
        if (player == null) return;
        Location loc = player.getLocation();
        if (loc.getWorld() == null) return;

        // 1. Crisp tear / snip
        playSound(loc, tearSound != null ? tearSound : "ENTITY_SHEEP_SHEAR", 1.0f, 1.15f, "ENTITY_SHEEP_SHEAR", logger);
        // 2. Paper rustle / page flutter
        playSound(loc, paperSound != null ? paperSound : "ITEM_BOOK_PAGE_TURN", 1.0f, 0.85f, "ITEM_BOOK_PAGE_TURN", logger);
        // 3. Fibrous fabric rip
        playSound(loc, "BLOCK_WOOL_BREAK", 0.8f, 1.25f, null, logger);
    }

    /**
     * Plays the Breeze wind burst effect when a player is whisked away.
     */
    public static void playBreezeWhiskSound(Player player, String breezeSound, Logger logger) {
        if (player == null) return;
        Location loc = player.getLocation();
        if (loc.getWorld() == null) return;

        String soundToPlay = (breezeSound != null && !breezeSound.equalsIgnoreCase("none")) ? breezeSound : "ENTITY_BREEZE_SHOOT";
        playSound(loc, soundToPlay, 1.0f, 1.0f, "ENTITY_GENERIC_WIND_BURST", logger);
    }

    private static Sound resolveSound(String primary, String fallback, Logger logger) {
        if (primary != null && !primary.trim().isEmpty() && !primary.equalsIgnoreCase("none")) {
            try {
                return Sound.valueOf(primary.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                if (fallback == null && logger != null) {
                    logger.warning("Invalid sound configured: '" + primary + "'.");
                }
            }
        }
        if (fallback != null && !fallback.trim().isEmpty() && !fallback.equalsIgnoreCase("none")) {
            try {
                return Sound.valueOf(fallback.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                if (logger != null) {
                    logger.warning("Invalid fallback sound: '" + fallback + "'.");
                }
            }
        }
        return null;
    }

    public static void spawnParticles(Location location, String particleName, int count, double offsetX, double offsetY, double offsetZ, Logger logger) {
        if (location == null || location.getWorld() == null || particleName == null || particleName.trim().isEmpty() || particleName.equalsIgnoreCase("none")) {
            return;
        }

        try {
            Particle particle = Particle.valueOf(particleName.trim().toUpperCase());
            location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, 0.05);
        } catch (IllegalArgumentException e) {
            if (logger != null) {
                logger.warning("Invalid particle configured: '" + particleName + "'.");
            }
        } catch (Throwable ignored) {
        }
    }
}
