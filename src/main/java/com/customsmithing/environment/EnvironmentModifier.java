package com.customsmithing.environment;

import com.customsmithing.CustomSmithing;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

public class EnvironmentModifier {

    private final CustomSmithing plugin;

    public EnvironmentModifier(CustomSmithing plugin) {
        this.plugin = plugin;
    }

    /**
     * Calcula el modificador total de calidad (puede ser negativo) segun
     * el entorno del jugador en el momento de forjar.
     */
    public double calculateModifier(Location loc) {
        if (!plugin.getConfig().getBoolean("entorno.activar", true)) return 0.0;

        double mod = 0.0;
        World world = loc.getWorld();
        if (world == null) return 0.0;

        // Lluvia
        if (world.hasStorm() && world.getHighestBlockYAt(loc) <= loc.getBlockY() + 2) {
            mod += plugin.getConfig().getDouble("entorno.lluvia_penalizacion", -8);
        }

        // Cerca de lava
        int radius = plugin.getConfig().getInt("entorno.radio_deteccion_lava", 4);
        if (isNearLava(loc, radius)) {
            mod += plugin.getConfig().getDouble("entorno.cerca_de_lava_bonus", 10);
        }

        // Bioma frio
        Biome biome = world.getBiome(loc);
        if (isColdBiome(biome)) {
            mod += plugin.getConfig().getDouble("entorno.bioma_frio_penalizacion", -5);
        }

        // Altura (posibilidad de objetos raros = bonus de calidad)
        int minY = plugin.getConfig().getInt("entorno.altura_rara_y_minimo", 100);
        if (loc.getBlockY() >= minY) {
            mod += plugin.getConfig().getDouble("entorno.altura_rara_bonus", 6);
        }

        // Hora del dia (modificador oculto, favorece objetos magicos de noche)
        long time = world.getTime();
        boolean isNight = time >= 13000 && time <= 23000;
        if (isNight) {
            mod += plugin.getConfig().getDouble("entorno.hora_noche_bonus_magico", 5);
        }

        // Estaciones del año (temperatura estacional)
        if (plugin.getSeasonManager().isActive()) {
            var season = plugin.getSeasonManager().getCurrent(world);
            mod += plugin.getSeasonManager().getModifier(season);
        }

        return mod;
    }

    /** Desglose de cada contribucion, usado por los Lentes del Herrero para mostrar info. */
    public java.util.LinkedHashMap<String, Double> breakdown(Location loc) {
        java.util.LinkedHashMap<String, Double> map = new java.util.LinkedHashMap<>();
        World world = loc.getWorld();
        if (world == null || !plugin.getConfig().getBoolean("entorno.activar", true)) return map;

        double lluvia = (world.hasStorm() && world.getHighestBlockYAt(loc) <= loc.getBlockY() + 2)
                ? plugin.getConfig().getDouble("entorno.lluvia_penalizacion", -8) : 0;
        map.put("Lluvia", lluvia);

        int radius = plugin.getConfig().getInt("entorno.radio_deteccion_lava", 4);
        double lava = isNearLava(loc, radius) ? plugin.getConfig().getDouble("entorno.cerca_de_lava_bonus", 10) : 0;
        map.put("Cerca de lava", lava);

        double bioma = isColdBiome(world.getBiome(loc)) ? plugin.getConfig().getDouble("entorno.bioma_frio_penalizacion", -5) : 0;
        map.put("Bioma frío", bioma);

        int minY = plugin.getConfig().getInt("entorno.altura_rara_y_minimo", 100);
        double altura = loc.getBlockY() >= minY ? plugin.getConfig().getDouble("entorno.altura_rara_bonus", 6) : 0;
        map.put("Altura", altura);

        long time = world.getTime();
        boolean isNight = time >= 13000 && time <= 23000;
        double hora = isNight ? plugin.getConfig().getDouble("entorno.hora_noche_bonus_magico", 5) : 0;
        map.put("Hora (noche)", hora);

        if (plugin.getSeasonManager().isActive()) {
            var season = plugin.getSeasonManager().getCurrent(world);
            map.put("Estación (" + season.getLabel() + ")", plugin.getSeasonManager().getModifier(season));
        }

        return map;
    }

    /** Resultado del último escaneo por bloque, para no recorrer (2r+1)^3 bloques en cada minijuego. */
    private record LavaResult(boolean near, long at) {
    }

    private final java.util.Map<String, LavaResult> lavaCache = new java.util.HashMap<>();

    private boolean isNearLava(Location loc, int radius) {
        World world = loc.getWorld();
        if (world == null) return false;
        long ttl = plugin.getConfig().getLong("entorno.cache_lava_segundos", 5) * 1000L;
        String key = world.getUID() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ() + ":" + radius;
        long now = System.currentTimeMillis();
        LavaResult cached = lavaCache.get(key);
        if (cached != null && now - cached.at() < ttl) return cached.near();

        if (lavaCache.size() > 256) lavaCache.clear(); // tope sencillo: nunca crece sin control
        boolean near = scanLava(world, loc, radius);
        lavaCache.put(key, new LavaResult(near, now));
        return near;
    }

    private boolean scanLava(World world, Location loc, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block b = world.getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                    if (b.getType() == Material.LAVA) return true;
                }
            }
        }
        return false;
    }

    private boolean isColdBiome(Biome biome) {
        // Biome ya no es un enum en versiones nuevas: se usa su clave (p. ej. "snowy_plains").
        String name = biome.getKey().getKey();
        return name.contains("snow") || name.contains("ice") || name.contains("frozen")
                || name.contains("taiga") || name.contains("tundra");
    }
}
