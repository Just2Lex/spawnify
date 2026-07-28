package com.spawnify.storage;

import com.spawnify.model.PersonalSpawn;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PersonalSpawnRepository {

    private static final int CURRENT_SCHEMA_VERSION = 2;

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Map<String, PersonalSpawn>> spawns = new HashMap<>();

    public PersonalSpawnRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "personal-spawns.yml");
        load();
    }

    public synchronized void load() {
        spawns.clear();

        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        int schemaVersion = cfg.getInt("schema-version", 1);
        ConfigurationSection playersSection = cfg.getConfigurationSection("players");
        if (playersSection == null) {
            if (schemaVersion < CURRENT_SCHEMA_VERSION) {
                save();
            }
            return;
        }

        for (String uuidString : playersSection.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidString);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidString);
            if (playerSection == null) {
                continue;
            }

            Map<String, PersonalSpawn> byWorld = new HashMap<>();
            for (String worldKey : playerSection.getKeys(false)) {
                ConfigurationSection section = playerSection.getConfigurationSection(worldKey);
                if (section == null) {
                    continue;
                }

                String worldName = section.getString("world", worldKey);
                if (worldName == null || worldName.isBlank()) {
                    continue;
                }

                byWorld.put(normalize(worldName), new PersonalSpawn(
                        uuid,
                        worldName,
                        section.getDouble("x"),
                        section.getDouble("y"),
                        section.getDouble("z"),
                        (float) section.getDouble("yaw", 0.0D),
                        (float) section.getDouble("pitch", 0.0D)
                ));
            }
            spawns.put(uuid, byWorld);
        }

        if (schemaVersion < CURRENT_SCHEMA_VERSION) {
            save();
        }
    }

    public synchronized void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("schema-version", CURRENT_SCHEMA_VERSION);

        for (Map.Entry<UUID, Map<String, PersonalSpawn>> entry : spawns.entrySet()) {
            for (Map.Entry<String, PersonalSpawn> worldEntry : entry.getValue().entrySet()) {
                PersonalSpawn spawn = worldEntry.getValue();
                ConfigurationSection section = cfg.createSection("players." + entry.getKey() + "." + worldEntry.getKey());
                section.set("world", spawn.worldName());
                section.set("x", spawn.x());
                section.set("y", spawn.y());
                section.set("z", spawn.z());
                section.set("yaw", spawn.yaw());
                section.set("pitch", spawn.pitch());
            }
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save personal-spawns.yml: " + e.getMessage());
        }
    }

    public synchronized Optional<PersonalSpawn> find(UUID playerId, String worldName) {
        Map<String, PersonalSpawn> byWorld = spawns.get(playerId);
        if (byWorld == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byWorld.get(normalize(worldName)));
    }

    public synchronized Map<String, PersonalSpawn> findAll(UUID playerId) {
        Map<String, PersonalSpawn> byWorld = spawns.get(playerId);
        return byWorld == null ? Map.of() : Map.copyOf(byWorld);
    }

    public synchronized void put(PersonalSpawn spawn) {
        spawns.computeIfAbsent(spawn.playerId(), key -> new HashMap<>())
                .put(normalize(spawn.worldName()), spawn);
        save();
    }

    public synchronized boolean remove(UUID playerId, String worldName) {
        Map<String, PersonalSpawn> byWorld = spawns.get(playerId);
        if (byWorld == null) {
            return false;
        }

        boolean removed = byWorld.remove(normalize(worldName)) != null;
        if (byWorld.isEmpty()) {
            spawns.remove(playerId);
        }
        if (removed) {
            save();
        }
        return removed;
    }

    private String normalize(String worldName) {
        return worldName == null ? "" : worldName.toLowerCase(java.util.Locale.ROOT);
    }
}
