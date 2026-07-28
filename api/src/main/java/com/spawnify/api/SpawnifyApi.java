
package com.spawnify.api;

import com.spawnify.model.PersonalSpawn;
import com.spawnify.model.SpawnPoint;
import com.spawnify.model.SpawnReason;
import com.spawnify.model.SpawnTarget;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SpawnifyApi {

    List<SpawnTarget> getAvailableTargets(Player player);

    default List<SpawnTarget> getAvailableTargets(Player player, String worldName) {
        return getAvailableTargets(player);
    }

    Optional<SpawnTarget> resolveBestTarget(Player player, SpawnReason reason);

    default Optional<SpawnTarget> resolveBestTarget(Player player, SpawnReason reason, String worldName) {
        return resolveBestTarget(player, reason);
    }

    Optional<SpawnTarget> resolveSelectableTarget(Player player, String identifier);

    default Optional<SpawnTarget> resolveSelectableTarget(Player player, String identifier, String worldName) {
        return resolveSelectableTarget(player, identifier);
    }

    void requestTeleport(Player player, SpawnTarget target, SpawnReason reason);

    void requestTeleport(Player player, SpawnTarget target, SpawnReason reason, boolean silent, boolean applyCooldown, int cooldownSeconds);

    void requestTeleport(Player player, SpawnTarget target, SpawnReason reason, boolean silent, boolean applyCooldown, int cooldownSeconds, int delaySecondsOverride);

    void cancelTeleport(Player player);

    boolean hasActiveTeleport(Player player);

    boolean isOnCooldown(Player player);

    long remainingCooldownSeconds(Player player);

    void reload();

    void showConnectionTitle(Player player, boolean firstJoin);

    Optional<SpawnPoint> getNamedSpawn(String id);

    default Optional<SpawnPoint> getNamedSpawn(String worldName, String id) {
        return getNamedSpawn(id);
    }

    default List<SpawnPoint> getNamedSpawns(String worldName) {
        return Collections.emptyList();
    }

    default Map<String, List<SpawnPoint>> getNamedSpawnsByWorld() {
        return Collections.emptyMap();
    }

    Optional<SpawnPoint> getWorldSpawn(String worldName);

    Optional<SpawnTarget> resolveFirstJoinTarget(Player player);

    Optional<SpawnTarget> resolveRepeatJoinTarget(Player player);

    Optional<SpawnTarget> resolveDeathTarget(Player player);

    Optional<SpawnTarget> resolveVoidTarget(Player player);

    default List<SpawnPoint> getWorldSpawns() {
        return Collections.emptyList();
    }

    Optional<SpawnPoint> getFirstJoinSpawn();

    Optional<SpawnPoint> getDeathSpawn();

    boolean setNamedSpawn(Player player, String id, String permission);

    boolean removeNamedSpawn(String id);

    boolean removeNamedSpawn(String worldName, String id);

    void setWorldSpawn(String worldName, Location location);

    void setFirstJoinSpawn(Location location);

    void setDeathSpawn(Location location);

    void setPersonalSpawn(Player owner, Location location);

    boolean clearPersonalSpawn(Player owner, String worldName);

    default Optional<PersonalSpawn> getPersonalSpawn(UUID playerId, String worldName) {
        return Optional.empty();
    }

    default Map<String, PersonalSpawn> getPersonalSpawns(UUID playerId) {
        return Collections.emptyMap();
    }

    List<String> spawnNames();

    List<String> worldNames();

    default Optional<SpawnTarget> getSelectedTarget(Player player) {
        return Optional.empty();
    }

    default void rememberSelectedTarget(Player player, SpawnTarget target) {
    }
}
