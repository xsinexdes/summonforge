package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.PlayerData;
import com.customsmithing.data.SmithingKeys;
import com.customsmithing.recipes.SmithingRecipe;
import com.customsmithing.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ResearchGUI implements SmithingGUIHolder {

    private final CustomSmithing plugin;
    private final Player player;
    private final Inventory inventory;
    private final String recipeId;
    private boolean resolved = false;

    private static final int RESULT_PREVIEW_SLOT = 11;
    private static final int RESEARCH_BUTTON_SLOT = 15;

    public ResearchGUI(CustomSmithing plugin, Player player, String recipeId) {
        this.plugin = plugin;
        this.player = player;
        this.recipeId = recipeId;
        this.inventory = Bukkit.createInventory(this, 27, "\u270D Mesa de Investigación");
        render();
    }

    private void render() {
        inventory.clear();
        SmithingRecipe recipe = plugin.getRecipeManager().get(recipeId);
        PlayerData data = plugin.getPlayerDataManager().get(player);

        if (recipe == null) {
            ItemStack err = new ItemStack(Material.BARRIER);
            ItemMeta meta = err.getItemMeta();
            meta.setDisplayName("\u00a7cEsta receta ya no existe");
            err.setItemMeta(meta);
            inventory.setItem(13, err);
            return;
        }

        boolean alreadyLearned = data.hasLearned(recipeId);

        ItemStack preview = new ItemStack(recipe.getResultMaterial());
        ItemMeta previewMeta = preview.getItemMeta();
        previewMeta.setDisplayName("\u00a7f" + recipe.getResultName());
        previewMeta.setLore(List.of(
                "\u00a77Nivel requerido: \u00a7f" + recipe.getRequiredLevel(),
                alreadyLearned ? "\u00a7aYa investigada" : "\u00a7eAún no investigada"
        ));
        preview.setItemMeta(previewMeta);
        inventory.setItem(RESULT_PREVIEW_SLOT, preview);

        Material buttonMat = alreadyLearned ? Material.GRAY_DYE : Material.LIME_DYE;
        ItemStack button = new ItemStack(buttonMat);
        ItemMeta buttonMeta = button.getItemMeta();
        buttonMeta.setDisplayName(alreadyLearned ? "\u00a77Ya conoces esta receta" : "\u00a7a\u00a7lINVESTIGAR (consume el papel)");
        buttonMeta.setLore(List.of("\u00a77Esto desbloquea la receta para siempre,", "\u00a77incluso si pierdes el papel."));
        button.setItemMeta(buttonMeta);
        inventory.setItem(RESEARCH_BUTTON_SLOT, button);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getRawSlot() != RESEARCH_BUTTON_SLOT || resolved) return;

        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data.hasLearned(recipeId)) return;

        SmithingRecipe recipe = plugin.getRecipeManager().get(recipeId);
        if (recipe == null) return;

        // El papel debe seguir en la mano: el jugador pudo cambiar de slot con el menú abierto,
        // y antes se consumía lo que hubiera en la mano en ese momento.
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isBlueprintFor(hand, recipeId)) {
            Messages.send(player, "planos.papel_no_coincide");
            return;
        }
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        data.learnRecipe(recipeId);
        plugin.getPlayerDataManager().save(player);
        resolved = true;
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
        Messages.send(player, "planos.investigado", "%receta%", recipe.getResultName());
        player.closeInventory();
    }

    private static boolean isBlueprintFor(ItemStack stack, String id) {
        if (stack == null || !stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        return id.equals(meta.getPersistentDataContainer().get(SmithingKeys.BLUEPRINT_RECIPE, PersistentDataType.STRING));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
