package com.summonforge.status;

import com.summonforge.data.Codec;
import com.summonforge.data.Definition;
import com.summonforge.stats.Element;
import com.summonforge.util.Enums;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/** Estado alterado data-driven: daño continuo + efectos de poción + acumulación (stacks). */
public final class StatusDef implements Definition {
    public final String id;
    public String name;
    public Material icon = Material.SPIDER_EYE;
    public Element element = Element.NONE;
    /** Daño por segundo por stack. */
    public double dotDamage = 0;
    public int maxStacks = 3;
    /** Amplificador de Lentitud (-1 = ninguno). */
    public int slowAmp = -1;
    /** Amplificador de Debilidad (-1 = ninguno). */
    public int weakAmp = -1;
    /** Inmoviliza casi por completo (Lentitud alta). */
    public boolean stun = false;
    public String particle = "SMOKE";

    public StatusDef(String id) {
        this.id = id;
        this.name = id;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return name; }
    @Override public Material icon() { return icon; }

    public static final Codec<StatusDef> CODEC = new Codec<>() {
        @Override
        public StatusDef read(String id, ConfigurationSection s) {
            StatusDef d = new StatusDef(id);
            d.name = s.getString("name", id);
            d.icon = Enums.material(s.getString("icon"), Material.SPIDER_EYE);
            d.element = Enums.parse(Element.class, s.getString("element"), Element.NONE);
            d.dotDamage = s.getDouble("dot-damage", 0);
            d.maxStacks = Math.max(1, s.getInt("max-stacks", 3));
            d.slowAmp = s.getInt("slow-amp", -1);
            d.weakAmp = s.getInt("weak-amp", -1);
            d.stun = s.getBoolean("stun", false);
            d.particle = s.getString("particle", "SMOKE");
            return d;
        }

        @Override
        public void write(StatusDef d, ConfigurationSection s) {
            s.set("name", d.name);
            s.set("icon", d.icon.name());
            s.set("element", d.element.name());
            s.set("dot-damage", d.dotDamage);
            s.set("max-stacks", d.maxStacks);
            s.set("slow-amp", d.slowAmp);
            s.set("weak-amp", d.weakAmp);
            s.set("stun", d.stun);
            s.set("particle", d.particle);
        }
    };
}
