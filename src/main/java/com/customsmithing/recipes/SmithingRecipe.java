package com.customsmithing.recipes;

import com.customsmithing.data.ProcessStage;
import com.customsmithing.data.Specialization;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class SmithingRecipe {

    private final String id;
    private Material resultMaterial;
    private String resultName;
    private int requiredLevel = 1;
    private boolean requiresBlueprint = false;
    private final Map<Material, Integer> ingredients = new LinkedHashMap<>();
    private EnumSet<ProcessStage> steps = EnumSet.of(
            ProcessStage.FUNDICION, ProcessStage.CALENTADO, ProcessStage.GOLPEADO,
            ProcessStage.ENFRIADO, ProcessStage.ENSAMBLAJE
    );
    private Specialization specialization = Specialization.HERRAMIENTAS;
    private double difficulty = 1.0;
    private String prerequisiteId = null; // id de la receta que debe fabricarse antes (arbol de recetas)
    private Material metalMaterial = null; // que metal de la Fundicion/Calentado/Golpeado/Enfriado usa esta receta
    private String metalCustomMineralId = null; // O, en vez de un metal vanilla, un mineral 100% custom
    private com.customsmithing.data.MoldType moldShape = null; // si se necesita un molde especifico (pico, hacha, etc)
    private Material woodMaterial = null;  // que material de Talla/Tejido usa esta receta

    public SmithingRecipe(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public Material getResultMaterial() { return resultMaterial; }
    public void setResultMaterial(Material resultMaterial) { this.resultMaterial = resultMaterial; }

    public String getResultName() { return resultName; }
    public void setResultName(String resultName) { this.resultName = resultName; }

    public int getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(int requiredLevel) { this.requiredLevel = requiredLevel; }

    public boolean isRequiresBlueprint() { return requiresBlueprint; }
    public void setRequiresBlueprint(boolean requiresBlueprint) { this.requiresBlueprint = requiresBlueprint; }

    public Map<Material, Integer> getIngredients() { return ingredients; }

    public EnumSet<ProcessStage> getSteps() { return steps; }
    public void setSteps(EnumSet<ProcessStage> steps) { this.steps = steps; }

    public boolean requiresStep(ProcessStage step) { return steps.contains(step); }

    /** Clave unificada del "metal" de esta receta: vanilla (MAT:X) o mineral custom (MIN:x),
     *  envuelta en el molde (MLD:FORMA:...) si esta receta requiere una forma especifica
     *  (pico, hacha, peto, etc). Funciona con CUALQUIER material vanilla o custom. */
    public String getMetalBaseKey() {
        String inner;
        if (metalCustomMineralId != null) inner = "MIN:" + metalCustomMineralId;
        else if (metalMaterial != null) inner = "MAT:" + metalMaterial.name();
        else return null;

        if (moldShape != null) return "MLD:" + moldShape.name() + ":" + inner;
        return inner;
    }

    public Specialization getSpecialization() { return specialization; }
    public void setSpecialization(Specialization specialization) { this.specialization = specialization; }

    public double getDifficulty() { return difficulty; }
    public void setDifficulty(double difficulty) { this.difficulty = difficulty; }

    public String getPrerequisiteId() { return prerequisiteId; }
    public void setPrerequisiteId(String prerequisiteId) { this.prerequisiteId = prerequisiteId; }

    public Material getMetalMaterial() { return metalMaterial; }
    public void setMetalMaterial(Material metalMaterial) { this.metalMaterial = metalMaterial; }

    public String getMetalCustomMineralId() { return metalCustomMineralId; }
    public void setMetalCustomMineralId(String metalCustomMineralId) { this.metalCustomMineralId = metalCustomMineralId; }

    public com.customsmithing.data.MoldType getMoldShape() { return moldShape; }
    public void setMoldShape(com.customsmithing.data.MoldType moldShape) { this.moldShape = moldShape; }

    public Material getWoodMaterial() { return woodMaterial; }
    public void setWoodMaterial(Material woodMaterial) { this.woodMaterial = woodMaterial; }
}
