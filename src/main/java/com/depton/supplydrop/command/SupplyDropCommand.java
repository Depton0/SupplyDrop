package com.depton.supplydrop.command;

import com.depton.supplydrop.SupplyDropPlugin;
import com.depton.supplydrop.config.ConfigManager;
import com.depton.supplydrop.drop.SupplyDrop;
import com.depton.supplydrop.drop.SupplyDropManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

//handles admin commands and tab completions
public class SupplyDropCommand implements CommandExecutor, TabCompleter {

    private final SupplyDropPlugin plugin;

    //construct command handler
    public SupplyDropCommand(SupplyDropPlugin plugin) {
        this.plugin = plugin;
    }

    //process incoming command executions
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        ConfigManager cfg = plugin.getConfigManager();

        //check admin permission
        if (!sender.hasPermission("supplydrop.admin")) {
            sender.sendMessage(cfg.formatMessage(cfg.getNoPermissionMsg(), null));
            return true;
        }

        //display help if no args provided
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();
        SupplyDropManager manager = plugin.getSupplyDropManager();

        switch (sub) {
            //start new supply drop manually
            case "start" -> {
                World world = null;
                Integer x = null;
                Integer z = null;

                if (args.length >= 2) {
                    world = Bukkit.getWorld(args[1]);
                }
                if (args.length >= 4) {
                    try {
                        x = Integer.parseInt(args[2]);
                        z = Integer.parseInt(args[3]);
                    } catch (NumberFormatException ignored) {
                        //ignore invalid number
                    }
                }

                if (!manager.startSupplyDrop(world, x, z)) {
                    sender.sendMessage(cfg.formatMessage(cfg.getAlreadyActiveMsg(), null));
                }
            }
            //cancel active drop
            case "stop", "cancel" -> {
                if (manager.stopActiveDrop()) {
                    sender.sendMessage(cfg.formatMessage(cfg.getStoppedMsg(), null));
                } else {
                    sender.sendMessage(cfg.formatMessage(cfg.getNoActiveDropMsg(), null));
                }
            }
            //reload plugin configuration
            case "reload" -> {
                plugin.reloadPluginConfig();
                sender.sendMessage(cfg.formatMessage(cfg.getReloadedMsg(), null));
            }
            //teleport player to active drop
            case "tp", "teleport" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can teleport.");
                    return true;
                }
                SupplyDrop active = manager.getActiveDrop();
                if (active == null) {
                    sender.sendMessage(cfg.formatMessage(cfg.getNoActiveDropMsg(), null));
                    return true;
                }
                player.teleport(active.getCurrentLocation());
                player.sendMessage(cfg.formatMessage("&aTeleported to active supply drop!", null));
            }
            //default fallback usage
            default -> sendUsage(sender, label);
        }

        return true;
    }

    //send usage string to sender
    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(plugin.getConfigManager().formatMessage("&eUsage: /" + label + " [start|stop|reload|tp]", null));
    }

    //tab completion options
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("supplydrop.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> options = List.of("start", "stop", "reload", "tp");
            return options.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return Bukkit.getWorlds().stream().map(World::getName).filter(w -> w.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }

        return List.of();
    }
}
