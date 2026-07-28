package com.spawnify.event;

import com.spawnify.model.SpawnTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public final class SpawnCommandEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String label;
    private final String[] args;
    private final List<SpawnTarget> availableTargets;
    private SpawnTarget target;
    private boolean cancelled;

    public SpawnCommandEvent(Player player, String label, String[] args, List<SpawnTarget> availableTargets, SpawnTarget target) {
        this.player = player;
        this.label = label;
        this.args = args == null ? new String[0] : args.clone();
        this.availableTargets = List.copyOf(availableTargets);
        this.target = target;
    }

    public Player getPlayer() {
        return player;
    }

    public String getLabel() {
        return label;
    }

    public String[] getArgs() {
        return args.clone();
    }

    public List<SpawnTarget> getAvailableTargets() {
        return availableTargets;
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
