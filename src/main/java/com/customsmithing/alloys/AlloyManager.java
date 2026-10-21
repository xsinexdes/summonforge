package com.customsmithing.alloys;

import com.customsmithing.CustomSmithing;
import com.customsmithing.item.ItemRef;
import com.customsmithing.util.IdValidator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aleaciones en alloys.yml. Formato nuevo: {@code entrada}/{@code salida} (material + modelo + nombre + etiqueta).
 * Se sigue leyendo el formato antiguo {@code material_entrada}/{@code material_salida} (solo el nombre del material);
 * al guardar una aleación se escribe el formato nuevo.
 */
public class AlloyManager {

    private final CustomSmithing plugin;
    private final File file;
    private YamlConfiguration yml;
    private final Map<String, AlloyRecipe> recipes = new LinkedHashMap<>();

    public AlloyManager(CustomSmithing plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "alloys.yml");
        load();
    }

    public void load() {
        if (!file.exists()) plugin.saveResource("alloys.yml", false);
        yml = YamlConfiguration.loadConfiguration(file);
        recipes.clear();

        ConfigurationSection root = yml.getConfigurationSection("aleaciones");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            String idProblem = IdValidator.problem(id);
            if (idProblem != null) {
                skip(id, null, "id no válido (" + idProblem + ")");
                continue;
            }
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) {
                skip(id, null, "no es una sección");
                continue;
            }
            ItemRef in;
            ItemRef out;
            try {
                in = readRef(sec, "entrada", "material_entrada");
            } catch (IllegalArgumentException e) {
                skip(id, "entrada", e.getMessage());
                continue;
            }
            try {
                out = readRef(sec, "salida", "material_salida");
            } catch (IllegalArgumentException e) {
                skip(id, "salida", e.getMessage());
                continue;
            }

            AlloyRecipe recipe = new AlloyRecipe(id);
            recipe.setDisplayName(sec.getString("nombre", id));
            recipe.setInput(in);
            recipe.setInputAmount(sec.getInt("cantidad_entrada", 1));
            recipe.setOutput(out);
            recipe.setOutputAmount(sec.getInt("cantidad_salida", 1));
            recipes.put(id, recipe);
        }
    }

    /** Lee el formato nuevo, o el antiguo (texto con el nombre del material), o falla con un motivo legible. */
    private static ItemRef readRef(ConfigurationSection sec, String newKey, String legacyKey) {
        if (sec.contains(newKey)) return ItemRef.load(sec, newKey);
        if (sec.contains(legacyKey)) return ItemRef.load(sec, legacyKey);
        throw new IllegalArgumentException("falta '" + newKey + "'");
    }

    private void skip(String id, String key, String reason) {
        plugin.getLogger().warning("alloys.yml › aleaciones." + id + (key == null ? "" : "." + key)
                + ": " + reason + " — aleación omitida.");
    }

    public void save(AlloyRecipe recipe) {
        if (!IdValidator.isValid(recipe.getId())) {
            throw new IllegalArgumentException("id de aleación no válido: " + recipe.getId());
        }
        if (recipe.getInput() == null || recipe.getOutput() == null) {
            throw new IllegalArgumentException("la aleación " + recipe.getId() + " necesita entrada y salida");
        }
        String base = "aleaciones." + recipe.getId();
        yml.set(base, null); // no dejar claves viejas
        yml.set(base + ".nombre", recipe.getDisplayName());
        recipe.getInput().save(yml.createSection(base + ".entrada"));
        yml.set(base + ".cantidad_entrada", recipe.getInputAmount());
        recipe.getOutput().save(yml.createSection(base + ".salida"));
        yml.set(base + ".cantidad_salida", recipe.getOutputAmount());
        recipes.put(recipe.getId(), recipe);
        saveToDisk();
    }

    public void delete(String id) {
        yml.set("aleaciones." + id, null);
        recipes.remove(id);
        saveToDisk();
    }

    private void saveToDisk() {
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar alloys.yml: " + e.getMessage());
        }
    }

    public AlloyRecipe get(String id) { return recipes.get(id); }
    public boolean exists(String id) { return recipes.containsKey(id); }
    public Collection<AlloyRecipe> getAll() { return recipes.values(); }

    /** Aleaciones cuyo item de entrada coincide EXACTAMENTE con el que tienes en la mano. */
    public List<AlloyRecipe> getByInput(ItemStack held) {
        List<AlloyRecipe> list = new ArrayList<>();
        for (AlloyRecipe r : recipes.values()) {
            if (r.getInput().matches(held)) list.add(r);
        }
        return list;
    }
}
