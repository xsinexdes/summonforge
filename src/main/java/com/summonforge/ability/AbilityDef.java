package com.summonforge.ability;

import com.summonforge.data.Codec;
import com.summonforge.data.Definition;
import com.summonforge.stats.Element;
import com.summonforge.util.Enums;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/** Habilidad data-driven: forma + efecto + visual. Las ~30 con lógica especial irán en clases propias. */
public final class AbilityDef implements Definition {
    public final String id;
    public String name;
    public String description = "";
    public Material icon = Material.BLAZE_POWDER;
    public Shape shape = Shape.CIRCLE;
    public Effect effect = Effect.DAMAGE;
    public Element element = Element.NONE;
    public double damage = 6;
    public double radius = 4;
    public double range = 8;
    public double angle = 90;
    public double speed = 1.0;
    public double knockback = 0.5;
    public int cooldownTicks = 60;
    /** Ticks de aviso (zona telegrafiada) antes del impacto. 0 = sin aviso. */
    public int warnTicks = 0;
    public String particle = "FLAME";
    public String sound = "entity.generic.explode";
    /** Estado que aplica al impactar ("" = ninguno). */
    /** Id de una habilidad especial con lógica propia (ver SpecialAbilities); "" = comportamiento genérico. */
    public String special = "";
    public String status = "";
    public int statusTicks = 100;
    public int statusStacks = 1;

    public AbilityDef(String id) {
        this.id = id;
        this.name = id;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return name; }
    @Override public Material icon() { return icon; }

    public static final Codec<AbilityDef> CODEC = new Codec<>() {
        @Override
        public AbilityDef read(String id, ConfigurationSection s) {
            AbilityDef d = new AbilityDef(id);
            d.name = s.getString("name", id);
            d.description = s.getString("description", "");
            d.icon = Enums.material(s.getString("icon"), Material.BLAZE_POWDER);
            d.shape = Enums.parse(Shape.class, s.getString("shape"), Shape.CIRCLE);
            d.effect = Enums.parse(Effect.class, s.getString("effect"), Effect.DAMAGE);
            d.element = Enums.parse(Element.class, s.getString("element"), Element.NONE);
            d.damage = s.getDouble("damage", 6);
            d.radius = s.getDouble("radius", 4);
            d.range = s.getDouble("range", 8);
            d.angle = s.getDouble("angle", 90);
            d.speed = s.getDouble("speed", 1.0);
            d.knockback = s.getDouble("knockback", 0.5);
            d.cooldownTicks = s.getInt("cooldown-ticks", 60);
            d.warnTicks = s.getInt("warn-ticks", 0);
            d.particle = s.getString("particle", "FLAME");
            d.sound = s.getString("sound", "");
            d.special = s.getString("special", "");
            d.status = s.getString("status", "");
            d.statusTicks = s.getInt("status-ticks", 100);
            d.statusStacks = s.getInt("status-stacks", 1);
            return d;
        }

        @Override
        public void write(AbilityDef d, ConfigurationSection s) {
            s.set("name", d.name);
            s.set("description", d.description);
            s.set("icon", d.icon.name());
            s.set("shape", d.shape.name());
            s.set("effect", d.effect.name());
            s.set("element", d.element.name());
            s.set("damage", d.damage);
            s.set("radius", d.radius);
            s.set("range", d.range);
            s.set("angle", d.angle);
            s.set("speed", d.speed);
            s.set("knockback", d.knockback);
            s.set("cooldown-ticks", d.cooldownTicks);
            s.set("warn-ticks", d.warnTicks);
            s.set("particle", d.particle);
            s.set("sound", d.sound);
            s.set("special", d.special);
            s.set("status", d.status);
            s.set("status-ticks", d.statusTicks);
            s.set("status-stacks", d.statusStacks);
        }
    };
}
