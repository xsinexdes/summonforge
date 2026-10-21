package com.customsmithing.item;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.MoldType;
import com.customsmithing.data.SmithingKeys;
import com.customsmithing.util.Theme;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/** Moldes con usos limitados: se desgastan al completar un Calentado y se rompen al llegar a 0. */
public final class Molds {

    private Molds() {
    }

    public static int defaultUses(CustomSmithing plugin) {
        return Math.max(1, plugin.getConfig().getInt("moldes.usos", 15));
    }

    /** Forma del molde, o null si el item no es un molde (o su forma es desconocida). */
    public static MoldType shapeOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        String name = meta.getPersistentDataContainer().get(SmithingKeys.MOLD_SHAPE, PersistentDataType.STRING);
        if (name == null) return null;
        try {
            return MoldType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Usos restantes. Un molde antiguo (sin contador) se considera nuevo. */
    public static int usesOf(CustomSmithing plugin, ItemStack mold) {
        ItemMeta meta = mold.getItemMeta();
        if (meta == null) return defaultUses(plugin);
        Integer uses = meta.getPersistentDataContainer().get(SmithingKeys.MOLD_USES, PersistentDataType.INTEGER);
        return uses != null ? uses : defaultUses(plugin);
    }

    public static ItemStack create(CustomSmithing plugin, MoldType type, int uses) {
        ItemStack mold = new ItemStack(type.getMoldIcon());
        ItemMeta meta = mold.getItemMeta();
        meta.setDisplayName("\u00a7eMolde de " + type.getLabel());
        meta.setLore(List.of(
                Theme.muted("Sostenlo en la mano secundaria junto"),
                Theme.muted("con un metal en la principal, y golpea"),
                Theme.muted("el Calentador. La pieza final queda"),
                Theme.muted("con esta forma exacta."),
                " ",
                Theme.field("Usos restantes", String.valueOf(uses))));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(SmithingKeys.MOLD_SHAPE, PersistentDataType.STRING, type.name());
        pdc.set(SmithingKeys.MOLD_USES, PersistentDataType.INTEGER, uses);
        mold.setItemMeta(meta);
        return mold;
    }

    /** El mismo molde con un uso menos; null si se rompió. */
    public static ItemStack worn(CustomSmithing plugin, ItemStack mold) {
        MoldType type = shapeOf(mold);
        if (type == null) return mold;
        int left = usesOf(plugin, mold) - 1;
        return left <= 0 ? null : create(plugin, type, left);
    }
}
