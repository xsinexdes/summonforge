package com.customsmithing.admin;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.ProcessStage;
import com.customsmithing.item.ItemRef;
import com.customsmithing.item.PieceData;
import com.customsmithing.material.MaterialRegistry;
import com.customsmithing.recipes.RecipeTree;
import com.customsmithing.recipes.SmithingRecipe;
import com.customsmithing.util.BaseKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprobaciones para administradores, sin abrir ninguna GUI:
 *  - {@link #validate()}: coherencia de los datos (recetas, árbol, materiales).
 *  - {@link #selftest()}: recorre el circuito de cada material registrado (item -> clave -> etapas -> pieza).
 * Devuelven líneas ya coloreadas; la última es el resumen.
 */
public final class Diagnostics {

    private final CustomSmithing plugin;

    public Diagnostics(CustomSmithing plugin) {
        this.plugin = plugin;
    }

    private static final class Report {
        final List<String> lines = new ArrayList<>();
        int failures = 0;
        int warnings = 0;

        void fail(String text) { failures++; lines.add("\u00a7c\u2716 " + text); }
        void warn(String text) { warnings++; lines.add("\u00a7e! " + text); }
        void ok(String text) { lines.add("\u00a7a\u2714 " + text); }

        List<String> finish(String title) {
            lines.add((failures == 0 ? "\u00a7a" : "\u00a7c") + title + ": " + failures + " error(es), " + warnings + " aviso(s).");
            return lines;
        }
    }

    // ---------------------------------------------------------------- validate

    public List<String> validate() {
        Report r = new Report();

        Map<String, String> parents = new LinkedHashMap<>();
        for (SmithingRecipe recipe : plugin.getRecipeManager().getAll()) {
            parents.put(recipe.getId(), recipe.getPrerequisiteId());
        }
        List<String> cycle = RecipeTree.findCycle(parents);
        if (cycle.isEmpty()) r.ok("Árbol de recetas sin ciclos (" + parents.size() + " recetas).");
        else r.fail("Ciclo en el árbol de recetas: " + String.join(" -> ", cycle));
        for (String id : RecipeTree.missingParents(parents)) {
            r.fail("La receta '" + id + "' requiere '" + parents.get(id) + "', que no existe.");
        }

        MaterialRegistry registry = plugin.getMaterialRegistry();
        for (SmithingRecipe recipe : plugin.getRecipeManager().getAll()) {
            boolean metal = recipe.requiresStep(ProcessStage.GOLPEADO) || recipe.requiresStep(ProcessStage.ENFRIADO);
            if (metal) {
                if (recipe.getMetalBaseKey() == null) {
                    r.fail("Receta '" + recipe.getId() + "': usa golpeado/enfriado pero no tiene metal asignado.");
                } else if (registry.forBaseKey(recipe.getMetalBaseKey()) == null) {
                    r.fail("Receta '" + recipe.getId() + "': su metal " + recipe.getMetalBaseKey() + " no está en materiales.yml.");
                }
            }
            boolean carve = recipe.requiresStep(ProcessStage.TALLADO) || recipe.requiresStep(ProcessStage.TEJIDO);
            if (carve) {
                if (recipe.getWoodMaterial() == null) {
                    r.fail("Receta '" + recipe.getId() + "': usa tallado/tejido pero no tiene madera/tela asignada.");
                } else {
                    MaterialRegistry.Entry entry = registry.forBaseKey(BaseKey.ofMaterial(recipe.getWoodMaterial()));
                    ProcessStage needed = recipe.requiresStep(ProcessStage.TEJIDO) ? ProcessStage.TEJIDO : ProcessStage.TALLADO;
                    if (entry == null) r.fail("Receta '" + recipe.getId() + "': " + recipe.getWoodMaterial() + " no está en materiales.yml.");
                    else if (!entry.kind().allows(needed)) r.fail("Receta '" + recipe.getId() + "': " + recipe.getWoodMaterial() + " es " + entry.kind() + " y no admite " + needed + ".");
                }
            }
            if (recipe.getIngredients().isEmpty() && !metal && !carve) {
                r.warn("Receta '" + recipe.getId() + "' no consume nada: se podría fabricar gratis.");
            }
        }

        // Misiones, jefes, aleaciones y piezas
        for (var quest : plugin.getQuestManager().getAll()) {
            String target = quest.getTargetRecipeId();
            if (target != null && !plugin.getRecipeManager().exists(target)) {
                r.fail("Misión '" + quest.getId() + "': la receta objetivo '" + target + "' no existe.");
            }
        }
        for (var boss : plugin.getBossManager().getAll()) {
            String structure = boss.getStructureId();
            if (structure != null && !structure.isBlank() && !plugin.getStructureManager().exists(structure)) {
                r.warn("Jefe '" + boss.getId() + "': la estructura '" + structure + "' no existe.");
            }
        }
        for (var alloy : plugin.getAlloyManager().getAll()) {
            if (alloy.getInput().getSignature().equals(alloy.getOutput().getSignature())) {
                r.warn("Aleación '" + alloy.getId() + "': entrada y salida son el mismo item (se podría repetir sin fin).");
            }
        }
        if (plugin.getSkinService().isEnabled()) {
            for (ProcessStage stage : ProcessStage.values()) {
                String skin = plugin.getPieceCatalog().skinOf(stage);
                if (!skin.isEmpty() && !plugin.getSkinService().has(skin)) {
                    r.warn("piezas.yml: la etapa " + stage + " usa la skin '" + skin + "', que no está en skins.yml (se verá el icono vanilla).");
                }
            }
        }

        if (registry.entries().isEmpty()) r.fail("materiales.yml no tiene ningún material: no se podrá procesar nada.");
        for (MaterialRegistry.Smelt smelt : registry.smelts()) {
            if (registry.fuels().isEmpty()) {
                r.warn("Hay fundiciones pero ninguna lista de combustibles: no se podrá fundir.");
                break;
            }
            if (registry.forItem(new ItemStack(smelt.output())) == null) {
                r.warn("Fundición '" + smelt.id() + "': su salida " + smelt.output() + " no está en materiales.yml (no se podrá procesar).");
            }
        }
        return r.finish("Validación");
    }

    // ---------------------------------------------------------------- selftest

    public List<String> selftest() {
        Report r = new Report();
        MaterialRegistry registry = plugin.getMaterialRegistry();
        int checked = 0;

        for (MaterialRegistry.Entry entry : registry.entries()) {
            checked++;
            String label = entry.id() + " (" + entry.item() + ")";
            ItemStack stack = new ItemStack(entry.item());

            MaterialRegistry.Entry found = registry.forItem(stack);
            if (found == null || !found.id().equals(entry.id())) { r.fail(label + ": el registro no reconoce su propio item."); continue; }
            if (!ItemRef.of(stack).matches(stack)) { r.fail(label + ": ItemRef no coincide con su propio item."); continue; }

            String baseKey = BaseKey.ofItem(stack);
            MaterialRegistry.Entry byKey = registry.forBaseKey(baseKey);
            if (byKey == null || !byKey.id().equals(entry.id())) { r.fail(label + ": la clave base " + baseKey + " no vuelve al material."); continue; }
            if (BaseKey.iconFor(plugin, baseKey, entry.item()) == null) { r.fail(label + ": sin icono."); continue; }

            boolean pieceOk = true;
            for (ProcessStage stage : ProcessStage.values()) {
                if (!entry.kind().allows(stage)) continue;
                ItemStack piece = new ItemStack(entry.item());
                ItemMeta meta = piece.getItemMeta();
                if (meta == null) { pieceOk = false; break; }
                PieceData.write(meta, stage, baseKey, 77.5);
                piece.setItemMeta(meta);
                PieceData.Data back = PieceData.read(piece);
                if (back == null || back.stage() != stage || !baseKey.equals(back.baseKey()) || Math.abs(back.score() - 77.5) > 1e-9) {
                    r.fail(label + ": la pieza en etapa " + stage + " no se lee igual que se escribió.");
                    pieceOk = false;
                    break;
                }
                if (ItemRef.of(entry.item()).matches(piece)) {
                    r.fail(label + ": una pieza procesada se confunde con el material normal (etapa " + stage + ").");
                    pieceOk = false;
                    break;
                }
            }
            if (pieceOk) r.ok(label + ": " + entry.kind() + " correcto.");
        }
        if (checked == 0) r.fail("No hay materiales registrados que probar.");
        return r.finish("Autoprueba de " + checked + " material(es)");
    }
}
