package com.spawnify.storage;

import com.spawnify.model.SpawnPoint;
import com.spawnify.model.SpawnTargetType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class SpawnRepository {

    private static final int CURRENT_SCHEMA_VERSION = 2;

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, WorldSpawns> worlds = new LinkedHashMap<>();
    private SpawnPoint firstJoinSpawn;
    private SpawnPoint deathSpawn;

    public SpawnRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spawns.yml");
        load();
    }

    public synchronized void load() {
        worlds.clear();
        firstJoinSpawn = null;
        deathSpawn = null;

        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        int schemaVersion = cfg.getInt("schema-version", 1);
        if (cfg.isConfigurationSection("worlds")) {
            loadV2(cfg);
        } else {
            loadLegacy(cfg);
        }

        if (schemaVersion < CURRENT_SCHEMA_VERSION) {
            save();
        }
    }

    private void loadV2(YamlConfiguration cfg) {
        ConfigurationSection worldsSection = cfg.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldKey : worldsSection.getKeys(false)) {
                ConfigurationSection worldSection = worldsSection.getConfigurationSection(worldKey);
                if (worldSection == null) {
                    continue;
                }

                String worldName = worldSection.getString("world", worldKey);
                WorldSpawns worldSpawns = world(worldName);

                ConfigurationSection defaultSection = worldSection.getConfigurationSection("default");
                if (defaultSection != null) {
                    readPoint(defaultSection, SpawnTargetType.WORLD_DEFAULT, worldName).ifPresent(worldSpawns::setDefaultSpawn);
                }

                ConfigurationSection namedSection = worldSection.getConfigurationSection("named");
                if (namedSection != null) {
                    for (String id : namedSection.getKeys(false)) {
                        ConfigurationSection pointSection = namedSection.getConfigurationSection(id);
                        if (pointSection != null) {
                            readPoint(pointSection, SpawnTargetType.NAMED, id).ifPresent(worldSpawns::putNamed);
                        }
                    }
                }
            }
        }

        ConfigurationSection specialSection = cfg.getConfigurationSection("special");
        if (specialSection != null) {
            ConfigurationSection first = specialSection.getConfigurationSection("first-join");
            if (first != null) {
                firstJoinSpawn = readPoint(first, SpawnTargetType.FIRST_JOIN, "first-join").orElse(null);
            }
            ConfigurationSection death = specialSection.getConfigurationSection("death");
            if (death != null) {
                deathSpawn = readPoint(death, SpawnTargetType.DEATH, "death").orElse(null);
            }
        }
    }

    private void loadLegacy(YamlConfiguration cfg) {
        ConfigurationSection namedSection = cfg.getConfigurationSection("named");
        if (namedSection != null) {
            for (String key : namedSection.getKeys(false)) {
                ConfigurationSection pointSection = namedSection.getConfigurationSection(key);
                if (pointSection != null) {
                    readPoint(pointSection, SpawnTargetType.NAMED, key).ifPresent(this::putNamedWithoutSave);
                }
            }
        }

        ConfigurationSection worldSpawnsSection = cfg.getConfigurationSection("world-spawns");
        if (worldSpawnsSection != null) {
            for (String key : worldSpawnsSection.getKeys(false)) {
                ConfigurationSection pointSection = worldSpawnsSection.getConfigurationSection(key);
                if (pointSection != null) {
                    readPoint(pointSection, SpawnTargetType.WORLD_DEFAULT, key).ifPresent(this::putWorldSpawnWithoutSave);
                }
            }
        }

        ConfigurationSection specialSection = cfg.getConfigurationSection("special");
        if (specialSection != null) {
            ConfigurationSection first = specialSection.getConfigurationSection("first-join");
            if (first != null) {
                firstJoinSpawn = readPoint(first, SpawnTargetType.FIRST_JOIN, "first-join").orElse(null);
            }
            ConfigurationSection death = specialSection.getConfigurationSection("death");
            if (death != null) {
                deathSpawn = readPoint(death, SpawnTargetType.DEATH, "death").orElse(null);
            }
        }
    }

    private Optional<SpawnPoint> readPoint(ConfigurationSection section, SpawnTargetType type, String id) {
        String worldName = section.getString("world", "");
        if (worldName == null || worldName.isBlank()) {
            plugin.getLogger().warning("Skipping spawn '" + id + "' because world is missing.");
            return Optional.empty();
        }

        SpawnPoint point = new SpawnPoint();
        point.setId(id);
        point.setType(type);
        point.setDisplayName(section.getString("display-name", id));
        point.setWorldName(worldName);
        point.setX(section.getDouble("x"));
        point.setY(section.getDouble("y"));
        point.setZ(section.getDouble("z"));
        point.setYaw((float) section.getDouble("yaw", 0.0D));
        point.setPitch((float) section.getDouble("pitch", 0.0D));
        point.setPermission(section.getString("permission", ""));
        point.setIcon(section.getString("icon", type == SpawnTargetType.WORLD_DEFAULT ? "COMPASS" : "ENDER_PEARL"));
        point.setEnabled(section.getBoolean("enabled", true));
        point.setSlot(section.getInt("gui-slot", -1));
        return Optional.of(point);
    }

    public synchronized void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("schema-version", CURRENT_SCHEMA_VERSION);

        for (WorldSpawns worldSpawns : worlds.values()) {
            ConfigurationSection worldSection = cfg.createSection("worlds." + worldSpawns.worldKey());
            worldSection.set("world", worldSpawns.worldName());

            if (worldSpawns.defaultSpawn() != null) {
                ConfigurationSection defaultSection = worldSection.createSection("default");
                worldSpawns.defaultSpawn().save(defaultSection);
            }

            if (!worldSpawns.namedSpawns().isEmpty()) {
                ConfigurationSection namedSection = worldSection.createSection("named");
                for (SpawnPoint point : worldSpawns.namedSpawns().values()) {
                    ConfigurationSection pointSection = namedSection.createSection(point.getId());
                    point.save(pointSection);
                }
            }
        }

        if (firstJoinSpawn != null) {
            ConfigurationSection section = cfg.createSection("special.first-join");
            firstJoinSpawn.save(section);
        }

        if (deathSpawn != null) {
            ConfigurationSection section = cfg.createSection("special.death");
            deathSpawn.save(section);
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save spawns.yml: " + e.getMessage());
        }
    }

    public synchronized Collection<SpawnPoint> namedSpawns() {
        List<SpawnPoint> result = new ArrayList<>();
        for (WorldSpawns worldSpawns : worlds.values()) {
            result.addAll(worldSpawns.namedSpawns().values());
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized Collection<SpawnPoint> namedSpawns(String worldName) {
        WorldSpawns data = getWorldData(worldName);
        if (data == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(data.namedSpawns().values()));
    }

    public synchronized Map<String, List<SpawnPoint>> namedSpawnsByWorld() {
        Map<String, List<SpawnPoint>> result = new LinkedHashMap<>();
        for (WorldSpawns worldSpawns : worlds.values()) {
            result.put(worldSpawns.worldName(), Collections.unmodifiableList(new ArrayList<>(worldSpawns.namedSpawns().values())));
        }
        return Collections.unmodifiableMap(result);
    }

    public synchronized Collection<SpawnPoint> worldSpawns() {
        List<SpawnPoint> result = new ArrayList<>();
        for (WorldSpawns worldSpawns : worlds.values()) {
            if (worldSpawns.defaultSpawn() != null) {
                result.add(worldSpawns.defaultSpawn());
            }
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized Optional<SpawnPoint> findNamed(String id) {
        String normalized = normalize(id);
        for (WorldSpawns worldSpawns : worlds.values()) {
            SpawnPoint point = worldSpawns.namedSpawns().get(normalized);
            if (point != null) {
                return Optional.of(point);
            }
        }
        return Optional.empty();
    }

    public synchronized Optional<SpawnPoint> findNamed(String worldName, String id) {
        WorldSpawns data = getWorldData(worldName);
        if (data == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(data.namedSpawns().get(normalize(id)));
    }

    public synchronized Optional<SpawnPoint> findWorldSpawn(String worldName) {
        WorldSpawns data = getWorldData(worldName);
        if (data == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(data.defaultSpawn());
    }

    public synchronized Optional<SpawnPoint> firstJoinSpawn() {
        return Optional.ofNullable(firstJoinSpawn);
    }

    public synchronized Optional<SpawnPoint> deathSpawn() {
        return Optional.ofNullable(deathSpawn);
    }

    public synchronized void putNamed(SpawnPoint point) {
        putPoint(point, true);
    }

    public synchronized void putWorldSpawn(SpawnPoint point) {
        point.setType(SpawnTargetType.WORLD_DEFAULT);
        putPoint(point, true);
    }

    public synchronized void setFirstJoinSpawn(SpawnPoint point) {
        point.setType(SpawnTargetType.FIRST_JOIN);
        point.setId("first-join");
        firstJoinSpawn = point;
        save();
    }

    public synchronized void setDeathSpawn(SpawnPoint point) {
        point.setType(SpawnTargetType.DEATH);
        point.setId("death");
        deathSpawn = point;
        save();
    }

    public synchronized boolean removeNamed(String id) {
        String normalized = normalize(id);
        for (WorldSpawns worldSpawns : worlds.values()) {
            if (worldSpawns.namedSpawns().remove(normalized) != null) {
                save();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean removeNamed(String worldName, String id) {
        WorldSpawns data = getWorldData(worldName);
        if (data == null) {
            return false;
        }
        boolean removed = data.namedSpawns().remove(normalize(id)) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public synchronized Set<String> worldNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (WorldSpawns data : worlds.values()) {
            names.add(data.worldName());
        }
        return Collections.unmodifiableSet(names);
    }

    public synchronized boolean hasWorld(String worldName) {
        return worlds.containsKey(normalize(worldName));
    }

    private void putPoint(SpawnPoint point, boolean save) {
        Objects.requireNonNull(point, "point");
        String worldName = sanitizeWorldName(point.getWorldName());
        point.setWorldName(worldName);

        WorldSpawns world = world(worldName);
        if (point.getType() == SpawnTargetType.WORLD_DEFAULT) {
            world.setDefaultSpawn(point);
        } else {
            world.putNamed(point);
        }

        if (save) {
            save();
        }
    }

    private void putNamedWithoutSave(SpawnPoint point) {
        putPoint(point, false);
    }

    private void putWorldSpawnWithoutSave(SpawnPoint point) {
        point.setType(SpawnTargetType.WORLD_DEFAULT);
        putPoint(point, false);
    }

    private WorldSpawns getWorldData(String worldName) {
        return worlds.get(normalize(sanitizeWorldName(worldName)));
    }

    private WorldSpawns world(String worldName) {
        String sanitized = sanitizeWorldName(worldName);
        return worlds.computeIfAbsent(normalize(sanitized), key -> new WorldSpawns(sanitized));
    }

    private String sanitizeWorldName(String worldName) {
        return worldName == null || worldName.isBlank() ? "world" : worldName;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static final class WorldSpawns {
        private final String worldName;
        private SpawnPoint defaultSpawn;
        private final Map<String, SpawnPoint> namedSpawns = new LinkedHashMap<>();

        private WorldSpawns(String worldName) {
            this.worldName = worldName;
        }

        private String worldKey() {
            return worldName.toLowerCase(Locale.ROOT);
        }

        private String worldName() {
            return worldName;
        }

        private SpawnPoint defaultSpawn() {
            return defaultSpawn;
        }

        private void setDefaultSpawn(SpawnPoint defaultSpawn) {
            this.defaultSpawn = defaultSpawn;
        }

        private Map<String, SpawnPoint> namedSpawns() {
            return namedSpawns;
        }

        private void putNamed(SpawnPoint point) {
            point.setType(point.getType() == SpawnTargetType.WORLD_DEFAULT ? SpawnTargetType.WORLD_DEFAULT : SpawnTargetType.NAMED);
            namedSpawns.put(point.getId().toLowerCase(Locale.ROOT), point);
        }
    }
}
