package com.summonforge.item;

import com.summonforge.core.Keys;
import com.summonforge.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ItemFactory {
    private ItemFactory() {}

    /** Cada ítem lleva un UID único en PDC (anti-duplicado y auditoría futura). */
    public static ItemStack create(ItemDef d) {
        ItemStack s = new ItemStack(d.material);
        ItemMeta m = s.getItemMeta();
        m.displayName(Text.mm(d.name));
        List<Component> lore = new ArrayList<>();
        if (!d.lore.isBlank()) {
            for (String line : d.lore.split("\\|")) lore.add(Text.mm("<gray>" + line.trim()));
        }
        switch (d.kind) {
            case SUMMON -> {
                if (!d.summonId.isEmpty()) lore.add(Text.mm("<yellow>Clic derecho: <white>obtener invocación"));
            }
            case AWAKEN -> lore.add(Text.mm("<yellow>Clic derecho: <white>despertar tu invocación"));
            case POWER -> lore.add(Text.mm("<yellow>Clic derecho: <white>poder temporal (luego fatiga)"));
            case RUNE -> lore.add(Text.mm("<yellow>Clic derecho: <white>equipar runa (" + d.runeStat.label + " +" + Math.round(d.runeValue) + "%)"));
        }
        m.lore(lore);
        if (d.customModelData > 0) m.setCustomModelData(d.customModelData);
        m.setEnchantmentGlintOverride(true);
        m.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, d.id);
        m.getPersistentDataContainer().set(Keys.ITEM_UID, PersistentDataType.STRING, UUID.randomUUID().toString());
        s.setItemMeta(m);
        return s;
    }

    public static String idOf(ItemStack s) {
        if (s == null || !s.hasItemMeta()) return null;
        return s.getItemMeta().getPersistentDataContainer().get(Keys.ITEM_ID, PersistentDataType.STRING);
    }

    public static String uidOf(ItemStack s) {
        if (s == null || !s.hasItemMeta()) return null;
        return s.getItemMeta().getPersistentDataContainer().get(Keys.ITEM_UID, PersistentDataType.STRING);
    }
}
