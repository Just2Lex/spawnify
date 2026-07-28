
package com.spawnify.listener;

import com.spawnify.SpawnifyPlugin;
import com.spawnify.event.SpawnVoidEvent;
import com.spawnify.model.SpawnReason;
import com.spawnify.model.SpawnTarget;
import com.spawnify.service.SpawnService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class VoidListener implements Listener {

    private final SpawnifyPlugin plugin;
    private final SpawnService spawnService;

    public VoidListener(SpawnifyPlugin plugin, SpawnService spawnService) {
        this.plugin = plugin;
        this.spawnService = spawnService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!spawnService.getConfig().voidEnabled()) {
            return;
        }
        if (spawnService.hasActiveTeleport(event.getPlayer())) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() == null || to.getWorld() == null) {
            return;
        }
        if (!from.getWorld().equals(to.getWorld())) {
            return;
        }

        double threshold = spawnService.getConfig().voidYThreshold();
        boolean crossed = from.getY() > threshold && to.getY() <= threshold;
        boolean alreadyBelow = to.getY() <= threshold;
        if (!crossed && !alreadyBelow) {
            return;
        }

        spawnService.resolveVoidTarget(event.getPlayer()).ifPresent(target -> {
            SpawnVoidEvent voidEvent = new SpawnVoidEvent(event.getPlayer(), target, threshold);
            Bukkit.getPluginManager().callEvent(voidEvent);
            if (voidEvent.isCancelled()) {
                return;
            }

            SpawnTarget resolvedTarget = voidEvent.getTarget();
            if (resolvedTarget == null || resolvedTarget.toLocation() == null) {
                return;
            }

            spawnService.requestTeleport(
                    event.getPlayer(),
                    resolvedTarget,
                    SpawnReason.VOID,
                    spawnService.voidPresentation(),
                    false,
                    spawnService.getConfig().voidApplyCooldown(),
                    spawnService.getConfig().voidTeleportCooldownSeconds(),
                    spawnService.getConfig().voidTeleportDelaySeconds()
            );
        });
    }
}
