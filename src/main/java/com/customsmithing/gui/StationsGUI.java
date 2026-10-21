package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.PlayerData;
import com.customsmithing.unlock.ProgressService;
import com.customsmithing.util.Messages;
import com.customsmithing.util.Text;
import com.customsmithing.util.Theme;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/** Mejora de estaciones: cada nivel da un % extra al puntaje de las piezas que haces en ella. */
public class StationsGUI extends MenuBase {

    private static final int[] SLOTS = {10, 11, 12, 14, 15};

    public StationsGUI(CustomSmithing plugin, Player player) {
        super(plugin, player, 3, "\u2692 Mejoras de Estación");
    }

    private static String label(String station) {
        return switch (station) {
            case "calentador" -> "Calentador";
            case "forja" -> "Forja";
            case "enfriado" -> "Enfriado";
            case "cortapiedras" -> "Cortapiedras";
            case "telar" -> "Telar";
            default -> station;
        };
    }

    private static Material material(String station) {
        return switch (station) {
            case "calentador" -> Material.BLAST_FURNACE;
            case "forja" -> Material.ANVIL;
            case "enfriado" -> Material.CAULDRON;
            case "cortapiedras" -> Material.STONECUTTER;
            default -> Material.LOOM;
        };
    }

    @Override
    protected void render() {
        clear();
        ProgressService progress = plugin.getProgressService();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        List<String> stations = ProgressService.STATIONS;

        for (int i = 0; i < stations.size() && i < SLOTS.length; i++) {
            String station = stations.get(i);
            int level = data.getStationLevel(station);
            boolean max = level >= progress.maxLevel();
            String cost = progress.upgradeCost(level) + "x " + Text.prettify(progress.upgradeMaterial().name());
            button(SLOTS[i], icon("menu.estaciones", material(station), Theme.title(label(station)),
                    Theme.field("Nivel", level + " / " + progress.maxLevel()),
                    Theme.field("Bonus de puntaje", "+" + Text.fmt1(progress.stationBonusPercent(level)) + "%"),
                    " ",
                    max ? Theme.ok("Nivel máximo") : Theme.warn("Clic para mejorar: " + cost)), e -> {
                String key = switch (progress.upgrade(player, station)) {
                    case OK -> "estaciones.mejorada";
                    case MAX_LEVEL -> "estaciones.maximo";
                    case MISSING_ITEMS -> "estaciones.faltan";
                    case DISABLED -> "estaciones.desactivado";
                };
                Messages.send(player, key, "%estacion%", label(station), "%costo%", cost);
                render();
            });
        }
        fillEmpty();
    }
}
