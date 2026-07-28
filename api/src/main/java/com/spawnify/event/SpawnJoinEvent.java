package com.spawnify.event;

import com.spawnify.model.SpawnTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class SpawnJoinEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final boolean firstJoin;
    private SpawnTarget target;
    private boolean cancelled;

    public SpawnJoinEvent(Player player, boolean firstJoin, SpawnTarget target) {
        this.player = player;
        this.firstJoin = firstJoin;
        this.target = target;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isFirstJoin() {
        return firstJoin;
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
