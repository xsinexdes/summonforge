package com.summonforge.ability;

import com.summonforge.data.DefinitionStore;
import com.summonforge.stats.Element;
import org.bukkit.Material;

/** Instala una AbilityDef por cada habilidad especial de SpecialAbilities (no pisa lo editado). */
public final class SpecialPresets {
    private SpecialPresets() {}

    private static final Element N = Element.NONE, FIRE = Element.FIRE, EAR = Element.EARTH, WIND = Element.WIND,
            LIG = Element.LIGHTNING, ICE = Element.ICE, SHA = Element.SHADOW, LGT = Element.LIGHT, VOID = Element.VOID;

    public static void install(DefinitionStore<AbilityDef> st) {
        // id, nombre, especial, icono, elemento, daño/valor, radio, alcance, cooldown, aviso, duración(ticks), cantidad, velocidad, empuje, partícula, sonido, descripción
        sp(st, "blink", "Paso Relámpago", Material.FEATHER, WIND, 0, 0, 10, 100, 0, 0, 1, 1, 0, "CLOUD", "entity.enderman.teleport", "Teletransporte corto hacia delante.");
        sp(st, "teleport_tras", "Golpe por la Espalda", Material.ECHO_SHARD, SHA, 14, 0, 12, 160, 0, 0, 1, 1, 0, "SMOKE", "entity.enderman.teleport", "Aparece tras el enemigo más cercano y lo golpea.");
        sp(st, "intercambio", "Intercambio", Material.ENDER_PEARL, VOID, 6, 0, 12, 200, 0, 0, 1, 1, 0, "PORTAL", "entity.enderman.teleport", "Cambia de lugar con un enemigo.");
        sp(st, "embestida", "Embestida", Material.IRON_BOOTS, N, 12, 0, 8, 140, 0, 0, 1, 1.2, 1.2, "CLOUD", "entity.ravager.attack", "Carga hacia delante golpeando todo.");
        sp(st, "salto_sismico", "Salto Sísmico", Material.SLIME_BLOCK, EAR, 14, 5, 0, 200, 0, 0, 1, 1, 1.2, "CLOUD", "entity.generic.explode", "Salta y aplasta al caer.");
        sp(st, "tornado", "Tornado", Material.WHITE_BANNER, WIND, 8, 3, 0, 350, 0, 80, 1, 1, 0, "CLOUD", "entity.phantom.flap", "Columna de viento que avanza y levanta enemigos.");
        sp(st, "latigazo", "Látigo", Material.LEAD, N, 10, 0, 10, 120, 0, 0, 1, 1, 0, "CRIT", "entity.player.attack.sweep", "Atrae a un enemigo hacia ti y lo golpea.");
        sp(st, "clones", "Clones de Sombra", Material.PLAYER_HEAD, SHA, 0, 0, 0, 400, 0, 200, 2, 1, 0, "SMOKE", "entity.enderman.teleport", "Crea clones temporales que atacan.");
        sp(st, "aliados", "Convocar Aliados", Material.TOTEM_OF_UNDYING, LGT, 0, 0, 0, 600, 0, 300, 2, 1, 0, "END_ROD", "entity.evoker.prepare_summon", "Invoca aliados temporales.");
        sp(st, "escudo", "Escudo Protector", Material.SHIELD, LGT, 20, 0, 0, 400, 0, 160, 1, 1, 0, "END_ROD", "item.shield.block", "Absorbe daño (tú y tu dueño).");
        sp(st, "escudo_grupal", "Escudo de Grupo", Material.NETHERITE_CHESTPLATE, LGT, 12, 7, 0, 500, 0, 160, 1, 1, 0, "END_ROD", "item.shield.block", "Escudo para todos los aliados cercanos.");
        sp(st, "invisibilidad", "Velo de Sombras", Material.GLASS, SHA, 0, 0, 0, 400, 0, 100, 1, 1, 0, "SMOKE", "entity.enderman.teleport", "Invisibilidad temporal.");
        sp(st, "furia", "Furia", Material.BLAZE_ROD, FIRE, 0, 0, 0, 500, 0, 160, 1, 1, 0, "FLAME", "entity.ravager.roar", "Más fuerza y velocidad un rato.");
        sp(st, "grito_de_guerra", "Grito de Guerra", Material.GOAT_HORN, N, 0, 8, 0, 500, 0, 200, 1, 1, 0, "CRIT", "entity.ravager.roar", "Da fuerza a los aliados cercanos.");
        sp(st, "curacion_porcentual", "Plegaria", Material.GOLDEN_CARROT, LGT, 30, 6, 0, 600, 0, 0, 1, 1, 0, "HEART", "entity.player.levelup", "Cura un % de la vida máxima a los aliados.");
        sp(st, "tormenta_de_rayos", "Tormenta de Rayos", Material.LIGHTNING_ROD, LIG, 10, 10, 0, 300, 0, 0, 5, 1, 0, "ELECTRIC_SPARK", "entity.lightning_bolt.thunder", "Rayos sobre varios enemigos.");
        sp(st, "lluvia_de_meteoros", "Lluvia de Meteoros", Material.FIRE_CHARGE, FIRE, 12, 8, 12, 400, 0, 0, 6, 1, 0, "FLAME", "entity.generic.explode", "Meteoros con aviso en una zona.");
        sp(st, "terremoto", "Terremoto", Material.DEEPSLATE, EAR, 9, 9, 0, 260, 0, 0, 1, 1, 1.0, "CLOUD", "entity.generic.explode", "Tres ondas expansivas.");
        sp(st, "lluvia_de_flechas", "Lluvia de Flechas", Material.ARROW, N, 6, 6, 14, 250, 0, 0, 1, 1, 0, "CRIT", "entity.arrow.shoot", "Tres oleadas de flechas en una zona.");
        sp(st, "explosion_nuclear", "Explosión Colosal", Material.TNT, FIRE, 40, 12, 0, 800, 40, 0, 1, 1, 2.5, "EXPLOSION", "entity.generic.explode", "Enorme explosión con aviso largo.");
        sp(st, "congelacion_total", "Congelación Total", Material.BLUE_ICE, ICE, 7, 7, 0, 320, 0, 100, 1, 1, 0, "SNOWFLAKE", "block.glass.break", "Congela y ralentiza el área.");
        sp(st, "drenar_vida", "Drenar Vida", Material.GHAST_TEAR, SHA, 8, 5, 0, 220, 0, 0, 1, 1, 0, "SMOKE", "entity.wither.shoot", "Daña y te cura la mitad del daño.");
        sp(st, "gravedad_inversa", "Gravedad Inversa", Material.SHULKER_SHELL, VOID, 6, 6, 0, 300, 0, 0, 1, 1, 0, "PORTAL", "entity.shulker.shoot", "Levanta a los enemigos del suelo.");
        sp(st, "terror", "Terror", Material.WITHER_SKELETON_SKULL, SHA, 0, 7, 0, 300, 0, 80, 1, 1, 1.8, "SMOKE", "entity.ghast.scream", "Aleja y debilita a los enemigos.");
        sp(st, "sellado", "Sellado", Material.PAPER, LGT, 0, 7, 0, 400, 0, 100, 1, 1, 0, "END_ROD", "block.beacon.activate", "Debilita y ralentiza a los enemigos.");
        sp(st, "tiempo_lento", "Distorsión Temporal", Material.CLOCK, VOID, 0, 8, 0, 450, 0, 120, 1, 1, 0, "PORTAL", "block.beacon.deactivate", "Ralentiza a enemigos y acelera a aliados.");
        sp(st, "espiral_de_fuego", "Espiral de Fuego", Material.MAGMA_CREAM, FIRE, 8, 4, 0, 300, 0, 100, 1, 1, 0, "FLAME", "entity.blaze.shoot", "Espiral de llamas alrededor.");
        sp(st, "torbellino_continuo", "Torbellino Continuo", Material.PHANTOM_MEMBRANE, WIND, 5, 6, 0, 280, 0, 80, 1, 1, 0, "CLOUD", "entity.phantom.flap", "Atrae y daña durante unos segundos.");
        sp(st, "campo_de_curacion", "Campo de Curación", Material.GLISTERING_MELON_SLICE, LGT, 3, 6, 0, 400, 0, 100, 1, 1, 0, "HEART", "entity.player.levelup", "Curación continua en el área.");
        sp(st, "aura_de_dano", "Aura Abrasadora", Material.BLAZE_POWDER, FIRE, 4, 4, 0, 350, 0, 100, 1, 1, 0, "FLAME", "entity.blaze.shoot", "Daño continuo alrededor.");
        sp(st, "mina", "Mina Explosiva", Material.TNT_MINECART, FIRE, 18, 4, 0, 200, 0, 300, 1, 1, 1.0, "CRIT", "entity.generic.explode", "Deja una mina que explota al acercarse.");
        sp(st, "cadena_de_rayos", "Cadena de Rayos", Material.COPPER_INGOT, LIG, 12, 0, 12, 160, 0, 0, 5, 1, 0, "ELECTRIC_SPARK", "entity.lightning_bolt.impact", "Salta entre varios enemigos.");
        sp(st, "marca_de_muerte", "Marca de la Muerte", Material.SKELETON_SKULL, SHA, 30, 0, 14, 400, 0, 60, 1, 1, 0, "SMOKE", "entity.wither.spawn", "Marca a un enemigo y explota tras unos segundos.");
    }

    private static void sp(DefinitionStore<AbilityDef> st, String id, String name, Material icon, Element el, double dmg,
                           double radius, double range, int cd, int warn, int dur, int count, double speed, double kb,
                           String particle, String sound, String desc) {
        if (st.has(id)) return;
        AbilityDef d = new AbilityDef(id);
        d.name = name;
        d.description = desc;
        d.icon = icon;
        d.special = id;
        d.element = el;
        d.damage = dmg;
        d.radius = radius;
        d.range = range;
        // Con radio usa forma de círculo (el aviso y la IA usan radio); sin radio, de línea (usan alcance).
        d.shape = radius > 0 ? Shape.CIRCLE : Shape.LINE;
        d.cooldownTicks = cd;
        d.warnTicks = warn;
        d.statusTicks = dur > 0 ? dur : 100;
        d.statusStacks = count;
        d.speed = speed;
        d.knockback = kb;
        d.particle = particle;
        d.sound = sound;
        st.save(d);
    }
}
