package com.spawnify.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public final class SpawnPoint {

    private String id;
    private String displayName;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private String permission;
    private String icon;
    private boolean enabled = true;
    private int slot = -1;
    private SpawnTargetType type = SpawnTargetType.NAMED;

    public SpawnPoint() {}

    public static SpawnPoint fromSection(ConfigurationSection section, SpawnTargetType type, String id) {
        SpawnPoint point = new SpawnPoint();
        point.id = id;
        point.type = type;
        point.displayName = section.getString("display-name", id);
        point.worldName = section.getString("world", "world");
        point.x = section.getDouble("x");
        point.y = section.getDouble("y");
        point.z = section.getDouble("z");
        point.yaw = (float) section.getDouble("yaw", 0.0);
        point.pitch = (float) section.getDouble("pitch", 0.0);
        point.permission = section.getString("permission", "");
        point.icon = section.getString("icon", "ENDER_PEARL");
        point.enabled = section.getBoolean("enabled", true);
        point.slot = section.getInt("gui-slot", -1);
        return point;
    }

    public void save(ConfigurationSection section) {
        section.set("display-name", displayName);
        section.set("world", worldName);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("yaw", yaw);
        section.set("pitch", pitch);
        section.set("permission", permission);
        section.set("icon", icon);
        section.set("enabled", enabled);
        section.set("gui-slot", slot);
    }

    public SpawnTarget toTarget() {
        return new SpawnTarget(type, id, displayName, worldName, x, y, z, yaw, pitch, permission, icon, enabled, slot);
    }

    public Location toLocation() {
        if (Bukkit.getWorld(worldName) == null) {
            return null;
        }
        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }
    public SpawnTargetType getType() { return type; }
    public void setType(SpawnTargetType type) { this.type = type; }

    public Material iconMaterialOrDefault(Material fallback) {
        Material material = icon == null ? null : Material.matchMaterial(icon);
        return material == null ? fallback : material;
    }
}
