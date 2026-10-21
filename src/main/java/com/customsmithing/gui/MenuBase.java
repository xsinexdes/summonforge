package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.util.Theme;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base de los menús nuevos: botones con acción asociada, relleno, iconos con skin
 * (con respaldo vanilla) y sonido de clic. Los menús antiguos se migran aquí cuando se reescriben.
 */
public abstract class MenuBase implements SmithingGUIHolder {

    protected final CustomSmithing plugin;
    protected final Player player;
    protected final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected MenuBase(CustomSmithing plugin, Player player, int rows, String title) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    /** Dibuja el contenido: debe empezar con {@link #clear()}. Se llama al abrir y al refrescar. */
    protected abstract void render();

    @Override
    public void open(Player target) {
        render();
        target.openInventory(inventory);
    }

    protected final void clear() {
        inventory.clear();
        actions.clear();
    }

    protected final void set(int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) inventory.setItem(slot, item);
    }

    protected final void button(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        inventory.setItem(slot, item);
        actions.put(slot, action);
    }

    /** Rellena con panel vacío los slots que sigan libres. */
    protected final void fillEmpty() {
        ItemStack filler = icon("nav.relleno", Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) inventory.setItem(i, filler);
        }
    }

    /** Icono con skin (o respaldo vanilla), nombre y lore. Oculta atributos para que no ensucien el tooltip. */
    protected final ItemStack icon(String skinId, Material fallback, String name, String... lore) {
        ItemStack item = plugin.getSkinService().icon(skinId, fallback);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(List.of(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    protected final String muted(String text) {
        return Theme.muted(text);
    }

    @Override
    public final void handleClick(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> action = actions.get(event.getRawSlot());
        if (action == null) return;
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
        action.accept(event);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
