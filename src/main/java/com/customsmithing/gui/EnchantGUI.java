package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.Quality;
import com.customsmithing.data.SmithingKeys;
import com.customsmithing.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Mesa de Encantamiento custom: se combina un arma/herramienta con un "Libro Mágico"
 * (creado por un admin) para aplicarle un encantamiento.
 *
 * Los slots del arma y del libro son REALES (guardan los items del jugador, que debe poder
 * sacarlos si se arrepiente) y están vacíos cuando no hay nada: la indicación de qué va en cada
 * uno está en la fila de arriba, no dentro del slot. Así no hay items de muestra que se puedan
 * agarrar, confundir con items reales ni restaurar con tareas diferidas.
 */
public class EnchantGUI implements SmithingGUIHolder {

    private final CustomSmithing plugin;
    private final Player player;
    private final Inventory inventory;

    private static final int WEAPON_SLOT = 11;
    private static final int BOOK_SLOT = 15;
    private static final int APPLY_SLOT = 22;
    private static final int WEAPON_LABEL_SLOT = 2;
    private static final int BOOK_LABEL_SLOT = 6;

    public EnchantGUI(CustomSmithing plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, "\u2728 Mesa de Encantamiento Mágica");
        render();
    }

    /** Dibuja solo la decoración y el botón: nunca toca los slots reales del arma y del libro. */
    private void render() {
        ItemStack filler = labeled(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i == WEAPON_SLOT || i == BOOK_SLOT) continue;
            inventory.setItem(i, filler);
        }
        inventory.setItem(WEAPON_LABEL_SLOT, labeled(Material.LIME_STAINED_GLASS_PANE,
                "\u00a7aArma o herramienta", "\u00a77Deja el objeto a encantar", "\u00a77en el espacio de abajo."));
        inventory.setItem(BOOK_LABEL_SLOT, labeled(Material.MAGENTA_STAINED_GLASS_PANE,
                "\u00a7dLibro Mágico", "\u00a77Deja el libro en el", "\u00a77espacio de abajo."));
        inventory.setItem(APPLY_SLOT, labeled(Material.ENCHANTED_BOOK, "\u00a7d\u00a7lAPLICAR ENCANTAMIENTO",
                "\u00a77Consume el libro SIEMPRE (aunque falle).",
                "\u00a77Probabilidad de éxito: base " + (int) plugin.getConfig().getDouble("encantamiento.probabilidad_base", 65) + "%,",
                "\u00a77+bonus si el objeto tiene más calidad."));
    }

    private ItemStack labeled(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean isInteractiveSlot(int rawSlot) {
        return rawSlot == WEAPON_SLOT || rawSlot == BOOK_SLOT;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getRawSlot() == APPLY_SLOT) {
            applyEnchant();
        }
    }

    private void applyEnchant() {
        ItemStack weapon = inventory.getItem(WEAPON_SLOT);
        ItemStack book = inventory.getItem(BOOK_SLOT);

        if (weapon == null) {
            Messages.send(player, "encantamiento.sin_objeto");
            return;
        }
        if (book == null || !book.hasItemMeta()) {
            Messages.send(player, "encantamiento.sin_libro");
            return;
        }
        if (weapon.getAmount() != 1) {
            Messages.send(player, "encantamiento.una_sola_unidad");
            return;
        }
        ItemMeta bookMeta = book.getItemMeta();
        if (bookMeta == null) {
            Messages.send(player, "encantamiento.libro_invalido");
            return;
        }
        PersistentDataContainer bookPdc = bookMeta.getPersistentDataContainer();
        String enchantKey = bookPdc.get(SmithingKeys.MAGIC_BOOK_ENCHANT, PersistentDataType.STRING);
        Integer level = bookPdc.get(SmithingKeys.MAGIC_BOOK_LEVEL, PersistentDataType.INTEGER);
        if (enchantKey == null || level == null) {
            Messages.send(player, "encantamiento.libro_invalido");
            return;
        }

        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantKey));
        if (enchantment == null) {
            Messages.send(player, "encantamiento.desconocido", "%id%", enchantKey);
            return;
        }
        if (!enchantment.canEnchantItem(weapon)) {
            Messages.send(player, "encantamiento.incompatible");
            return;
        }

        double chance = plugin.getConfig().getDouble("encantamiento.probabilidad_base", 65);
        ItemMeta weaponMeta = weapon.getItemMeta();
        if (weaponMeta == null) {
            Messages.send(player, "encantamiento.sin_objeto");
            return;
        }
        Quality quality = Quality.parse(weaponMeta.getPersistentDataContainer().get(SmithingKeys.QUALITY, PersistentDataType.STRING));
        if (quality != null) {
            chance += plugin.getConfig().getDouble("encantamiento.bonus_" + quality.getConfigKey(), 0);
        }
        chance = Math.min(95, chance);

        // El libro se consume siempre: es el riesgo de intentarlo.
        if (book.getAmount() > 1) {
            book.setAmount(book.getAmount() - 1);
        } else {
            inventory.setItem(BOOK_SLOT, null);
        }

        if (Math.random() * 100 > chance) {
            Messages.send(player, "encantamiento.fallo");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        weaponMeta.addEnchant(enchantment, level, true);
        weapon.setItemMeta(weaponMeta);

        Messages.send(player, "encantamiento.exito", "%encantamiento%", enchantment.getKey().getKey(),
                "%nivel%", String.valueOf(level), "%probabilidad%", String.valueOf(Math.round(chance)));
    }

    @Override
    public void handleClose(Player closing) {
        // Idempotente: cada slot se vacía ANTES de entregar, así que una segunda llamada
        // (cierre + desconexión, recarga, apagado...) no puede devolver el mismo item dos veces.
        for (int slot : new int[]{WEAPON_SLOT, BOOK_SLOT}) {
            ItemStack item = inventory.getItem(slot);
            if (item == null) continue;
            inventory.setItem(slot, null);
            plugin.getGiveService().give(closing, item);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
