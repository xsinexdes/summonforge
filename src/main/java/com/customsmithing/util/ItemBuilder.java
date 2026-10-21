package com.customsmithing.util;

import com.customsmithing.data.ItemStats;
import com.customsmithing.data.ProcessStage;
import com.customsmithing.data.Quality;
import com.customsmithing.data.SmithingKeys;
import com.customsmithing.item.PieceData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    private final ItemStack stack;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.stack = new ItemStack(material);
        this.meta = stack.getItemMeta();
    }

    public ItemBuilder(ItemStack base) {
        this.stack = base.clone();
        this.meta = stack.getItemMeta();
    }

    public ItemBuilder name(String name) {
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        List<String> colored = new ArrayList<>();
        for (String l : lines) colored.add(ChatColor.translateAlternateColorCodes('&', l));
        meta.setLore(colored);
        return this;
    }

    public ItemBuilder amount(int amount) {
        stack.setAmount(amount);
        return this;
    }

    public ItemBuilder markSmithingItem() {
        meta.getPersistentDataContainer().set(SmithingKeys.IS_SMITHING_ITEM, PersistentDataType.BYTE, (byte) 1);
        return this;
    }

    public ItemBuilder recipeId(String id) {
        meta.getPersistentDataContainer().set(SmithingKeys.RECIPE_ID, PersistentDataType.STRING, id);
        return this;
    }

    /** Marca el item como pieza intermedia: etapa, material base y puntaje de calidad. */
    public ItemBuilder piece(ProcessStage stage, String baseKey, double score) {
        PieceData.write(meta, stage, baseKey, score);
        return this;
    }

    public ItemBuilder blueprintFor(String recipeId) {
        meta.getPersistentDataContainer().set(SmithingKeys.BLUEPRINT_RECIPE, PersistentDataType.STRING, recipeId);
        return this;
    }

    /** Aplica calidad + stats ocultos al item y actualiza el lore visual. */
    public ItemBuilder applyQualityAndStats(Quality quality, ItemStats stats) {
        var pdc = meta.getPersistentDataContainer();
        pdc.set(SmithingKeys.QUALITY, PersistentDataType.STRING, quality.name());
        pdc.set(SmithingKeys.STAT_CRIT, PersistentDataType.DOUBLE, stats.getCritChance());
        pdc.set(SmithingKeys.STAT_LIFESTEAL, PersistentDataType.DOUBLE, stats.getLifesteal());
        pdc.set(SmithingKeys.STAT_ATTACK_SPEED, PersistentDataType.DOUBLE, stats.getAttackSpeedBonus());
        pdc.set(SmithingKeys.STAT_ELEMENTAL_TYPE, PersistentDataType.STRING, stats.getElementalType());
        pdc.set(SmithingKeys.STAT_ELEMENTAL_DAMAGE, PersistentDataType.DOUBLE, stats.getElementalDamage());
        pdc.set(SmithingKeys.STAT_RESISTANCE, PersistentDataType.DOUBLE, stats.getResistance());
        pdc.set(SmithingKeys.STAT_BONUS_DAMAGE, PersistentDataType.DOUBLE, stats.getBonusDamage());

        List<String> lore = new ArrayList<>();
        lore.add(quality.formatted());
        lore.add(ChatColor.DARK_GRAY + flavorFor(quality));
        lore.add(" ");
        if (stats.getCritChance() > 0) lore.add(ChatColor.YELLOW + "💥 Crítico: " + ChatColor.WHITE + round(stats.getCritChance()) + "%" + ChatColor.DARK_GRAY + "  (chance de golpe doble)");
        if (stats.getLifesteal() > 0) lore.add(ChatColor.RED + "❤ Robo de vida: " + ChatColor.WHITE + round(stats.getLifesteal()) + "%" + ChatColor.DARK_GRAY + "  (cura al golpear)");
        if (stats.getAttackSpeedBonus() > 0) lore.add(ChatColor.AQUA + "⚡ Vel. ataque: " + ChatColor.WHITE + "+" + round(stats.getAttackSpeedBonus()) + "%");
        if (!stats.getElementalType().equals("NINGUNO")) lore.add(ChatColor.LIGHT_PURPLE + "🔥 Daño " + stats.getElementalType() + ": " + ChatColor.WHITE + round(stats.getElementalDamage()) + ChatColor.DARK_GRAY + "  (extra)");
        if (stats.getBonusDamage() > 0) lore.add(ChatColor.GOLD + "⚔ Daño extra del material: " + ChatColor.WHITE + "+" + round(stats.getBonusDamage()));
        if (stats.getResistance() > 0) lore.add(ChatColor.GREEN + "🛡 Resistencia especial: " + ChatColor.WHITE + round(stats.getResistance()) + "%" + ChatColor.DARK_GRAY + "  (reduce daño recibido)");

        boolean isArmorPiece = stack.getType().name().contains("HELMET") || stack.getType().name().contains("CHESTPLATE")
                || stack.getType().name().contains("LEGGINGS") || stack.getType().name().contains("BOOTS");
        if (isArmorPiece) {
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Usa las 4 piezas de un set forjado por ti");
            lore.add(ChatColor.GRAY + "para un bonus extra de regeneración/resistencia.");
        }

        if (com.customsmithing.CustomSmithing.getInstance() != null
                && com.customsmithing.CustomSmithing.getInstance().getConfig().getBoolean("durabilidad.activar", true)) {
            lore.add(" ");
            lore.add(ChatColor.DARK_GRAY + "Más calidad = se rompe más lento");
        }

        // Solo se reemplaza el bloque propio; el resto del lore (firma, otros plugins) se conserva.
        List<String> existing = meta.getLore();
        meta.setLore(LoreBlock.apply(existing, lore, isLegacyLore(existing)));
        return this;
    }

    /** Lore de un item antiguo (sin marcas): empieza por la línea de calidad que ponía este plugin. */
    private static boolean isLegacyLore(List<String> existing) {
        if (existing == null || existing.isEmpty() || LoreBlock.hasBlock(existing)) return false;
        for (Quality q : Quality.values()) {
            if (existing.get(0).equals(q.formatted())) return true;
        }
        return false;
    }

    private String flavorFor(Quality quality) {
        return switch (quality) {
            case COMUN -> "Un trabajo honesto, sin lujos.";
            case RARO -> "Terminado con más cuidado de lo normal.";
            case EPICO -> "Se nota el pulso firme de quien lo forjó.";
            case LEGENDARIO -> "Pocos herreros logran algo así.";
        };
    }

    private String round(double v) {
        return Text.fmt1(v);
    }

    public ItemStack build() {
        stack.setItemMeta(meta);
        return stack;
    }
}
