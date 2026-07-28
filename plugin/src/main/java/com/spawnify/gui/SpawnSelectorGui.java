
package com.spawnify.gui;

import com.spawnify.SpawnifyPlugin;
import com.spawnify.config.Messages;
import com.spawnify.config.PluginConfig;
import com.spawnify.model.SpawnReason;
import com.spawnify.model.SpawnTarget;
import com.spawnify.service.SpawnService;
import com.spawnify.util.ComponentUtil;
import com.spawnify.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SpawnSelectorGui {

    private final SpawnifyPlugin plugin;
    private final PluginConfig config;
    private final Messages messages;
    private final SpawnService spawnService;

    public SpawnSelectorGui(SpawnifyPlugin plugin, PluginConfig config, Messages messages, SpawnService spawnService) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.spawnService = spawnService;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        List<SpawnTarget> targets = spawnService.availableTargets(player);
        if (targets.isEmpty()) {
            player.sendMessage(spawnService.message("spawn-available-none", Map.of()));
            return;
        }

        int contentSlots = Math.max(9, config.guiRows() * 9 - 9);
        int maxPages = Math.max(1, (int) Math.ceil(targets.size() / (double) contentSlots));
        int currentPage = Math.max(0, Math.min(page, maxPages - 1));
        int start = currentPage * contentSlots;
        int end = Math.min(targets.size(), start + contentSlots);
        List<SpawnTarget> pageTargets = new ArrayList<>(targets.subList(start, end));

        SpawnSelectorHolder holder = new SpawnSelectorHolder(player.getUniqueId(), targets, currentPage);
        Inventory inventory = Bukkit.createInventory(holder, config.guiRows() * 9, ComponentUtil.legacy(config.guiTitle(), Map.of(
                "%page%", String.valueOf(currentPage + 1),
                "%pages%", String.valueOf(maxPages),
                "%count%", String.valueOf(targets.size())
        )));
        holder.setInventory(inventory);

        if (config.fillerEnabled()) {
            ItemStack filler = new ItemBuilder(config.fillerMaterial())
                    .name(Component.empty())
                    .build();
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, filler);
            }
        }

        int slotCursor = 0;
        Optional<SpawnTarget> selected = spawnService.getSelectedTarget(player);
        for (SpawnTarget target : pageTargets) {
            int actualSlot = target.slot() >= 0 && target.slot() < inventory.getSize() - 9
                    ? target.slot()
                    : findNextFree(inventory, slotCursor);
            if (actualSlot == -1) {
                continue;
            }
            slotCursor = actualSlot + 1;
            inventory.setItem(actualSlot, createItem(target, selected.orElse(null)));
            holder.setTarget(actualSlot, target);
        }

        int bottomRow = inventory.getSize() - 9;
        inventory.setItem(bottomRow + 3, createNavItem(Material.ARROW, messages.raw("gui-prev")));
        inventory.setItem(bottomRow + 4, createNavItem(Material.BARRIER, messages.raw("gui-close")));
        inventory.setItem(bottomRow + 5, createNavItem(Material.ARROW, messages.raw("gui-next")));

        player.openInventory(inventory);
    }

    private int findNextFree(Inventory inventory, int from) {
        for (int i = from; i < inventory.getSize() - 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType().isAir() || stack.getType() == config.fillerMaterial()) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack createNavItem(Material material, String rawName) {
        return new ItemBuilder(material)
                .name(ComponentUtil.legacy(rawName, Map.of()))
                .build();
    }

    private ItemStack createItem(SpawnTarget target, SpawnTarget selected) {
        List<Component> lore = new ArrayList<>(messages.components("spawn-item-lore", Map.of(
                "%world%", target.worldName(),
                "%type%", target.type().name(),
                "%permission%", target.permission() == null || target.permission().isBlank() ? "none" : target.permission()
        )));

        if (selected != null && sameTarget(selected, target)) {
            lore.add(Component.empty());
            lore.add(ComponentUtil.legacy(messages.raw("spawn-item-selected"), Map.of()));
        }

        Material icon = target.iconMaterialOrDefault(Material.ENDER_PEARL);
        return new ItemBuilder(icon)
                .name(ComponentUtil.legacy(target.displayName(), Map.of()))
                .lore(lore)
                .build();
    }

    private boolean sameTarget(SpawnTarget a, SpawnTarget b) {
        return a != null && b != null
                && a.type() == b.type()
                && a.id().equalsIgnoreCase(b.id())
                && a.worldName().equalsIgnoreCase(b.worldName());
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SpawnSelectorHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= topSize) {
            return;
        }

        int bottomRow = topSize - 9;
        if (slot == bottomRow + 4) {
            player.closeInventory();
            return;
        }
        if (slot == bottomRow + 3) {
            open(player, Math.max(0, holder.page() - 1));
            return;
        }
        if (slot == bottomRow + 5) {
            open(player, holder.page() + 1);
            return;
        }

        SpawnTarget target = holder.targetAt(slot);
        if (target == null) {
            return;
        }

        if (target.permission() != null && !target.permission().isBlank() && !player.hasPermission(target.permission())) {
            player.sendMessage(spawnService.message("no-permission", Map.of()));
            return;
        }

        spawnService.rememberSelectedTarget(player, target);
        player.closeInventory();
        spawnService.requestTeleport(player, target, SpawnReason.GUI);
    }

    public void handleDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof SpawnSelectorHolder) {
            event.setCancelled(true);
        }
    }
}
