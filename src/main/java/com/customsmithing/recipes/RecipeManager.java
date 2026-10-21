package com.customsmithing.recipes;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.MoldType;
import com.customsmithing.data.ProcessStage;
import com.customsmithing.data.Specialization;
import com.customsmithing.util.IdValidator;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecipeManager {

    private final CustomSmithing plugin;
    private final File file;
    private YamlConfiguration yml;
    private final Map<String, SmithingRecipe> recipes = new LinkedHashMap<>();

    public RecipeManager(CustomSmithing plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "recipes.yml");
        load();
    }

    /** Aviso con archivo + clave + motivo. Un dato malo nunca rompe el arranque: se salta la receta. */
    private void skip(String id, String key, String reason) {
        plugin.getLogger().warning("recipes.yml › recetas." + id + (key == null ? "" : "." + key)
                + ": " + reason + " — receta omitida.");
    }

    /** Convierte un texto en un Material que sirva como item; null si no existe o no es un item. */
    private static Material parseItem(String name) {
        if (name == null) return null;
        Material mat = Material.matchMaterial(name.trim());
        return (mat == null || !mat.isItem() || mat.isAir()) ? null : mat;
    }

    public void load() {
        if (!file.exists()) plugin.saveResource("recipes.yml", false);
        yml = YamlConfiguration.loadConfiguration(file);
        recipes.clear();

        ConfigurationSection root = yml.getConfigurationSection("recetas");
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
            SmithingRecipe recipe = parseRecipe(id, sec);
            if (recipe != null) recipes.put(id, recipe);
        }
    }

    private SmithingRecipe parseRecipe(String id, ConfigurationSection sec) {
        SmithingRecipe recipe = new SmithingRecipe(id);

        Material result = parseItem(sec.getString("resultado_material"));
        if (result == null) {
            skip(id, "resultado_material", "'" + sec.getString("resultado_material") + "' no es un item válido");
            return null;
        }
        recipe.setResultMaterial(result);
        recipe.setResultName(sec.getString("resultado_nombre", id));
        recipe.setRequiredLevel(Math.max(1, sec.getInt("nivel_requerido", 1)));
        recipe.setRequiresBlueprint(sec.getBoolean("requiere_plano", false));

        double difficulty = sec.getDouble("dificultad", 1.0);
        if (difficulty < 0.5 || difficulty > 3.0) {
            plugin.getLogger().warning("recipes.yml › recetas." + id + ".dificultad: " + difficulty
                    + " fuera de 0.5-3.0, se ajusta al límite.");
            difficulty = Math.max(0.5, Math.min(3.0, difficulty));
        }
        recipe.setDifficulty(difficulty);

        ConfigurationSection ingSec = sec.getConfigurationSection("ingredientes");
        if (ingSec != null) {
            for (String matName : ingSec.getKeys(false)) {
                Material mat = parseItem(matName);
                if (mat == null) {
                    skip(id, "ingredientes." + matName, "no es un item válido");
                    return null;
                }
                int amount = ingSec.getInt(matName, 0);
                if (amount <= 0) {
                    skip(id, "ingredientes." + matName, "la cantidad debe ser mayor que 0");
                    return null;
                }
                recipe.getIngredients().put(mat, amount);
            }
        }

        List<String> stepNames = sec.getStringList("procesos");
        if (!stepNames.isEmpty()) {
            EnumSet<ProcessStage> steps = EnumSet.noneOf(ProcessStage.class);
            for (String name : stepNames) {
                ProcessStage stage = ProcessStage.fromName(name);
                if (stage == null) {
                    skip(id, "procesos", "etapa desconocida '" + name + "'");
                    return null;
                }
                steps.add(stage);
            }
            recipe.setSteps(steps);
        }

        String specName = sec.getString("especializacion", "HERRAMIENTAS");
        try {
            recipe.setSpecialization(Specialization.valueOf(specName));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("recipes.yml › recetas." + id + ".especializacion: '" + specName
                    + "' no existe, se usa HERRAMIENTAS.");
        }

        recipe.setPrerequisiteId(sec.getString("requiere_receta", null));

        String metalName = sec.getString("metal_procesado", null);
        if (metalName != null) {
            Material metal = parseItem(metalName);
            if (metal == null) {
                skip(id, "metal_procesado", "'" + metalName + "' no es un item válido");
                return null;
            }
            recipe.setMetalMaterial(metal);
        }
        recipe.setMetalCustomMineralId(sec.getString("mineral_procesado", null));

        String moldName = sec.getString("molde_forma", null);
        if (moldName != null) {
            try {
                recipe.setMoldShape(MoldType.valueOf(moldName));
            } catch (IllegalArgumentException e) {
                skip(id, "molde_forma", "molde desconocido '" + moldName + "'");
                return null;
            }
        }

        String woodName = sec.getString("madera_procesada", null);
        if (woodName != null) {
            Material wood = parseItem(woodName);
            if (wood == null) {
                skip(id, "madera_procesada", "'" + woodName + "' no es un item válido");
                return null;
            }
            recipe.setWoodMaterial(wood);
        }
        return recipe;
    }

    public void save(SmithingRecipe recipe) {
        if (!IdValidator.isValid(recipe.getId())) {
            throw new IllegalArgumentException("id de receta no válido: " + recipe.getId());
        }
        if (recipe.getResultMaterial() == null) {
            throw new IllegalArgumentException("la receta " + recipe.getId() + " no tiene resultado");
        }
        String base = "recetas." + recipe.getId() + ".";
        // Se borra la sección completa antes de escribir: lo que el admin quitó
        // (ingredientes, metal, molde...) no puede reaparecer al recargar.
        yml.set("recetas." + recipe.getId(), null);

        yml.set(base + "resultado_material", recipe.getResultMaterial().name());
        yml.set(base + "resultado_nombre", recipe.getResultName());
        yml.set(base + "nivel_requerido", recipe.getRequiredLevel());
        yml.set(base + "requiere_plano", recipe.isRequiresBlueprint());
        yml.set(base + "dificultad", recipe.getDifficulty());
        yml.set(base + "especializacion", recipe.getSpecialization().name());
        yml.set(base + "requiere_receta", recipe.getPrerequisiteId());
        yml.set(base + "metal_procesado", recipe.getMetalMaterial() == null ? null : recipe.getMetalMaterial().name());
        yml.set(base + "mineral_procesado", recipe.getMetalCustomMineralId());
        yml.set(base + "molde_forma", recipe.getMoldShape() == null ? null : recipe.getMoldShape().name());
        yml.set(base + "madera_procesada", recipe.getWoodMaterial() == null ? null : recipe.getWoodMaterial().name());

        List<String> steps = new ArrayList<>();
        for (ProcessStage s : recipe.getSteps()) steps.add(s.name());
        yml.set(base + "procesos", steps);

        for (Map.Entry<Material, Integer> e : recipe.getIngredients().entrySet()) {
            yml.set(base + "ingredientes." + e.getKey().name(), e.getValue());
        }

        recipes.put(recipe.getId(), recipe);
        saveToDisk();
    }

    public void delete(String id) {
        yml.set("recetas." + id, null);
        recipes.remove(id);
        saveToDisk();
    }

    private void saveToDisk() {
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar recipes.yml: " + e.getMessage());
        }
    }

    public SmithingRecipe get(String id) { return recipes.get(id); }
    public boolean exists(String id) { return recipes.containsKey(id); }
    public Collection<SmithingRecipe> getAll() { return recipes.values(); }

    public List<SmithingRecipe> getByResultMaterial(Material material) {
        List<SmithingRecipe> list = new ArrayList<>();
        for (SmithingRecipe r : recipes.values()) {
            if (r.getResultMaterial() == material) list.add(r);
        }
        return list;
    }
}
