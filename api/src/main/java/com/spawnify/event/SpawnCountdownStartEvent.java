package com.spawnify.event;

import com.spawnify.model.SpawnReason;
import com.spawnify.model.SpawnTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class SpawnCountdownStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final SpawnReason reason;
    private final SpawnTarget target;
    private final int seconds;

    public SpawnCountdownStartEvent(Player player, SpawnReason reason, SpawnTarget target, int seconds) {
        this.player = player;
        this.reason = reason;
        this.target = target;
        this.seconds = seconds;
    }

    public Player getPlayer() { return player; }
    public SpawnReason getReason() { return reason; }
    public SpawnTarget getTarget() { return target; }
    public int getSeconds() { return seconds; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
