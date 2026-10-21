package com.summonforge.cinematic;

import com.summonforge.data.Codec;
import com.summonforge.data.Definition;
import com.summonforge.util.Enums;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Secuencia de pasos, uno por línea:
 * TITLE;título;subtítulo;ticks | SOUND;clave | MESSAGE;texto | WAIT;ticks | FREEZE;ticks | PARTICLES;nombre;cantidad
 */
public final class CinematicDef implements Definition {
    public final String id;
    public String name;
    public Material icon = Material.FILLED_MAP;
    public final List<String> steps = new ArrayList<>();

    public CinematicDef(String id) {
        this.id = id;
        this.name = id;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return name; }
    @Override public Material icon() { return icon; }

    public static final Codec<CinematicDef> CODEC = new Codec<>() {
        @Override
        public CinematicDef read(String id, ConfigurationSection s) {
            CinematicDef d = new CinematicDef(id);
            d.name = s.getString("name", id);
            d.icon = Enums.material(s.getString("icon"), Material.FILLED_MAP);
            d.steps.addAll(s.getStringList("steps"));
            return d;
        }

        @Override
        public void write(CinematicDef d, ConfigurationSection s) {
            s.set("name", d.name);
            s.set("icon", d.icon.name());
            s.set("steps", d.steps);
        }
    };
}
