package com.customsmithing.migration;

import com.customsmithing.CustomSmithing;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Al arrancar, convierte los ids antiguos que ya no son válidos (mayúsculas, espacios, tildes...) a ids válidos
 * en recipes/alloys/minerales/jefes/misiones, y actualiza las referencias a recetas (requisitos, planos,
 * misiones y recetas aprendidas de los jugadores). Antes de tocar un archivo hace una copia
 * {@code <archivo>.bak-<fecha>}. Si no hay nada que migrar, no escribe nada.
 */
public final class MigrationService {

    private MigrationService() {
    }

    private record Target(String file, String root) {
    }

    private static final Target RECIPES = new Target("recipes.yml", "recetas");
    private static final Target[] OTHERS = {
            new Target("alloys.yml", "aleaciones"),
            new Target("custom_minerals.yml", "minerales"),
            new Target("bosses.yml", "jefes"),
            new Target("quests.yml", "misiones"),
    };

    public static void run(CustomSmithing plugin) {
        File folder = plugin.getDataFolder();
        try {
            Map<String, String> recipeMap = keyMapping(new File(folder, RECIPES.file()), RECIPES.root());
            if (!recipeMap.isEmpty()) {
                rewrite(plugin, new File(folder, RECIPES.file()), lines -> {
                    List<String> renamed = IdMigration.renameChildKeys(lines, RECIPES.root(), recipeMap);
                    return IdMigration.replaceScalar(renamed, "requiere_receta", recipeMap);
                });
                rewrite(plugin, new File(folder, "blueprints.yml"), lines -> {
                    List<String> a = IdMigration.replaceScalar(lines, "receta", recipeMap);
                    return IdMigration.renameChildKeys(a, "cofres", recipeMap);
                });
                rewrite(plugin, new File(folder, "quests.yml"), lines -> IdMigration.replaceScalar(lines, "receta", recipeMap));
                File[] players = new File(folder, "playerdata").listFiles((dir, name) -> name.endsWith(".yml"));
                if (players != null) {
                    for (File playerFile : players) {
                        rewrite(plugin, playerFile, lines -> IdMigration.replaceListItems(lines, "recetas_aprendidas", recipeMap));
                    }
                }
                for (Map.Entry<String, String> e : recipeMap.entrySet()) {
                    plugin.getLogger().info("Migración: receta '" + e.getKey() + "' -> '" + e.getValue() + "'");
                }
            }
            for (Target target : OTHERS) {
                File file = new File(folder, target.file());
                Map<String, String> map = keyMapping(file, target.root());
                if (map.isEmpty()) continue;
                rewrite(plugin, file, lines -> IdMigration.renameChildKeys(lines, target.root(), map));
                for (Map.Entry<String, String> e : map.entrySet()) {
                    plugin.getLogger().info("Migración en " + target.file() + ": '" + e.getKey() + "' -> '" + e.getValue() + "'");
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo completar la migración de ids (los ids no válidos se omitirán con aviso): " + e.getMessage());
        }
    }

    /** Plan de renombrado de las claves de primer nivel de una sección; vacío si el archivo no existe o todo es válido. */
    private static Map<String, String> keyMapping(File file, String root) {
        if (!file.exists()) return new LinkedHashMap<>();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yml.getConfigurationSection(root);
        if (section == null) return new LinkedHashMap<>();
        return IdMigration.plan(section.getKeys(false));
    }

    /** Reescribe el archivo solo si el contenido cambia, con copia de seguridad previa. */
    private static void rewrite(CustomSmithing plugin, File file, UnaryOperator<List<String>> transform) throws IOException {
        if (!file.exists()) return;
        List<String> original = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        List<String> changed = transform.apply(original);
        if (changed.equals(original)) return;

        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File backup = new File(file.getParentFile(), file.getName() + ".bak-" + stamp);
        Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.write(file.toPath(), changed, StandardCharsets.UTF_8);
        plugin.getLogger().info("Migración: " + file.getName() + " actualizado (copia: " + backup.getName() + ")");
    }
}
