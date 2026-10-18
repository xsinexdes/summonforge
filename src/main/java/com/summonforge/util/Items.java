package com.summonforge.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public final class Items {
    private Items() {}

    public static ItemStack icon(Material m, String name, String... lore) {
        return icon(m, name, Arrays.asList(lore));
    }

    public static ItemStack icon(Material m, String name, List<String> lore) {
        Material mat = (m == null || m.isAir() || !m.isItem()) ? Material.STONE : m;
        ItemStack s = new ItemStack(mat);
        ItemMeta meta = s.getItemMeta();
        meta.displayName(Text.mm(name));
        if (lore != null && !lore.isEmpty()) meta.lore(Text.mm(lore));
        meta.addItemFlags(ItemFlag.values());
        s.setItemMeta(meta);
        return s;
    }

    public static ItemStack glow(ItemStack s) {
        ItemMeta meta = s.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        s.setItemMeta(meta);
        return s;
    }
}
