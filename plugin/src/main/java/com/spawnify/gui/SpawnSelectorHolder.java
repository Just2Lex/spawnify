
package com.spawnify.gui;

import com.spawnify.model.SpawnTarget;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SpawnSelectorHolder implements InventoryHolder {

    private final UUID playerId;
    private final List<SpawnTarget> targets;
    private final int page;
    private final Map<Integer, SpawnTarget> slotTargets = new HashMap<>();
    private Inventory inventory;

    public SpawnSelectorHolder(UUID playerId, List<SpawnTarget> targets, int page) {
        this.playerId = playerId;
        this.targets = targets;
        this.page = page;
    }

    public UUID playerId() {
        return playerId;
    }

    public List<SpawnTarget> targets() {
        return targets;
    }

    public int page() {
        return page;
    }

    public void setTarget(int slot, SpawnTarget target) {
        slotTargets.put(slot, target);
    }

    public SpawnTarget targetAt(int slot) {
        return slotTargets.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
