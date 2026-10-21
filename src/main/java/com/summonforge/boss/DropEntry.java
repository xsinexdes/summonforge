package com.summonforge.boss;

import org.bukkit.configuration.ConfigurationSection;

/** Una línea de la tabla de drops de un boss: objeto, probabilidad (%, admite decimales) y cantidad. */
public final class DropEntry {
    public String itemId = "";
    public double chance = 25;
    public int min = 1;
    public int max = 1;

    public void read(ConfigurationSection s) {
        if (s == null) return;
        itemId = s.getString("item", "");
        chance = s.getDouble("chance", 25);
        min = Math.max(1, s.getInt("min", 1));
        max = Math.max(min, s.getInt("max", 1));
    }

    public void write(ConfigurationSection s) {
        s.set("item", itemId);
        s.set("chance", chance);
        s.set("min", min);
        s.set("max", max);
    }
}
