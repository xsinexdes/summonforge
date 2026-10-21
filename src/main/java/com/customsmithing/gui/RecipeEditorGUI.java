package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.ProcessStage;
import com.customsmithing.data.Specialization;
import com.customsmithing.recipes.SmithingRecipe;
import com.customsmithing.util.Messages;
import com.customsmithing.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * GUI para crear/editar recetas completamente in-game, sin tocar archivos ni codigo.
 *
 * Layout (54 slots, 6 filas):
 *  - Fila 0 (0-8)   : banner explicativo
 *  - Fila 1 (9-17)  : ingredientes (agarra un item de tu inventario y clic aqui)
 *  - Fila 2 (18-26) : slot 22 = resultado de la receta
 *  - Fila 3 (27-35) : toggles de procesos requeridos (28-33)
 *  - Fila 4 (36-44) : requiere plano (38), especializacion (40), nivel (42), dificultad (44)
 *  - Fila 5 (45-53) : generar papel (48), GUARDAR (49), CANCELAR (53)
 */
public class RecipeEditorGUI implements SmithingGUIHolder {

    private final CustomSmithing plugin;
    private final Player player;
    private final Inventory inventory;
    private final SmithingRecipe working;
    private final boolean isNew;

    private static final int BANNER_SLOT = 4;
    private static final int[] INGREDIENT_SLOTS = {9,10,11,12,13,14,15,16,17};
    private static final int RESULT_SLOT = 22;
    private static final int METAL_SLOT = 20;
    private static final int MINERAL_TOGGLE_SLOT = 19;
    private static final int WOOD_SLOT = 24;
    private static final int MOLD_SHAPE_SLOT = 26;
    private static final int[] PROCESS_SLOTS = {27,28,29,30,31,32,33,34};
    private static final ProcessStage[] PROCESS_ORDER = {
            ProcessStage.FUNDICION, ProcessStage.CALENTADO, ProcessStage.GOLPEADO,
            ProcessStage.ENFRIADO, ProcessStage.ENSAMBLAJE, ProcessStage.REFINADO,
            ProcessStage.TALLADO, ProcessStage.TEJIDO
    };
    private static final int BLUEPRINT_TOGGLE_SLOT = 38;
    private static final int SPEC_SLOT = 40;
    private static final int LEVEL_SLOT = 42;
    private static final int DIFFICULTY_SLOT = 44;
    private static final int GENERATE_PAPER_SLOT = 48;
    private static final int SAVE_SLOT = 49;
    private static final int CANCEL_SLOT = 53;

    public RecipeEditorGUI(CustomSmithing plugin, Player player, String recipeId) {
        this.plugin = plugin;
        this.player = player;
        this.isNew = !plugin.getRecipeManager().exists(recipeId);
        this.working = isNew ? new SmithingRecipe(recipeId) : cloneOf(plugin.getRecipeManager().get(recipeId));
        this.inventory = Bukkit.createInventory(this, 54, "\u270E Editor de Receta: " + recipeId);
        render();
    }

    private SmithingRecipe cloneOf(SmithingRecipe src) {
        SmithingRecipe copy = new SmithingRecipe(src.getId());
        copy.setResultMaterial(src.getResultMaterial());
        copy.setResultName(src.getResultName());
        copy.setRequiredLevel(src.getRequiredLevel());
        copy.setRequiresBlueprint(src.isRequiresBlueprint());
        copy.getIngredients().putAll(src.getIngredients());
        copy.setSteps(EnumSet.copyOf(src.getSteps()));
        copy.setSpecialization(src.getSpecialization());
        copy.setDifficulty(src.getDifficulty());
        copy.setPrerequisiteId(src.getPrerequisiteId());
        copy.setMetalMaterial(src.getMetalMaterial());
        copy.setMetalCustomMineralId(src.getMetalCustomMineralId());
        copy.setMoldShape(src.getMoldShape());
        copy.setWoodMaterial(src.getWoodMaterial());
        return copy;
    }

    private void render() {
        inventory.clear();
        fillGlass();

        inventory.setItem(BANNER_SLOT, labeled(Material.KNOWLEDGE_BOOK,
                "\u00a7e\u00a7l\u00bfC\u00f3mo se usa este editor?",
                "\u00a771. Toma un item de TU inventario (abajo)",
                "\u00a77   con el mouse (clic normal para levantarlo).",
                "\u00a772. Haz clic en un slot verde de ingrediente",
                "\u00a77   (fila de abajo) para ponerlo ah\u00ed.",
                "\u00a773. Haz lo mismo en el slot celeste para",
                "\u00a77   definir el RESULTADO de la receta.",
                "\u00a774. Si esta receta usa Calentado/Golpeado/",
                "\u00a77   Enfriado, marca el METAL en el slot naranja.",
                "\u00a775. Si usa Tallado/Tejido, marca la MADERA",
                "\u00a77   en el slot marr\u00f3n. As\u00ed el mismo lingote",
                "\u00a77   procesado sirve para CUALQUIER receta",
                "\u00a77   que use ese mismo metal (espada, hacha...).",
                "\u00a776. Ingrediente puesto: clic izq/der sin nada",
                "\u00a77   en la mano ajusta o quita la cantidad."));

        // Fila de ingredientes
        for (int slot : INGREDIENT_SLOTS) inventory.setItem(slot, null);
        int i = 0;
        List<Material> ingredientOrder = new ArrayList<>(working.getIngredients().keySet());
        for (Material mat : ingredientOrder) {
            if (i >= INGREDIENT_SLOTS.length) break;
            int amount = working.getIngredients().get(mat);
            ItemStack ing = new ItemStack(mat, Math.max(1, amount));
            ItemMeta ingMeta = ing.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add("\u00a7eCantidad necesaria: \u00a7f" + amount);
            lore.add("\u00a77Clic izq: +1  |  Clic der: quitar 1");
            lore.add("\u00a77(en 0 se elimina el ingrediente)");
            ingMeta.setLore(lore);
            ing.setItemMeta(ingMeta);
            inventory.setItem(INGREDIENT_SLOTS[i], ing);
            i++;
        }
        // Slots de ingrediente vacios: marcarlos como "zona para soltar"
        for (int s = i; s < INGREDIENT_SLOTS.length; s++) {
            inventory.setItem(INGREDIENT_SLOTS[s], labeled(Material.LIME_STAINED_GLASS_PANE,
                    "\u00a7a+ Ingrediente vac\u00edo",
                    "\u00a77Toma un item de tu inventario",
                    "\u00a77y haz clic aqu\u00ed para ponerlo"));
        }

        // Resultado
        if (working.getResultMaterial() != null) {
            ItemStack result = new ItemStack(working.getResultMaterial());
            ItemMeta meta = result.getItemMeta();
            meta.setDisplayName("\u00a7bResultado: " + (working.getResultName() != null ? working.getResultName() : working.getResultMaterial().name()));
            meta.setLore(List.of("\u00a77Este es el item que se va a poder", "\u00a77fabricar con esta receta.", "\u00a77Pon otro item aqu\u00ed para cambiarlo."));
            result.setItemMeta(meta);
            inventory.setItem(RESULT_SLOT, result);
        } else {
            inventory.setItem(RESULT_SLOT, labeled(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                    "\u00a7b+ Resultado de la receta",
                    "\u00a77Toma el item que quieras que la",
                    "\u00a77gente pueda fabricar y haz clic aqu\u00ed"));
        }

        // Metal universal (Calentado/Golpeado/Enfriado) - vanilla
        if (working.getMetalCustomMineralId() != null && plugin.getCustomMineralManager().exists(working.getMetalCustomMineralId())) {
            com.customsmithing.minerals.CustomMineral mineral = plugin.getCustomMineralManager().get(working.getMetalCustomMineralId());
            inventory.setItem(METAL_SLOT, labeled(mineral.getIconMaterial(),
                    "\u00a75Mineral custom: \u00a7f" + mineral.getDisplayName(),
                    "\u00a77Este mineral se procesa en el",
                    "\u00a77Calentado/Golpeado/Enfriado.",
                    "\u00a77Clic derecho sin nada en la mano: quitar"));
        } else if (working.getMetalMaterial() != null) {
            ItemStack metal = new ItemStack(working.getMetalMaterial());
            ItemMeta metalMeta = metal.getItemMeta();
            metalMeta.setDisplayName("\u00a76Metal a procesar: \u00a7f" + Text.prettify(working.getMetalMaterial().name()));
            metalMeta.setLore(List.of("\u00a77Este es el metal que se usa en", "\u00a77Calentado/Golpeado/Enfriado.",
                    "\u00a77Cualquier receta que use este mismo", "\u00a77metal puede reusar la pieza procesada.",
                    "\u00a77Clic derecho sin nada en la mano: quitar"));
            metal.setItemMeta(metalMeta);
            inventory.setItem(METAL_SLOT, metal);
        } else {
            inventory.setItem(METAL_SLOT, labeled(Material.ORANGE_STAINED_GLASS_PANE,
                    "\u00a76+ Metal a procesar (opcional)",
                    "\u00a77Solo necesario si esta receta activa",
                    "\u00a77Calentado, Golpeado o Enfriado."));
        }

        // Mineral 100% custom, alternativa al metal vanilla (mutuamente excluyentes)
        List<com.customsmithing.minerals.CustomMineral> allMinerals = new ArrayList<>(plugin.getCustomMineralManager().getAll());
        if (working.getMetalCustomMineralId() != null && plugin.getCustomMineralManager().exists(working.getMetalCustomMineralId())) {
            com.customsmithing.minerals.CustomMineral mineral = plugin.getCustomMineralManager().get(working.getMetalCustomMineralId());
            inventory.setItem(MINERAL_TOGGLE_SLOT, labeled(mineral.getIconMaterial(),
                    "\u00a75Mineral custom: \u00a7f" + mineral.getDisplayName(),
                    "\u00a77Clic para cambiar / ciclar",
                    "\u00a77Clic derecho: volver a metal vanilla"));
        } else if (!allMinerals.isEmpty()) {
            inventory.setItem(MINERAL_TOGGLE_SLOT, labeled(Material.PURPLE_STAINED_GLASS_PANE,
                    "\u00a75+ Usar un mineral custom en vez de metal",
                    "\u00a77Clic para elegir uno (" + allMinerals.size() + " disponibles)"));
        } else {
            inventory.setItem(MINERAL_TOGGLE_SLOT, labeled(Material.GRAY_STAINED_GLASS_PANE,
                    "\u00a77No hay minerales custom creados",
                    "\u00a77Usa /smithing mineral new <id> <nombre>"));
        }

        // Madera/material universal (Tallado/Tejido)
        if (working.getWoodMaterial() != null) {
            ItemStack wood = new ItemStack(working.getWoodMaterial());
            ItemMeta woodMeta = wood.getItemMeta();
            woodMeta.setDisplayName("\u00a76Madera/tela a procesar: \u00a7f" + Text.prettify(working.getWoodMaterial().name()));
            woodMeta.setLore(List.of("\u00a77Este es el material que se usa en", "\u00a77Tallado (Cortapiedras) o Tejido (Telar).",
                    "\u00a77Clic derecho sin nada en la mano: quitar"));
            wood.setItemMeta(woodMeta);
            inventory.setItem(WOOD_SLOT, wood);
        } else {
            inventory.setItem(WOOD_SLOT, labeled(Material.SPRUCE_PLANKS,
                    "\u00a76+ Madera/tela a procesar (opcional)",
                    "\u00a77Solo necesario si esta receta activa",
                    "\u00a77Tallado o Tejido."));
        }

        // Molde (forma exacta: pico, hacha, peto, etc). Se usa junto con el metal/mineral
        // de arriba: el jugador sostiene el metal en la mano y el molde en la mano
        // secundaria al golpear en el Calentador, y la pieza final queda con esa forma
        // EXACTA — un molde de Pico nunca sirve para una receta que pide un Peto.
        if (working.getMoldShape() != null) {
            inventory.setItem(MOLD_SHAPE_SLOT, labeled(working.getMoldShape().getMoldIcon(),
                    "\u00a7eForma requerida: \u00a7f" + working.getMoldShape().getLabel(),
                    "\u00a77El jugador necesita el Molde de " + working.getMoldShape().getLabel(),
                    "\u00a77en la mano secundaria al calentar el metal.",
                    "\u00a77Clic para cambiar  |  Clic derecho: quitar (sin forma)"));
        } else {
            inventory.setItem(MOLD_SHAPE_SLOT, labeled(Material.PAPER,
                    "\u00a77+ Requerir un Molde (opcional)",
                    "\u00a77Si lo activás, la pieza procesada",
                    "\u00a77sirve SOLO para este molde exacto",
                    "\u00a77(pico, hacha, peto, etc). Sin esto,",
                    "\u00a77cualquier receta con el mismo metal",
                    "\u00a77comparte la pieza (como antes)."));
        }

        // Toggles de procesos
        for (int p = 0; p < PROCESS_ORDER.length; p++) {
            ProcessStage step = PROCESS_ORDER[p];
            boolean active = working.getSteps().contains(step);
            Material mat = active ? Material.LIME_DYE : Material.GRAY_DYE;
            inventory.setItem(PROCESS_SLOTS[p], labeled(mat,
                    (active ? "\u00a7a\u2714 " : "\u00a7c\u2716 ") + step.getIcon() + " " + step.getLabel(),
                    "\u00a77Clic para " + (active ? "desactivar" : "activar")));
        }

        inventory.setItem(BLUEPRINT_TOGGLE_SLOT, labeled(
                working.isRequiresBlueprint() ? Material.WRITABLE_BOOK : Material.PAPER,
                "\u00a7dRequiere plano: " + (working.isRequiresBlueprint() ? "\u00a7aSI" : "\u00a7cNO"),
                "\u00a77Clic para alternar"));

        inventory.setItem(SPEC_SLOT, labeled(Material.NETHER_STAR,
                "\u00a76Especialización: \u00a7f" + working.getSpecialization().getLabel(),
                "\u00a77" + working.getSpecialization().getDescription(),
                "\u00a77Clic para cambiar"));

        inventory.setItem(LEVEL_SLOT, labeled(Material.EXPERIENCE_BOTTLE,
                "\u00a7bNivel requerido: \u00a7f" + working.getRequiredLevel(),
                "\u00a77Clic izq: +1  |  Clic der: -1"));

        inventory.setItem(DIFFICULTY_SLOT, labeled(Material.REDSTONE,
                "\u00a7cDificultad: \u00a7f" + Text.fmt1(working.getDifficulty()),
                "\u00a77Más alta = minijuego más exigente", "\u00a77y más riesgo al refinar",
                "\u00a77Clic izq: +0.1  |  Clic der: -0.1"));

        if (working.isRequiresBlueprint()) {
            inventory.setItem(GENERATE_PAPER_SLOT, labeled(Material.PAPER,
                    "\u00a7dGenerar papel de investigación",
                    "\u00a77Te da un papel para esta receta.",
                    "\u00a77Guarda la receta primero si es nueva."));
        }

        inventory.setItem(SAVE_SLOT, labeled(Material.EMERALD_BLOCK, "\u00a7a\u00a7lGUARDAR RECETA", "\u00a77Guarda esta receta en recipes.yml"));
        inventory.setItem(CANCEL_SLOT, labeled(Material.REDSTONE_BLOCK, "\u00a7c\u00a7lCANCELAR", "\u00a77Cierra sin guardar"));
    }

    private void fillGlass() {
        ItemStack filler = labeled(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);
    }

    private ItemStack labeled(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        boolean isIngredientSlot = contains(INGREDIENT_SLOTS, slot);
        if (isIngredientSlot || slot == RESULT_SLOT) {
            ItemStack cursor = event.getCursor();
            boolean hasCursorItem = cursor != null && cursor.getType() != Material.AIR;

            if (hasCursorItem) {
                if (slot == RESULT_SLOT) {
                    working.setResultMaterial(cursor.getType());
                    ItemMeta meta = cursor.getItemMeta();
                    working.setResultName(meta != null && meta.hasDisplayName() ? meta.getDisplayName() : Text.prettify(cursor.getType().name()));
                } else {
                    working.getIngredients().put(cursor.getType(), Math.max(1, cursor.getAmount()));
                }
                render();
                return;
            }

            // Sin nada en la mano: si el slot es un ingrediente ya puesto, ajustar cantidad
            if (isIngredientSlot) {
                int index = indexOf(INGREDIENT_SLOTS, slot);
                List<Material> order = new ArrayList<>(working.getIngredients().keySet());
                if (index >= 0 && index < order.size()) {
                    Material mat = order.get(index);
                    int current = working.getIngredients().get(mat);
                    if (event.getClick() == ClickType.RIGHT) {
                        current--;
                        if (current <= 0) {
                            working.getIngredients().remove(mat);
                        } else {
                            working.getIngredients().put(mat, current);
                        }
                    } else {
                        working.getIngredients().put(mat, current + 1);
                    }
                    render();
                }
            }
            return;
        }

        if (slot == METAL_SLOT) {
            // Un solo slot: acepta un metal vanilla registrado en materiales.yml O un mineral custom.
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                String mineralId = cursor.hasItemMeta() && cursor.getItemMeta() != null
                        ? cursor.getItemMeta().getPersistentDataContainer().get(com.customsmithing.data.SmithingKeys.CUSTOM_MINERAL_ID,
                                org.bukkit.persistence.PersistentDataType.STRING) : null;
                var registered = plugin.getMaterialRegistry().forItem(cursor);
                if (mineralId != null && plugin.getCustomMineralManager().exists(mineralId)) {
                    working.setMetalCustomMineralId(mineralId);
                    working.setMetalMaterial(null);
                } else if (registered != null && registered.kind() == com.customsmithing.material.MaterialRegistry.Kind.METAL) {
                    working.setMetalMaterial(cursor.getType());
                    working.setMetalCustomMineralId(null);
                } else {
                    com.customsmithing.util.Messages.send(player, "editor.no_es_metal");
                }
            } else if (event.getClick() == ClickType.RIGHT) {
                working.setMetalMaterial(null);
                working.setMetalCustomMineralId(null);
            }
            render();
            return;
        }

        if (slot == MINERAL_TOGGLE_SLOT) {
            List<com.customsmithing.minerals.CustomMineral> allMinerals = new ArrayList<>(plugin.getCustomMineralManager().getAll());
            if (allMinerals.isEmpty()) return;

            if (event.getClick() == ClickType.RIGHT) {
                working.setMetalCustomMineralId(null);
                render();
                return;
            }
            String current = working.getMetalCustomMineralId();
            int currentIndex = -1;
            for (int i = 0; i < allMinerals.size(); i++) {
                if (allMinerals.get(i).getId().equals(current)) { currentIndex = i; break; }
            }
            int next = (currentIndex + 1) % allMinerals.size();
            working.setMetalCustomMineralId(allMinerals.get(next).getId());
            working.setMetalMaterial(null);
            render();
            return;
        }

        if (slot == MOLD_SHAPE_SLOT) {
            if (event.getClick() == ClickType.RIGHT) {
                working.setMoldShape(null);
                render();
                return;
            }
            com.customsmithing.data.MoldType[] shapes = com.customsmithing.data.MoldType.values();
            int idx = working.getMoldShape() == null ? -1 : working.getMoldShape().ordinal();
            int next2 = (idx + 1) % shapes.length;
            working.setMoldShape(shapes[next2]);
            render();
            return;
        }

        if (slot == WOOD_SLOT) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                working.setWoodMaterial(cursor.getType());
            } else if (event.getClick() == ClickType.RIGHT) {
                working.setWoodMaterial(null);
            }
            render();
            return;
        }

        if (slot == BLUEPRINT_TOGGLE_SLOT) {
            working.setRequiresBlueprint(!working.isRequiresBlueprint());
            render();
            return;
        }

        if (slot == SPEC_SLOT) {
            Specialization[] values = Specialization.values();
            int next = (working.getSpecialization().ordinal() + 1) % values.length;
            working.setSpecialization(values[next]);
            render();
            return;
        }

        if (slot == LEVEL_SLOT) {
            int delta = event.getClick() == ClickType.RIGHT ? -1 : 1;
            working.setRequiredLevel(Math.max(1, working.getRequiredLevel() + delta));
            render();
            return;
        }

        if (slot == DIFFICULTY_SLOT) {
            double delta = event.getClick() == ClickType.RIGHT ? -0.1 : 0.1;
            double newValue = Math.max(0.5, Math.min(3.0, working.getDifficulty() + delta));
            working.setDifficulty(Math.round(newValue * 10.0) / 10.0);
            render();
            return;
        }

        if (slot == GENERATE_PAPER_SLOT && working.isRequiresBlueprint()) {
            if (!plugin.getRecipeManager().exists(working.getId())) {
                Messages.send(player, "editor.guardar_antes_de_plano");
                return;
            }
            plugin.getBlueprintManager().giveBlueprint(player, working.getId());
            return;
        }

        for (int p = 0; p < PROCESS_SLOTS.length; p++) {
            if (slot == PROCESS_SLOTS[p]) {
                ProcessStage step = PROCESS_ORDER[p];
                EnumSet<ProcessStage> steps = working.getSteps();
                if (steps.contains(step)) steps.remove(step); else steps.add(step);
                render();
                return;
            }
        }

        if (slot == SAVE_SLOT) {
            save();
            return;
        }

        if (slot == CANCEL_SLOT) {
            player.closeInventory();
            Messages.send(player, "editor.cancelado");
        }
    }

    private boolean contains(int[] arr, int v) {
        for (int x : arr) if (x == v) return true;
        return false;
    }

    private int indexOf(int[] arr, int v) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == v) return i;
        return -1;
    }

    private void save() {
        if (working.getResultMaterial() == null) {
            Messages.send(player, "editor.falta_resultado");
            return;
        }
        if (working.getIngredients().isEmpty()) {
            Messages.send(player, "editor.falta_ingrediente");
            return;
        }
        plugin.getRecipeManager().save(working);
        Messages.send(player, "recetas.guardada", "%id%", working.getId());
        player.closeInventory();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
