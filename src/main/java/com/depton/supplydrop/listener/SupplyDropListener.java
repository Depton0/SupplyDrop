package com.depton.supplydrop.listener;

import com.depton.supplydrop.SupplyDropPlugin;
import com.depton.supplydrop.drop.SupplyDrop;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SupplyDropListener implements Listener {

    //plugin reference
    private final SupplyDropPlugin plugin;

    //construct listener
    public SupplyDropListener(SupplyDropPlugin plugin) {
        this.plugin = plugin;
    }

    //handle right click interactions on landed crate
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        SupplyDrop active = plugin.getSupplyDropManager().getActiveDrop();
        if (active == null || active.getState() != SupplyDrop.State.LANDED || active.isLooted()) {
            return;
        }

        Location target = active.getTargetLocation();
        if (isSameBlock(block.getLocation(), target)) {
            //trigger claim event
            active.claim(event.getPlayer().getName());
        }
    }

    //prevent block breaking target while drop is falling
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        SupplyDrop active = plugin.getSupplyDropManager().getActiveDrop();
        if (active != null && active.getState() == SupplyDrop.State.FALLING && isSameBlock(block.getLocation(), active.getTargetLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getConfigManager().formatMessage("&cYou cannot break the target location while a supply drop is falling!", null));
        }
    }

    //protect active drop crate from entity explosions
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        SupplyDrop active = plugin.getSupplyDropManager().getActiveDrop();
        if (active != null && active.getState() == SupplyDrop.State.LANDED) {
            Location target = active.getTargetLocation();
            event.blockList().removeIf(b -> isSameBlock(b.getLocation(), target));
        }
    }

    //protect active drop crate from block explosions
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        SupplyDrop active = plugin.getSupplyDropManager().getActiveDrop();
        if (active != null && active.getState() == SupplyDrop.State.LANDED) {
            Location target = active.getTargetLocation();
            event.blockList().removeIf(b -> isSameBlock(b.getLocation(), target));
        }
    }

    //check if two locations point to same block
    private boolean isSameBlock(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null || loc1.getWorld() == null || loc2.getWorld() == null) {
            return false;
        }
        return loc1.getWorld().equals(loc2.getWorld())
                && loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }
}
