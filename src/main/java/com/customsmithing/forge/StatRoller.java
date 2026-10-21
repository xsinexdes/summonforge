package com.customsmithing.forge;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.ItemStats;
import com.customsmithing.data.Passives;
import com.customsmithing.data.PlayerData;
import com.customsmithing.data.Quality;
import com.customsmithing.material.MaterialRegistry;
import com.customsmithing.recipes.SmithingRecipe;
import com.customsmithing.util.Text;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

/** Tirada de stats de un objeto fabricado (Ensamblaje y Reforja comparten exactamente la misma regla). */
public final class StatRoller {

    private static final Random RANDOM = new Random();

    private StatRoller() {
    }

    public static ItemStats roll(CustomSmithing plugin, Player player, SmithingRecipe recipe, Quality quality) {
        ItemStats stats = new ItemStats();
        double qualityFactor = plugin.getQualityConfig().multiplier(quality);

        // Nodos pasivos del árbol de habilidades: solo cuentan si fabricas algo de TU especialización.
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data.getSpecialization() == recipe.getSpecialization()) {
            List<Integer> levels = plugin.getConfig().getIntegerList("habilidades.niveles");
            if (levels.isEmpty()) levels = List.of(5, 10, 15);
            qualityFactor *= Passives.multiplier(Passives.tiersReached(data.getLevel(), levels),
                    plugin.getConfig().getDouble("habilidades.bonus_por_nivel", 10));
        }

        switch (recipe.getSpecialization()) {
            case ESPADAS -> {
                stats.setCritChance(Text.round1(RANDOM.nextDouble() * 15 * qualityFactor));
                stats.setAttackSpeedBonus(Text.round1(RANDOM.nextDouble() * 10 * qualityFactor));
                if (quality == Quality.EPICO || quality == Quality.LEGENDARIO) {
                    stats.setLifesteal(Text.round1(RANDOM.nextDouble() * 8 * qualityFactor));
                }
            }
            case ARMADURAS -> stats.setResistance(Text.round1(RANDOM.nextDouble() * 20 * qualityFactor));
            case HERRAMIENTAS -> stats.setAttackSpeedBonus(Text.round1(RANDOM.nextDouble() * 12 * qualityFactor));
            case FORJA_MAGICA -> {
                String[] elementos = {"FUEGO", "HIELO", "VENENO", "RAYO"};
                stats.setElementalType(elementos[RANDOM.nextInt(elementos.length)]);
                stats.setElementalDamage(Text.round1(2 + RANDOM.nextDouble() * 6 * qualityFactor));
            }
        }
        applyMaterialBonuses(plugin, recipe, quality, stats);
        return stats;
    }

    /**
     * Suma lo que aporta el material de la pieza (materiales.yml o mineral custom): resistencia a las
     * armaduras y daño extra al resto. Con calidad.escalar_bonos_material el multiplicador de calidad también lo escala.
     */
    private static void applyMaterialBonuses(CustomSmithing plugin, SmithingRecipe recipe, Quality quality, ItemStats stats) {
        if (recipe.getMetalBaseKey() == null) return;
        MaterialRegistry.Entry entry = plugin.getMaterialRegistry().forBaseKey(recipe.getMetalBaseKey());
        if (entry == null) return;
        double scale = plugin.getConfig().getBoolean("calidad.escalar_bonos_material", true)
                ? plugin.getQualityConfig().multiplier(quality) : 1.0;
        String name = recipe.getResultMaterial().name();
        boolean armor = name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS");
        if (armor) stats.setResistance(Math.min(100, stats.getResistance() + Text.round1(entry.bonusResistance() * scale)));
        else stats.setBonusDamage(Text.round1(stats.getBonusDamage() + entry.bonusDamage() * scale));
    }
}
