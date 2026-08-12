package com.depton.supplydrop;

import com.depton.supplydrop.command.SupplyDropCommand;
import com.depton.supplydrop.config.ConfigManager;
import com.depton.supplydrop.drop.SupplyDropManager;
import com.depton.supplydrop.listener.SupplyDropListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class SupplyDropPlugin extends JavaPlugin {

    //plugin state and managers
    private ConfigManager configManager;
    private SupplyDropManager supplyDropManager;
    private BukkitTask autoDropTask;

    //initialize plugin on startup
    @Override
    public void onEnable() {
        //load configuration settings
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        //initialize drop manager
        supplyDropManager = new SupplyDropManager(this);

        //register event listener
        getServer().getPluginManager().registerEvents(new SupplyDropListener(this), this);

        //register admin commands
        SupplyDropCommand commandExecutor = new SupplyDropCommand(this);
        var cmd = getCommand("supplydrop");
        if (cmd != null) {
            cmd.setExecutor(commandExecutor);
            cmd.setTabCompleter(commandExecutor);
        }

        //start periodic auto drops
        startAutoDropScheduler();

        getLogger().info("SupplyDrop plugin enabled successfully!");
    }

    //cleanup resources on shutdown
    @Override
    public void onDisable() {
        //cancel background tasks
        stopAutoDropScheduler();

        //stop active supply drop
        if (supplyDropManager != null) {
            supplyDropManager.stopActiveDrop();
        }

        getLogger().info("SupplyDrop plugin disabled.");
    }

    //reload configuration settings
    public void reloadPluginConfig() {
        configManager.loadConfig();
        startAutoDropScheduler();
    }

    //start periodic supply drop timer
    private void startAutoDropScheduler() {
        stopAutoDropScheduler();

        if (configManager.isAutoDropEnabled()) {
            long intervalTicks = configManager.getAutoDropIntervalSeconds() * 20L;
            autoDropTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
                //trigger periodic drop
                supplyDropManager.startSupplyDrop(null, null, null);
            }, intervalTicks, intervalTicks);
        }
    }

    //cancel auto drop task
    private void stopAutoDropScheduler() {
        if (autoDropTask != null) {
            autoDropTask.cancel();
            autoDropTask = null;
        }
    }

    //getter for config manager
    public ConfigManager getConfigManager() {
        return configManager;
    }

    //getter for drop manager
    public SupplyDropManager getSupplyDropManager() {
        return supplyDropManager;
    }
}
