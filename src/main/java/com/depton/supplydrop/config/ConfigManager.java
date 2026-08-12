package com.depton.supplydrop.config;

import com.depton.supplydrop.SupplyDropPlugin;
import com.depton.supplydrop.drop.LootItem;
import com.depton.supplydrop.util.BoundingBox2D;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    //plugin instance
    private final SupplyDropPlugin plugin;

    //settings fields
    private String worldName;
    private BoundingBox2D boundingBox;
    private int heightAboveSurface;
    private double fallSpeed;
    private int despawnSeconds;

    private Material crateMaterial;
    private Material parachuteMaterial;

    private Sound soundFalling;
    private Sound soundLanding;
    private Sound soundClaim;

    private Particle particleFalling;
    private Particle particleLanding;

    private boolean autoDropEnabled;
    private int autoDropIntervalSeconds;

    private int minLootItems;
    private int maxLootItems;

    private String prefix;
    private String dropStartMsg;
    private String dropLandMsg;
    private String dropLootedMsg;
    private String dropDespawnedMsg;
    private String alreadyActiveMsg;
    private String noActiveDropMsg;
    private String noPermissionMsg;
    private String reloadedMsg;
    private String stoppedMsg;

    private final List<LootItem> lootTable = new ArrayList<>();

    //construct config manager
    public ConfigManager(SupplyDropPlugin plugin) {
        this.plugin = plugin;
    }

    //load config settings from file
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        //load world name
        worldName = config.getString("settings.world", "world");
        
        //load 2d bounding box
        double x1 = config.contains("settings.bounding-box.x1") ? config.getDouble("settings.bounding-box.x1") : config.getDouble("settings.min-x", -1000);
        double z1 = config.contains("settings.bounding-box.z1") ? config.getDouble("settings.bounding-box.z1") : config.getDouble("settings.min-z", -1000);
        double x2 = config.contains("settings.bounding-box.x2") ? config.getDouble("settings.bounding-box.x2") : config.getDouble("settings.max-x", 1000);
        double z2 = config.contains("settings.bounding-box.z2") ? config.getDouble("settings.bounding-box.z2") : config.getDouble("settings.max-z", 1000);
        boundingBox = new BoundingBox2D(x1, z1, x2, z2);

        heightAboveSurface = config.getInt("settings.height-above-surface", 60);
        fallSpeed = config.getDouble("settings.fall-speed", 0.35);
        despawnSeconds = config.getInt("settings.despawn-seconds", 600);

        //load materials
        crateMaterial = parseMaterial(config.getString("settings.crate-block", "BARREL"), Material.BARREL);
        parachuteMaterial = parseMaterial(config.getString("settings.parachute-block", "RED_WOOL"), Material.RED_WOOL);

        //load auto drop options
        autoDropEnabled = config.getBoolean("settings.auto-drop.enabled", true);
        autoDropIntervalSeconds = config.getInt("settings.auto-drop.interval-seconds", 1800);

        //load container loot limits
        minLootItems = config.getInt("loot-settings.min-items", 3);
        maxLootItems = config.getInt("loot-settings.max-items", 7);

        //load sound effects
        soundFalling = parseSound(config.getString("sounds.falling", "ENTITY_WIND_CHARGE_WIND_BURST"), Sound.ENTITY_WIND_CHARGE_WIND_BURST);
        soundLanding = parseSound(config.getString("sounds.landing", "ENTITY_GENERIC_EXPLODE"), Sound.ENTITY_GENERIC_EXPLODE);
        soundClaim = parseSound(config.getString("sounds.claim", "UI_STONECUTTER_TAKE_RESULT"), Sound.UI_STONECUTTER_TAKE_RESULT);

        //load particles
        particleFalling = parseParticle(config.getString("particles.falling", "FLAME"), Particle.FLAME);
        particleLanding = parseParticle(config.getString("particles.landing", "EXPLOSION"), Particle.EXPLOSION);

        //load broadcast messages
        prefix = config.getString("messages.prefix", "&8[&bSupplyDrop&8] ");
        dropStartMsg = config.getString("messages.drop-start", "&eA supply drop is falling at X: %x%, Y: %y%, Z: %z%!");
        dropLandMsg = config.getString("messages.drop-land", "&aThe supply drop has landed at X: %x%, Y: %y%, Z: %z%!");
        dropLootedMsg = config.getString("messages.drop-looted", "&b%player% &eclaimed the supply drop!");
        dropDespawnedMsg = config.getString("messages.drop-despawned", "&cThe supply drop expired.");
        alreadyActiveMsg = config.getString("messages.already-active", "&cA supply drop event is already active!");
        noActiveDropMsg = config.getString("messages.no-active-drop", "&cThere is no active supply drop right now.");
        noPermissionMsg = config.getString("messages.no-permission", "&cYou do not have permission to use this command.");
        reloadedMsg = config.getString("messages.reloaded", "&aSupplyDrop configuration reloaded successfully!");
        stoppedMsg = config.getString("messages.stopped", "&cActive supply drop has been cancelled.");

        //load weighted loot items
        loadLootTable(config);
    }

    //parse loot table entries
    private void loadLootTable(FileConfiguration config) {
        lootTable.clear();
        List<?> list = config.getList("loot");
        if (list == null) return;

        for (Object obj : list) {
            try {
                if (obj instanceof Map<?, ?> map) {
                    int weight = map.containsKey("weight") ? ((Number) map.get("weight")).intValue() : 10;
                    int min = map.containsKey("min-amount") ? ((Number) map.get("min-amount")).intValue() : 1;
                    int max = map.containsKey("max-amount") ? ((Number) map.get("max-amount")).intValue() : 1;

                    Object itemObj = map.get("item");
                    ItemStack itemStack = parseItemStack(itemObj);
                    if (itemStack != null) {
                        lootTable.add(new LootItem(itemStack, weight, min, max));
                    }
                }
            } catch (Exception ignored) {
                //ignore invalid loot entry
            }
        }
    }

    //parse yaml itemstack serialization
    @SuppressWarnings("unchecked")
    private ItemStack parseItemStack(Object obj) {
        if (obj instanceof ItemStack is) {
            return is;
        }
        if (obj instanceof Map<?, ?> map) {
            try {
                return ItemStack.deserialize((Map<String, Object>) map);
            } catch (Exception ignored) {
                //ignore deserialization error
            }
        }
        if (obj instanceof String str) {
            Material mat = parseMaterial(str, null);
            if (mat != null) return new ItemStack(mat);
        }
        return null;
    }

    //parse material safely
    private Material parseMaterial(String name, Material fallback) {
        try {
            Material m = Material.matchMaterial(name);
            return m != null ? m : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    //parse sound enum safely
    private Sound parseSound(String name, Sound fallback) {
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return fallback;
        }
    }

    //parse particle enum safely
    private Particle parseParticle(String name, Particle fallback) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return fallback;
        }
    }

    //format message with legacy colors and placeholders
    public Component formatMessage(String message, Map<String, String> placeholders) {
        String formatted = prefix + message;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                formatted = formatted.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(formatted);
    }

    //getters for config values
    public String getWorldName() { return worldName; }
    public BoundingBox2D getBoundingBox() { return boundingBox; }
    public int getHeightAboveSurface() { return heightAboveSurface; }
    public double getFallSpeed() { return fallSpeed; }
    public int getDespawnSeconds() { return despawnSeconds; }

    public Material getCrateMaterial() { return crateMaterial; }
    public Material getParachuteMaterial() { return parachuteMaterial; }

    public Sound getSoundFalling() { return soundFalling; }
    public Sound getSoundLanding() { return soundLanding; }
    public Sound getSoundClaim() { return soundClaim; }

    public Particle getParticleFalling() { return particleFalling; }
    public Particle getParticleLanding() { return particleLanding; }

    public boolean isAutoDropEnabled() { return autoDropEnabled; }
    public int getAutoDropIntervalSeconds() { return autoDropIntervalSeconds; }

    public int getMinLootItems() { return minLootItems; }
    public int getMaxLootItems() { return maxLootItems; }

    public String getDropStartMsg() { return dropStartMsg; }
    public String getDropLandMsg() { return dropLandMsg; }
    public String getDropLootedMsg() { return dropLootedMsg; }
    public String getDropDespawnedMsg() { return dropDespawnedMsg; }
    public String getAlreadyActiveMsg() { return alreadyActiveMsg; }
    public String getNoActiveDropMsg() { return noActiveDropMsg; }
    public String getNoPermissionMsg() { return noPermissionMsg; }
    public String getReloadedMsg() { return reloadedMsg; }
    public String getStoppedMsg() { return stoppedMsg; }

    public List<LootItem> getLootTable() { return lootTable; }
}
