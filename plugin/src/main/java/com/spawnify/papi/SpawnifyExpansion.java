
package com.spawnify.papi;

import com.spawnify.SpawnifyPlugin;
import com.spawnify.model.PersonalSpawn;
import com.spawnify.model.SpawnPoint;
import com.spawnify.model.SpawnTarget;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;

public final class SpawnifyExpansion extends PlaceholderExpansion {

    private final SpawnifyPlugin plugin;

    public SpawnifyExpansion(SpawnifyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "spawnify";
    }

    @Override
    public String getAuthor() {
        return "OpenAI";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null) {
            return "";
        }

        String key = params.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "cooldown", "cooldown_remaining" -> String.valueOf(plugin.getSpawnService().getTeleportSessionManager().remainingCooldownSeconds(player));
            case "cooldown_active" -> String.valueOf(plugin.getSpawnService().isOnCooldown(player));
            case "cooldown_formatted" -> formatSeconds(plugin.getSpawnService().getTeleportSessionManager().remainingCooldownSeconds(player));
            case "available", "available_count" -> String.valueOf(plugin.getSpawnService().availableTargets(player).size());
            case "world" -> player.getWorld().getName();
            case "has_personal" -> String.valueOf(plugin.getSpawnService().getPersonalSpawnRepository().find(player.getUniqueId(), player.getWorld().getName()).isPresent());
            case "personal_world" -> plugin.getSpawnService().getPersonalSpawnRepository().find(player.getUniqueId(), player.getWorld().getName()).map(PersonalSpawn::worldName).orElse("");
            case "spawn", "selected_spawn" -> selected(player).map(SpawnTarget::displayName).orElse("");
            case "selected_spawn_id" -> selected(player).map(SpawnTarget::id).orElse("");
            case "selected_spawn_type" -> selected(player).map(target -> target.type().name()).orElse("");
            case "selected_spawn_world" -> selected(player).map(SpawnTarget::worldName).orElse("");
            case "selected_spawn_permission" -> selected(player).map(target -> target.permission() == null ? "" : target.permission()).orElse("");
            case "selected_spawn_coords" -> selected(player).map(target -> String.format(Locale.US, "%.2f,%.2f,%.2f", target.x(), target.y(), target.z())).orElse("");
            case "selected_spawn_x" -> selected(player).map(target -> String.valueOf(target.x())).orElse("");
            case "selected_spawn_y" -> selected(player).map(target -> String.valueOf(target.y())).orElse("");
            case "selected_spawn_z" -> selected(player).map(target -> String.valueOf(target.z())).orElse("");
            case "selected_spawn_yaw" -> selected(player).map(target -> String.valueOf(target.yaw())).orElse("");
            case "selected_spawn_pitch" -> selected(player).map(target -> String.valueOf(target.pitch())).orElse("");
            case "spawn_names" -> String.join(", ", plugin.getSpawnService().spawnNames());
            case "first_join_spawn" -> plugin.getSpawnService().getFirstJoinSpawn().map(SpawnPoint::getId).orElse("");
            case "death_spawn" -> plugin.getSpawnService().getDeathSpawn().map(SpawnPoint::getId).orElse("");
            case "void_enabled" -> String.valueOf(plugin.getSpawnService().getConfig().voidEnabled());
            case "void_threshold" -> String.valueOf(plugin.getSpawnService().getConfig().voidYThreshold());
            default -> null;
        };
    }

    private Optional<SpawnTarget> selected(Player player) {
        return plugin.getSpawnService().getSelectedTarget(player)
                .or(() -> plugin.getSpawnService().availableTargets(player).stream().findFirst());
    }

    private String formatSeconds(long seconds) {
        long safe = Math.max(0L, seconds);
        long minutes = safe / 60L;
        long rest = safe % 60L;
        return String.format(Locale.US, "%02d:%02d", minutes, rest);
    }
}
