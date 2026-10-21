package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GogglesGUI implements SmithingGUIHolder {

    private final CustomSmithing plugin;
    private final Player player;
    private final Inventory inventory;

    private static final int INFO_SLOT = 4;
    private static final int SKILLS_SLOT = 19;
    private static final int TREE_SLOT = 21;
    private static final int BLUEPRINTS_SLOT = 23;
    private static final int ASSEMBLY_SLOT = 25;
    private static final int SEASONS_SLOT = 31;

    public GogglesGUI(CustomSmithing plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 36, "\u00a7b\ud83e\udd7d Lentes del Herrero");
        render();
    }

    private void render() {
        inventory.clear();
        var breakdown = plugin.getEnvironmentModifier().breakdown(player.getLocation());
        double total = breakdown.values().stream().mapToDouble(Double::doubleValue).sum();

        ItemStack info = new ItemStack(Material.SPYGLASS);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("\u00a7bCondiciones actuales");
        List<String> lore = new ArrayList<>();
        for (var entry : breakdown.entrySet()) {
            double v = entry.getValue();
            String color = v > 0 ? "\u00a7a" : v < 0 ? "\u00a7c" : "\u00a77";
            lore.add("\u00a77" + entry.getKey() + ": " + color + (v >= 0 ? "+" : "") + Text.fmt1(v));
        }
        lore.add(" ");
        lore.add("\u00a7eModificador total: " + (total >= 0 ? "\u00a7a+" : "\u00a7c") + Text.fmt1(total));
        infoMeta.setLore(lore);
        info.setItemMeta(infoMeta);
        inventory.setItem(INFO_SLOT, info);

        inventory.setItem(SKILLS_SLOT, item(Material.EXPERIENCE_BOTTLE, "\u00a7bÁrbol de Habilidades", "\u00a77Acceso rápido"));
        inventory.setItem(TREE_SLOT, item(Material.FILLED_MAP, "\u00a72Árbol de Recetas", "\u00a77Acceso rápido"));
        inventory.setItem(BLUEPRINTS_SLOT, item(Material.WRITTEN_BOOK, "\u00a7dMesa de Planos", "\u00a77Acceso rápido"));
        inventory.setItem(ASSEMBLY_SLOT, item(Material.SMITHING_TABLE, "\u00a7aMesa de Ensamblaje", "\u00a77Acceso rápido"));

        if (player.hasPermission("customsmithing.admin")) {
            inventory.setItem(SEASONS_SLOT, item(Material.SNOWBALL, "\u00a7bEstaciones del Año (Admin)", "\u00a77Acceso rápido"));
        }
    }

    private ItemStack item(Material mat, String name, String... lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(lore));
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        switch (event.getRawSlot()) {
            case SKILLS_SLOT -> new SkillTreeGUI(plugin, player).open(player);
            case TREE_SLOT -> new RecipeTreeGUI(plugin, player).open(player);
            case BLUEPRINTS_SLOT -> new BlueprintTableGUI(plugin, player).open(player);
            case ASSEMBLY_SLOT -> new AssemblyGUI(plugin, player).open(player);
            case SEASONS_SLOT -> {
                if (player.hasPermission("customsmithing.admin")) new SeasonConfigGUI(plugin, player).open(player);
            }
            default -> {}
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
