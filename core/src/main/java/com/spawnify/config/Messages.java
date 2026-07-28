package com.spawnify.config;

import com.spawnify.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Messages {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            if (!plugin.getDataFolder().exists()) {
                //noinspection ResultOfMethodCallIgnored
                plugin.getDataFolder().mkdirs();
            }
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public YamlConfiguration config() {
        return config;
    }

    public Component prefix() {
        return ComponentUtil.legacy(raw("prefix"), Map.of());
    }

    public String prefixRaw() {
        return raw("prefix");
    }

    public boolean hasValue(String key) {
        return resolveString(key) != null || config.isList(key);
    }

    public boolean enabled(String key, boolean defaultValue) {
        return config.getBoolean(key + ".enabled", defaultValue);
    }

    public Component component(String key) {
        return component(key, Map.of());
    }

    public Component component(String key, Map<String, String> placeholders) {
        String raw = resolveString(key);
        return ComponentUtil.legacy(raw == null ? key : raw, placeholders);
    }

    public Optional<Component> componentIfEnabled(String key, Map<String, String> placeholders) {
        if (!hasValue(key)) {
            return Optional.empty();
        }
        if (!enabled(key, true)) {
            return Optional.empty();
        }
        return Optional.of(component(key, placeholders));
    }

    public List<Component> components(String key) {
        return ComponentUtil.legacyList(config.getStringList(key), Map.of());
    }

    public List<Component> components(String key, Map<String, String> placeholders) {
        return ComponentUtil.legacyList(config.getStringList(key), placeholders);
    }

    public String raw(String key) {
        String raw = resolveString(key);
        return raw == null ? key : raw;
    }

    private String resolveString(String key) {
        if (config.isString(key)) {
            return config.getString(key);
        }
        if (config.isString(key + ".text")) {
            return config.getString(key + ".text");
        }
        if (config.isString(key + ".message")) {
            return config.getString(key + ".message");
        }
        if (config.isString(key + ".value")) {
            return config.getString(key + ".value");
        }
        return null;
    }
}
