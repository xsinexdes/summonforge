package com.customsmithing.data;

import com.customsmithing.CustomSmithing;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final CustomSmithing plugin;
    private final Map<UUID, PlayerData> cache = new HashMap<>();
    private final File folder;

    public PlayerDataManager(CustomSmithing plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("No se pudo crear la carpeta playerdata.");
        }
    }

    public PlayerData get(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), this::load);
    }

    public void unload(Player player) {
        PlayerData data = cache.remove(player.getUniqueId());
        if (data != null) save(data);
    }

    /** Guarda a un jugador ahora mismo (p. ej. justo después de desbloquear una receta). */
    public void save(Player player) {
        PlayerData data = cache.get(player.getUniqueId());
        if (data != null) save(data);
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) save(data);
    }

    private PlayerData load(UUID uuid) {
        File file = new File(folder, uuid + ".yml");
        PlayerData data = new PlayerData(uuid);
        if (!file.exists()) return data;

        YamlConfiguration yml = new YamlConfiguration();
        try {
            yml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            // Un archivo ilegible NO se sobrescribe con datos vacíos: se aparta y se avisa.
            File quarantine = new File(folder, uuid + ".corrupto-" + System.currentTimeMillis() + ".yml");
            boolean moved = file.renameTo(quarantine);
            plugin.getLogger().severe("playerdata de " + uuid + " ilegible (" + e.getMessage() + "). "
                    + (moved ? "Se guardó una copia en " + quarantine.getName() : "No se pudo apartar el archivo")
                    + " y se empieza con datos nuevos.");
            return data;
        }

        data.setLevel(Math.max(1, yml.getInt("nivel", 1)));
        data.setXp(Math.max(0, yml.getInt("xp", 0)));
        String spec = yml.getString("especializacion", null);
        if (spec != null) {
            try {
                data.setSpecialization(Specialization.valueOf(spec));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("playerdata " + uuid + ": especialización '" + spec + "' no existe, se ignora.");
            }
        }
        data.getLearnedRecipes().addAll(yml.getStringList("recetas_aprendidas"));
        data.setSpentPoints(yml.getInt("puntos_gastados", 0));
        data.setSpecChangedAt(yml.getLong("especializacion_cambiada", 0));
        var stations = yml.getConfigurationSection("niveles_estacion");
        if (stations != null) for (String key : stations.getKeys(false)) data.setStationLevel(key, stations.getInt(key));
        var mastery = yml.getConfigurationSection("maestria");
        if (mastery != null) {
            for (String key : mastery.getKeys(false)) data.getMastery().put(key, Math.max(0, mastery.getInt(key)));
        }
        data.setTotalCrafts(yml.getInt("fabricaciones", 0));
        data.setTotalHits(yml.getInt("golpes_totales", 0));
        data.setAvgTimingAccuracy(yml.getDouble("precision_media", 50.0));
        return data;
    }

    private void save(PlayerData data) {
        File file = new File(folder, data.getUuid() + ".yml");
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("nivel", data.getLevel());
        yml.set("xp", data.getXp());
        yml.set("especializacion", data.getSpecialization() == null ? null : data.getSpecialization().name());
        yml.set("recetas_aprendidas", new java.util.ArrayList<>(data.getLearnedRecipes()));
        yml.set("puntos_gastados", data.getSpentPoints());
        yml.set("especializacion_cambiada", data.getSpecChangedAt());
        for (var e : data.getStationLevels().entrySet()) yml.set("niveles_estacion." + e.getKey(), e.getValue());
        for (var e : data.getMastery().entrySet()) yml.set("maestria." + e.getKey(), e.getValue());
        yml.set("fabricaciones", data.getTotalCrafts());
        yml.set("golpes_totales", data.getTotalHits());
        yml.set("precision_media", data.getAvgTimingAccuracy());
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar playerdata de " + data.getUuid() + ": " + e.getMessage());
        }
    }
}
