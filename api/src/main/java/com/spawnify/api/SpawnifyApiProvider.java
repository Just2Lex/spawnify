package com.spawnify.api;

import org.bukkit.Bukkit;

import java.util.Optional;

public final class SpawnifyApiProvider {

    private SpawnifyApiProvider() {
    }

    public static Optional<SpawnifyApi> get() {
        return Optional.ofNullable(Bukkit.getServicesManager().load(SpawnifyApi.class));
    }

    public static boolean isAvailable() {
        return get().isPresent();
    }
}
