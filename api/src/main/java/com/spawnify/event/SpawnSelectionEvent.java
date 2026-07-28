package com.spawnify.event;

import com.spawnify.model.SpawnReason;
import com.spawnify.model.SpawnTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class SpawnSelectionEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final SpawnReason reason;
    private SpawnTarget target;
    private boolean cancelled;

    public SpawnSelectionEvent(Player player, SpawnReason reason, SpawnTarget target) {
        this.player = player;
        this.reason = reason;
        this.target = target;
    }

    public Player getPlayer() {
        return player;
    }

    public SpawnReason getReason() {
        return reason;
    }

    public SpawnTarget getTarget() {
        return target;
    }

    public void setTarget(SpawnTarget target) {
        this.target = target;
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
