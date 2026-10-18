package com.summonforge.util;

import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

public final class Enums {
    private Enums() {}

    public static <E extends Enum<E>> E parse(Class<E> type, String s, E def) {
        if (s == null) return def;
        try {
            return Enum.valueOf(type, s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    public static <E> E cycle(List<E> options, E current, int dir) {
        int i = options.indexOf(current);
        if (i < 0) return options.get(0);
        return options.get(Math.floorMod(i + dir, options.size()));
    }

    public static Material material(String s, Material def) {
        if (s == null) return def;
        Material m = Material.matchMaterial(s);
        return m == null ? def : m;
    }
}
