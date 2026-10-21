package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.minerals.CustomMineral;
import com.customsmithing.util.Messages;
import com.customsmithing.util.Text;
import com.customsmithing.util.Theme;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Editor de minerales custom: icono (suelta un item con el cursor), bonos de resistencia y daño (+/-), y recibir unidades
 * para probarlo. Cada cambio se guarda al instante. El nombre se cambia con /smithing mineral rename.
 */
public class MineralEditorGUI extends MenuBase {

    private final CustomMineral mineral;

    public MineralEditorGUI(CustomSmithing plugin, Player player, CustomMineral mineral) {
        super(plugin, player, 3, "\u2692 Mineral: " + mineral.getId());
        this.mineral = mineral;
    }

    private void save() {
        plugin.getCustomMineralManager().save(mineral);
    }

    private static double step(InventoryClickEvent e, double normal, double big) {
        double amount = e.getClick().isShiftClick() ? big : normal;
        return e.getClick().isRightClick() ? -amount : amount;
    }

    @Override
    protected void render() {
        clear();
        button(10, icon("menu.editor", mineral.getIconMaterial(), Theme.title("Icono"),
                Theme.muted("Material actual: " + Text.prettify(mineral.getIconMaterial().name())),
                Theme.warn("Suelta aquí un item con el cursor para cambiarlo")), e -> {
            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType().isItem() && !cursor.getType().isAir()) {
                mineral.setIconMaterial(cursor.getType());
                save();
                render();
            }
        });
        set(12, icon("menu.editor", Material.NAME_TAG, Theme.title(mineral.getDisplayName()),
                Theme.muted("Para cambiar el nombre:"), Theme.warn("/smithing mineral rename " + mineral.getId() + " <nombre...>")));
        button(14, icon("menu.editor", Material.IRON_CHESTPLATE, Theme.title("Bonus de resistencia"),
                Theme.field("Valor", Text.fmt1(mineral.getBonusResistance()) + "%"),
                Theme.muted("Clic izq: +1  ·  Clic der: -1"), Theme.muted("Con Shift: de 5 en 5")), e -> {
            mineral.setBonusResistance(Math.max(0, Math.min(100, mineral.getBonusResistance() + step(e, 1, 5))));
            save();
            render();
        });
        button(15, icon("menu.editor", Material.IRON_SWORD, Theme.title("Daño extra"),
                Theme.field("Valor", "+" + Text.fmt1(mineral.getBonusDamageFlat())),
                Theme.muted("Clic izq: +0.5  ·  Clic der: -0.5"), Theme.muted("Con Shift: de 2.5 en 2.5")), e -> {
            mineral.setBonusDamageFlat(Math.max(0, Math.min(50, mineral.getBonusDamageFlat() + step(e, 0.5, 2.5))));
            save();
            render();
        });
        button(16, icon("menu.editor", mineral.getIconMaterial(), Theme.ok("Recibir 8 unidades"),
                Theme.muted("Para probarlo en las estaciones")), e -> {
            plugin.getGiveService().give(player, plugin.getCustomMineralManager().createRawItem(mineral.getId(), 8));
            Messages.send(player, "minerales.entregado", "%mineral%", mineral.getDisplayName());
        });
        fillEmpty();
    }
}
