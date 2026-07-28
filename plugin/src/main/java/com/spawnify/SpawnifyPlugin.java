
package com.spawnify;

import com.spawnify.api.SpawnifyApi;
import com.spawnify.command.PlayerSpawnCommand;
import com.spawnify.command.SpawnifyAdminCommand;
import com.spawnify.config.Messages;
import com.spawnify.config.PluginConfig;
import com.spawnify.gui.SpawnSelectorGui;
import com.spawnify.listener.PlayerLifecycleListener;
import com.spawnify.listener.SpawnGuiListener;
import com.spawnify.listener.VoidListener;
import com.spawnify.model.PersonalSpawn;
import com.spawnify.model.SpawnPoint;
import com.spawnify.model.SpawnReason;
import com.spawnify.model.SpawnTarget;
import com.spawnify.papi.SpawnifyExpansion;
import com.spawnify.service.SpawnService;
import com.spawnify.storage.PersonalSpawnRepository;
import com.spawnify.storage.SpawnRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SpawnifyPlugin extends JavaPlugin implements SpawnifyApi {

    private PluginConfig pluginConfig;
    private Messages messages;
    private SpawnRepository spawnRepository;
    private PersonalSpawnRepository personalSpawnRepository;
    private SpawnService spawnService;
    private SpawnSelectorGui spawnSelectorGui;
    private SpawnifyExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        try {
            if (!getDataFolder().exists()) {
                //noinspection ResultOfMethodCallIgnored
                getDataFolder().mkdirs();
            }
            saveDefaultConfig();
            saveIfMissing("messages.yml");

            pluginConfig = new PluginConfig(this);
            messages = new Messages(this);
            spawnRepository = new SpawnRepository(this);
            personalSpawnRepository = new PersonalSpawnRepository(this);
            spawnService = new SpawnService(this, pluginConfig, messages, spawnRepository, personalSpawnRepository);
            spawnSelectorGui = new SpawnSelectorGui(this, pluginConfig, messages, spawnService);

            getServer().getServicesManager().register(SpawnifyApi.class, this, this, ServicePriority.Normal);

            PlayerSpawnCommand playerSpawnCommand = new PlayerSpawnCommand(this, spawnService, spawnSelectorGui);
            SpawnifyAdminCommand adminCommand = new SpawnifyAdminCommand(this, spawnService);

            if (getCommand("spawn") != null) {
                getCommand("spawn").setExecutor(playerSpawnCommand);
                getCommand("spawn").setTabCompleter(playerSpawnCommand);
            }

            if (getCommand("spawnify") != null) {
                getCommand("spawnify").setExecutor(adminCommand);
                getCommand("spawnify").setTabCompleter(adminCommand);
            }

            Bukkit.getPluginManager().registerEvents(new PlayerLifecycleListener(this, spawnService), this);
            Bukkit.getPluginManager().registerEvents(new VoidListener(this, spawnService), this);
            Bukkit.getPluginManager().registerEvents(new SpawnGuiListener(spawnSelectorGui), this);

            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                placeholderExpansion = new SpawnifyExpansion(this);
                placeholderExpansion.register();
            }

            getLogger().info("Spawnify enabled.");
        } catch (Throwable t) {
            getLogger().severe("Failed to enable Spawnify: " + t.getMessage());
            t.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (spawnService != null) {
            spawnService.shutdown();
        }
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
        getServer().getServicesManager().unregister(SpawnifyApi.class, this);
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("Spawnify disabled.");
    }

    private void saveIfMissing(String resourceName) {
        File target = new File(getDataFolder(), resourceName);
        if (!target.exists()) {
            saveResource(resourceName, false);
        }
    }

    public SpawnService getSpawnService() {
        return spawnService;
    }

    @Override
    public List<SpawnTarget> getAvailableTargets(Player player) {
        return spawnService.availableTargets(player);
    }

    @Override
    public List<SpawnTarget> getAvailableTargets(Player player, String worldName) {
        return spawnService.availableTargets(player, worldName);
    }

    @Override
    public Optional<SpawnTarget> resolveBestTarget(Player player, SpawnReason reason) {
        return spawnService.resolveBestTarget(player, reason);
    }

    @Override
    public Optional<SpawnTarget> resolveBestTarget(Player player, SpawnReason reason, String worldName) {
        return spawnService.resolveBestTarget(player, reason, worldName);
    }

    @Override
    public Optional<SpawnTarget> resolveSelectableTarget(Player player, String identifier) {
        return spawnService.resolveSelectableTarget(player, identifier);
    }

    @Override
    public Optional<SpawnTarget> resolveSelectableTarget(Player player, String identifier, String worldName) {
        return spawnService.resolveSelectableTarget(player, identifier, worldName);
    }

    @Override
    public void requestTeleport(Player player, SpawnTarget target, SpawnReason reason) {
        spawnService.requestTeleport(player, target, reason);
    }

    @Override
    public void requestTeleport(Player player, SpawnTarget target, SpawnReason reason, boolean silent, boolean applyCooldown, int cooldownSeconds) {
        spawnService.requestTeleport(player, target, reason, silent, applyCooldown, cooldownSeconds);
    }

    @Override
    public void requestTeleport(Player player, SpawnTarget target, SpawnReason reason, boolean silent, boolean applyCooldown, int cooldownSeconds, int delaySecondsOverride) {
        spawnService.requestTeleport(player, target, reason, silent, applyCooldown, cooldownSeconds, delaySecondsOverride);
    }

    @Override
    public void cancelTeleport(Player player) {
        spawnService.cancelTeleport(player);
    }

    @Override
    public boolean hasActiveTeleport(Player player) {
        return spawnService.hasActiveTeleport(player);
    }

    @Override
    public boolean isOnCooldown(Player player) {
        return spawnService.isOnCooldown(player);
    }

    @Override
    public long remainingCooldownSeconds(Player player) {
        return spawnService.remainingCooldownSeconds(player);
    }

    @Override
    public void reload() {
        spawnService.reload();
    }

    @Override
    public void showConnectionTitle(Player player, boolean firstJoin) {
        spawnService.showConnectionTitle(player, firstJoin);
    }

    @Override
    public Optional<SpawnPoint> getNamedSpawn(String id) {
        return spawnService.getNamedSpawn(id);
    }

    @Override
    public Optional<SpawnPoint> getNamedSpawn(String worldName, String id) {
        return spawnService.getNamedSpawn(worldName, id);
    }

    @Override
    public List<SpawnPoint> getNamedSpawns(String worldName) {
        return spawnService.getNamedSpawns(worldName);
    }

    @Override
    public Map<String, List<SpawnPoint>> getNamedSpawnsByWorld() {
        return spawnService.getNamedSpawnsByWorld();
    }

    @Override
    public Optional<SpawnPoint> getWorldSpawn(String worldName) {
        return spawnService.getWorldSpawn(worldName);
    }

    @Override
    public Optional<SpawnTarget> resolveFirstJoinTarget(Player player) {
        return spawnService.resolveFirstJoinTarget(player);
    }

    @Override
    public Optional<SpawnTarget> resolveRepeatJoinTarget(Player player) {
        return spawnService.resolveRepeatJoinTarget(player);
    }

    @Override
    public Optional<SpawnTarget> resolveDeathTarget(Player player) {
        return spawnService.resolveDeathTarget(player);
    }

    @Override
    public Optional<SpawnTarget> resolveVoidTarget(Player player) {
        return spawnService.resolveVoidTarget(player);
    }

    @Override
    public List<SpawnPoint> getWorldSpawns() {
        return spawnService.getWorldSpawns();
    }

    @Override
    public Optional<SpawnPoint> getFirstJoinSpawn() {
        return spawnService.getFirstJoinSpawn();
    }

    @Override
    public Optional<SpawnPoint> getDeathSpawn() {
        return spawnService.getDeathSpawn();
    }

    @Override
    public boolean setNamedSpawn(Player player, String id, String permission) {
        return spawnService.setNamedSpawn(player, id, permission);
    }

    @Override
    public boolean removeNamedSpawn(String id) {
        return spawnService.removeNamedSpawn(id);
    }

    @Override
    public boolean removeNamedSpawn(String worldName, String id) {
        return spawnService.removeNamedSpawn(worldName, id);
    }

    @Override
    public void setWorldSpawn(String worldName, Location location) {
        spawnService.setWorldSpawn(worldName, location);
    }

    @Override
    public void setFirstJoinSpawn(Location location) {
        spawnService.setFirstJoinSpawn(location);
    }

    @Override
    public void setDeathSpawn(Location location) {
        spawnService.setDeathSpawn(location);
    }

    @Override
    public void setPersonalSpawn(Player owner, Location location) {
        spawnService.setPersonalSpawn(owner, location);
    }

    @Override
    public boolean clearPersonalSpawn(Player owner, String worldName) {
        return spawnService.clearPersonalSpawn(owner, worldName);
    }

    @Override
    public Optional<PersonalSpawn> getPersonalSpawn(UUID playerId, String worldName) {
        return spawnService.getPersonalSpawnRepository().find(playerId, worldName);
    }

    @Override
    public Map<String, PersonalSpawn> getPersonalSpawns(UUID playerId) {
        return spawnService.getPersonalSpawnRepository().findAll(playerId);
    }

    @Override
    public List<String> spawnNames() {
        return spawnService.spawnNames();
    }

    @Override
    public List<String> worldNames() {
        return spawnService.worldNames();
    }

    @Override
    public Optional<SpawnTarget> getSelectedTarget(Player player) {
        return spawnService.getSelectedTarget(player);
    }

    @Override
    public void rememberSelectedTarget(Player player, SpawnTarget target) {
        spawnService.rememberSelectedTarget(player, target);
    }
}
