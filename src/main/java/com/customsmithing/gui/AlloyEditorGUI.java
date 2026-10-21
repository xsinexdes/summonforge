package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.alloys.AlloyRecipe;
import com.customsmithing.item.ItemRef;
import com.customsmithing.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class AlloyEditorGUI implements SmithingGUIHolder {

    private final CustomSmithing plugin;
    private final Player player;
    private final Inventory inventory;
    private final AlloyRecipe working;

    private static final int BANNER_SLOT = 4;
    private static final int INPUT_SLOT = 11;
    private static final int INPUT_AMOUNT_SLOT = 19;
    private static final int OUTPUT_SLOT = 15;
    private static final int OUTPUT_AMOUNT_SLOT = 23;
    private static final int SAVE_SLOT = 31;
    private static final int CANCEL_SLOT = 33;

    public AlloyEditorGUI(CustomSmithing plugin, Player player, String id) {
        this.plugin = plugin;
        this.player = player;
        boolean exists = plugin.getAlloyManager().exists(id);
        if (exists) {
            AlloyRecipe src = plugin.getAlloyManager().get(id);
            this.working = new AlloyRecipe(id);
            working.setDisplayName(src.getDisplayName());
            working.setInput(src.getInput());
            working.setInputAmount(src.getInputAmount());
            working.setOutput(src.getOutput());
            working.setOutputAmount(src.getOutputAmount());
        } else {
            this.working = new AlloyRecipe(id);
        }
        this.inventory = Bukkit.createInventory(this, 36, "\u2697 Editor de Aleación: " + id);
        render();
    }

    private void render() {
        inventory.clear();
        ItemStack filler = labeled(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        inventory.setItem(BANNER_SLOT, labeled(Material.KNOWLEDGE_BOOK,
                "\u00a7eEditor de Aleación",
                "\u00a77Toma un item y ponlo en el slot",
                "\u00a77de ENTRADA (izq) y otro en el de",
                "\u00a77SALIDA (der). Ajusta las cantidades abajo."));

        if (working.getInput() != null) {
            inventory.setItem(INPUT_SLOT, working.getInput().toItemStack(1));
        } else {
            inventory.setItem(INPUT_SLOT, labeled(Material.LIME_STAINED_GLASS_PANE, "\u00a7a+ Material de ENTRADA", "\u00a77Pon aquí el metal que se consume"));
        }
        inventory.setItem(INPUT_AMOUNT_SLOT, labeled(Material.HOPPER,
                "\u00a7bCantidad de entrada: \u00a7f" + working.getInputAmount(),
                "\u00a77Clic izq: +1  |  Clic der: -1"));

        if (working.getOutput() != null) {
            inventory.setItem(OUTPUT_SLOT, working.getOutput().toItemStack(1));
        } else {
            inventory.setItem(OUTPUT_SLOT, labeled(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "\u00a7b+ Material de SALIDA", "\u00a77Pon aquí el metal que se obtiene"));
        }
        inventory.setItem(OUTPUT_AMOUNT_SLOT, labeled(Material.DISPENSER,
                "\u00a7bCantidad de salida: \u00a7f" + working.getOutputAmount(),
                "\u00a77Clic izq: +1  |  Clic der: -1"));

        inventory.setItem(SAVE_SLOT, labeled(Material.EMERALD_BLOCK, "\u00a7a\u00a7lGUARDAR", "\u00a77Guarda esta aleación"));
        inventory.setItem(CANCEL_SLOT, labeled(Material.REDSTONE_BLOCK, "\u00a7c\u00a7lCANCELAR", "\u00a77Cierra sin guardar"));
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
        ItemStack cursor = event.getCursor();
        boolean hasCursorItem = cursor != null && cursor.getType() != Material.AIR;

        if (slot == INPUT_SLOT && hasCursorItem) {
            working.setInput(ItemRef.of(cursor));
            render();
            return;
        }
        if (slot == OUTPUT_SLOT && hasCursorItem) {
            working.setOutput(ItemRef.of(cursor));
            if (working.getDisplayName() == null && working.getInput() != null) {
                working.setDisplayName(working.getInput().displayName() + " -> " + working.getOutput().displayName());
            }
            render();
            return;
        }

        if (slot == INPUT_AMOUNT_SLOT) {
            int delta = event.getClick() == ClickType.RIGHT ? -1 : 1;
            working.setInputAmount(working.getInputAmount() + delta);
            render();
            return;
        }
        if (slot == OUTPUT_AMOUNT_SLOT) {
            int delta = event.getClick() == ClickType.RIGHT ? -1 : 1;
            working.setOutputAmount(working.getOutputAmount() + delta);
            render();
            return;
        }

        if (slot == SAVE_SLOT) {
            if (working.getInput() == null || working.getOutput() == null) {
                Messages.send(player, "editor.aleacion_incompleta");
                return;
            }
            if (working.getDisplayName() == null) working.setDisplayName(working.getId());
            plugin.getAlloyManager().save(working);
            Messages.send(player, "editor.aleacion_guardada", "%id%", working.getId());
            player.closeInventory();
            return;
        }

        if (slot == CANCEL_SLOT) {
            player.closeInventory();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
