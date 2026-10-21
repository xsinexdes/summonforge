package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.admin.TestBench;
import com.customsmithing.data.PlayerData;
import com.customsmithing.recipes.RecipeTree;
import com.customsmithing.recipes.SmithingRecipe;
import com.customsmithing.unlock.UnlockService;
import com.customsmithing.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Árbol de recetas: cada fila es un nivel de profundidad según los prerequisitos. Un clic en una
 * receta disponible la DESBLOQUEA gastando puntos de receta (fabricar ya no desbloquea nada).
 * Los admins enlazan prerequisitos: Shift+Clic en la receta padre y luego clic normal en la hija;
 * clic derecho quita el prerequisito. No se permite crear ciclos.
 */
public class RecipeTreeGUI implements SmithingGUIHolder {

    private static final int MAX_TIER = 4;          // filas 0..4; la fila 5 es la barra de información
    private static final int INFO_SLOT = 49;

    private final CustomSmithing plugin;
    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, String> slotToRecipe = new HashMap<>();
    private String pendingParent = null;

    public RecipeTreeGUI(CustomSmithing plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "\u2618 Árbol de Recetas");
        render();
    }

    private Map<String, String> parentMap() {
        Map<String, String> parents = new LinkedHashMap<>();
        for (SmithingRecipe recipe : plugin.getRecipeManager().getAll()) {
            parents.put(recipe.getId(), recipe.getPrerequisiteId());
        }
        return parents;
    }

    private void render() {
        inventory.clear();
        slotToRecipe.clear();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        boolean isAdmin = player.hasPermission("customsmithing.admin");
        Map<String, String> parents = parentMap();

        Map<Integer, List<SmithingRecipe>> byTier = new TreeMap<>();
        for (SmithingRecipe recipe : plugin.getRecipeManager().getAll()) {
            if (recipe.getId().startsWith(TestBench.TEST_PREFIX) && !isAdmin) continue;
            int tier = Math.min(RecipeTree.tier(parents, recipe.getId()), MAX_TIER);
            byTier.computeIfAbsent(tier, k -> new ArrayList<>()).add(recipe);
        }

        int hidden = 0;
        for (var entry : byTier.entrySet()) {
            List<SmithingRecipe> inTier = entry.getValue();
            for (int col = 0; col < inTier.size(); col++) {
                if (col >= 9) { hidden++; continue; }
                int slot = entry.getKey() * 9 + col;
                inventory.setItem(slot, buildIcon(inTier.get(col), data, isAdmin));
                slotToRecipe.put(slot, inTier.get(col).getId());
            }
        }
        inventory.setItem(INFO_SLOT, infoItem(data, hidden));
    }

    private ItemStack infoItem(PlayerData data, int hidden) {
        UnlockService unlock = plugin.getUnlockService();
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7b\u00a7lPuntos de receta: \u00a7f" + unlock.availablePoints(data));
        List<String> lore = new ArrayList<>();
        lore.add("\u00a77Cada receta cuesta \u00a7f" + plugin.getConfig().getInt("desbloqueo.costo_por_tier", 1) + " \u00a77punto(s) por nivel del nodo.");
        lore.add("\u00a77Ganas puntos al subir de nivel de herrería.");
        lore.add("\u00a77Clic en una receta disponible para desbloquearla.");
        if (hidden > 0) lore.add("\u00a7e" + hidden + " receta(s) más no caben en este nivel del árbol.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildIcon(SmithingRecipe recipe, PlayerData data, boolean isAdmin) {
        UnlockService.Status status = plugin.getUnlockService().statusOf(data, recipe);

        Material mat = recipe.getResultMaterial() != null ? recipe.getResultMaterial() : Material.BARRIER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        String statusLine = switch (status) {
            case LEARNED -> "\u00a7a\u2714 Desbloqueada: fabrícala en la Mesa de Ensamblaje";
            case AVAILABLE -> "\u00a7e\u25B6 Clic para desbloquear (" + plugin.getUnlockService().cost(recipe) + " punto/s)";
            case NEEDS_PREREQUISITE -> "\u00a7c\u2716 Bloqueada: falta la receta previa";
            case NEEDS_LEVEL -> "\u00a7c\u2716 Falta nivel de herrería";
            case NEEDS_BLUEPRINT -> "\u00a7d\u2726 Se aprende investigando su plano (papel)";
            case NEEDS_POINTS -> "\u00a7c\u2716 No tienes puntos de receta suficientes";
        };

        meta.setDisplayName((status == UnlockService.Status.LEARNED ? "\u00a7a" : "\u00a7f") + recipe.getResultName());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        List<String> lore = new ArrayList<>();
        lore.add(statusLine);
        lore.add("\u00a77Nivel requerido: \u00a7f" + recipe.getRequiredLevel());
        if (recipe.getPrerequisiteId() != null) {
            SmithingRecipe parent = plugin.getRecipeManager().get(recipe.getPrerequisiteId());
            lore.add("\u00a77Requiere: \u00a7f" + (parent != null ? parent.getResultName() : recipe.getPrerequisiteId() + " \u00a7c(no existe)"));
        }
        if (isAdmin) {
            lore.add(" ");
            lore.add("\u00a76[Admin] Shift+Clic: marcar como padre");
            lore.add("\u00a76[Admin] Clic normal (con padre marcado): enlazar");
            lore.add("\u00a76[Admin] Clic derecho: quitar prerequisito");
            if (recipe.getId().equals(pendingParent)) lore.add("\u00a7b\u25CF Marcada como padre pendiente");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        String recipeId = slotToRecipe.get(event.getRawSlot());
        if (recipeId == null) return;
        SmithingRecipe recipe = plugin.getRecipeManager().get(recipeId);
        if (recipe == null) return;
        boolean isAdmin = player.hasPermission("customsmithing.admin");

        if (isAdmin && event.getClick().isShiftClick()) {
            pendingParent = recipeId;
            Messages.send(player, "desbloqueo.padre_marcado", "%receta%", recipeId);
            render();
            return;
        }

        if (isAdmin && event.getClick().isRightClick()) {
            recipe.setPrerequisiteId(null);
            plugin.getRecipeManager().save(recipe);
            Messages.send(player, "desbloqueo.prerequisito_quitado", "%receta%", recipeId);
            render();
            return;
        }

        if (isAdmin && pendingParent != null && !pendingParent.equals(recipeId)) {
            if (RecipeTree.wouldCreateCycle(parentMap(), recipeId, pendingParent)) {
                Messages.send(player, "desbloqueo.ciclo", "%receta%", recipeId, "%padre%", pendingParent);
            } else {
                recipe.setPrerequisiteId(pendingParent);
                plugin.getRecipeManager().save(recipe);
                Messages.send(player, "desbloqueo.enlazado", "%receta%", recipeId, "%padre%", pendingParent);
            }
            pendingParent = null;
            render();
            return;
        }

        // Jugador (o admin sin padre marcado): intentar desbloquear
        if (plugin.getPlayerDataManager().get(player).hasLearned(recipeId)) {
            Messages.send(player, "desbloqueo.ya_aprendida", "%receta%", recipe.getResultName());
            return;
        }
        UnlockService.Status result = plugin.getUnlockService().unlock(player, recipe);
        String key = switch (result) {
            case NEEDS_PREREQUISITE -> "desbloqueo.falta_prerequisito";
            case NEEDS_LEVEL -> "desbloqueo.falta_nivel";
            case NEEDS_BLUEPRINT -> "desbloqueo.requiere_plano";
            case NEEDS_POINTS -> "desbloqueo.sin_puntos";
            case LEARNED, AVAILABLE -> "desbloqueo.exito"; // unlock() devuelve LEARNED solo si desbloqueó ahora
        };
        Messages.send(player, key, "%receta%", recipe.getResultName(), "%nivel%", String.valueOf(recipe.getRequiredLevel()));
        render();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
