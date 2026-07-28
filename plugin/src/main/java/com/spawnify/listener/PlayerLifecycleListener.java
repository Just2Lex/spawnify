
package com.spawnify.listener;

import com.spawnify.SpawnifyPlugin;
import com.spawnify.event.SpawnDeathEvent;
import com.spawnify.event.SpawnJoinEvent;
import com.spawnify.model.SpawnReason;
import com.spawnify.model.SpawnTarget;
import com.spawnify.service.SpawnService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerLifecycleListener implements Listener {

    private final SpawnifyPlugin plugin;
    private final SpawnService spawnService;

    public PlayerLifecycleListener(SpawnifyPlugin plugin, SpawnService spawnService) {
        this.plugin = plugin;
        this.spawnService = spawnService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean firstJoin = !player.hasPlayedBefore();

        SpawnTarget target = null;
        if (firstJoin) {
            if (spawnService.getConfig().firstJoinEnabled()) {
                target = spawnService.resolveFirstJoinTarget(player).orElse(null);
            }
        } else if (spawnService.getConfig().repeatJoinEnabled()) {
            target = spawnService.resolveRepeatJoinTarget(player).orElse(null);
        }

        SpawnJoinEvent joinEvent = new SpawnJoinEvent(player, firstJoin, target);
        Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) {
            return;
        }

        spawnService.showConnectionTitle(player, firstJoin);

        SpawnTarget resolvedTarget = joinEvent.getTarget();
        if (resolvedTarget != null && resolvedTarget.toLocation() != null) {
            int delay = firstJoin ? spawnService.getConfig().firstJoinTeleportDelaySeconds() : spawnService.getConfig().repeatJoinTeleportDelaySeconds();
            boolean applyCooldown = firstJoin ? spawnService.getConfig().firstJoinApplyCooldown() : spawnService.getConfig().repeatJoinApplyCooldown();
            int cooldownSeconds = firstJoin ? spawnService.getConfig().firstJoinCooldownSeconds() : spawnService.getConfig().repeatJoinCooldownSeconds();
            spawnService.requestTeleport(player, resolvedTarget, firstJoin ? SpawnReason.FIRST_JOIN : SpawnReason.JOIN, firstJoin ? spawnService.firstJoinPresentation() : spawnService.repeatJoinPresentation(), false, applyCooldown, cooldownSeconds, delay);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!spawnService.getConfig().deathEnabled()) {
            return;
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!spawnService.getConfig().deathEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        SpawnTarget target = spawnService.resolveDeathTarget(player).orElse(null);
        SpawnDeathEvent deathEvent = new SpawnDeathEvent(player, target, spawnService.getConfig().deathTeleportDelaySeconds());
        Bukkit.getPluginManager().callEvent(deathEvent);
        if (deathEvent.isCancelled()) {
            return;
        }

        SpawnTarget resolvedTarget = deathEvent.getTarget();
        if (resolvedTarget == null || resolvedTarget.toLocation() == null) {
            return;
        }

        event.setRespawnLocation(resolvedTarget.toLocation());
        int respawnDelay = Math.max(0, spawnService.getConfig().deathRespawnDelaySeconds());
        int teleportDelay = Math.max(0, deathEvent.getRespawnDelaySeconds());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            spawnService.requestTeleport(player, resolvedTarget, SpawnReason.DEATH, spawnService.deathPresentation(), false, spawnService.getConfig().deathApplyCooldown(), spawnService.getConfig().deathCooldownSeconds(), teleportDelay);
        }, respawnDelay * 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        spawnService.cancelTeleport(event.getPlayer());
    }
}
