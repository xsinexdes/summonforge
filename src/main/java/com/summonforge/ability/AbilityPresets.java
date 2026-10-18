package com.summonforge.ability;

import com.summonforge.data.DefinitionStore;
import com.summonforge.stats.Element;
import org.bukkit.Material;

/**
 * Habilidades prediseñadas expresables con el motor data-driven (forma + efecto + estado).
 * Las que necesitan lógica especial (teletransporte, clones, dimensiones...) irán en clases propias.
 */
public final class AbilityPresets {
    private AbilityPresets() {}

    private static final Shape C = Shape.CIRCLE, K = Shape.CONE, L = Shape.LINE, P = Shape.PROJECTILE;
    private static final Effect DMG = Effect.DAMAGE, HEAL = Effect.HEAL, PUSH = Effect.PUSH, PULL = Effect.PULL, STAT = Effect.STATUS;
    private static final Element N = Element.NONE, FIRE = Element.FIRE, ICE = Element.ICE, LIG = Element.LIGHTNING,
            POI = Element.POISON, SHA = Element.SHADOW, LGT = Element.LIGHT, WIND = Element.WIND, EAR = Element.EARTH, VOID = Element.VOID;

    /** Crea las que no existan (no pisa lo que el admin ya editó). */
    public static void install(DefinitionStore<AbilityDef> st) {
        // ---- cuerpo a cuerpo / área ----
        mk(st, "tajo_giratorio", "Tajo Giratorio", "Tajo en 360° alrededor del lanzador.", Material.IRON_SWORD, C, DMG, N, 8, 3.5, 0, 0, 0.8, 80, 0, "SWEEP_ATTACK", "entity.player.attack.sweep", "");
        mk(st, "pisoton_sismico", "Pisotón Sísmico", "Golpe al suelo con aviso previo.", Material.ANVIL, C, DMG, EAR, 10, 6, 0, 0, 1.4, 160, 20, "CLOUD", "entity.generic.explode", "");
        mk(st, "zarpazo_triple", "Zarpazo Triple", "Zarpazo frontal en cono.", Material.IRON_AXE, K, DMG, N, 7, 0, 5, 100, 0.4, 60, 0, "SWEEP_ATTACK", "entity.player.attack.sweep", "");
        mk(st, "corte_vertical", "Corte Vertical", "Tajo estrecho que hace sangrar.", Material.NETHERITE_SWORD, K, DMG, N, 9, 0, 4, 60, 0.5, 50, 0, "SWEEP_ATTACK", "entity.player.attack.sweep", "sangrado");
        mk(st, "remolino_cortante", "Remolino Cortante", "Giro de cuchillas que provoca sangrado.", Material.SHEARS, C, DMG, N, 5, 5, 0, 0, 0.6, 60, 0, "SWEEP_ATTACK", "entity.player.attack.sweep", "sangrado");
        mk(st, "golpe_aplastante", "Golpe Aplastante", "Martillazo que aturde.", Material.IRON_BLOCK, C, DMG, EAR, 14, 2.5, 0, 0, 0.3, 120, 15, "CLOUD", "entity.generic.explode", "aturdimiento");
        mk(st, "sacudida_terrestre", "Sacudida Terrestre", "Onda amplia que ralentiza.", Material.COARSE_DIRT, C, DMG, EAR, 6, 9, 0, 0, 1.0, 220, 20, "CLOUD", "entity.generic.explode", "ralentizacion");
        mk(st, "onda_de_impacto", "Onda de Impacto", "Empuja a todos los enemigos cercanos.", Material.SHIELD, C, PUSH, WIND, 0, 7, 0, 0, 2.2, 200, 0, "CLOUD", "entity.generic.explode", "");
        mk(st, "grito_aturdidor", "Grito Aturdidor", "Grito que aturde alrededor.", Material.BELL, C, DMG, N, 2, 8, 0, 0, 0.2, 260, 10, "CRIT", "entity.ender_dragon.growl", "aturdimiento");
        mk(st, "vortice_gravitatorio", "Vórtice Gravitatorio", "Atrae a los enemigos hacia el centro.", Material.ENDER_EYE, C, PULL, VOID, 0, 10, 0, 0, 1.5, 240, 0, "PORTAL", "entity.enderman.teleport", "");
        // ---- fuego ----
        mk(st, "bola_de_fuego", "Bola de Fuego", "Proyectil explosivo de fuego.", Material.FIRE_CHARGE, P, DMG, FIRE, 12, 3, 30, 0, 1.2, 100, 0, "FLAME", "entity.blaze.shoot", "quemadura");
        mk(st, "lluvia_de_brasas", "Lluvia de Brasas", "Zona de fuego con aviso previo.", Material.MAGMA_BLOCK, C, DMG, FIRE, 6, 7, 0, 0, 0.3, 200, 30, "FLAME", "entity.blaze.shoot", "quemadura");
        mk(st, "aliento_de_fuego", "Aliento de Fuego", "Cono de llamas.", Material.BLAZE_POWDER, K, DMG, FIRE, 5, 0, 7, 60, 0.3, 70, 0, "FLAME", "entity.blaze.shoot", "quemadura");
        mk(st, "muro_de_llamas", "Muro de Llamas", "Línea de fuego frente al lanzador.", Material.MAGMA_CREAM, L, DMG, FIRE, 4, 0, 12, 0, 0.2, 120, 0, "FLAME", "entity.blaze.shoot", "quemadura");
        mk(st, "explosion_igneo", "Explosión Ígnea", "Detonación grande con aviso.", Material.TNT, C, DMG, FIRE, 16, 5, 0, 0, 1.6, 240, 25, "EXPLOSION", "entity.generic.explode", "quemadura");
        mk(st, "meteoro", "Meteoro", "Proyectil lento y devastador.", Material.FIRE_CHARGE, P, DMG, FIRE, 20, 5, 40, 0, 0.8, 300, 0, "FLAME", "entity.generic.explode", "quemadura");
        // ---- hielo ----
        mk(st, "lanza_de_hielo", "Lanza de Hielo", "Proyectil rápido de hielo.", Material.PACKED_ICE, P, DMG, ICE, 9, 0, 35, 0, 1.6, 70, 0, "SNOWFLAKE", "entity.snowball.throw", "ralentizacion");
        mk(st, "nova_gelida", "Nova Gélida", "Explosión de hielo que congela.", Material.BLUE_ICE, C, DMG, ICE, 7, 6, 0, 0, 0.5, 180, 0, "SNOWFLAKE", "block.glass.break", "congelacion");
        mk(st, "aliento_helado", "Aliento Helado", "Cono gélido.", Material.SNOWBALL, K, DMG, ICE, 5, 0, 7, 70, 0.2, 80, 0, "SNOWFLAKE", "entity.snowball.throw", "congelacion");
        mk(st, "ventisca", "Ventisca", "Tormenta de nieve que ralentiza.", Material.SNOW_BLOCK, C, DMG, ICE, 3, 10, 0, 0, 0.1, 300, 20, "SNOWFLAKE", "entity.snowball.throw", "ralentizacion");
        // ---- rayo ----
        mk(st, "rayo_lineal", "Rayo Lineal", "Rayo instantáneo en línea recta.", Material.LIGHTNING_ROD, L, DMG, LIG, 11, 0, 18, 0, 0.3, 140, 0, "ELECTRIC_SPARK", "entity.lightning_bolt.impact", "");
        mk(st, "cadena_de_chispas", "Cadena de Chispas", "Descarga alrededor del lanzador.", Material.REDSTONE, C, DMG, LIG, 6, 8, 0, 0, 0.3, 120, 0, "ELECTRIC_SPARK", "entity.lightning_bolt.impact", "");
        mk(st, "tormenta_electrica", "Tormenta Eléctrica", "Gran descarga con aviso que aturde.", Material.LIGHTNING_ROD, C, DMG, LIG, 10, 9, 0, 0, 0.4, 260, 20, "ELECTRIC_SPARK", "entity.lightning_bolt.thunder", "aturdimiento");
        mk(st, "flecha_relampago", "Flecha Relámpago", "Proyectil veloz de rayo.", Material.ARROW, P, DMG, LIG, 10, 0, 40, 0, 2.5, 60, 0, "ELECTRIC_SPARK", "entity.arrow.shoot", "");
        // ---- veneno ----
        mk(st, "nube_toxica", "Nube Tóxica", "Área que envenena.", Material.FERMENTED_SPIDER_EYE, C, DMG, POI, 2, 6, 0, 0, 0.1, 200, 0, "WITCH", "entity.splash_potion.break", "veneno");
        mk(st, "dardo_venenoso", "Dardo Venenoso", "Proyectil que envenena.", Material.SPIDER_EYE, P, DMG, POI, 5, 0, 30, 0, 1.8, 50, 0, "WITCH", "entity.arrow.shoot", "veneno");
        // ---- sombra ----
        mk(st, "zarpazo_sombrio", "Zarpazo Sombrío", "Cono oscuro que debilita.", Material.ECHO_SHARD, K, DMG, SHA, 9, 0, 5, 90, 0.3, 70, 0, "SMOKE", "entity.phantom.bite", "debilidad");
        mk(st, "pulso_sombrio", "Pulso Sombrío", "Onda de oscuridad que debilita.", Material.OBSIDIAN, C, DMG, SHA, 8, 6, 0, 0, 0.5, 150, 0, "SMOKE", "entity.wither.shoot", "debilidad");
        mk(st, "esfera_oscura", "Esfera Oscura", "Proyectil de sombra explosivo.", Material.COAL, P, DMG, SHA, 14, 3, 30, 0, 1.0, 120, 0, "SMOKE", "entity.wither.shoot", "debilidad");
        mk(st, "vacio_devorador", "Vacío Devorador", "Explosión del vacío con aviso.", Material.ENDER_PEARL, C, DMG, VOID, 18, 5, 0, 0, 0.0, 280, 25, "PORTAL", "entity.enderman.teleport", "");
        // ---- luz ----
        mk(st, "rayo_sagrado", "Rayo Sagrado", "Haz de luz en línea.", Material.GLOWSTONE_DUST, L, DMG, LGT, 10, 0, 20, 0, 0.2, 120, 0, "END_ROD", "block.beacon.activate", "");
        mk(st, "destello_cegador", "Destello Cegador", "Destello que debilita a los enemigos.", Material.SEA_LANTERN, C, STAT, LGT, 0, 7, 0, 0, 0, 200, 0, "END_ROD", "block.beacon.activate", "debilidad");
        mk(st, "curacion_en_area", "Curación en Área", "Cura al lanzador y a sus aliados cercanos.", Material.GOLDEN_APPLE, C, HEAL, LGT, 8, 6, 0, 0, 0, 300, 0, "HEART", "entity.player.levelup", "");
        mk(st, "curacion_menor", "Curación Menor", "Cura pequeña y rápida.", Material.APPLE, C, HEAL, LGT, 4, 4, 0, 0, 0, 150, 0, "HEART", "entity.player.levelup", "");
        mk(st, "regeneracion_mayor", "Regeneración Mayor", "Gran curación de grupo.", Material.ENCHANTED_GOLDEN_APPLE, C, HEAL, LGT, 14, 8, 0, 0, 0, 400, 10, "HEART", "entity.player.levelup", "");
        // ---- viento / tierra ----
        mk(st, "tajo_de_viento", "Tajo de Viento", "Cuchilla de aire a distancia.", Material.FEATHER, P, DMG, WIND, 7, 0, 20, 0, 0.3, 60, 0, "CLOUD", "entity.player.attack.sweep", "");
        mk(st, "torbellino", "Torbellino", "Lanza lejos a los enemigos.", Material.PHANTOM_MEMBRANE, C, PUSH, WIND, 0, 6, 0, 0, 2.5, 180, 0, "CLOUD", "entity.phantom.flap", "");
        mk(st, "rafaga", "Ráfaga", "Empuje frontal en cono.", Material.STRING, K, PUSH, WIND, 0, 0, 8, 80, 2.0, 90, 0, "CLOUD", "entity.phantom.flap", "");
        mk(st, "lanza_de_roca", "Lanza de Roca", "Proyectil de piedra.", Material.COBBLESTONE, P, DMG, EAR, 11, 0, 25, 0, 1.3, 80, 0, "CRIT", "block.stone.break", "");
        AbilityGenerator.install(st);
        SpecialPresets.install(st);
        mk(st, "kamui_limbo", "Kamui: Dimensión", "Envía a los jugadores enemigos al limbo unos segundos.", Material.ENDER_EYE, C, Effect.LIMBO, VOID, 0, 6, 0, 0, 0, 600, 20, "PORTAL", "entity.enderman.teleport", "");
        if (!st.has("kamui_fase")) {
            AbilityDef d = new AbilityDef("kamui_fase");
            d.name = "Kamui: Intangible";
            d.description = "Te vuelve invulnerable unos instantes.";
            d.icon = Material.PHANTOM_MEMBRANE;
            d.shape = C;
            d.effect = Effect.PHASE;
            d.element = VOID;
            d.damage = 0;
            d.radius = 1;
            d.cooldownTicks = 500;
            d.particle = "PORTAL";
            d.sound = "entity.enderman.teleport";
            d.statusTicks = 60;
            st.save(d);
        }
    }

    private static void mk(DefinitionStore<AbilityDef> store, String id, String name, String desc, Material icon,
                           Shape shape, Effect effect, Element element, double damage, double radius, double range,
                           double angle, double knockback, int cooldown, int warn, String particle, String sound, String status) {
        if (store.has(id)) return;
        AbilityDef d = new AbilityDef(id);
        d.name = name;
        d.description = desc;
        d.icon = icon;
        d.shape = shape;
        d.effect = effect;
        d.element = element;
        d.damage = damage;
        d.radius = radius;
        d.range = range;
        d.angle = angle > 0 ? angle : 90;
        d.knockback = knockback;
        d.cooldownTicks = cooldown;
        d.warnTicks = warn;
        d.particle = particle;
        d.sound = sound;
        d.status = status;
        d.statusTicks = 100;
        if (shape == Shape.PROJECTILE) d.speed = switch (id) {
            case "lanza_de_hielo" -> 1.6;
            case "flecha_relampago" -> 2.5;
            case "dardo_venenoso" -> 1.8;
            case "meteoro" -> 0.8;
            case "lanza_de_roca" -> 1.3;
            case "esfera_oscura" -> 1.0;
            default -> 1.2;
        };
        store.save(d);
    }
}
