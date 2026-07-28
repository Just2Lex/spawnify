package com.spawnify.event;

import com.spawnify.model.SpawnTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class SpawnDeathEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private SpawnTarget target;
    private int respawnDelaySeconds;
    private boolean cancelled;

    public SpawnDeathEvent(Player player, SpawnTarget target, int respawnDelaySeconds) {
        this.player = player;
        this.target = target;
        this.respawnDelaySeconds = respawnDelaySeconds;
    }

    public Player getPlayer() {
        return player;
    }

    public SpawnTarget getTarget() {
        return target;
    }

    public void setTarget(SpawnTarget target) {
        this.target = target;
    }

    public int getRespawnDelaySeconds() {
        return respawnDelaySeconds;
    }

    public void setRespawnDelaySeconds(int respawnDelaySeconds) {
        this.respawnDelaySeconds = Math.max(0, respawnDelaySeconds);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
