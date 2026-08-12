package com.depton.supplydrop.drop;

import com.depton.supplydrop.SupplyDropPlugin;
import com.depton.supplydrop.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SupplyDrop {

    //supply drop lifecycle states
    public enum State {
        FALLING,
        LANDED,
        CLEANED_UP
    }

    private final SupplyDropPlugin plugin;
    private final Location targetLocation;
    private final Location startLocation;
    private Location currentLocation;

    private State state;
    private ArmorStand crateEntity;
    private ArmorStand parachuteEntity;

    private BukkitTask fallTask;
    private BukkitTask landedEffectTask;
    private BukkitTask despawnTask;

    private boolean looted = false;
    private int soundCounter = 0;

    //construct new supply drop instance
    public SupplyDrop(SupplyDropPlugin plugin, Location targetLocation) {
        this.plugin = plugin;
        this.targetLocation = targetLocation.clone();
        ConfigManager cfg = plugin.getConfigManager();
        this.startLocation = targetLocation.clone().add(0, cfg.getHeightAboveSurface(), 0);
        this.currentLocation = startLocation.clone();
        this.state = State.FALLING;
    }

    //start drop animation and tasks
    public void startDrop() {
        ConfigManager cfg = plugin.getConfigManager();

        //spawn falling crate armorstand
        crateEntity = (ArmorStand) currentLocation.getWorld().spawnEntity(currentLocation, EntityType.ARMOR_STAND);
        crateEntity.setVisible(false);
        crateEntity.setGravity(false);
        crateEntity.setMarker(true);
        crateEntity.getEquipment().setHelmet(new ItemStack(cfg.getCrateMaterial()));

        //spawn parachute armorstand
        Location paraLoc = currentLocation.clone().add(0, 1.2, 0);
        parachuteEntity = (ArmorStand) currentLocation.getWorld().spawnEntity(paraLoc, EntityType.ARMOR_STAND);
        parachuteEntity.setVisible(false);
        parachuteEntity.setGravity(false);
        parachuteEntity.setMarker(true);
        parachuteEntity.getEquipment().setHelmet(new ItemStack(cfg.getParachuteMaterial()));

        //start falling movement scheduler task
        fallTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickFall, 1L, 1L);
    }

    //tick drop position and particles
    private void tickFall() {
        if (state != State.FALLING) {
            return;
        }

        ConfigManager cfg = plugin.getConfigManager();
        double nextY = currentLocation.getY() - cfg.getFallSpeed();

        if (nextY <= targetLocation.getY()) {
            land();
            return;
        }

        //update entity positions
        currentLocation.setY(nextY);
        crateEntity.teleport(currentLocation);
        parachuteEntity.teleport(currentLocation.clone().add(0, 1.2, 0));

        //spawn trailing smoke and cloud particles
        currentLocation.getWorld().spawnParticle(cfg.getParticleFalling(), currentLocation.clone().add(0, 0.5, 0), 5, 0.2, 0.2, 0.2, 0.02);
        currentLocation.getWorld().spawnParticle(Particle.CLOUD, currentLocation.clone().add(0, 1.8, 0), 3, 0.3, 0.1, 0.3, 0.01);

        //play falling sound periodically
        soundCounter++;
        if (soundCounter % 20 == 0) {
            currentLocation.getWorld().playSound(currentLocation, cfg.getSoundFalling(), 1.0f, 1.0f);
        }
    }

    //land drop and place container block
    private void land() {
        if (fallTask != null) {
            fallTask.cancel();
        }

        state = State.LANDED;
        ConfigManager cfg = plugin.getConfigManager();

        //remove falling entities
        removeEntities();

        //place container block
        Block block = targetLocation.getBlock();
        block.setType(cfg.getCrateMaterial());

        //fill container with randomized weighted loot
        if (block.getState() instanceof Container container) {
            fillContainer(container);
        }

        //play landing explosion particles and sound
        targetLocation.getWorld().spawnParticle(cfg.getParticleLanding(), targetLocation.clone().add(0.5, 0.5, 0.5), 30, 0.5, 0.5, 0.5, 0.1);
        targetLocation.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, targetLocation.clone().add(0.5, 1.0, 0.5), 20, 0.3, 0.5, 0.3, 0.05);
        targetLocation.getWorld().playSound(targetLocation, cfg.getSoundLanding(), 1.5f, 1.0f);

        //broadcast landing message
        broadcastLandMessage();

        //start landed firework particle task
        landedEffectTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state == State.LANDED && targetLocation.getWorld() != null) {
                targetLocation.getWorld().spawnParticle(Particle.FIREWORK, targetLocation.clone().add(0.5, 1.2, 0.5), 4, 0.2, 0.5, 0.2, 0.02);
            }
        }, 20L, 20L);

        //start despawn timer task
        despawnTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state == State.LANDED && !looted) {
                broadcastDespawnMessage();
                cleanup();
            }
        }, cfg.getDespawnSeconds() * 20L);
    }

    //populate container with weighted loot items
    private void fillContainer(Container container) {
        ConfigManager cfg = plugin.getConfigManager();
        List<LootItem> lootList = cfg.getLootTable();
        if (lootList.isEmpty()) {
            return;
        }

        var inv = container.getInventory();
        inv.clear();

        int totalWeight = lootList.stream().mapToInt(LootItem::getWeight).sum();
        if (totalWeight <= 0) {
            return;
        }

        int minItems = cfg.getMinLootItems();
        int maxItems = cfg.getMaxLootItems();
        int itemsToGenerate = minItems >= maxItems ? minItems : ThreadLocalRandom.current().nextInt(minItems, maxItems + 1);

        int slots = inv.getSize();
        Set<Integer> usedSlots = new HashSet<>();

        for (int i = 0; i < itemsToGenerate; i++) {
            if (usedSlots.size() >= slots) break;

            int roll = ThreadLocalRandom.current().nextInt(totalWeight);
            int currentWeight = 0;
            LootItem selected = null;

            for (LootItem lootItem : lootList) {
                currentWeight += lootItem.getWeight();
                if (roll < currentWeight) {
                    selected = lootItem;
                    break;
                }
            }

            if (selected != null) {
                ItemStack item = selected.createItemStack();
                if (item != null && item.getType() != Material.AIR) {
                    int slot;
                    do {
                        slot = ThreadLocalRandom.current().nextInt(slots);
                    } while (usedSlots.contains(slot));
                    usedSlots.add(slot);
                    inv.setItem(slot, item);
                }
            }
        }
    }

    //handle player claiming supply drop
    public void claim(String playerName) {
        if (looted || state != State.LANDED) {
            return;
        }
        looted = true;
        
        ConfigManager cfg = plugin.getConfigManager();
        targetLocation.getWorld().playSound(targetLocation, cfg.getSoundClaim(), 1.0f, 1.0f);
        targetLocation.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, targetLocation.clone().add(0.5, 1.0, 0.5), 40, 0.4, 0.4, 0.4, 0.2);

        //broadcast claim message
        broadcastClaimMessage(playerName);

        //end event without destroying container block
        cleanup();
    }

    //clean up tasks and entities
    public void cleanup() {
        if (state == State.CLEANED_UP) {
            return;
        }
        state = State.CLEANED_UP;

        //cancel background tasks
        if (fallTask != null) fallTask.cancel();
        if (landedEffectTask != null) landedEffectTask.cancel();
        if (despawnTask != null) despawnTask.cancel();

        //remove display entities
        removeEntities();
    }

    //remove armorstand entities
    private void removeEntities() {
        if (crateEntity != null && !crateEntity.isDead()) {
            crateEntity.remove();
        }
        if (parachuteEntity != null && !parachuteEntity.isDead()) {
            parachuteEntity.remove();
        }
    }

    //broadcast landing coordinates message
    private void broadcastLandMessage() {
        ConfigManager cfg = plugin.getConfigManager();
        Map<String, String> placeholders = Map.of(
                "x", String.valueOf(targetLocation.getBlockX()),
                "y", String.valueOf(targetLocation.getBlockY()),
                "z", String.valueOf(targetLocation.getBlockZ()),
                "world", targetLocation.getWorld().getName()
        );
        Bukkit.broadcast(cfg.formatMessage(cfg.getDropLandMsg(), placeholders));
    }

    //broadcast player claim message
    private void broadcastClaimMessage(String playerName) {
        ConfigManager cfg = plugin.getConfigManager();
        Map<String, String> placeholders = Map.of(
                "x", String.valueOf(targetLocation.getBlockX()),
                "y", String.valueOf(targetLocation.getBlockY()),
                "z", String.valueOf(targetLocation.getBlockZ()),
                "world", targetLocation.getWorld().getName(),
                "player", playerName
        );
        Bukkit.broadcast(cfg.formatMessage(cfg.getDropLootedMsg(), placeholders));
    }

    //broadcast drop expiration message
    private void broadcastDespawnMessage() {
        ConfigManager cfg = plugin.getConfigManager();
        Map<String, String> placeholders = Map.of(
                "x", String.valueOf(targetLocation.getBlockX()),
                "y", String.valueOf(targetLocation.getBlockY()),
                "z", String.valueOf(targetLocation.getBlockZ()),
                "world", targetLocation.getWorld().getName()
        );
        Bukkit.broadcast(cfg.formatMessage(cfg.getDropDespawnedMsg(), placeholders));
    }

    //getters for drop properties
    public State getState() { return state; }
    public Location getTargetLocation() { return targetLocation; }
    public Location getCurrentLocation() { return currentLocation; }
    public boolean isLooted() { return looted; }
}
