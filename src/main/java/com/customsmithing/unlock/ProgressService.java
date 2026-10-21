package com.customsmithing.unlock;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.PlayerData;
import com.customsmithing.item.ItemRef;
import com.customsmithing.material.MaterialRegistry;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Progreso que mejora la calidad de las piezas: nivel de cada estación (se mejora con items) y maestría por material
 * (cuantas más piezas hagas con un material, mejor sale). Ambos dan un bonus porcentual al puntaje, con tope.
 */
public final class ProgressService {

    public static final List<String> STATIONS = List.of("calentador", "forja", "enfriado", "cortapiedras", "telar");

    public enum UpgradeResult { OK, DISABLED, MAX_LEVEL, MISSING_ITEMS }

    private final CustomSmithing plugin;

    public ProgressService(CustomSmithing plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    // ---------------------------------------------------------------- niveles de estación

    public boolean stationLevelsEnabled() {
        return cfg().getBoolean("estaciones_nivel.activar", true);
    }

    public int maxLevel() {
        return Math.max(0, cfg().getInt("estaciones_nivel.nivel_maximo", 5));
    }

    public double stationBonusPercent(int level) {
        return level * cfg().getDouble("estaciones_nivel.bonus_por_nivel", 3);
    }

    public Material upgradeMaterial() {
        Material mat = Material.matchMaterial(cfg().getString("estaciones_nivel.costo_material", "IRON_INGOT"));
        return mat != null && mat.isItem() && !mat.isAir() ? mat : Material.IRON_INGOT;
    }

    /** Items que cuesta pasar del nivel actual al siguiente. */
    public int upgradeCost(int currentLevel) {
        return Math.max(1, cfg().getInt("estaciones_nivel.costo_base", 8)) * (currentLevel + 1);
    }

    public UpgradeResult upgrade(Player player, String station) {
        if (!stationLevelsEnabled() || !STATIONS.contains(station)) return UpgradeResult.DISABLED;
        PlayerData data = plugin.getPlayerDataManager().get(player);
        int level = data.getStationLevel(station);
        if (level >= maxLevel()) return UpgradeResult.MAX_LEVEL;
        if (!ItemRef.of(upgradeMaterial()).remove(player.getInventory(), upgradeCost(level))) return UpgradeResult.MISSING_ITEMS;
        data.setStationLevel(station, level + 1);
        plugin.getPlayerDataManager().save(player);
        return UpgradeResult.OK;
    }

    // ---------------------------------------------------------------- maestría

    private String materialId(String baseKey) {
        MaterialRegistry.Entry entry = plugin.getMaterialRegistry().forBaseKey(baseKey);
        return entry == null ? null : entry.id();
    }

    public double masteryPercent(PlayerData data, String baseKey) {
        String id = materialId(baseKey);
        if (id == null || !cfg().getBoolean("maestria.activar", true)) return 0;
        return Mastery.bonusPercent(data.getMasteryCount(id),
                cfg().getInt("maestria.piezas_por_punto", 10),
                cfg().getDouble("maestria.porcentaje_por_punto", 1),
                cfg().getDouble("maestria.tope_porcentaje", 10));
    }

    /** Multiplicador de puntaje para una pieza hecha en {@code station} con el material de {@code baseKey}. */
    public double scoreMultiplier(Player player, String station, String baseKey) {
        PlayerData data = plugin.getPlayerDataManager().get(player);
        double stationPercent = stationLevelsEnabled() ? stationBonusPercent(data.getStationLevel(station)) : 0;
        return Mastery.multiplier(stationPercent, masteryPercent(data, baseKey));
    }

    /** Aplica el multiplicador a un puntaje 0-100, sin pasarse de 100. */
    public double applyTo(Player player, String station, String baseKey, double score) {
        return Math.max(0, Math.min(100, score * scoreMultiplier(player, station, baseKey)));
    }

    /** Cuenta una pieza más hecha con ese material (para la maestría). */
    public void recordPiece(Player player, String baseKey) {
        String id = materialId(baseKey);
        if (id != null) plugin.getPlayerDataManager().get(player).addMastery(id);
    }
}
