
package com.spawnify.command;

import com.spawnify.SpawnifyPlugin;
import com.spawnify.model.SpawnPoint;
import com.spawnify.model.SpawnTargetType;
import com.spawnify.service.SpawnService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class SpawnifyAdminCommand implements CommandExecutor, TabCompleter {

    private final SpawnifyPlugin plugin;
    private final SpawnService spawnService;

    public SpawnifyAdminCommand(SpawnifyPlugin plugin, SpawnService spawnService) {
        this.plugin = plugin;
        this.spawnService = spawnService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spawnify.admin")) {
            sender.sendMessage(spawnService.message("no-permission", Map.of()));
            return true;
        }

        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                spawnService.reload();
                sender.sendMessage(spawnService.message("reloaded", Map.of()));
                return true;
            }
            case "list" -> {
                sendSpawnList(sender, args.length >= 2 ? args[1] : null);
                return true;
            }
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(spawnService.message("player-only", Map.of()));
                    return true;
                }
                if (args.length < 2) {
                    sendHelp(sender);
                    return true;
                }
                String id = args[1];
                String permission = args.length >= 3 ? args[2] : "";
                if (spawnService.setNamedSpawn(player, id, permission)) {
                    sender.sendMessage(spawnService.message("spawn-set", Map.of("%spawn%", id)));
                } else {
                    sender.sendMessage(spawnService.message("spawn-not-found", Map.of()));
                }
                return true;
            }
            case "delete" -> {
                if (args.length < 2) {
                    sendHelp(sender);
                    return true;
                }
                String id = args[1];
                boolean removed;
                if (args.length >= 3) {
                    removed = spawnService.removeNamedSpawn(args[2], id);
                } else {
                    removed = spawnService.removeNamedSpawn(id);
                    if (!removed && spawnService.getNamedSpawn(id).isPresent()) {
                        sender.sendMessage(spawnService.message("spawn-ambiguous-delete", Map.of("%spawn%", id)));
                        return true;
                    }
                }
                sender.sendMessage(removed
                        ? spawnService.message("spawn-removed", Map.of("%spawn%", id))
                        : spawnService.message("spawn-not-found", Map.of("%spawn%", id)));
                return true;
            }
            case "world" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(spawnService.message("player-only", Map.of()));
                    return true;
                }
                String world = args.length >= 2 ? args[1] : player.getWorld().getName();
                spawnService.setWorldSpawn(world, player.getLocation());
                sender.sendMessage(spawnService.message("world-spawn-set", Map.of("%world%", world)));
                return true;
            }
            case "firstjoin" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(spawnService.message("player-only", Map.of()));
                    return true;
                }
                spawnService.setFirstJoinSpawn(player.getLocation());
                sender.sendMessage(spawnService.message("first-join-set", Map.of()));
                return true;
            }
            case "death" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(spawnService.message("player-only", Map.of()));
                    return true;
                }
                spawnService.setDeathSpawn(player.getLocation());
                sender.sendMessage(spawnService.message("death-spawn-set", Map.of()));
                return true;
            }
            case "personal" -> {
                handlePersonal(sender, args);
                return true;
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(spawnService.message("admin-help-title", Map.of()));
        for (Component line : plugin.getSpawnService().getMessages().components("admin-help-lines")) {
            sender.sendMessage(line);
        }
    }

    private void sendSpawnList(CommandSender sender, String worldFilter) {
        if (worldFilter != null && !worldFilter.isBlank()) {
            List<SpawnPoint> points = new ArrayList<>();
            spawnService.getWorldSpawn(worldFilter).ifPresent(points::add);
            points.addAll(spawnService.getNamedSpawns(worldFilter));
            if (points.isEmpty()) {
                sender.sendMessage(spawnService.message("spawn-not-found", Map.of("%spawn%", worldFilter)));
                return;
            }
            sender.sendMessage(Component.text("§8[§aSpawnify§8] §7Spawns in world §f" + worldFilter + "§7:"));
            for (SpawnPoint point : points) {
                sender.sendMessage(Component.text("§7- §f" + point.getId() + " §8(§7" + point.getDisplayName() + "§8)"));
            }
            return;
        }

        sender.sendMessage(Component.text("§8[§aSpawnify§8] §7All spawn groups:"));
        Map<String, List<SpawnPoint>> groups = new LinkedHashMap<>();
        for (SpawnPoint worldSpawn : spawnService.getWorldSpawns()) {
            groups.computeIfAbsent(worldSpawn.getWorldName(), key -> new ArrayList<>()).add(worldSpawn);
        }
        spawnService.getNamedSpawnsByWorld().forEach((world, points) -> groups.computeIfAbsent(world, key -> new ArrayList<>()).addAll(points));
        groups.forEach((world, points) -> {
            sender.sendMessage(Component.text("§7World §f" + world + "§7:"));
            for (SpawnPoint point : points) {
                String suffix = SpawnTargetType.WORLD_DEFAULT.equals(point.getType()) ? "world default" : point.getDisplayName();
                sender.sendMessage(Component.text("§8  - §f" + point.getId() + " §8(§7" + suffix + "§8)"));
            }
        });
        if (spawnService.getFirstJoinSpawn().isPresent()) {
            sender.sendMessage(Component.text("§7FirstJoin: §ffirst-join"));
        }
        if (spawnService.getDeathSpawn().isPresent()) {
            sender.sendMessage(Component.text("§7Death: §fdeath"));
        }
    }

    private void handlePersonal(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendHelp(sender);
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(spawnService.message("player-not-found", Map.of()));
            return;
        }

        if ("set".equals(action)) {
            spawnService.setPersonalSpawn(target, target.getLocation());
            sender.sendMessage(spawnService.message("personal-set", Map.of("%player%", target.getName(), "%world%", target.getWorld().getName())));
            return;
        }

        if ("clear".equals(action)) {
            String world = args.length >= 4 ? args[3] : target.getWorld().getName();
            boolean cleared = spawnService.clearPersonalSpawn(target, world);
            sender.sendMessage(cleared
                    ? spawnService.message("personal-cleared", Map.of("%player%", target.getName(), "%world%", world))
                    : spawnService.message("spawn-not-found", Map.of()));
            return;
        }

        sendHelp(sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(List.of("help", "reload", "list", "create", "delete", "world", "firstjoin", "death", "personal"), args[0]);
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                if (args.length == 2) {
                    return filter(spawnService.worldNames(), args[1]);
                }
            }
            case "create" -> {
                if (args.length == 3) {
                    return filter(List.of("spawnify.use", "spawnify.admin", "spawnify.bypass.cooldown", "spawnify.bypass.delay"), args[2]);
                }
            }
            case "delete" -> {
                if (args.length == 2) {
                    return filter(spawnService.spawnNames(), args[1]);
                }
                if (args.length == 3) {
                    return filter(spawnService.worldNames(), args[2]);
                }
            }
            case "world" -> {
                if (args.length == 2) {
                    return filter(spawnService.worldNames(), args[1]);
                }
            }
            case "personal" -> {
                if (args.length == 2) {
                    return filter(List.of("set", "clear"), args[1]);
                }
                if (args.length == 3) {
                    return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[2]);
                }
                if (args.length == 4 && "clear".equalsIgnoreCase(args[1])) {
                    return filter(spawnService.worldNames(), args[3]);
                }
            }
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String token) {
        String prefix = token == null ? "" : token.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(Objects::nonNull)
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .distinct()
                .limit(50)
                .toList();
    }
}
