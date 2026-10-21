package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.recipes.SmithingRecipe;
import com.customsmithing.util.Theme;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Lista de recetas que requieren plano, con paginación (antes se cortaba en 54). Solo lectura. */
public class BlueprintTableGUI extends PagedMenu<SmithingRecipe> {

    public BlueprintTableGUI(CustomSmithing plugin, Player player) {
        super(plugin, player, "\u270D Mesa de Planos");
    }

    @Override
    protected List<SmithingRecipe> entries() {
        List<SmithingRecipe> list = new ArrayList<>();
        for (SmithingRecipe recipe : plugin.getRecipeManager().getAll()) {
            if (recipe.isRequiresBlueprint()) list.add(recipe);
        }
        return list;
    }

    @Override
    protected ItemStack entryIcon(SmithingRecipe recipe) {
        boolean owned = plugin.getPlayerDataManager().get(player).hasLearned(recipe.getId());
        return icon(owned ? "plano.investigado" : "plano.pendiente",
                owned ? Material.WRITTEN_BOOK : Material.BOOK,
                (owned ? Theme.OK : Theme.MUTED) + recipe.getResultName(),
                owned ? Theme.ok("Ya investigada (aprendida para siempre)") : Theme.bad("Aún no investigada"),
                Theme.muted("Nivel requerido: " + recipe.getRequiredLevel()),
                Theme.muted("Consigue el papel en cofres, misiones o"),
                Theme.muted("derrotando ciertos enemigos, y tráelo aquí."));
    }

    @Override
    protected String emptyText() {
        return "No hay recetas que requieran plano todavía.";
    }
}
