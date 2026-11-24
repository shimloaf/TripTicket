package com.vibecode.tripticket.service;

import com.vibecode.tripticket.TripTicketPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages TripTicket configuration settings, permissions, and messages.
 */
public class ConfigManager {

    private final TripTicketPlugin plugin;

    // Ticket settings
    private long defaultDurationSeconds;
    private long warningSeconds;
    private List<Integer> countdownSeconds;
    private long inviteTimeoutSeconds;
    private long cooldownSeconds;
    private long warmupSeconds;
    private boolean cancelWarmupOnMove;
    private boolean cancelWarmupOnDamage;
    private String onDeath;
    private boolean returnOnDisconnect;
    private boolean returnOnShutdown;
    private boolean safeTeleportCheck;

    // Permissions settings
    private boolean allowEveryoneDefaultDuration;
    private boolean customDurationOpsOnly;
    private boolean allowEveryoneAccept;
    private boolean allowEveryoneReturn;
    private boolean allowEveryoneCheckTime;
    private boolean allowEveryoneRip;

    // Permission node names
    private String permNodeInvite;
    private String permNodeAccept;
    private String permNodeReturn;
    private String permNodeCheckTime;
    private String permNodeCustomDuration;
    private String permNodeBypassCooldown;
    private String permNodeBypassWarmup;
    private String permNodeReload;
    private String permNodeAdmin;
    private String permNodeRip;
    private String permNodeSetRip;

    // Visuals & Audio
    private boolean showActionBar;
    private boolean showTitleOnWarning;
    private boolean playSounds;
    private String soundEmbark;
    private String soundWarning;
    private String soundReturn;
    private String soundWhisk;
    private boolean playParticles;
    private String particleEmbark;
    private String particleReturn;

    // RIP settings
    private long ripCountdownSeconds;
    private boolean ripCancelOnMove;
    private boolean ripCancelOnDamage;
    private Location ripAnchor;
    private String ripSoundCountdown;
    private String ripSoundTear;
    private String ripSoundPaper;

    // Self-Trip settings
    private boolean selfTripEnabled;
    private long selfTripConfirmTimeoutSeconds;
    private int selfTripDefaultRange;
    private final Map<String, Integer> selfTripAllowedWorlds = new HashMap<>();

    // Messages
    private String prefix;
    private final Map<String, String> messages = new HashMap<>();

    public ConfigManager(TripTicketPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Ticket
        this.defaultDurationSeconds = config.getLong("ticket.duration-seconds", 180);
        this.warningSeconds = config.getLong("ticket.warning-seconds", 10);
        this.countdownSeconds = config.getIntegerList("ticket.countdown-seconds");
        if (this.countdownSeconds.isEmpty()) {
            this.countdownSeconds = List.of(10, 5, 4, 3, 2, 1);
        }
        this.inviteTimeoutSeconds = config.getLong("ticket.invite-timeout-seconds", 60);
        this.cooldownSeconds = config.getLong("ticket.cooldown-seconds", 0);
        this.warmupSeconds = config.getLong("ticket.warmup-seconds", 0);
        this.cancelWarmupOnMove = config.getBoolean("ticket.cancel-warmup-on-move", true);
        this.cancelWarmupOnDamage = config.getBoolean("ticket.cancel-warmup-on-damage", true);
        this.onDeath = config.getString("ticket.on-death", "cancel");
        this.returnOnDisconnect = config.getBoolean("ticket.return-on-disconnect", true);
        this.returnOnShutdown = config.getBoolean("ticket.return-on-shutdown", true);
        this.safeTeleportCheck = config.getBoolean("ticket.safe-teleport-check", true);

        // Permissions
        this.allowEveryoneDefaultDuration = config.getBoolean("permissions.allow-everyone-default-duration", true);
        this.customDurationOpsOnly = config.getBoolean("permissions.custom-duration-ops-only", true);
        this.allowEveryoneAccept = config.getBoolean("permissions.allow-everyone-accept", true);
        this.allowEveryoneReturn = config.getBoolean("permissions.allow-everyone-return", true);
        this.allowEveryoneCheckTime = config.getBoolean("permissions.allow-everyone-check-time", true);
        this.allowEveryoneRip = config.getBoolean("permissions.allow-everyone-rip", true);

        // Permission Nodes
        this.permNodeInvite = config.getString("permissions.nodes.invite", "tripticket.invite");
        this.permNodeAccept = config.getString("permissions.nodes.accept", "tripticket.use");
        this.permNodeReturn = config.getString("permissions.nodes.return", "tripticket.return");
        this.permNodeCheckTime = config.getString("permissions.nodes.check-time", "tripticket.time");
        this.permNodeCustomDuration = config.getString("permissions.nodes.custom-duration", "tripticket.customduration");
        this.permNodeBypassCooldown = config.getString("permissions.nodes.bypass-cooldown", "tripticket.cooldown.bypass");
        this.permNodeBypassWarmup = config.getString("permissions.nodes.bypass-warmup", "tripticket.warmup.bypass");
        this.permNodeReload = config.getString("permissions.nodes.reload", "tripticket.reload");
        this.permNodeAdmin = config.getString("permissions.nodes.admin", "tripticket.admin");
        this.permNodeRip = config.getString("permissions.nodes.rip", "tripticket.rip");
        this.permNodeSetRip = config.getString("permissions.nodes.setrip", "tripticket.setrip");

        // RIP Settings
        this.ripCountdownSeconds = config.getLong("rip.countdown-seconds", 3);
        this.ripCancelOnMove = config.getBoolean("rip.cancel-on-move", true);
        this.ripCancelOnDamage = config.getBoolean("rip.cancel-on-damage", true);
        String ripWorld = config.getString("rip.anchor.world", "");
        if (!ripWorld.isEmpty()) {
            World w = Bukkit.getWorld(ripWorld);
            double x = config.getDouble("rip.anchor.x", 0.0);
            double y = config.getDouble("rip.anchor.y", 64.0);
            double z = config.getDouble("rip.anchor.z", 0.0);
            float yaw = (float) config.getDouble("rip.anchor.yaw", 0.0);
            float pitch = (float) config.getDouble("rip.anchor.pitch", 0.0);
            this.ripAnchor = new Location(w, x, y, z, yaw, pitch);
        } else {
            this.ripAnchor = null;
        }

        // Self-Trip Settings
        this.selfTripEnabled = config.getBoolean("self-trip.enabled", true);
        this.selfTripConfirmTimeoutSeconds = config.getLong("self-trip.confirm-timeout-seconds", 30);
        this.selfTripDefaultRange = config.getInt("self-trip.default-range", 10000);
        this.selfTripAllowedWorlds.clear();
        if (config.isConfigurationSection("self-trip.allowed-worlds")) {
            ConfigurationSection sec = config.getConfigurationSection("self-trip.allowed-worlds");
            for (String worldKey : sec.getKeys(false)) {
                int range = sec.getInt(worldKey, this.selfTripDefaultRange);
                this.selfTripAllowedWorlds.put(worldKey, range);
            }
        } else if (config.isList("self-trip.allowed-worlds")) {
            for (String worldName : config.getStringList("self-trip.allowed-worlds")) {
                this.selfTripAllowedWorlds.put(worldName, this.selfTripDefaultRange);
            }
        }

        // Visuals
        this.showActionBar = config.getBoolean("visuals.show-actionbar", true);
        this.showTitleOnWarning = config.getBoolean("visuals.show-title-on-warning", true);
        this.playSounds = config.getBoolean("visuals.play-sounds", true);
        this.soundEmbark = config.getString("visuals.sounds.embark", "ENTITY_BREEZE_SHOOT");
        this.soundWarning = config.getString("visuals.sounds.warning", "BLOCK_NOTE_BLOCK_PLING");
        this.soundReturn = config.getString("visuals.sounds.return", "ENTITY_BREEZE_IDLE_AIR");
        this.soundWhisk = config.getString("visuals.sounds.whisk", "ENTITY_BREEZE_SHOOT");
        this.ripSoundCountdown = config.getString("rip.sounds.countdown", config.getString("visuals.sounds.rip-countdown", "ITEM_BOOK_PAGE_TURN"));
        this.ripSoundTear = config.getString("rip.sounds.tear", config.getString("visuals.sounds.rip", "ENTITY_SHEEP_SHEAR"));
        this.ripSoundPaper = config.getString("rip.sounds.paper", config.getString("visuals.sounds.rip-paper", "ITEM_BOOK_PAGE_TURN"));
        this.playParticles = config.getBoolean("visuals.play-particles", true);
        this.particleEmbark = config.getString("visuals.particles.embark", "PORTAL");
        this.particleReturn = config.getString("visuals.particles.return", "REVERSE_PORTAL");

        // Messages
        this.prefix = config.getString("messages.prefix", "&8[&6TripTicket&8]&r ");
        this.messages.clear();
        if (config.isConfigurationSection("messages")) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                this.messages.put(key, config.getString("messages." + key, ""));
            }
        }
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String template = messages.getOrDefault(key, "&cMissing message: " + key);
        String result = template.replace("%prefix%", prefix);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                result = result.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return result;
    }

    public String getMessage(String key) {
        return getMessage(key, Collections.emptyMap());
    }

    // Permission Check Helpers
    public boolean canInvite(Player player) {
        if (player.isOp()) return true;
        if (allowEveryoneDefaultDuration) return true;
        return player.hasPermission(permNodeInvite) || player.hasPermission(permNodeAccept);
    }

    public boolean canUseCustomDuration(Player player) {
        if (player.isOp()) return true;
        if (player.hasPermission(permNodeCustomDuration)) return true;
        if (!customDurationOpsOnly) return canInvite(player);
        return false;
    }

    public boolean canAccept(Player player) {
        if (player.isOp()) return true;
        if (allowEveryoneAccept) return true;
        return player.hasPermission(permNodeAccept);
    }

    public boolean canReturn(Player player) {
        if (player.isOp()) return true;
        if (allowEveryoneReturn) return true;
        return player.hasPermission(permNodeReturn);
    }

    public boolean canCheckTime(Player player) {
        if (player.isOp()) return true;
        if (allowEveryoneCheckTime) return true;
        return player.hasPermission(permNodeCheckTime);
    }

    public boolean canRip(Player player) {
        if (player.isOp()) return true;
        if (allowEveryoneRip) return true;
        return player.hasPermission(permNodeRip);
    }

    public boolean canSetRip(CommandSender sender) {
        if (sender.isOp()) return true;
        return sender.hasPermission(permNodeSetRip) || sender.hasPermission(permNodeAdmin);
    }

    public void setRipAnchor(Location loc) {
        this.ripAnchor = loc.clone();
        FileConfiguration config = plugin.getConfig();
        config.set("rip.anchor.world", loc.getWorld().getName());
        config.set("rip.anchor.x", loc.getX());
        config.set("rip.anchor.y", loc.getY());
        config.set("rip.anchor.z", loc.getZ());
        config.set("rip.anchor.yaw", (double) loc.getYaw());
        config.set("rip.anchor.pitch", (double) loc.getPitch());
        plugin.saveConfig();
    }

    public Location getRipAnchor() {
        if (ripAnchor != null && ripAnchor.getWorld() == null) {
            String worldName = plugin.getConfig().getString("rip.anchor.world", "");
            if (!worldName.isEmpty()) {
                World w = Bukkit.getWorld(worldName);
                if (w != null) {
                    ripAnchor.setWorld(w);
                }
            }
        }
        return ripAnchor != null && ripAnchor.getWorld() != null ? ripAnchor.clone() : null;
    }

    public int getRangeForWorld(String worldName) {
        if (worldName == null) return selfTripDefaultRange;
        return selfTripAllowedWorlds.getOrDefault(worldName, selfTripDefaultRange);
    }

    public boolean canReload(CommandSender sender) {
        if (sender.isOp()) return true;
        return sender.hasPermission(permNodeReload) || sender.hasPermission(permNodeAdmin);
    }

    public boolean isAdmin(CommandSender sender) {
        if (sender.isOp()) return true;
        return sender.hasPermission(permNodeAdmin);
    }

    public boolean bypassesCooldown(Player player) {
        if (player.isOp()) return true;
        return player.hasPermission(permNodeBypassCooldown);
    }

    public boolean bypassesWarmup(Player player) {
        if (player.isOp()) return true;
        return player.hasPermission(permNodeBypassWarmup);
    }

    public static long parseDuration(String input) {
        if (input == null || input.trim().isEmpty()) {
            return -1;
        }
        input = input.trim().toLowerCase();

        try {
            if (input.endsWith("s")) {
                return Long.parseLong(input.substring(0, input.length() - 1));
            } else if (input.endsWith("m")) {
                return Long.parseLong(input.substring(0, input.length() - 1)) * 60L;
            } else if (input.endsWith("h")) {
                return Long.parseLong(input.substring(0, input.length() - 1)) * 3600L;
            } else {
                return Long.parseLong(input);
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String formatDuration(long seconds) {
        if (seconds >= 60) {
            long minutes = seconds / 60;
            long rem = seconds % 60;
            if (rem == 0) {
                return minutes + (minutes == 1 ? " minute" : " minutes");
            }
            return minutes + "m " + rem + "s";
        }
        return seconds + (seconds == 1 ? " second" : " seconds");
    }

    // Getters
    public long getDefaultDurationSeconds() { return defaultDurationSeconds; }
    public long getWarningSeconds() { return warningSeconds; }
    public List<Integer> getCountdownSeconds() { return countdownSeconds; }
    public long getInviteTimeoutSeconds() { return inviteTimeoutSeconds; }
    public long getCooldownSeconds() { return cooldownSeconds; }
    public long getWarmupSeconds() { return warmupSeconds; }
    public boolean isCancelWarmupOnMove() { return cancelWarmupOnMove; }
    public boolean isCancelWarmupOnDamage() { return cancelWarmupOnDamage; }
    public String getOnDeath() { return onDeath; }
    public boolean isReturnOnDisconnect() { return returnOnDisconnect; }
    public boolean isReturnOnShutdown() { return returnOnShutdown; }
    public boolean isSafeTeleportCheck() { return safeTeleportCheck; }
    public boolean isAllowEveryoneDefaultDuration() { return allowEveryoneDefaultDuration; }
    public boolean isCustomDurationOpsOnly() { return customDurationOpsOnly; }
    public boolean isAllowEveryoneAccept() { return allowEveryoneAccept; }
    public boolean isAllowEveryoneReturn() { return allowEveryoneReturn; }
    public boolean isAllowEveryoneCheckTime() { return allowEveryoneCheckTime; }
    public boolean isShowActionBar() { return showActionBar; }
    public boolean isShowTitleOnWarning() { return showTitleOnWarning; }
    public boolean isPlaySounds() { return playSounds; }
    public String getSoundEmbark() { return soundEmbark; }
    public String getSoundWarning() { return soundWarning; }
    public String getSoundReturn() { return soundReturn; }
    public String getSoundWhisk() { return soundWhisk; }
    public String getRipSoundCountdown() { return ripSoundCountdown; }
    public String getRipSoundTear() { return ripSoundTear; }
    public String getRipSoundPaper() { return ripSoundPaper; }
    public boolean isPlayParticles() { return playParticles; }
    public String getParticleEmbark() { return particleEmbark; }
    public String getParticleReturn() { return particleReturn; }
    public String getPrefix() { return prefix; }

    // RIP Getters
    public long getRipCountdownSeconds() { return ripCountdownSeconds; }
    public boolean isRipCancelOnMove() { return ripCancelOnMove; }
    public boolean isRipCancelOnDamage() { return ripCancelOnDamage; }

    // Self-Trip Getters
    public boolean isSelfTripEnabled() { return selfTripEnabled; }
    public long getSelfTripConfirmTimeoutSeconds() { return selfTripConfirmTimeoutSeconds; }
    public int getSelfTripDefaultRange() { return selfTripDefaultRange; }
    public Map<String, Integer> getSelfTripAllowedWorlds() { return Collections.unmodifiableMap(selfTripAllowedWorlds); }
}
