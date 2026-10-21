package com.summonforge.status;

import com.summonforge.data.DefinitionStore;
import com.summonforge.stats.Element;
import org.bukkit.Material;

public final class StatusPresets {
    private StatusPresets() {}

    public static void install(DefinitionStore<StatusDef> store) {
        mk(store, "sangrado", "<red>Sangrado", Material.REDSTONE, Element.NONE, 1.5, 5, -1, -1, false, "CRIT", "");
        mk(store, "quemadura", "<gold>Quemadura", Material.BLAZE_POWDER, Element.FIRE, 2.0, 3, -1, -1, false, "FLAME", "");
        mk(store, "congelacion", "<aqua>Congelación", Material.BLUE_ICE, Element.ICE, 0.5, 3, 2, -1, false, "SNOWFLAKE", "");
        mk(store, "veneno", "<dark_green>Veneno", Material.SPIDER_EYE, Element.POISON, 1.0, 5, -1, -1, false, "WITCH", "");
        mk(store, "aturdimiento", "<yellow>Aturdimiento", Material.GOLD_NUGGET, Element.NONE, 0, 1, 6, 1, true, "CRIT", "");
        mk(store, "ralentizacion", "<gray>Ralentización", Material.COBWEB, Element.NONE, 0, 1, 1, -1, false, "CLOUD", "");
        mk(store, "debilidad", "<dark_purple>Debilidad", Material.FERMENTED_SPIDER_EYE, Element.SHADOW, 0, 1, -1, 1, false, "SMOKE", "");
        // estados con efecto de juego propio
        mk(store, "silencio", "<light_purple>Silencio", Material.NOTE_BLOCK, Element.NONE, 0, 1, -1, -1, false, "ENCHANT", "");
        mk(store, "reflejo", "<aqua>Reflejo", Material.GLASS, Element.LIGHT, 0, 1, -1, -1, false, "END_ROD", "");
        mk(store, "paralisis", "<yellow>Parálisis", Material.LIGHTNING_ROD, Element.LIGHTNING, 0, 1, -1, -1, true, "ELECTRIC_SPARK", "");
        mk(store, "ceguera", "<dark_gray>Ceguera", Material.INK_SAC, Element.SHADOW, 0, 1, -1, -1, false, "SMOKE", "blindness:0");
        mk(store, "pesadilla", "<dark_purple>Pesadilla", Material.WITHER_ROSE, Element.SHADOW, 0, 1, 1, -1, false, "SMOKE", "blindness:0,nausea:0");
        mk(store, "hambre", "<green>Hambre", Material.ROTTEN_FLESH, Element.NONE, 0, 1, -1, -1, false, "WITCH", "hunger:3");
        mk(store, "raices", "<dark_green>Raíces", Material.VINE, Element.EARTH, 0, 1, 7, -1, false, "CRIT", "");
        mk(store, "sueno", "<blue>Sueño profundo", Material.BLUE_BED, Element.SHADOW, 0, 1, -1, -1, true, "ENCHANT", "blindness:0");
        mk(store, "quemadura_negra", "<dark_gray>Llamas negras", Material.SOUL_TORCH, Element.FIRE, 3.0, 5, -1, -1, false, "SOUL_FIRE_FLAME", "");
    }

    private static void mk(DefinitionStore<StatusDef> store, String id, String name, Material icon, Element el,
                           double dot, int stacks, int slow, int weak, boolean stun, String particle, String effects) {
        if (store.has(id)) return;
        StatusDef d = new StatusDef(id);
        d.name = name;
        d.icon = icon;
        d.element = el;
        d.dotDamage = dot;
        d.maxStacks = stacks;
        d.slowAmp = slow;
        d.weakAmp = weak;
        d.stun = stun;
        d.particle = particle;
        d.effects = effects;
        store.save(d);
    }
}
