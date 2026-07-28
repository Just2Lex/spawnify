package com.spawnify.listener;

import com.spawnify.gui.SpawnSelectorGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class SpawnGuiListener implements Listener {

    private final SpawnSelectorGui gui;

    public SpawnGuiListener(SpawnSelectorGui gui) {
        this.gui = gui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        gui.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        gui.handleDrag(event);
    }
}
