package com.vibecode.tripticket;

import com.vibecode.tripticket.command.TripTicketCommand;
import com.vibecode.tripticket.listener.TripListener;
import com.vibecode.tripticket.service.ConfigManager;
import com.vibecode.tripticket.service.TicketManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for TripTicket.
 */
public class TripTicketPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private TicketManager ticketManager;

    @Override
    public void onEnable() {
        // Initialize config manager
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();

        // Initialize ticket manager
        this.ticketManager = new TicketManager(this, configManager);
        this.ticketManager.loadSessions();
        this.ticketManager.startTicker();

        // Register event listener
        TripListener listener = new TripListener(this, configManager, ticketManager);
        getServer().getPluginManager().registerEvents(listener, this);

        // Register commands and tab completion
        TripTicketCommand cmdExecutor = new TripTicketCommand(this, configManager, ticketManager);
        PluginCommand command = getCommand("tripticket");
        if (command != null) {
            command.setExecutor(cmdExecutor);
            command.setTabCompleter(cmdExecutor);
        } else {
            getLogger().severe("Failed to find command 'tripticket' in plugin.yml!");
        }

        getLogger().info("TripTicket v" + getDescription().getVersion() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        if (ticketManager != null) {
            ticketManager.onDisable();
        }
        getLogger().info("TripTicket has been safely disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TicketManager getTicketManager() {
        return ticketManager;
    }
}
