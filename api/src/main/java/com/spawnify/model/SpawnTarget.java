package com.spawnify.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public final class SpawnTarget {

    private final SpawnTargetType type;
    private final String id;
    private final String displayName;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final String permission;
    private final String icon;
    private final boolean enabled;
    private final int slot;

    public SpawnTarget(SpawnTargetType type,
                       String id,
                       String displayName,
                       String worldName,
                       double x,
                       double y,
                       double z,
                       float yaw,
                       float pitch,
                       String permission,
                       String icon,
                       boolean enabled,
                       int slot) {
        this.type = type;
        this.id = id;
        this.displayName = displayName;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.permission = permission;
        this.icon = icon;
        this.enabled = enabled;
        this.slot = slot;
    }

    public SpawnTargetType type() { return type; }
    public String id() { return id; }
    public String displayName() { return displayName; }
    public String worldName() { return worldName; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public String permission() { return permission; }
    public String icon() { return icon; }
    public boolean enabled() { return enabled; }
    public int slot() { return slot; }

    public Location toLocation() {
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public Material iconMaterialOrDefault(Material fallback) {
        Material material = icon == null ? null : Material.matchMaterial(icon);
        return material == null ? fallback : material;
    }
}
