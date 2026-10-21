package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.MoldType;
import com.customsmithing.item.ItemRef;
import com.customsmithing.item.Molds;
import com.customsmithing.util.Messages;
import com.customsmithing.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Catalogo de moldes: /molde. Muestra las 9 formas disponibles (pico, hacha,
 * espada, pala, azada, casco, peto, pantalon, botas). Clic en una te da 1
 * Molde de esa forma. El molde se usa sosteniendolo en la mano SECUNDARIA
 * junto con un metal (o mineral custom) en la mano principal, al golpear
 * el Calentador — asi toda la pieza que sale del circuito de forjado ya
 * tiene esa forma exacta en vez de ser generica.
 */
public class MoldCatalogGUI implements SmithingGUIHolder {

    private final CustomSmithing plugin;
    private final Player player;
    private final Inventory inventory;
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 20, 22};

    public MoldCatalogGUI(CustomSmithing plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, "\u2692 Catálogo de Moldes");
        render();
    }

    private String costText() {
        boolean free = player.hasPermission("customsmithing.admin") && plugin.getConfig().getBoolean("moldes.admin_gratis", true);
        if (free) return "gratis para admins";
        return plugin.getConfig().getInt("moldes.costo_cantidad", 4) + "x "
                + Text.prettify(plugin.getConfig().getString("moldes.costo_material", "IRON_INGOT"));
    }

    private void render() {
        MoldType[] types = MoldType.values();
        for (int i = 0; i < types.length && i < SLOTS.length; i++) {
            MoldType type = types[i];
            ItemStack icon = new ItemStack(type.getMoldIcon());
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName("\u00a7eMolde de " + type.getLabel());
            meta.setLore(List.of(
                    "\u00a77Clic para recibir 1 (" + costText() + ").",
                    " ",
                    "\u00a77Cómo se usa:",
                    "\u00a77Sostenlo en la mano SECUNDARIA",
                    "\u00a77junto con un metal en la principal,",
                    "\u00a77y golpea el Calentador como siempre.",
                    "\u00a77La pieza final queda con esta forma",
                    "\u00a77exacta — sirve solo para recetas que",
                    "\u00a77pidan " + type.getLabel() + " de ese mismo metal."
            ));
            icon.setItemMeta(meta);
            inventory.setItem(SLOTS[i], icon);
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int index = -1;
        for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i] == event.getRawSlot()) { index = i; break; }
        if (index < 0 || index >= MoldType.values().length) return;

        MoldType type = MoldType.values()[index];

        // Los moldes cuestan material (los admins pueden pedirlos gratis si moldes.admin_gratis está activo).
        boolean free = player.hasPermission("customsmithing.admin") && plugin.getConfig().getBoolean("moldes.admin_gratis", true);
        if (!free) {
            Material costMaterial = Material.matchMaterial(plugin.getConfig().getString("moldes.costo_material", "IRON_INGOT"));
            int costAmount = Math.max(0, plugin.getConfig().getInt("moldes.costo_cantidad", 4));
            if (costMaterial != null && costMaterial.isItem() && !costMaterial.isAir() && costAmount > 0) {
                if (!ItemRef.of(costMaterial).remove(player.getInventory(), costAmount)) {
                    Messages.send(player, "moldes.sin_material", "%cantidad%", String.valueOf(costAmount),
                            "%material%", Text.prettify(costMaterial.name()));
                    return;
                }
            }
        }

        plugin.getGiveService().give(player, Molds.create(plugin, type, Molds.defaultUses(plugin)));
        Messages.send(player, "moldes.recibido", "%forma%", type.getLabel());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
