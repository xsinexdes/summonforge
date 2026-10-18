package com.summonforge.status;

import com.summonforge.data.DefinitionStore;
import com.summonforge.stats.Element;
import org.bukkit.Material;

public final class StatusPresets {
    private StatusPresets() {}

    public static void install(DefinitionStore<StatusDef> store) {
        mk(store, "sangrado", "<red>Sangrado", Material.REDSTONE, Element.NONE, 1.5, 5, -1, -1, false, "CRIT");
        mk(store, "quemadura", "<gold>Quemadura", Material.BLAZE_POWDER, Element.FIRE, 2.0, 3, -1, -1, false, "FLAME");
        mk(store, "congelacion", "<aqua>Congelación", Material.BLUE_ICE, Element.ICE, 0.5, 3, 2, -1, false, "SNOWFLAKE");
        mk(store, "veneno", "<dark_green>Veneno", Material.SPIDER_EYE, Element.POISON, 1.0, 5, -1, -1, false, "WITCH");
        mk(store, "aturdimiento", "<yellow>Aturdimiento", Material.GOLD_NUGGET, Element.NONE, 0, 1, 6, 1, true, "CRIT");
        mk(store, "ralentizacion", "<gray>Ralentización", Material.COBWEB, Element.NONE, 0, 1, 1, -1, false, "CLOUD");
        mk(store, "debilidad", "<dark_purple>Debilidad", Material.FERMENTED_SPIDER_EYE, Element.SHADOW, 0, 1, -1, 1, false, "SMOKE");
    }

    private static void mk(DefinitionStore<StatusDef> store, String id, String name, Material icon, Element el,
                           double dot, int stacks, int slow, int weak, boolean stun, String particle) {
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
        store.save(d);
    }
}
