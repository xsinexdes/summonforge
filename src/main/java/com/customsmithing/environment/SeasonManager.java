package com.customsmithing.environment;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.Season;
import org.bukkit.World;

public class SeasonManager {

    private final CustomSmithing plugin;
    private Season forcedSeason = null; // null = ciclo automático

    public SeasonManager(CustomSmithing plugin) {
        this.plugin = plugin;
        loadFromConfig();
    }

    public void loadFromConfig() {
        String forced = plugin.getConfig().getString("estaciones_clima.forzar", "NINGUNA");
        if (forced == null || forced.equalsIgnoreCase("NINGUNA")) {
            forcedSeason = null;
        } else {
            try {
                forcedSeason = Season.valueOf(forced.toUpperCase());
            } catch (IllegalArgumentException e) {
                forcedSeason = null;
            }
        }
    }

    public Season getForcedSeason() { return forcedSeason; }

    public void setForcedSeason(Season season) {
        this.forcedSeason = season;
        plugin.getConfig().set("estaciones_clima.forzar", season == null ? "NINGUNA" : season.name());
        plugin.saveConfig();
    }

    /** Calcula la estacion actual segun el tiempo total del mundo (o la forzada por un admin). */
    public Season getCurrent(World world) {
        if (forcedSeason != null) return forcedSeason;
        if (world == null) return Season.PRIMAVERA;

        int diasPorEstacion = Math.max(1, plugin.getConfig().getInt("estaciones_clima.duracion_dias", 7));
        long ticksPorEstacion = diasPorEstacion * 24000L;
        long fullTime = world.getFullTime();
        int index = (int) ((fullTime / ticksPorEstacion) % Season.values().length);
        return Season.values()[index];
    }

    public double getModifier(Season season) {
        return plugin.getConfig().getDouble("estaciones_clima.modificador_" + season.getConfigKey(), 0.0);
    }

    public void setModifier(Season season, double value) {
        plugin.getConfig().set("estaciones_clima.modificador_" + season.getConfigKey(), value);
        plugin.saveConfig();
    }

    public int getDuracionDias() {
        return plugin.getConfig().getInt("estaciones_clima.duracion_dias", 7);
    }

    public void setDuracionDias(int dias) {
        plugin.getConfig().set("estaciones_clima.duracion_dias", Math.max(1, dias));
        plugin.saveConfig();
    }

    public boolean isActive() {
        return plugin.getConfig().getBoolean("estaciones_clima.activar", true);
    }
}
