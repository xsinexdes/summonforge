package com.summonforge.gui;

import com.summonforge.core.SummonForge;
import com.summonforge.util.Items;
import com.summonforge.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** Base de todos los menús. El permiso se vuelve a comprobar en cada clic (MenuManager). */
public abstract class AbstractMenu implements InventoryHolder {
    protected final SummonForge plugin;
    protected final Player viewer;
    private final Inventory inv;
    private final Map<Integer, ClickAction> actions = new HashMap<>();

    protected AbstractMenu(SummonForge plugin, Player viewer, String title, int rows) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.inv = Bukkit.createInventory(this, rows * 9, Text.mm(title));
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    protected abstract void build();

    public String permission() {
        return "summonforge.admin";
    }

    public void open() {
        refresh();
        viewer.openInventory(inv);
    }

    public void refresh() {
        actions.clear();
        inv.clear();
        build();
    }

    protected void set(int slot, ItemStack item, ClickAction action) {
        inv.setItem(slot, item);
        if (action != null) actions.put(slot, action);
    }

    protected ItemStack filler() {
        return Items.icon(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    public void click(int slot, ClickType type) {
        ClickAction a = actions.get(slot);
        if (a != null) a.run(viewer, type);
    }
}
