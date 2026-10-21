package com.customsmithing.data;

import com.customsmithing.CustomSmithing;
import org.bukkit.NamespacedKey;

/**
 * Todas las claves de PersistentDataContainer usadas por el plugin,
 * centralizadas para evitar errores de tipeo y duplicados.
 *
 * IMPORTANTE: las claves se construyen dentro de init(), DESPUES de que
 * el campo "plugin" ya tiene valor. Si se construyeran como
 * "static final ... = key(...)" directamente en la declaracion del campo,
 * Java las inicializaria en el momento en que la clase se carga (que ocurre
 * ANTES de que init() asigne "plugin"), causando un NamespacedKey(null, ...)
 * y una excepcion al arrancar el plugin.
 */
public class SmithingKeys {

    private static CustomSmithing plugin;

    public static NamespacedKey QUALITY;
    public static NamespacedKey RECIPE_ID;
    public static NamespacedKey PROCESS_STATE;
    public static NamespacedKey QUALITY_SCORE;
    public static NamespacedKey BASE_MATERIAL;

    public static NamespacedKey STAT_CRIT;
    public static NamespacedKey STAT_LIFESTEAL;
    public static NamespacedKey STAT_ATTACK_SPEED;
    public static NamespacedKey STAT_ELEMENTAL_TYPE;
    public static NamespacedKey STAT_ELEMENTAL_DAMAGE;
    public static NamespacedKey STAT_RESISTANCE;
    public static NamespacedKey STAT_BONUS_DAMAGE;

    public static NamespacedKey BLUEPRINT_RECIPE;
    public static NamespacedKey IS_SMITHING_ITEM;
    public static NamespacedKey GOGGLES_ITEM;
    public static NamespacedKey MAGIC_BOOK_ENCHANT;
    public static NamespacedKey MAGIC_BOOK_LEVEL;
    public static NamespacedKey CUSTOM_MINERAL_ID;
    public static NamespacedKey BOSS_ID;
    public static NamespacedKey MOLD_SHAPE;
    public static NamespacedKey MOLD_LOADED_MATERIAL;
    public static NamespacedKey MOLD_USES;
    public static NamespacedKey REFORGES;
    public static NamespacedKey TOOL_TYPE;
    public static NamespacedKey TOOL_USES;
    /** Etiqueta propia del plugin para items custom que no son minerales (ver ItemRef). */
    public static NamespacedKey REF_TAG;

    public static void init(CustomSmithing pl) {
        plugin = pl;

        QUALITY = key("quality");
        RECIPE_ID = key("recipe_id");
        PROCESS_STATE = key("process_state");
        QUALITY_SCORE = key("quality_score");
        BASE_MATERIAL = key("base_material");

        STAT_CRIT = key("stat_crit");
        STAT_BONUS_DAMAGE = key("stat_bonus_damage");
        STAT_LIFESTEAL = key("stat_lifesteal");
        STAT_ATTACK_SPEED = key("stat_attack_speed");
        STAT_ELEMENTAL_TYPE = key("stat_elemental_type");
        STAT_ELEMENTAL_DAMAGE = key("stat_elemental_damage");
        STAT_RESISTANCE = key("stat_resistance");

        BLUEPRINT_RECIPE = key("blueprint_recipe");
        IS_SMITHING_ITEM = key("is_smithing_item");
        GOGGLES_ITEM = key("goggles_item");
        MAGIC_BOOK_ENCHANT = key("magic_book_enchant");
        MAGIC_BOOK_LEVEL = key("magic_book_level");
        CUSTOM_MINERAL_ID = key("custom_mineral_id");
        BOSS_ID = key("boss_id");
        MOLD_SHAPE = key("mold_shape");
        MOLD_LOADED_MATERIAL = key("mold_loaded_material");
        MOLD_USES = key("mold_uses");
        REFORGES = key("reforges");
        TOOL_TYPE = key("tool_type");
        TOOL_USES = key("tool_uses");
        REF_TAG = key("ref_tag");
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(plugin, name);
    }
}
