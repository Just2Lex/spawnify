package com.spawnify.event;

import com.spawnify.model.SpawnTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class SpawnVoidEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private SpawnTarget target;
    private double threshold;
    private boolean cancelled;

    public SpawnVoidEvent(Player player, SpawnTarget target, double threshold) {
        this.player = player;
        this.target = target;
        this.threshold = threshold;
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

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
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
