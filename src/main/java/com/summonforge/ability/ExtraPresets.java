package com.summonforge.ability;

import com.summonforge.data.DefinitionStore;
import com.summonforge.stats.Element;
import org.bukkit.Material;

/** Habilidades "chetas" custom: dimensiones, ilusiones, control, y un pack pensado para las invocaciones. */
public final class ExtraPresets {
    private ExtraPresets() {}

    private static final Element N = Element.NONE, FIRE = Element.FIRE, EAR = Element.EARTH, WIND = Element.WIND,
            LIG = Element.LIGHTNING, SHA = Element.SHADOW, LGT = Element.LIGHT, VOID = Element.VOID;

    public static void install(DefinitionStore<AbilityDef> st) {
        // ---- dimensiones e ilusiones (viajan jugadores y mobs, "matar ahí") ----
        SpecialPresets.sp(st, "dimension_duelo", "Duelo Dimensional", Material.ENDER_EYE, VOID, 4, 0, 14, 900, 20, 300, 1, 1, 0, "PORTAL", "entity.enderman.teleport", "Tú y un enemigo a una arena aparte con daño continuo; se vuelve al morir uno.");
        SpecialPresets.sp(st, "kamui_objetivo", "Kamui: Enviar", Material.ENDER_PEARL, VOID, 0, 0, 12, 500, 10, 160, 1, 1, 0, "PORTAL", "entity.enderman.teleport", "Manda al enemigo más cercano a la dimensión Kamui.");
        SpecialPresets.sp(st, "kamui_ambos", "Kamui: Duelo en el Limbo", Material.ENDER_EYE, VOID, 0, 0, 12, 700, 10, 200, 1, 1, 0, "PORTAL", "entity.enderman.teleport", "Tú y el objetivo a la dimensión Kamui.");
        SpecialPresets.sp(st, "tsukuyomi", "Tsukuyomi", Material.ENDER_EYE, SHA, 6, 8, 0, 700, 20, 160, 3, 1, 0, "SMOKE", "entity.wither.spawn", "Atrapa a hasta 3 enemigos en una ilusión con tortura.");
        SpecialPresets.sp(st, "dominio_real", "Expansión de Dominio", Material.NETHER_STAR, VOID, 5, 10, 0, 1200, 30, 300, 6, 1, 0, "PORTAL", "entity.wither.spawn", "Arrastra a hasta 6 enemigos a tu dominio con daño continuo.");
        SpecialPresets.sp(st, "amaterasu", "Amaterasu", Material.SOUL_TORCH, FIRE, 0, 0, 20, 500, 0, 200, 1, 1, 0, "SOUL_FIRE_FLAME", "entity.blaze.shoot", "Fuego negro que persigue al objetivo.");
        SpecialPresets.sp(st, "reflejo", "Reflejo", Material.GLASS, LGT, 0, 1, 0, 500, 0, 120, 1, 1, 0, "END_ROD", "block.beacon.activate", "Devuelve el 30% del daño recibido.");
        // ---- utilidad y combate ----
        SpecialPresets.sp(st, "torreta", "Torreta", Material.DISPENSER, N, 6, 0, 12, 500, 0, 200, 1, 1, 0, "CRIT", "block.dispenser.dispense", "Coloca una torreta que dispara a los enemigos.");
        SpecialPresets.sp(st, "oleada_minions", "Oleada de Minions", Material.ZOMBIE_HEAD, N, 0, 0, 0, 600, 0, 300, 4, 1, 0, "SMOKE", "entity.evoker.prepare_summon", "Invoca una oleada de minions temporales.");
        SpecialPresets.sp(st, "marca_tp", "Marca de Teletransporte", Material.ENDER_EYE, VOID, 0, 0, 0, 100, 0, 0, 1, 1, 0, "PORTAL", "block.beacon.activate", "Guarda tu posición actual.");
        SpecialPresets.sp(st, "recall_tp", "Regreso a la Marca", Material.ENDER_PEARL, VOID, 0, 0, 0, 300, 0, 0, 1, 1, 0, "PORTAL", "entity.enderman.teleport", "Vuelve a tu última marca.");
        SpecialPresets.sp(st, "rastreo", "Rastreo de Presa", Material.SPYGLASS, N, 0, 20, 0, 400, 0, 200, 1, 1, 0, "END_ROD", "entity.experience_orb.pickup", "Resalta a los enemigos cercanos.");
        SpecialPresets.sp(st, "campo_minado", "Campo Minado", Material.TNT, FIRE, 14, 4, 0, 500, 0, 400, 1, 1, 1.0, "CRIT", "entity.tnt.primed", "Planta cinco minas frente a ti.");
        SpecialPresets.sp(st, "berserk", "Berserk", Material.REDSTONE_BLOCK, FIRE, 0, 0, 0, 700, 0, 200, 1, 1, 0, "FLAME", "entity.ravager.roar", "Furia extrema; después quedas agotado.");
        SpecialPresets.sp(st, "armadura_roca", "Armadura de Roca", Material.STONE, EAR, 25, 0, 0, 500, 0, 200, 1, 1, 0, "CRIT", "block.stone.place", "Escudo de absorción y resistencia.");
        SpecialPresets.sp(st, "sacrificio_vida", "Sacrificio de Vida", Material.REDSTONE, SHA, 0, 0, 0, 600, 0, 200, 1, 1, 0, "SMOKE", "entity.wither.hurt", "Pierdes 30% de vida a cambio de fuerza y velocidad.");
        SpecialPresets.sp(st, "chidori", "Chidori", Material.LIGHTNING_ROD, LIG, 18, 0, 12, 200, 0, 0, 1, 1.5, 1.0, "ELECTRIC_SPARK", "entity.lightning_bolt.impact", "Carga eléctrica que aturde lo que golpea.");
        // ---- pack para invocaciones ----
        SpecialPresets.sp(st, "vinculo_vital", "Vínculo Vital", Material.GLISTERING_MELON_SLICE, LGT, 0, 0, 0, 400, 0, 0, 1, 1, 0, "HEART", "entity.player.levelup", "[Invocación] Sacrifica vida propia para curar el doble a su dueño.");
        SpecialPresets.sp(st, "coordinado", "Ataque Coordinado", Material.CROSSBOW, N, 20, 0, 20, 300, 0, 100, 1, 1, 0, "CRIT", "entity.enderman.teleport", "[Invocación] Salta tras el enemigo del dueño y le da fuerza.");
        SpecialPresets.sp(st, "regreso_dueno", "Regreso al Dueño", Material.LEAD, LGT, 15, 0, 0, 250, 0, 160, 1, 1, 0, "CLOUD", "entity.enderman.teleport", "[Invocación] Vuelve junto al dueño y lo protege con un escudo.");
        SpecialPresets.sp(st, "proteccion_guardian", "Protección del Guardián", Material.SHIELD, LGT, 20, 6, 0, 400, 0, 160, 1, 1, 1.5, "END_ROD", "item.shield.block", "[Invocación] Aparta a los enemigos del dueño y lo blinda.");
        SpecialPresets.spx(st, "rugido_invocacion", "terror", "Rugido de la Invocación", Material.GOAT_HORN, SHA, 0, 10, 0, 350, 0, 120, 1, 1, 2.0, "SMOKE", "entity.ravager.roar", "[Invocación] Aterroriza a los enemigos alrededor.");
        SpecialPresets.spx(st, "furia_invocacion", "furia", "Furia Bestial", Material.BLAZE_ROD, FIRE, 0, 0, 0, 500, 0, 200, 1, 1, 0, "FLAME", "entity.ravager.roar", "[Invocación] Fuerza y velocidad para la invocación y su dueño.");

        // ---- control (estados) ----
        ab(st, "raices", "Raíces", "Inmoviliza a los enemigos con raíces.", Material.VINE, Shape.CIRCLE, Effect.STATUS, EAR, 0, 7, 0, 0, 0, 300, 0, 1, "CRIT", "block.grass.break", "raices", 80, false);
        ab(st, "silencio_area", "Silencio", "Los enemigos no pueden usar habilidades.", Material.NOTE_BLOCK, Shape.CIRCLE, Effect.STATUS, LGT, 0, 8, 0, 0, 0, 500, 0, 1, "ENCHANT", "block.beacon.activate", "silencio", 160, false);
        ab(st, "cegar", "Cegar", "Ciega a los enemigos cercanos.", Material.INK_SAC, Shape.CIRCLE, Effect.STATUS, SHA, 0, 8, 0, 0, 0, 300, 0, 1, "SMOKE", "entity.wither.ambient", "ceguera", 120, false);
        ab(st, "paralisis", "Parálisis", "Paraliza a lo que miras.", Material.LIGHTNING_ROD, Shape.LINE, Effect.STATUS, LIG, 0, 0, 15, 0, 0, 300, 0, 1, "ELECTRIC_SPARK", "entity.lightning_bolt.impact", "paralisis", 80, false);
        ab(st, "sueno_profundo", "Sueño Profundo", "Duerme a los enemigos un instante.", Material.BLUE_BED, Shape.CIRCLE, Effect.STATUS, SHA, 0, 6, 0, 0, 0, 500, 0, 1, "ENCHANT", "entity.evoker.prepare_summon", "sueno", 80, false);
        ab(st, "pesadilla", "Pesadilla", "Ceguera, náuseas y lentitud.", Material.WITHER_ROSE, Shape.CIRCLE, Effect.STATUS, SHA, 0, 7, 0, 0, 0, 400, 0, 1, "SMOKE", "entity.wither.ambient", "pesadilla", 140, false);
        ab(st, "maldicion_hambre", "Maldición del Hambre", "Los enemigos pasan hambre.", Material.ROTTEN_FLESH, Shape.CIRCLE, Effect.STATUS, SHA, 0, 8, 0, 0, 0, 400, 0, 1, "WITCH", "entity.zombie.ambient", "hambre", 200, false);
        ab(st, "maldicion_vida", "Maldición Debilitante", "Debilita a los enemigos.", Material.FERMENTED_SPIDER_EYE, Shape.CIRCLE, Effect.STATUS, SHA, 0, 7, 0, 0, 0, 400, 0, 1, "SMOKE", "entity.wither.ambient", "debilidad", 200, false);
        ab(st, "inmunidad_temporal", "Inmunidad Temporal", "Invulnerable unos segundos.", Material.TOTEM_OF_UNDYING, Shape.CIRCLE, Effect.PHASE, LGT, 0, 1, 0, 0, 0, 800, 0, 1, "END_ROD", "item.totem.use", "", 100, false);
        // ---- técnicas de anime (datos puros) ----
        ab(st, "shinra_tensei", "Shinra Tensei", "Repulsión enorme.", Material.HEAVY_CORE, Shape.CIRCLE, Effect.PUSH, WIND, 0, 14, 0, 0, 3.5, 400, 10, 1, "CLOUD", "entity.generic.explode", "", 100, false);
        ab(st, "bansho_tenin", "Bansho Ten'in", "Atracción enorme.", Material.ENDER_EYE, Shape.CIRCLE, Effect.PULL, VOID, 0, 14, 0, 0, 2.5, 350, 0, 1, "PORTAL", "entity.enderman.teleport", "", 100, false);
        ab(st, "susanoo_slash", "Susanoo Slash", "Tajo colosal en cono.", Material.NETHERITE_SWORD, Shape.CONE, Effect.DAMAGE, LGT, 35, 0, 12, 120, 1.5, 300, 0, 1, "SWEEP_ATTACK", "entity.player.attack.sweep", "", 100, false);
        ab(st, "rasengan", "Rasengan", "Esfera de viento giratoria.", Material.HEART_OF_THE_SEA, Shape.PROJECTILE, Effect.DAMAGE, WIND, 28, 3, 25, 0, 2.5, 200, 0, 1.3, "CLOUD", "entity.generic.explode", "", 100, false);
        ab(st, "gran_bola_de_fuego", "Gran Bola de Fuego", "Bola de fuego enorme.", Material.FIRE_CHARGE, Shape.PROJECTILE, Effect.DAMAGE, FIRE, 32, 7, 40, 0, 1.5, 400, 0, 1.0, "FLAME", "entity.blaze.shoot", "quemadura", 100, false);
        ab(st, "bijuu_dama", "Bijuu Dama", "Esfera devastadora de la bestia. [Invocación]", Material.NETHER_STAR, Shape.PROJECTILE, Effect.DAMAGE, VOID, 70, 10, 50, 0, 3.0, 1000, 30, 0.8, "EXPLOSION", "entity.generic.explode", "", 100, true);
        ab(st, "garra_kurama", "Garra de la Bestia", "Zarpazo gigante. [Invocación]", Material.NETHERITE_AXE, Shape.CONE, Effect.DAMAGE, FIRE, 30, 0, 12, 100, 2.0, 250, 0, 1, "SWEEP_ATTACK", "entity.ravager.attack", "", 100, false);
        ab(st, "susanoo_flecha", "Flecha de Susanoo", "Haz largo y letal. [Invocación]", Material.BOW, Shape.LINE, Effect.DAMAGE, LGT, 40, 0, 45, 0, 0.5, 400, 0, 1, "END_ROD", "entity.arrow.shoot", "", 100, false);
        ab(st, "cola_barredora", "Cola Barredora", "Barrido circular enorme. [Invocación]", Material.LEAD, Shape.CIRCLE, Effect.DAMAGE, EAR, 20, 8, 0, 0, 2.2, 220, 0, 1, "CLOUD", "entity.iron_golem.attack", "", 100, false);
    }

    private static void ab(DefinitionStore<AbilityDef> st, String id, String name, String desc, Material icon, Shape shape,
                           Effect effect, Element el, double dmg, double radius, double range, double angle, double kb,
                           int cd, int warn, double speed, String particle, String sound, String status, int ticks, boolean ultimate) {
        if (st.has(id)) return;
        AbilityDef d = new AbilityDef(id);
        d.name = name;
        d.description = desc;
        d.icon = icon;
        d.shape = shape;
        d.effect = effect;
        d.element = el;
        d.damage = dmg;
        d.radius = radius;
        d.range = range;
        d.angle = angle > 0 ? angle : 90;
        d.knockback = kb;
        d.cooldownTicks = cd;
        d.warnTicks = warn;
        d.speed = speed;
        d.particle = particle;
        d.sound = sound;
        d.status = status;
        d.statusTicks = ticks;
        d.ultimate = ultimate;
        st.save(d);
    }
}
