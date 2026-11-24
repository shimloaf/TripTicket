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
        if (player == null || soundName == null || soundName.trim().isEmpty() || soundName.equalsIgnoreCase("none")) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(soundName.trim().toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            if (logger != null) {
                logger.warning("Invalid sound configured: '" + soundName + "'.");
            }
        } catch (Throwable ignored) {
        }
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
