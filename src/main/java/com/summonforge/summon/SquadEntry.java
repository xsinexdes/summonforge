package com.summonforge.summon;

import com.summonforge.util.Enums;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

/** Un grupo de mobs del escuadrón: tipo, cantidad y fuerza relativa al líder. count 0 = desactivado. */
public final class SquadEntry {
    public EntityType type = EntityType.HUSK;
    public int count = 0;
    public double hpMul = 0.5;
    public double dmgMul = 0.5;
    public String name = "";
    /** Id de una de tus invocaciones para usarla como esbirro (vacío = mob normal del tipo elegido). */
    public String summonId = "";

    public void read(ConfigurationSection s) {
        if (s == null) return;
        type = Enums.parse(EntityType.class, s.getString("type"), EntityType.HUSK);
        count = s.getInt("count", 0);
        hpMul = s.getDouble("hp-mul", 0.5);
        dmgMul = s.getDouble("dmg-mul", 0.5);
        name = s.getString("name", "");
        summonId = s.getString("summon-id", "");
    }

    public void write(ConfigurationSection s) {
        s.set("type", type.name());
        s.set("count", count);
        s.set("hp-mul", hpMul);
        s.set("dmg-mul", dmgMul);
        s.set("name", name);
        s.set("summon-id", summonId);
    }
}
