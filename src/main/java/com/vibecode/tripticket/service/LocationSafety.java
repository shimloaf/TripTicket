package com.vibecode.tripticket.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Utility to verify and adjust return destinations to ensure player safety.
 */
public final class LocationSafety {

    private LocationSafety() {
    }

    public static Location getSafeLocation(Location candidate, boolean performSafetyCheck) {
        if (candidate == null) {
            return getFallbackLocation();
        }

        World world = candidate.getWorld();
        if (world == null) {
            world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (world == null) {
                return null;
            }
            candidate.setWorld(world);
        }

        if (!performSafetyCheck) {
            return candidate;
        }

        if (isLocationSafe(candidate)) {
            return candidate;
        }

        int blockX = candidate.getBlockX();
        int blockY = candidate.getBlockY();
        int blockZ = candidate.getBlockZ();

        int maxSearchY = Math.min(world.getMaxHeight() - 2, blockY + 15);
        for (int y = blockY; y <= maxSearchY; y++) {
            Location check = new Location(world, blockX + 0.5, y, blockZ + 0.5, candidate.getYaw(), candidate.getPitch());
            if (isLocationSafe(check)) {
                return check;
            }
        }

        int minSearchY = Math.max(world.getMinHeight(), blockY - 15);
        for (int y = blockY; y >= minSearchY; y--) {
            Location check = new Location(world, blockX + 0.5, y, blockZ + 0.5, candidate.getYaw(), candidate.getPitch());
            if (isLocationSafe(check)) {
                return check;
            }
        }

        int highestY = world.getHighestBlockYAt(blockX, blockZ);
        if (highestY > world.getMinHeight()) {
            Location surfaceLoc = new Location(world, blockX + 0.5, highestY + 1.0, blockZ + 0.5, candidate.getYaw(), candidate.getPitch());
            if (isLocationSafe(surfaceLoc)) {
                return surfaceLoc;
            }
        }

        return candidate;
    }

    public static boolean isLocationSafe(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        if (y < world.getMinHeight() || y >= world.getMaxHeight() - 1) {
            return false;
        }

        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block below = world.getBlockAt(x, y - 1, z);

        if (!isPassable(feet) || !isPassable(head)) {
            return false;
        }

        if (isHazard(feet) || isHazard(head)) {
            return false;
        }

        if (below.isPassable() || isHazard(below)) {
            return false;
        }

        return true;
    }

    private static boolean isPassable(Block block) {
        Material type = block.getType();
        return block.isPassable() || type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR;
    }

    private static boolean isHazard(Block block) {
        Material type = block.getType();
        return type == Material.LAVA || type == Material.FIRE || type == Material.SOUL_FIRE ||
                type == Material.SWEET_BERRY_BUSH || type == Material.WITHER_ROSE || type == Material.CACTUS;
    }

    private static Location getFallbackLocation() {
        if (Bukkit.getWorlds().isEmpty()) {
            return null;
        }
        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }
}
