package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.Season;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SeasonConfigGUI implements SmithingGUIHolder {

    private final CustomSmithing plugin;
    private final Player player;
    private final Inventory inventory;

    private static final int SLOT_PRIMAVERA = 10;
    private static final int SLOT_VERANO = 12;
    private static final int SLOT_OTONO = 14;
    private static final int SLOT_INVIERNO = 16;
    private static final int SLOT_DURACION = 4;
    private static final int SLOT_FORZAR = 22;
    private static final int SLOT_ACTIVAR = 31;

    public SeasonConfigGUI(CustomSmithing plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 36, "\u2744 Estaciones del Año (Admin)");
        render();
    }

    private void render() {
        inventory.clear();
        var seasons = plugin.getSeasonManager();

        inventory.setItem(SLOT_PRIMAVERA, seasonItem(Material.CHERRY_SAPLING, Season.PRIMAVERA));
        inventory.setItem(SLOT_VERANO, seasonItem(Material.SUNFLOWER, Season.VERANO));
        inventory.setItem(SLOT_OTONO, seasonItem(Material.SWEET_BERRIES, Season.OTONO));
        inventory.setItem(SLOT_INVIERNO, seasonItem(Material.SNOWBALL, Season.INVIERNO));

        inventory.setItem(SLOT_DURACION, labeled(Material.CLOCK,
                "\u00a7bDuración por estación: \u00a7f" + seasons.getDuracionDias() + " días de Minecraft",
                "\u00a77Clic izq: +1 día  |  Clic der: -1 día"));

        Season forced = seasons.getForcedSeason();
        inventory.setItem(SLOT_FORZAR, labeled(Material.COMPASS,
                "\u00a7dEstación forzada: \u00a7f" + (forced == null ? "Automática (ciclo normal)" : forced.getIcon() + " " + forced.getLabel()),
                "\u00a77Clic para cambiar (cicla entre todas + automática)"));

        boolean active = seasons.isActive();
        inventory.setItem(SLOT_ACTIVAR, labeled(active ? Material.LIME_DYE : Material.GRAY_DYE,
                "\u00a7eSistema de estaciones: " + (active ? "\u00a7aACTIVADO" : "\u00a7cDESACTIVADO"),
                "\u00a77Clic para alternar"));
    }

    private ItemStack seasonItem(Material mat, Season season) {
        double value = plugin.getSeasonManager().getModifier(season);
        return labeled(mat,
                season.getIcon() + " \u00a7f" + season.getLabel() + ": \u00a7e" + (value >= 0 ? "+" : "") + value,
                "\u00a77Modificador de calidad al forjar",
                "\u00a77Clic izq: +1  |  Clic der: -1  |  Shift: x5");
    }

    private ItemStack labeled(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        var seasons = plugin.getSeasonManager();
        boolean shift = event.getClick().isShiftClick();
        double step = shift ? 5.0 : 1.0;
        double delta = event.getClick().isRightClick() ? -step : step;

        Season target = switch (slot) {
            case SLOT_PRIMAVERA -> Season.PRIMAVERA;
            case SLOT_VERANO -> Season.VERANO;
            case SLOT_OTONO -> Season.OTONO;
            case SLOT_INVIERNO -> Season.INVIERNO;
            default -> null;
        };
        if (target != null) {
            seasons.setModifier(target, seasons.getModifier(target) + delta);
            render();
            return;
        }

        if (slot == SLOT_DURACION) {
            int dDelta = event.getClick() == ClickType.RIGHT ? -1 : 1;
            seasons.setDuracionDias(seasons.getDuracionDias() + dDelta);
            render();
            return;
        }

        if (slot == SLOT_FORZAR) {
            Season current = seasons.getForcedSeason();
            Season next = current == null ? Season.PRIMAVERA : (current.ordinal() == Season.values().length - 1 ? null : current.next());
            seasons.setForcedSeason(next);
            render();
            return;
        }

        if (slot == SLOT_ACTIVAR) {
            boolean active = seasons.isActive();
            plugin.getConfig().set("estaciones_clima.activar", !active);
            plugin.saveConfig();
            render();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
