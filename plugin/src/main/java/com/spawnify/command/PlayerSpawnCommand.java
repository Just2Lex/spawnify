
package com.spawnify.command;

import com.spawnify.SpawnifyPlugin;
import com.spawnify.event.SpawnCommandEvent;
import com.spawnify.gui.SpawnSelectorGui;
import com.spawnify.model.SpawnReason;
import com.spawnify.model.SpawnTarget;
import com.spawnify.service.SpawnService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class PlayerSpawnCommand implements CommandExecutor, TabCompleter {

    private final SpawnifyPlugin plugin;
    private final SpawnService spawnService;
    private final SpawnSelectorGui gui;

    public PlayerSpawnCommand(SpawnifyPlugin plugin, SpawnService spawnService, SpawnSelectorGui gui) {
        this.plugin = plugin;
        this.spawnService = spawnService;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(spawnService.message("player-only", Map.of()));
            return true;
        }

        if (!player.hasPermission("spawnify.use")) {
            player.sendMessage(spawnService.message("no-permission", Map.of()));
            return true;
        }

        if (args.length == 0) {
            return openOrTeleport(player, label, args);
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help", "?" -> {
                sendHelp(player);
                return true;
            }
            case "list", "gui", "menu" -> {
                gui.open(player);
                return true;
            }
            case "personal" -> {
                handlePersonal(player, args);
                return true;
            }
            default -> {
                if (spawnService.allowIdentifierSelection()) {
                    String identifier = String.join(" ", args);
                    List<SpawnTarget> matches = spawnService.findSelectableTargets(player, identifier);
                    if (matches.size() == 1) {
                        spawnService.rememberSelectedTarget(player, matches.get(0));
                        spawnService.requestTeleport(player, matches.get(0), SpawnReason.COMMAND);
                    } else if (matches.size() > 1) {
                        player.sendMessage(spawnService.message("spawn-ambiguous", Map.of("%spawn%", identifier)));
                        if (spawnService.openGuiWhenMultiple()) {
                            gui.open(player);
                        }
                    } else {
                        player.sendMessage(spawnService.message("spawn-unknown", Map.of("%spawn%", identifier)));
                    }
                    return true;
                }

                return openOrTeleport(player, label, args);
            }
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(spawnService.message("spawn-help-title", Map.of()));
        for (var line : plugin.getSpawnService().getMessages().components("spawn-help-lines")) {
            player.sendMessage(line);
        }
    }

    private void handlePersonal(Player player, String[] args) {
        if (args.length < 2) {
            sendHelp(player);
            return;
        }

        if ("set".equalsIgnoreCase(args[1])) {
            if (!player.hasPermission("spawnify.personal.set")) {
                player.sendMessage(spawnService.message("no-permission", Map.of()));
                return;
            }
            spawnService.setPersonalSpawn(player, player.getLocation());
            player.sendMessage(spawnService.message("personal-set", Map.of("%player%", player.getName(), "%world%", player.getWorld().getName())));
            return;
        }

        if ("clear".equalsIgnoreCase(args[1])) {
            if (!player.hasPermission("spawnify.personal.clear")) {
                player.sendMessage(spawnService.message("no-permission", Map.of()));
                return;
            }
            boolean cleared = spawnService.clearPersonalSpawn(player, player.getWorld().getName());
            if (cleared) {
                player.sendMessage(spawnService.message("personal-cleared", Map.of("%player%", player.getName(), "%world%", player.getWorld().getName())));
            } else {
                player.sendMessage(spawnService.message("spawn-not-found", Map.of()));
            }
            return;
        }

        sendHelp(player);
    }

    private boolean openOrTeleport(Player player, String label, String[] args) {
        List<SpawnTarget> targets = spawnService.availableTargets(player);
        SpawnTarget preselected = targets.size() == 1 && spawnService.directTeleportWhenSingle() ? targets.get(0) : null;

        SpawnCommandEvent commandEvent = new SpawnCommandEvent(
                player,
                label,
                args,
                Collections.unmodifiableList(targets),
                preselected
        );
        Bukkit.getPluginManager().callEvent(commandEvent);
        if (commandEvent.isCancelled()) {
            return true;
        }

        SpawnTarget selected = commandEvent.getTarget();
        if (selected != null) {
            spawnService.rememberSelectedTarget(player, selected);
            spawnService.requestTeleport(player, selected, SpawnReason.COMMAND);
            return true;
        }

        if (targets.isEmpty()) {
            player.sendMessage(spawnService.message("spawn-available-none", Map.of()));
            return true;
        }

        if (targets.size() == 1) {
            if (spawnService.directTeleportWhenSingle()) {
                player.sendMessage(spawnService.message("spawn-available-one", Map.of("%spawn%", targets.get(0).displayName())));
                spawnService.rememberSelectedTarget(player, targets.get(0));
                spawnService.requestTeleport(player, targets.get(0), SpawnReason.COMMAND);
            } else {
                gui.open(player);
            }
            return true;
        }

        if (spawnService.openGuiWhenMultiple()) {
            player.sendMessage(spawnService.message("spawn-available-multiple", Map.of()));
            gui.open(player);
            return true;
        }

        spawnService.rememberSelectedTarget(player, targets.get(0));
        spawnService.requestTeleport(player, targets.get(0), SpawnReason.COMMAND);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(List.of("help", "list", "gui", "menu", "personal"), args[0]);
        }

        if (args.length == 2 && "personal".equalsIgnoreCase(args[0])) {
            return filter(List.of("set", "clear"), args[1]);
        }

        List<String> options = new ArrayList<>();
        options.addAll(spawnService.spawnNames());
        options.addAll(spawnService.worldNames().stream().map(world -> world + ":spawn").collect(Collectors.toList()));
        options.addAll(List.of("personal", "me", "self"));
        return filter(options, args[args.length - 1]);
    }

    private List<String> filter(List<String> options, String token) {
        String prefix = token == null ? "" : token.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option != null && option.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .distinct()
                .limit(50)
                .toList();
    }
}
