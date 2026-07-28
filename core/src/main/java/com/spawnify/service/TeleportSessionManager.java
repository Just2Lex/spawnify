
package com.spawnify.service;

import com.spawnify.config.Messages;
import com.spawnify.config.PluginConfig;
import com.spawnify.event.SpawnCountdownEndEvent;
import com.spawnify.event.SpawnCountdownStartEvent;
import com.spawnify.event.SpawnCountdownTickEvent;
import com.spawnify.event.SpawnSelectionEvent;
import com.spawnify.event.SpawnTeleportEvent;
import com.spawnify.event.SpawnTeleportedEvent;
import com.spawnify.model.SpawnReason;
import com.spawnify.model.SpawnTarget;
import com.spawnify.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportSessionManager {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final Messages messages;
    private final Map<UUID, Long> cooldownEnd = new ConcurrentHashMap<>();
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, SpawnTarget> selectedTargets = new ConcurrentHashMap<>();

    public TeleportSessionManager(JavaPlugin plugin, PluginConfig config, Messages messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }

    public void shutdown() {
        sessions.values().forEach(session -> {
            BukkitTask task = session.task();
            if (task != null) {
                task.cancel();
            }
        });
        sessions.clear();
        selectedTargets.clear();
        cooldownEnd.clear();
    }

    public boolean hasActiveSession(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public void rememberSelectedTarget(UUID playerId, SpawnTarget target) {
        if (playerId == null || target == null) {
            return;
        }
        selectedTargets.put(playerId, target);
    }

    public Optional<SpawnTarget> getSelectedTarget(UUID playerId) {
        return Optional.ofNullable(selectedTargets.get(playerId));
    }

    public boolean isOnCooldown(Player player) {
        Long end = cooldownEnd.get(player.getUniqueId());
        return end != null && end > System.currentTimeMillis();
    }

    public long remainingCooldownSeconds(Player player) {
        Long end = cooldownEnd.get(player.getUniqueId());
        if (end == null) {
            return 0L;
        }
        return Math.max(0L, (end - System.currentTimeMillis() + 999L) / 1000L);
    }

    public void requestTeleport(Player player, SpawnTarget target, SpawnReason reason) {
        requestTeleport(player, target, reason, config.globalTeleportPresentation(), false, true, config.cooldownSeconds(), -1);
    }

    public void requestTeleport(Player player, SpawnTarget target, SpawnReason reason, boolean silent, boolean applyCooldown, int cooldownSeconds) {
        requestTeleport(player, target, reason, config.globalTeleportPresentation(), silent, applyCooldown, cooldownSeconds, -1);
    }

    public void requestTeleport(Player player, SpawnTarget target, SpawnReason reason, TeleportPresentation presentation, boolean silent, boolean applyCooldown, int cooldownSeconds) {
        requestTeleport(player, target, reason, presentation, silent, applyCooldown, cooldownSeconds, -1);
    }

    public void requestTeleport(Player player,
                                SpawnTarget target,
                                SpawnReason reason,
                                TeleportPresentation presentation,
                                boolean silent,
                                boolean applyCooldown,
                                int cooldownSeconds,
                                int delaySecondsOverride) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (target == null || target.toLocation() == null) {
            if (!silent) {
                sendConfiguredMessage(player, "spawn-not-found", Map.of());
            }
            return;
        }

        if (hasActiveSession(player.getUniqueId())) {
            return;
        }

        if (applyCooldown && cooldownSeconds > 0 && isOnCooldown(player) && !player.hasPermission("spawnify.bypass.cooldown")) {
            if (!silent) {
                sendConfiguredMessage(player, "teleport.cooldown-active", Map.of("%seconds%", String.valueOf(remainingCooldownSeconds(player))));
            }
            return;
        }

        SpawnSelectionEvent selectionEvent = new SpawnSelectionEvent(player, reason, target);
        Bukkit.getPluginManager().callEvent(selectionEvent);
        if (selectionEvent.isCancelled()) {
            if (!silent) {
                sendConfiguredMessage(player, "teleport.cancelled", Map.of());
            }
            return;
        }

        SpawnTarget resolvedTarget = selectionEvent.getTarget();
        if (resolvedTarget == null || resolvedTarget.toLocation() == null) {
            if (!silent) {
                sendConfiguredMessage(player, "spawn-not-found", Map.of());
            }
            return;
        }

        rememberSelectedTarget(player.getUniqueId(), resolvedTarget);
        TeleportPresentation sessionPresentation = presentation == null ? config.globalTeleportPresentation() : presentation;
        int delaySeconds = resolveDelay(reason, delaySecondsOverride);

        if (delaySeconds <= 0) {
            teleportNow(player, resolvedTarget, reason, sessionPresentation, silent, applyCooldown, cooldownSeconds);
            return;
        }

        Session session = new Session(
                player.getUniqueId(),
                player.getLocation().clone(),
                resolvedTarget,
                reason,
                sessionPresentation,
                silent,
                applyCooldown,
                Math.max(0, cooldownSeconds),
                delaySeconds,
                delaySeconds,
                null
        );
        sessions.put(session.playerId(), session);

        if (!silent) {
            sendConfiguredMessage(player, "teleport.start", Map.of("%seconds%", String.valueOf(delaySeconds)));
            showCountdownTitle(player, resolvedTarget, delaySeconds);
        }

        if (shouldApplyBlindness(sessionPresentation) && delaySeconds > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, delaySeconds * 20 + 20, 0, true, false, true));
        }

        Bukkit.getPluginManager().callEvent(new SpawnCountdownStartEvent(player, reason, resolvedTarget, delaySeconds));

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(session.playerId()), 20L, 20L);
        sessions.put(session.playerId(), session.withTask(task));
    }

    private int resolveDelay(SpawnReason reason, int delaySecondsOverride) {
        if (delaySecondsOverride >= 0) {
            return Math.max(0, delaySecondsOverride);
        }
        return switch (reason) {
            case DEATH -> config.deathTeleportDelaySeconds();
            case VOID -> config.voidTeleportDelaySeconds();
            case FIRST_JOIN -> config.firstJoinTeleportDelaySeconds();
            case JOIN, COMMAND, GUI, ADMIN -> config.teleportDelaySeconds();
        };
    }

    private void tick(UUID playerId) {
        Session session = sessions.get(playerId);
        if (session == null) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            cancel(playerId);
            return;
        }

        if (config.cancelOnMove() && moved(player.getLocation(), session.startLocation())) {
            if (!session.silent()) {
                sendConfiguredMessage(player, "teleport.cancelled", Map.of());
            }
            cancel(playerId);
            return;
        }

        int secondsLeft = session.remainingSeconds() - 1;
        Session updated = session.withRemainingSeconds(secondsLeft);
        sessions.put(playerId, updated);

        int displaySeconds = Math.max(0, secondsLeft);
        Bukkit.getPluginManager().callEvent(new SpawnCountdownTickEvent(player, session.reason(), session.target(), displaySeconds));

        if (displaySeconds <= 0) {
            Bukkit.getPluginManager().callEvent(new SpawnCountdownEndEvent(player, session.reason(), session.target(), session.initialDelaySeconds()));
            cancel(playerId);
            teleportNow(player, session.target(), session.reason(), session.presentation(), session.silent(), session.applyCooldown(), session.cooldownSeconds());
            return;
        }

        if (!session.silent()) {
            showCountdownTitle(player, session.target(), displaySeconds);
        }
    }

    private boolean moved(Location current, Location start) {
        if (current == null || start == null || current.getWorld() == null || start.getWorld() == null) {
            return false;
        }
        if (!current.getWorld().equals(start.getWorld())) {
            return true;
        }
        return current.distanceSquared(start) > 0.01D;
    }

    public void cancel(UUID playerId) {
        Session session = sessions.remove(playerId);
        if (session != null && session.task() != null) {
            session.task().cancel();
        }
    }

    private void teleportNow(Player player,
                             SpawnTarget target,
                             SpawnReason reason,
                             TeleportPresentation presentation,
                             boolean silent,
                             boolean applyCooldown,
                             int cooldownSeconds) {
        if (player == null || !player.isOnline() || target == null) {
            return;
        }

        SpawnTeleportEvent teleportEvent = new SpawnTeleportEvent(player, reason, target);
        Bukkit.getPluginManager().callEvent(teleportEvent);
        if (teleportEvent.isCancelled()) {
            if (!silent) {
                sendConfiguredMessage(player, "teleport.cancelled", Map.of());
            }
            return;
        }

        SpawnTarget resolvedTarget = teleportEvent.getTarget();
        if (resolvedTarget == null || resolvedTarget.toLocation() == null) {
            if (!silent) {
                sendConfiguredMessage(player, "spawn-not-found", Map.of());
            }
            return;
        }

        Location targetLocation = resolvedTarget.toLocation();
        if (targetLocation == null) {
            if (!silent) {
                sendConfiguredMessage(player, "spawn-not-found", Map.of());
            }
            return;
        }

        player.teleport(targetLocation);
        if (applyCooldown && cooldownSeconds > 0 && !player.hasPermission("spawnify.bypass.cooldown")) {
            cooldownEnd.put(player.getUniqueId(), System.currentTimeMillis() + cooldownSeconds * 1000L);
        }

        if (!silent) {
            applyPresentation(player, resolvedTarget, presentation);
        }

        Bukkit.getPluginManager().callEvent(new SpawnTeleportedEvent(player, reason, resolvedTarget));
    }

    private void showCountdownTitle(Player player, SpawnTarget target, int secondsLeft) {
        boolean showTitle = config.teleportCountdownTitleEnabled();
        boolean showSubtitle = config.teleportCountdownSubtitleEnabled();
        if (!showTitle && !showSubtitle) {
            return;
        }

        Map<String, String> placeholders = Map.of(
                "%seconds%", String.valueOf(secondsLeft),
                "%spawn%", target.displayName(),
                "%world%", target.worldName(),
                "%type%", target.type().name()
        );

        Component title = showTitle ? ComponentUtil.legacy(config.teleportCountdownTitleText(), placeholders) : Component.empty();
        Component subtitle = showSubtitle ? ComponentUtil.legacy(config.teleportCountdownSubtitleText(), placeholders) : Component.empty();
        player.showTitle(Title.title(
                title,
                subtitle,
                Title.Times.times(
                        Duration.ofMillis(config.teleportCountdownTitleFadeIn() * 50L),
                        Duration.ofMillis(config.teleportCountdownTitleStay() * 50L),
                        Duration.ofMillis(config.teleportCountdownTitleFadeOut() * 50L)
                )
        ));
    }

    private boolean shouldApplyBlindness(TeleportPresentation presentation) {
        if (presentation == null) {
            return false;
        }
        return config.useBlindness() || presentation.blindnessEnabled();
    }

    private void applyPresentation(Player player, SpawnTarget target, TeleportPresentation presentation) {
        Map<String, String> placeholders = Map.of(
                "%spawn%", target.displayName(),
                "%world%", target.worldName(),
                "%type%", target.type().name()
        );

        if (presentation.messageEnabled()) {
            resolveConfiguredComponent(presentation.messageKey(), presentation.messageText(), placeholders)
                    .ifPresent(message -> player.sendMessage(messages.prefix().append(message)));
        }

        if (presentation.titleEnabled()) {
            Component title = ComponentUtil.legacy(presentation.titleText(), placeholders);
            Component subtitle = ComponentUtil.legacy(presentation.titleSubtitle(), placeholders);
            player.showTitle(Title.title(
                    title,
                    subtitle,
                    Title.Times.times(
                            Duration.ofMillis(presentation.titleFadeIn() * 50L),
                            Duration.ofMillis(presentation.titleStay() * 50L),
                            Duration.ofMillis(presentation.titleFadeOut() * 50L)
                    )
            ));
        }

        if (presentation.soundEnabled()) {
            resolveSound(presentation.soundName()).ifPresent(sound ->
                    player.playSound(player.getLocation(), sound, presentation.soundVolume(), presentation.soundPitch())
            );
        }

        if (presentation.particlesEnabled()) {
            try {
                Particle particle = Particle.valueOf(presentation.particlesName().toUpperCase(java.util.Locale.ROOT));
                player.getWorld().spawnParticle(
                        particle,
                        player.getLocation(),
                        presentation.particlesCount(),
                        presentation.particlesOffset(),
                        presentation.particlesOffset(),
                        presentation.particlesOffset(),
                        presentation.particlesSpeed()
                );
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (presentation.completionMessageEnabled()) {
            resolveConfiguredComponent(presentation.completionMessageKey(), presentation.completionMessageText(), placeholders)
                    .ifPresent(completion -> player.sendMessage(messages.prefix().append(completion)));
        }
    }

    private void sendConfiguredMessage(Player player, String key, Map<String, String> placeholders) {
        messages.componentIfEnabled(key, placeholders)
                .ifPresent(component -> player.sendMessage(messages.prefix().append(component)));
    }

    private Optional<Component> resolveConfiguredComponent(String key, String text, Map<String, String> placeholders) {
        if (key != null && !key.isBlank() && messages.hasValue(key)) {
            return messages.componentIfEnabled(key, placeholders);
        }
        if (text != null && !text.isBlank()) {
            return Optional.of(ComponentUtil.legacy(text, placeholders));
        }
        return Optional.empty();
    }
    private Optional<Sound> resolveSound(String configuredName) {
        if (configuredName == null || configuredName.isBlank()) {
            return Optional.empty();
        }

        String trimmed = configuredName.trim();
        NamespacedKey key = NamespacedKey.fromString(trimmed.toLowerCase(java.util.Locale.ROOT));
        if (key == null) {
            key = NamespacedKey.minecraft(trimmed.toLowerCase(java.util.Locale.ROOT).replace(' ', '_'));
        }

        if (key == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(Registry.SOUNDS.get(key));
    }


    private record Session(
            UUID playerId,
            Location startLocation,
            SpawnTarget target,
            SpawnReason reason,
            TeleportPresentation presentation,
            boolean silent,
            boolean applyCooldown,
            int cooldownSeconds,
            int initialDelaySeconds,
            int remainingSeconds,
            BukkitTask task) {

        private Session withTask(BukkitTask task) {
            return new Session(playerId, startLocation, target, reason, presentation, silent, applyCooldown, cooldownSeconds, initialDelaySeconds, remainingSeconds, task);
        }

        private Session withRemainingSeconds(int remainingSeconds) {
            return new Session(playerId, startLocation, target, reason, presentation, silent, applyCooldown, cooldownSeconds, initialDelaySeconds, remainingSeconds, task);
        }
    }
}
