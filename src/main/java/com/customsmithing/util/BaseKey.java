package com.customsmithing.util;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.SmithingKeys;
import com.customsmithing.recipes.SmithingRecipe;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Unifica materiales vanilla y minerales 100% custom bajo una sola "clave base"
 * (String) para que todo el circuito de Calentado/Golpeado/Enfriado/Tallado/Tejido
 * pueda tratarlos exactamente igual sin importar cual es cual.
 *
 * Formato: "MAT:IRON_INGOT" para vanilla, "MIN:adamantita" para custom.
 */
public class BaseKey {

    public static String ofMaterial(Material material) {
        return "MAT:" + material.name();
    }

    public static String ofMineral(String mineralId) {
        return "MIN:" + mineralId;
    }

    /** Clave de una pieza con forma de molde (pico, hacha, etc) + un material interno (MAT:... o MIN:...). */
    public static String ofMold(com.customsmithing.data.MoldType shape, String innerKey) {
        return "MLD:" + shape.name() + ":" + innerKey;
    }

    /** Calcula la clave base del item que el jugador tiene en la mano, si aplica. */
    public static String ofItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        if (item.hasItemMeta()) {
            String mineralId = item.getItemMeta().getPersistentDataContainer().get(SmithingKeys.CUSTOM_MINERAL_ID, PersistentDataType.STRING);
            if (mineralId != null) return ofMineral(mineralId);
        }
        return ofMaterial(item.getType());
    }

    public static boolean matchesMetal(SmithingRecipe recipe, String baseKey) {
        String recipeKey = recipe.getMetalBaseKey();
        return recipeKey != null && recipeKey.equals(baseKey);
    }

    public static boolean matchesWood(SmithingRecipe recipe, String baseKey) {
        if (recipe.getWoodMaterial() == null) return false;
        return ofMaterial(recipe.getWoodMaterial()).equals(baseKey);
    }

    /** Nombre lindo para mostrar en lores y mensajes. */
    public static String displayName(CustomSmithing plugin, String baseKey) {
        if (baseKey == null) return "?";
        if (baseKey.startsWith("MLD:")) {
            String rest = baseKey.substring(4);
            int idx = rest.indexOf(':');
            if (idx < 0) return rest;
            String shapeName = rest.substring(0, idx);
            String innerKey = rest.substring(idx + 1);
            try {
                com.customsmithing.data.MoldType shape = com.customsmithing.data.MoldType.valueOf(shapeName);
                return shape.getLabel() + " de " + displayName(plugin, innerKey);
            } catch (IllegalArgumentException e) {
                return displayName(plugin, innerKey);
            }
        }
        if (baseKey.startsWith("MIN:")) {
            String id = baseKey.substring(4);
            var mineral = plugin.getCustomMineralManager().get(id);
            return mineral != null ? mineral.getDisplayName() : id;
        }
        if (baseKey.startsWith("MAT:")) {
            Material mat = Material.matchMaterial(baseKey.substring(4));
            return mat != null ? Text.prettify(mat.name()) : baseKey;
        }
        return baseKey;
    }

    /** Icono representativo. Si es un molde + metal vanilla conocido, intenta usar la herramienta real (ej: IRON_PICKAXE). */
    public static Material iconFor(CustomSmithing plugin, String baseKey, Material fallback) {
        if (baseKey == null) return fallback;
        if (baseKey.startsWith("MLD:")) {
            String rest = baseKey.substring(4);
            int idx = rest.indexOf(':');
            if (idx >= 0) {
                String shapeName = rest.substring(0, idx);
                String innerKey = rest.substring(idx + 1);
                try {
                    com.customsmithing.data.MoldType shape = com.customsmithing.data.MoldType.valueOf(shapeName);
                    Material smart = smartToolIcon(shape, innerKey);
                    if (smart != null) return smart;
                } catch (IllegalArgumentException ignored) {
                    // forma desconocida: se usa el icono del material interno
                }
                return iconFor(plugin, innerKey, fallback);
            }
            return fallback;
        }
        if (baseKey.startsWith("MAT:")) {
            Material mat = Material.matchMaterial(baseKey.substring(4));
            if (mat != null && mat.isItem() && !mat.isAir()) return mat;
        }
        if (baseKey.startsWith("MIN:")) {
            var mineral = plugin.getCustomMineralManager().get(baseKey.substring(4));
            if (mineral != null) return mineral.getIconMaterial();
        }
        return fallback;
    }

    private static Material smartToolIcon(com.customsmithing.data.MoldType shape, String innerKey) {
        if (!innerKey.startsWith("MAT:")) return null; // mineral custom: no hay herramienta vanilla real
        String matName = innerKey.substring(4);
        String prefix = switch (matName) {
            case "IRON_INGOT" -> "IRON";
            case "GOLD_INGOT" -> "GOLDEN";
            case "DIAMOND" -> "DIAMOND";
            case "NETHERITE_INGOT" -> "NETHERITE";
            case "COPPER_INGOT" -> null; // no existe herramienta de cobre en vanilla
            default -> null;
        };
        if (prefix == null) return null;
        String suffix = switch (shape) {
            case PICO -> "_PICKAXE";
            case HACHA -> "_AXE";
            case ESPADA -> "_SWORD";
            case PALA -> "_SHOVEL";
            case AZADA -> "_HOE";
            case CASCO -> "_HELMET";
            case PETO -> "_CHESTPLATE";
            case PANTALON -> "_LEGGINGS";
            case BOTAS -> "_BOOTS";
        };
        return Material.matchMaterial(prefix + suffix);
    }
}
