package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.alloys.AlloyRecipe;
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
import java.util.List;

public class AlloyGUI implements SmithingGUIHolder {

    private final CustomSmithing plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<AlloyRecipe> shown = new ArrayList<>();

    public AlloyGUI(CustomSmithing plugin, Player player, ItemStack held) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, "\u2697 Mesa de Aleación");
        populate(held);
    }

    private void populate(ItemStack held) {
        shown.clear();
        int slot = 0;
        for (AlloyRecipe recipe : plugin.getAlloyManager().getByInput(held)) {
            if (slot >= 27) break;
            int have = recipe.getInput().count(player.getInventory());
            boolean enough = have >= recipe.getInputAmount();

            ItemStack icon = recipe.getOutput().toItemStack(1);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName((enough ? "\u00a7a" : "\u00a7c") + recipe.getDisplayName());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            List<String> lore = new ArrayList<>();
            lore.add("\u00a77Necesita: " + (enough ? "\u00a7a" : "\u00a7c") + recipe.getInputAmount() + "x "
                    + recipe.getInput().displayName() + " \u00a77(tienes " + have + ")");
            lore.add("\u00a77Produce: \u00a7f" + recipe.getOutputAmount() + "x " + recipe.getOutput().displayName());
            lore.add(" ");
            lore.add("\u00a77Puedes volver a meter el resultado aquí");
            lore.add("\u00a77de nuevo si encaja con otra aleación.");
            meta.setLore(lore);
            icon.setItemMeta(meta);

            inventory.setItem(slot, icon);
            shown.add(recipe);
            slot++;
        }

        if (shown.isEmpty()) {
            ItemStack none = new ItemStack(Material.BARRIER);
            ItemMeta meta = none.getItemMeta();
            meta.setDisplayName("\u00a7cNo hay aleaciones para ese material");
            none.setItemMeta(meta);
            inventory.setItem(13, none);
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= shown.size()) return;
        AlloyRecipe recipe = shown.get(slot);

        // Se cuenta y se consume con la MISMA regla (ItemRef), y se comprueba el resultado.
        if (!recipe.getInput().remove(player.getInventory(), recipe.getInputAmount())) {
            Messages.send(player, "aleacion.faltan_materiales");
            return;
        }

        plugin.getGiveService().give(player, recipe.getOutput().toItemStack(recipe.getOutputAmount()));
        Messages.send(player, "aleacion.convertido", "%cantidad%", String.valueOf(recipe.getOutputAmount()),
                "%material%", recipe.getOutput().displayName());
        player.closeInventory();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
