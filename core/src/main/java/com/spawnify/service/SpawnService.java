
package com.spawnify.service;

import com.spawnify.api.SpawnifyApi;
import com.spawnify.config.Messages;
import com.spawnify.config.PluginConfig;
import com.spawnify.event.SpawnResolveEvent;
import com.spawnify.model.PersonalSpawn;
import com.spawnify.model.SpawnPoint;
import com.spawnify.model.SpawnReason;
import com.spawnify.model.SpawnTarget;
import com.spawnify.model.SpawnTargetType;
import com.spawnify.storage.PersonalSpawnRepository;
import com.spawnify.storage.SpawnRepository;
import com.spawnify.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SpawnService implements SpawnifyApi {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final Messages messages;
    private final SpawnRepository spawnRepository;
    private final PersonalSpawnRepository personalSpawnRepository;
    private final TeleportSessionManager teleportSessionManager;

    public SpawnService(JavaPlugin plugin,
                        PluginConfig config,
                        Messages messages,
                        SpawnRepository spawnRepository,
                        PersonalSpawnRepository personalSpawnRepository) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.spawnRepository = spawnRepository;
        this.personalSpawnRepository = personalSpawnRepository;
        this.teleportSessionManager = new TeleportSessionManager(plugin, config, messages);
    }

    public void shutdown() {
        teleportSessionManager.shutdown();
    }

    @Override
    public void reload() {
        config.reload();
        messages.reload();
        spawnRepository.load();
        personalSpawnRepository.load();
    }

    public PluginConfig getConfig() {
        return config;
    }

    public Messages getMessages() {
        return messages;
    }

    public SpawnRepository getSpawnRepository() {
        return spawnRepository;
    }

    public PersonalSpawnRepository getPersonalSpawnRepository() {
        return personalSpawnRepository;
    }

    public TeleportSessionManager getTeleportSessionManager() {
        return teleportSessionManager;
    }

    public TeleportPresentation globalTeleportPresentation() {
        return config.globalTeleportPresentation();
    }

    public TeleportPresentation firstJoinPresentation() {
        return config.firstJoinPresentation();
    }

    public TeleportPresentation repeatJoinPresentation() {
        return config.repeatJoinPresentation();
    }

    public TeleportPresentation deathPresentation() {
        return config.deathPresentation();
    }

    public TeleportPresentation voidPresentation() {
        return config.voidPresentation();
    }

    public List<SpawnTarget> availableTargets(Player player) {
        return availableTargets(player, player.getWorld().getName());
    }

    public List<SpawnTarget> availableTargets(Player player, String worldName) {
        if (player == null) {
            return List.of();
        }

        String resolvedWorld = worldName == null || worldName.isBlank() ? player.getWorld().getName() : worldName;
        List<SpawnTarget> targets = new ArrayList<>();

        if (config.personalSpawnEnabled()) {
            personalSpawnRepository.find(player.getUniqueId(), resolvedWorld).ifPresent(personal -> {
                SpawnTarget target = toPersonalTarget(personal, player);
                if (target != null) {
                    targets.add(target);
                }
            });
        }

        spawnRepository.findWorldSpawn(resolvedWorld).ifPresent(point -> {
            if (point.isEnabled() && isAccessible(player, point)) {
                targets.add(point.toTarget());
            }
        });

        for (SpawnPoint point : spawnRepository.namedSpawns(resolvedWorld)) {
            if (point.isEnabled() && isAccessible(player, point)) {
                targets.add(point.toTarget());
            }
        }

        return targets.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                target -> target.type() + ":" + target.worldName().toLowerCase(java.util.Locale.ROOT) + ":" + target.id().toLowerCase(java.util.Locale.ROOT),
                                target -> target,
                                (a, b) -> a,
                                java.util.LinkedHashMap::new
                        ),
                        map -> map.values().stream()
                                .sorted(java.util.Comparator.comparingInt(this::priority)
                                        .thenComparing(SpawnTarget::displayName, String.CASE_INSENSITIVE_ORDER))
                                .collect(Collectors.toList())
                ));
    }

    @Override
    public List<SpawnTarget> getAvailableTargets(Player player) {
        return availableTargets(player);
    }

    @Override
    public List<SpawnTarget> getAvailableTargets(Player player, String worldName) {
        return availableTargets(player, worldName);
    }

    public boolean directTeleportWhenSingle() {
        return config.directTeleportWhenSingle();
    }

    public boolean openGuiWhenMultiple() {
        return config.openGuiWhenMultiple();
    }

    public boolean allowIdentifierSelection() {
        return config.allowIdentifierSelection();
    }

    public List<SpawnTarget> findSelectableTargets(Player player, String identifier) {
        return findSelectableTargets(player, identifier, player.getWorld().getName());
    }

    public List<SpawnTarget> findSelectableTargets(Player player, String identifier, String worldName) {
        if (player == null || identifier == null || identifier.isBlank()) {
            return List.of();
        }

        String trimmed = identifier.trim();
        String resolvedWorld = worldName == null || worldName.isBlank() ? player.getWorld().getName() : worldName;
        List<SpawnTarget> matches = new ArrayList<>();

        if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":", 2);
            if (parts.length == 2) {
                String explicitWorld = parts[0].trim();
                String explicitId = parts[1].trim();
                if (!explicitWorld.isEmpty() && !explicitId.isEmpty()) {
                    resolveInWorld(player, explicitWorld, explicitId).ifPresent(matches::add);
                }
            }
        } else {
            resolveInWorld(player, resolvedWorld, trimmed).ifPresent(matches::add);

            if (matches.isEmpty()) {
                for (String world : worldNames()) {
                    if (looksLikeWorldAlias(world, trimmed)) {
                        spawnRepository.findWorldSpawn(world)
                                .filter(point -> point.isEnabled() && isAccessible(player, point))
                                .map(SpawnPoint::toTarget)
                                .ifPresent(matches::add);
                    } else {
                        resolveInWorld(player, world, trimmed).ifPresent(matches::add);
                    }
                }
            }
        }

        return matches.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public Optional<SpawnTarget> resolveSelectableTarget(Player player, String identifier) {
        return resolveSelectableTarget(player, identifier, player.getWorld().getName());
    }

    @Override
    public Optional<SpawnTarget> resolveSelectableTarget(Player player, String identifier, String worldName) {
        List<SpawnTarget> matches = findSelectableTargets(player, identifier, worldName);
        Optional<SpawnTarget> selected = matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
        selected.ifPresent(target -> teleportSessionManager.rememberSelectedTarget(player.getUniqueId(), target));
        return selected;
    }

    private Optional<SpawnTarget> resolveInWorld(Player player, String worldName, String id) {
        if (worldName == null || worldName.isBlank() || id == null || id.isBlank()) {
            return Optional.empty();
        }

        String normalizedWorld = worldName.trim();
        String normalizedId = id.trim();

        if (config.personalSpawnEnabled()) {
            Optional<PersonalSpawn> personal = personalSpawnRepository.find(player.getUniqueId(), normalizedWorld);
            if (personal.isPresent() && matchesPersonalIdentifier(normalizedId)) {
                SpawnTarget target = toPersonalTarget(personal.get(), player);
                if (target != null) {
                    return Optional.of(target);
                }
            }
        }

        Optional<SpawnPoint> named = spawnRepository.findNamed(normalizedWorld, normalizedId)
                .filter(point -> point.isEnabled() && isAccessible(player, point));
        if (named.isPresent()) {
            return named.map(SpawnPoint::toTarget);
        }

        Optional<SpawnPoint> worldSpawn = spawnRepository.findWorldSpawn(normalizedWorld)
                .filter(point -> point.isEnabled() && isAccessible(player, point));
        if (worldSpawn.isPresent() && looksLikeWorldAlias(normalizedWorld, normalizedId)) {
            return worldSpawn.map(SpawnPoint::toTarget);
        }

        return Optional.empty();
    }

    private boolean matchesPersonalIdentifier(String identifier) {
        return "personal".equalsIgnoreCase(identifier)
                || "me".equalsIgnoreCase(identifier)
                || "self".equalsIgnoreCase(identifier);
    }

    private boolean looksLikeWorldAlias(String worldName, String identifier) {
        return worldName != null && identifier != null && worldName.equalsIgnoreCase(identifier);
    }

    private int priority(SpawnTarget target) {
        return switch (target.type()) {
            case PERSONAL -> 0;
            case WORLD_DEFAULT -> 1;
            case NAMED -> 2;
            case FIRST_JOIN -> 3;
            case DEATH -> 4;
            case VOID -> 5;
        };
    }

    private boolean isAccessible(Player player, SpawnPoint point) {
        String permission = point.getPermission();
        return permission == null || permission.isBlank() || player.hasPermission(permission);
    }

    private SpawnTarget toPersonalTarget(PersonalSpawn personal, Player player) {
        if (Bukkit.getWorld(personal.worldName()) == null) {
            return null;
        }
        return new SpawnTarget(
                SpawnTargetType.PERSONAL,
                player.getUniqueId() + ":" + personal.worldName().toLowerCase(java.util.Locale.ROOT),
                player.getName() + " personal spawn",
                personal.worldName(),
                personal.x(),
                personal.y(),
                personal.z(),
                personal.yaw(),
                personal.pitch(),
                "",
                "ENDER_PEARL",
                true,
                -1
        );
    }

    @Override
    public Optional<SpawnTarget> resolveBestTarget(Player player, SpawnReason reason) {
        if (player == null) {
            return Optional.empty();
        }
        Optional<SpawnTarget> target = fireResolveEvent(player, reason, resolveBestTargetInternal(player, reason, player.getWorld().getName()));
        target.ifPresent(t -> teleportSessionManager.rememberSelectedTarget(player.getUniqueId(), t));
        return target;
    }

    @Override
    public Optional<SpawnTarget> resolveBestTarget(Player player, SpawnReason reason, String worldName) {
        if (player == null) {
            return Optional.empty();
        }
        Optional<SpawnTarget> target = fireResolveEvent(player, reason, resolveBestTargetInternal(player, reason, worldName));
        target.ifPresent(t -> teleportSessionManager.rememberSelectedTarget(player.getUniqueId(), t));
        return target;
    }

    private Optional<SpawnTarget> resolveBestTargetInternal(Player player, SpawnReason reason, String worldName) {
        Optional<SpawnTarget> resolved = availableTargets(player, worldName).stream().findFirst();

        if (resolved.isEmpty() && config.defaultWorldSpawnFallback()) {
            resolved = Optional.ofNullable(player.getWorld().getSpawnLocation())
                    .map(location -> new SpawnTarget(
                            SpawnTargetType.WORLD_DEFAULT,
                            player.getWorld().getName().toLowerCase(java.util.Locale.ROOT),
                            player.getWorld().getName() + " spawn",
                            location.getWorld().getName(),
                            location.getX(),
                            location.getY(),
                            location.getZ(),
                            location.getYaw(),
                            location.getPitch(),
                            "",
                            "COMPASS",
                            true,
                            -1
                    ));
        }

        return resolved;
    }

    @Override
    public Optional<SpawnTarget> resolveFirstJoinTarget(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        Optional<SpawnTarget> target = spawnRepository.firstJoinSpawn()
                .filter(SpawnPoint::isEnabled)
                .map(SpawnPoint::toTarget);

        if (target.isEmpty() && config.firstJoinFallbackToWorldSpawn()) {
            target = resolveBestTargetInternal(player, SpawnReason.FIRST_JOIN, player.getWorld().getName());
        }

        Optional<SpawnTarget> resolved = fireResolveEvent(player, SpawnReason.FIRST_JOIN, target);
        resolved.ifPresent(t -> teleportSessionManager.rememberSelectedTarget(player.getUniqueId(), t));
        return resolved;
    }

    @Override
    public Optional<SpawnTarget> resolveRepeatJoinTarget(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        String mode = config.repeatJoinTargetMode();
        Optional<SpawnTarget> target = switch (mode) {
            case "WORLD_SPAWN" -> spawnRepository.findWorldSpawn(player.getWorld().getName())
                    .filter(SpawnPoint::isEnabled)
                    .map(SpawnPoint::toTarget)
                    .or(() -> resolveBestTargetInternal(player, SpawnReason.JOIN, player.getWorld().getName()));
            case "NAMED" -> {
                String spawnId = config.repeatJoinSpawnId();
                if (spawnId != null && !spawnId.isBlank()) {
                    yield spawnRepository.findNamed(player.getWorld().getName(), spawnId)
                            .filter(SpawnPoint::isEnabled)
                            .map(SpawnPoint::toTarget)
                            .or(() -> resolveBestTargetInternal(player, SpawnReason.JOIN, player.getWorld().getName()));
                }
                yield resolveBestTargetInternal(player, SpawnReason.JOIN, player.getWorld().getName());
            }
            default -> resolveBestTargetInternal(player, SpawnReason.JOIN, player.getWorld().getName());
        };

        Optional<SpawnTarget> resolved = fireResolveEvent(player, SpawnReason.JOIN, target);
        resolved.ifPresent(t -> teleportSessionManager.rememberSelectedTarget(player.getUniqueId(), t));
        return resolved;
    }

    @Override
    public Optional<SpawnTarget> resolveDeathTarget(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        Optional<SpawnTarget> target = spawnRepository.deathSpawn()
                .filter(SpawnPoint::isEnabled)
                .map(SpawnPoint::toTarget);

        if (target.isEmpty() && config.deathFallbackToWorldSpawn()) {
            target = resolveBestTargetInternal(player, SpawnReason.DEATH, player.getWorld().getName());
        }

        Optional<SpawnTarget> resolved = fireResolveEvent(player, SpawnReason.DEATH, target);
        resolved.ifPresent(t -> teleportSessionManager.rememberSelectedTarget(player.getUniqueId(), t));
        return resolved;
    }

    @Override
    public Optional<SpawnTarget> resolveVoidTarget(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        Optional<SpawnTarget> resolved = fireResolveEvent(player, SpawnReason.VOID, resolveBestTargetInternal(player, SpawnReason.VOID, player.getWorld().getName()));
        resolved.ifPresent(t -> teleportSessionManager.rememberSelectedTarget(player.getUniqueId(), t));
        return resolved;
    }

    private Optional<SpawnTarget> fireResolveEvent(Player player, SpawnReason reason, Optional<SpawnTarget> target) {
        if (target.isEmpty()) {
            return Optional.empty();
        }

        SpawnResolveEvent event = new SpawnResolveEvent(player, reason, target.get());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTarget() == null) {
            return Optional.empty();
        }
        return Optional.of(event.getTarget());
    }

    @Override
    public void requestTeleport(Player player, SpawnTarget target, SpawnReason reason) {
        teleportSessionManager.requestTeleport(player, target, reason);
    }

    @Override
    public void requestTeleport(Player player, SpawnTarget target, SpawnReason reason, boolean silent, boolean applyCooldown, int cooldownSeconds) {
        teleportSessionManager.requestTeleport(player, target, reason, globalTeleportPresentation(), silent, applyCooldown, cooldownSeconds, -1);
    }

    @Override
    public void requestTeleport(Player player, SpawnTarget target, SpawnReason reason, boolean silent, boolean applyCooldown, int cooldownSeconds, int delaySecondsOverride) {
        teleportSessionManager.requestTeleport(player, target, reason, globalTeleportPresentation(), silent, applyCooldown, cooldownSeconds, delaySecondsOverride);
    }

    public void requestTeleport(Player player, SpawnTarget target, SpawnReason reason, TeleportPresentation presentation, boolean silent, boolean applyCooldown, int cooldownSeconds) {
        teleportSessionManager.requestTeleport(player, target, reason, presentation, silent, applyCooldown, cooldownSeconds, -1);
    }

    public void requestTeleport(Player player, SpawnTarget target, SpawnReason reason, TeleportPresentation presentation, boolean silent, boolean applyCooldown, int cooldownSeconds, int delaySecondsOverride) {
        teleportSessionManager.requestTeleport(player, target, reason, presentation, silent, applyCooldown, cooldownSeconds, delaySecondsOverride);
    }

    @Override
    public void cancelTeleport(Player player) {
        teleportSessionManager.cancel(player.getUniqueId());
    }

    @Override
    public boolean hasActiveTeleport(Player player) {
        return teleportSessionManager.hasActiveSession(player.getUniqueId());
    }

    @Override
    public boolean isOnCooldown(Player player) {
        return teleportSessionManager.isOnCooldown(player);
    }

    @Override
    public long remainingCooldownSeconds(Player player) {
        return teleportSessionManager.remainingCooldownSeconds(player);
    }

    @Override
    public void showConnectionTitle(Player player, boolean firstJoin) {
        if (!config.connectionTitleEnabled()) {
            return;
        }
        if (firstJoin && !config.connectionTitleFirstJoinEnabled()) {
            return;
        }
        if (!firstJoin && !config.connectionTitleRepeatJoinEnabled()) {
            return;
        }

        player.showTitle(Title.title(
                ComponentUtil.legacy(config.connectionTitleText(), Map.of()),
                ComponentUtil.legacy(config.connectionSubtitleText(), Map.of()),
                Title.Times.times(
                        Duration.ofMillis(config.connectionTitleFadeIn() * 50L),
                        Duration.ofMillis(config.connectionTitleStay() * 50L),
                        Duration.ofMillis(config.connectionTitleFadeOut() * 50L)
                )
        ));
    }

    @Override
    public boolean setNamedSpawn(Player player, String id, String permission) {
        if (player == null || id == null || id.isBlank()) {
            return false;
        }

        Location location = player.getLocation();
        SpawnPoint point = new SpawnPoint();
        point.setId(normalizeId(id));
        point.setDisplayName(id.trim());
        point.setWorldName(location.getWorld().getName());
        point.setX(location.getX());
        point.setY(location.getY());
        point.setZ(location.getZ());
        point.setYaw(location.getYaw());
        point.setPitch(location.getPitch());
        point.setPermission(permission == null ? "" : permission.trim());
        point.setIcon("ENDER_PEARL");
        point.setEnabled(true);
        point.setSlot(-1);
        spawnRepository.putNamed(point);
        return true;
    }

    @Override
    public boolean removeNamedSpawn(String id) {
        List<SpawnPoint> matches = namedSpawnMatches(id);
        if (matches.size() != 1) {
            return false;
        }
        return spawnRepository.removeNamed(matches.get(0).getWorldName(), matches.get(0).getId());
    }

    @Override
    public boolean removeNamedSpawn(String worldName, String id) {
        return spawnRepository.removeNamed(worldName, id);
    }

    private List<SpawnPoint> namedSpawnMatches(String id) {
        if (id == null || id.isBlank()) {
            return List.of();
        }
        String normalized = normalizeId(id);
        return spawnRepository.namedSpawns().stream()
                .filter(point -> point.getId() != null && point.getId().equalsIgnoreCase(normalized))
                .collect(Collectors.toList());
    }

    @Override
    public void setWorldSpawn(String worldName, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        String resolvedWorld = worldName == null || worldName.isBlank() ? location.getWorld().getName() : worldName;
        SpawnPoint point = new SpawnPoint();
        point.setId(normalizeId(resolvedWorld));
        point.setDisplayName(resolvedWorld + " spawn");
        point.setWorldName(location.getWorld().getName());
        point.setX(location.getX());
        point.setY(location.getY());
        point.setZ(location.getZ());
        point.setYaw(location.getYaw());
        point.setPitch(location.getPitch());
        point.setPermission("");
        point.setIcon("COMPASS");
        point.setEnabled(true);
        point.setSlot(-1);
        spawnRepository.putWorldSpawn(point);
    }

    @Override
    public void setFirstJoinSpawn(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        SpawnPoint point = new SpawnPoint();
        point.setId("first-join");
        point.setDisplayName("First Join Spawn");
        point.setWorldName(location.getWorld().getName());
        point.setX(location.getX());
        point.setY(location.getY());
        point.setZ(location.getZ());
        point.setYaw(location.getYaw());
        point.setPitch(location.getPitch());
        point.setPermission("");
        point.setIcon("NETHER_STAR");
        point.setEnabled(true);
        point.setSlot(-1);
        spawnRepository.setFirstJoinSpawn(point);
    }

    @Override
    public void setDeathSpawn(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        SpawnPoint point = new SpawnPoint();
        point.setId("death");
        point.setDisplayName("Death Spawn");
        point.setWorldName(location.getWorld().getName());
        point.setX(location.getX());
        point.setY(location.getY());
        point.setZ(location.getZ());
        point.setYaw(location.getYaw());
        point.setPitch(location.getPitch());
        point.setPermission("");
        point.setIcon("TOTEM_OF_UNDYING");
        point.setEnabled(true);
        point.setSlot(-1);
        spawnRepository.setDeathSpawn(point);
    }

    @Override
    public void setPersonalSpawn(Player owner, Location location) {
        if (owner == null || location == null || location.getWorld() == null) {
            return;
        }
        personalSpawnRepository.put(new PersonalSpawn(
                owner.getUniqueId(),
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        ));
    }

    @Override
    public boolean clearPersonalSpawn(Player owner, String worldName) {
        return owner != null && personalSpawnRepository.remove(owner.getUniqueId(), worldName);
    }

    @Override
    public Optional<SpawnPoint> getNamedSpawn(String id) {
        return spawnRepository.findNamed(id);
    }

    @Override
    public Optional<SpawnPoint> getNamedSpawn(String worldName, String id) {
        return spawnRepository.findNamed(worldName, id);
    }

    @Override
    public List<SpawnPoint> getNamedSpawns(String worldName) {
        return new ArrayList<>(spawnRepository.namedSpawns(worldName));
    }

    @Override
    public Map<String, List<SpawnPoint>> getNamedSpawnsByWorld() {
        return spawnRepository.namedSpawnsByWorld();
    }

    @Override
    public Optional<SpawnPoint> getWorldSpawn(String worldName) {
        return spawnRepository.findWorldSpawn(worldName);
    }

    @Override
    public List<SpawnPoint> getWorldSpawns() {
        return new ArrayList<>(spawnRepository.worldSpawns());
    }

    @Override
    public Optional<SpawnPoint> getFirstJoinSpawn() {
        return spawnRepository.firstJoinSpawn();
    }

    @Override
    public Optional<SpawnPoint> getDeathSpawn() {
        return spawnRepository.deathSpawn();
    }

    @Override
    public Optional<PersonalSpawn> getPersonalSpawn(UUID playerId, String worldName) {
        return personalSpawnRepository.find(playerId, worldName);
    }

    @Override
    public Map<String, PersonalSpawn> getPersonalSpawns(UUID playerId) {
        return personalSpawnRepository.findAll(playerId);
    }

    @Override
    public List<String> spawnNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (SpawnPoint point : spawnRepository.worldSpawns()) {
            names.add(point.getWorldName());
        }
        for (SpawnPoint point : spawnRepository.namedSpawns()) {
            names.add(point.getWorldName() + ":" + point.getId());
        }
        if (spawnRepository.firstJoinSpawn().isPresent()) {
            names.add("first-join");
        }
        if (spawnRepository.deathSpawn().isPresent()) {
            names.add("death");
        }
        return new ArrayList<>(names);
    }

    @Override
    public List<String> worldNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>(spawnRepository.worldNames());
        Bukkit.getWorlds().forEach(world -> names.add(world.getName()));
        return new ArrayList<>(names);
    }

    @Override
    public Optional<SpawnTarget> getSelectedTarget(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return teleportSessionManager.getSelectedTarget(player.getUniqueId());
    }

    @Override
    public void rememberSelectedTarget(Player player, SpawnTarget target) {
        if (player == null || target == null) {
            return;
        }
        teleportSessionManager.rememberSelectedTarget(player.getUniqueId(), target);
    }

    public Component message(String key, Map<String, String> placeholders) {
        return messages.prefix().append(messages.component(key, placeholders));
    }

    public boolean canAccess(Player player, SpawnTarget target) {
        return target.permission() == null || target.permission().isBlank() || player.hasPermission(target.permission());
    }

    private String normalizeId(String id) {
        return id.trim().toLowerCase(java.util.Locale.ROOT).replace(' ', '-');
    }
}
