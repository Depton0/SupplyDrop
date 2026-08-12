package com.depton.supplydrop.drop;

import com.depton.supplydrop.SupplyDropPlugin;
import com.depton.supplydrop.config.ConfigManager;
import com.depton.supplydrop.util.BoundingBox2D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Map;

public class SupplyDropManager {

    //plugin reference and active drop tracking
    private final SupplyDropPlugin plugin;
    private SupplyDrop activeDrop;

    //construct manager
    public SupplyDropManager(SupplyDropPlugin plugin) {
        this.plugin = plugin;
    }

    //check if a supply drop is active
    public boolean isDropActive() {
        return activeDrop != null && activeDrop.getState() != SupplyDrop.State.CLEANED_UP;
    }

    //start a new supply drop event
    public boolean startSupplyDrop(World worldOverride, Integer xOverride, Integer zOverride) {
        if (isDropActive()) {
            return false;
        }

        ConfigManager cfg = plugin.getConfigManager();
        World world = worldOverride;

        //resolve target world
        if (world == null) {
            world = Bukkit.getWorld(cfg.getWorldName());
            if (world == null && !Bukkit.getWorlds().isEmpty()) {
                world = Bukkit.getWorlds().get(0);
            }
        }

        if (world == null) {
            return false;
        }

        //find safe surface spawn location
        Location targetLoc = findDropLocation(world, xOverride, zOverride);
        if (targetLoc == null) {
            return false;
        }

        //instantiate new supply drop
        activeDrop = new SupplyDrop(plugin, targetLoc);

        //broadcast start coordinates to server
        broadcastStartMessage(targetLoc);

        //launch drop animation
        activeDrop.startDrop();
        return true;
    }

    //stop current active drop
    public boolean stopActiveDrop() {
        if (!isDropActive()) {
            return false;
        }
        activeDrop.cleanup();
        activeDrop = null;
        return true;
    }

    //calculate non-liquid surface location in bounding box
    private Location findDropLocation(World world, Integer xOverride, Integer zOverride) {
        ConfigManager cfg = plugin.getConfigManager();
        BoundingBox2D box = cfg.getBoundingBox();

        int targetX = xOverride != null ? xOverride : box.getRandomX();
        int targetZ = zOverride != null ? zOverride : box.getRandomZ();

        //find non-liquid surface block
        if (xOverride == null || zOverride == null) {
            for (int attempt = 0; attempt < 10; attempt++) {
                Block highest = world.getHighestBlockAt(targetX, targetZ);
                if (!highest.isLiquid()) {
                    return highest.getLocation().add(0, 1, 0);
                }
                targetX = box.getRandomX();
                targetZ = box.getRandomZ();
            }
        }

        Block highest = world.getHighestBlockAt(targetX, targetZ);
        return highest.getLocation().add(0, 1, 0);
    }

    //broadcast start message to chat
    private void broadcastStartMessage(Location targetLoc) {
        ConfigManager cfg = plugin.getConfigManager();
        Map<String, String> placeholders = Map.of(
                "x", String.valueOf(targetLoc.getBlockX()),
                "y", String.valueOf(targetLoc.getBlockY()),
                "z", String.valueOf(targetLoc.getBlockZ()),
                "world", targetLoc.getWorld().getName()
        );
        Bukkit.broadcast(cfg.formatMessage(cfg.getDropStartMsg(), placeholders));
    }

    //getter for current active drop
    public SupplyDrop getActiveDrop() {
        if (activeDrop != null && activeDrop.getState() == SupplyDrop.State.CLEANED_UP) {
            activeDrop = null;
        }
        return activeDrop;
    }
}
