package com.summonforge.ability;

import com.summonforge.stats.Element;
import org.bukkit.Material;

import java.util.List;
import java.util.function.Consumer;

/** Plantillas de creación rápida: eliges una, pones un id y ya tienes una habilidad lista para retocar. */
public final class AbilityTemplates {
    private AbilityTemplates() {}

    public record Template(String label, Material icon, String help, Consumer<AbilityDef> fill) {
        /** Aplica la plantilla sobre una definición (conserva su id). */
        public void apply(AbilityDef d) {
            fill.accept(d);
            d.name = label;
            d.description = help;
        }
    }

    private static Template t(String label, Material icon, String help, Consumer<AbilityDef> fill) {
        return new Template(label, icon, help, fill);
    }

    private static void base(AbilityDef d, Shape shape, Effect effect, Element el, double dmg, double radius, double range,
                             int cd, int warn, int dur, String particle, String sound) {
        d.special = "";
        d.shape = shape;
        d.effect = effect;
        d.element = el;
        d.damage = dmg;
        d.radius = radius;
        d.range = range;
        d.cooldownTicks = cd;
        d.warnTicks = warn;
        d.statusTicks = dur;
        d.statusStacks = 1;
        d.particle = particle;
        d.sound = sound;
    }

    public static final List<Template> ALL = List.of(
            t("Genjutsu: ceguera", Material.INK_SAC, "Ciega a los enemigos cercanos.", d -> {
                base(d, Shape.CIRCLE, Effect.STATUS, Element.SHADOW, 0, 8, 0, 300, 0, 120, "SMOKE", "entity.wither.ambient");
                d.status = "ceguera";
            }),
            t("Genjutsu: pesadilla", Material.WITHER_ROSE, "Ceguera, náuseas y lentitud.", d -> {
                base(d, Shape.CIRCLE, Effect.STATUS, Element.SHADOW, 0, 7, 0, 400, 0, 140, "SMOKE", "entity.wither.ambient");
                d.status = "pesadilla";
            }),
            t("Genjutsu: sueño profundo", Material.BLUE_BED, "Duerme (inmoviliza y ciega) un instante.", d -> {
                base(d, Shape.CIRCLE, Effect.STATUS, Element.SHADOW, 0, 6, 0, 500, 0, 80, "ENCHANT", "entity.evoker.prepare_summon");
                d.status = "sueno";
            }),
            t("Tsukuyomi (ilusión)", Material.ENDER_EYE, "Atrapa a hasta 3 enemigos en una ilusión con tortura.", d -> {
                base(d, Shape.CIRCLE, Effect.DAMAGE, Element.SHADOW, 6, 8, 0, 700, 20, 160, "SMOKE", "entity.wither.spawn");
                d.special = "tsukuyomi";
                d.statusStacks = 3;
                d.ultimate = true;
            }),
            t("Kamui: enviar objetivo", Material.ENDER_PEARL, "Manda al enemigo más cercano a la dimensión Kamui.", d -> {
                base(d, Shape.LINE, Effect.DAMAGE, Element.VOID, 0, 0, 12, 500, 10, 160, "PORTAL", "entity.enderman.teleport");
                d.special = "kamui_objetivo";
            }),
            t("Kamui: duelo en el limbo", Material.ENDER_EYE, "Tú y el objetivo a la dimensión Kamui; se vuelve al terminar o al morir uno.", d -> {
                base(d, Shape.LINE, Effect.DAMAGE, Element.VOID, 0, 0, 12, 700, 10, 200, "PORTAL", "entity.enderman.teleport");
                d.special = "kamui_ambos";
            }),
            t("Kamui: intangible", Material.PHANTOM_MEMBRANE, "Eres invulnerable unos segundos.", d -> {
                base(d, Shape.CIRCLE, Effect.PHASE, Element.VOID, 0, 1, 0, 500, 0, 60, "PORTAL", "entity.enderman.teleport");
            }),
            t("Duelo dimensional (arena)", Material.NETHERITE_SWORD, "Tú y un enemigo a una arena aparte, con daño continuo.", d -> {
                base(d, Shape.LINE, Effect.DAMAGE, Element.VOID, 4, 0, 14, 900, 20, 300, "PORTAL", "entity.enderman.teleport");
                d.special = "dimension_duelo";
                d.ultimate = true;
            }),
            t("Dominio (arena grupal)", Material.NETHER_STAR, "Arrastra a hasta 6 enemigos a tu dominio.", d -> {
                base(d, Shape.CIRCLE, Effect.DAMAGE, Element.VOID, 5, 10, 0, 1200, 30, 300, "PORTAL", "entity.wither.spawn");
                d.special = "dominio_real";
                d.statusStacks = 6;
                d.ultimate = true;
            }),
            t("Amaterasu: llamas negras", Material.SOUL_TORCH, "Fuego negro que persigue al objetivo.", d -> {
                base(d, Shape.LINE, Effect.DAMAGE, Element.FIRE, 0, 0, 20, 500, 0, 200, "SOUL_FIRE_FLAME", "entity.blaze.shoot");
                d.special = "amaterasu";
            }),
            t("Mirada paralizante", Material.LIGHTNING_ROD, "Paraliza a lo que miras.", d -> {
                base(d, Shape.LINE, Effect.STATUS, Element.LIGHTNING, 0, 0, 15, 300, 0, 80, "ELECTRIC_SPARK", "entity.lightning_bolt.impact");
                d.status = "paralisis";
            }),
            t("Mirada de terror", Material.WITHER_SKELETON_SKULL, "Aleja y debilita a los enemigos.", d -> {
                base(d, Shape.CIRCLE, Effect.DAMAGE, Element.SHADOW, 0, 8, 0, 300, 0, 100, "SMOKE", "entity.ghast.scream");
                d.special = "terror";
                d.knockback = 1.5;
            }),
            t("Sello ocular (silencio)", Material.NOTE_BLOCK, "Los enemigos no pueden usar habilidades.", d -> {
                base(d, Shape.CIRCLE, Effect.STATUS, Element.LIGHT, 0, 8, 0, 500, 0, 160, "ENCHANT", "block.beacon.activate");
                d.status = "silencio";
            }),
            t("Marca de muerte", Material.SKELETON_SKULL, "Marca a un enemigo; explota tras 3 segundos.", d -> {
                base(d, Shape.LINE, Effect.DAMAGE, Element.SHADOW, 40, 0, 16, 600, 0, 60, "SMOKE", "entity.wither.spawn");
                d.special = "marca_de_muerte";
            }),
            t("Predicción (reflejo)", Material.GLASS, "Devuelve el 30% del daño que recibes.", d -> {
                base(d, Shape.CIRCLE, Effect.DAMAGE, Element.LIGHT, 0, 1, 0, 500, 0, 120, "END_ROD", "block.beacon.activate");
                d.special = "reflejo";
            }),
            t("Rayo ocular", Material.ENDER_EYE, "Haz instantáneo de energía.", d -> {
                base(d, Shape.LINE, Effect.DAMAGE, Element.LIGHT, 22, 0, 25, 120, 0, 100, "END_ROD", "block.beacon.power_select");
            }),
            t("Torbellino ocular", Material.PHANTOM_MEMBRANE, "Atrae y daña en un remolino.", d -> {
                base(d, Shape.CIRCLE, Effect.DAMAGE, Element.WIND, 6, 7, 0, 400, 0, 100, "CLOUD", "entity.phantom.flap");
                d.special = "torbellino_continuo";
            })
    );
}
