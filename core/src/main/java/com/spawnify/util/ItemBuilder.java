package com.spawnify.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ItemBuilder {

    private final ItemStack item;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material == null ? Material.BARRIER : material);
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemBuilder name(Component name) {
        ItemMeta meta = ensureMeta();
        meta.displayName(name == null ? Component.empty() : name);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder lore(List<Component> lore) {
        ItemMeta meta = ensureMeta();
        meta.lore(lore == null ? List.of() : new ArrayList<>(lore));
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder hideAllFlags() {
        ItemMeta meta = ensureMeta();
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        return item.clone();
    }

    private ItemMeta ensureMeta() {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            return meta;
        }
        ItemMeta created = Bukkit.getItemFactory().getItemMeta(item.getType());
        if (created == null) {
            throw new IllegalStateException("Cannot create ItemMeta for " + item.getType());
        }
        return created;
    }
}
