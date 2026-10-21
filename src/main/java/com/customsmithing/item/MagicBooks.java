package com.customsmithing.item;

import com.customsmithing.data.SmithingKeys;
import com.customsmithing.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/** Creación del "Libro Mágico" que consume la Mesa de Encantamiento. */
public final class MagicBooks {

    private MagicBooks() {
    }

    public static ItemStack create(Enchantment enchant, int level) {
        ItemStack book = new ItemBuilder(Material.BOOK)
                .name("&d&lLibro Mágico: &f" + enchant.getKey().getKey() + " " + level)
                .lore(List.of("&7Llévalo a la Mesa de Encantamiento", "&7junto con un arma para aplicarlo."))
                .build();
        ItemMeta meta = book.getItemMeta();
        meta.getPersistentDataContainer().set(SmithingKeys.MAGIC_BOOK_ENCHANT, PersistentDataType.STRING, enchant.getKey().getKey());
        meta.getPersistentDataContainer().set(SmithingKeys.MAGIC_BOOK_LEVEL, PersistentDataType.INTEGER, level);
        book.setItemMeta(meta);
        return book;
    }
}
