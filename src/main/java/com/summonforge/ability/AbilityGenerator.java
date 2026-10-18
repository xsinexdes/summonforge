package com.summonforge.ability;

import com.summonforge.data.DefinitionStore;
import com.summonforge.stats.Element;
import org.bukkit.Material;

/**
 * Genera la matriz elemento x forma (10 elementos x 8 formas). Son variantes sobre el motor genérico:
 * se pueden retocar una a una desde el editor. Ids: forma_elemento (ej. nova_fuego, proyectil_hielo).
 */
public final class AbilityGenerator {
    private AbilityGenerator() {}

    private record El(String id, String label, Element element, String particle, String sound, Material icon, String status) {}

    private record Form(String id, String label, Shape shape, Effect effect, double dmg, double radius, double range,
                        double angle, double kb, int cd, int warn, double speed, boolean status) {}

    private static final El[] ELEMENTS = {
            new El("fuego", "Fuego", Element.FIRE, "FLAME", "entity.blaze.shoot", Material.BLAZE_POWDER, "quemadura"),
            new El("agua", "Agua", Element.WATER, "SPLASH", "entity.player.splash", Material.HEART_OF_THE_SEA, "ralentizacion"),
            new El("tierra", "Tierra", Element.EARTH, "CRIT", "block.stone.break", Material.COBBLESTONE, "ralentizacion"),
            new El("viento", "Viento", Element.WIND, "CLOUD", "entity.phantom.flap", Material.FEATHER, ""),
            new El("rayo", "Rayo", Element.LIGHTNING, "ELECTRIC_SPARK", "entity.lightning_bolt.impact", Material.LIGHTNING_ROD, "aturdimiento"),
            new El("hielo", "Hielo", Element.ICE, "SNOWFLAKE", "block.glass.break", Material.BLUE_ICE, "congelacion"),
            new El("veneno", "Veneno", Element.POISON, "WITCH", "entity.splash_potion.break", Material.SPIDER_EYE, "veneno"),
            new El("sombra", "Sombra", Element.SHADOW, "SMOKE", "entity.wither.shoot", Material.COAL, "debilidad"),
            new El("luz", "Luz", Element.LIGHT, "END_ROD", "block.beacon.activate", Material.GLOWSTONE_DUST, "debilidad"),
            new El("vacio", "Vacío", Element.VOID, "PORTAL", "entity.enderman.teleport", Material.ENDER_PEARL, "debilidad"),
    };

    private static final Form[] FORMS = {
            new Form("nova", "Nova", Shape.CIRCLE, Effect.DAMAGE, 7, 5, 0, 0, 0.6, 120, 0, 1, true),
            new Form("cono", "Cono", Shape.CONE, Effect.DAMAGE, 7, 0, 6, 80, 0.4, 90, 0, 1, true),
            new Form("rayo", "Rayo", Shape.LINE, Effect.DAMAGE, 9, 0, 16, 0, 0.3, 110, 0, 1, false),
            new Form("proyectil", "Proyectil", Shape.PROJECTILE, Effect.DAMAGE, 10, 2, 28, 0, 0.5, 90, 0, 1.4, true),
            new Form("golpe", "Golpe", Shape.CIRCLE, Effect.DAMAGE, 12, 3, 0, 0, 1.0, 70, 0, 1, false),
            new Form("nova_mayor", "Nova Mayor", Shape.CIRCLE, Effect.DAMAGE, 14, 8, 0, 0, 1.2, 240, 20, 1, true),
            new Form("lanza_mayor", "Lanza Mayor", Shape.PROJECTILE, Effect.DAMAGE, 20, 4, 35, 0, 0.8, 200, 0, 1.0, true),
            new Form("maldicion", "Maldición", Shape.CIRCLE, Effect.STATUS, 0, 7, 0, 0, 0, 220, 0, 1, true),
    };

    /** Crea las que no existan (no pisa lo ya editado). */
    public static void install(DefinitionStore<AbilityDef> store) {
        for (El el : ELEMENTS) {
            for (Form f : FORMS) {
                if (f.effect() == Effect.STATUS && el.status().isEmpty()) continue;
                String id = f.id() + "_" + el.id();
                if (store.has(id)) continue;
                AbilityDef d = new AbilityDef(id);
                d.name = f.label() + " de " + el.label();
                d.description = f.label() + " elemental (" + el.element().label + ").";
                d.icon = el.icon();
                d.shape = f.shape();
                d.effect = f.effect();
                d.element = el.element();
                d.damage = f.dmg();
                d.radius = f.radius();
                d.range = f.range();
                d.angle = f.angle() > 0 ? f.angle() : 90;
                d.knockback = f.kb();
                d.cooldownTicks = f.cd();
                d.warnTicks = f.warn();
                d.speed = f.speed();
                d.particle = el.particle();
                d.sound = el.sound();
                d.status = f.status() ? el.status() : "";
                d.statusTicks = 100;
                store.save(d);
            }
        }
    }
}
