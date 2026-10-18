package com.summonforge.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Animación asignada a cada estado básico. Se rellena sola leyendo los nombres reales del modelo. */
public final class AnimationSet {
    public static final List<String> STATES = List.of("spawn", "idle", "walk", "attack", "hurt", "death");

    private static final Map<String, List<String>> KEYWORDS = Map.of(
            "spawn", List.of("spawn", "appear", "summon"),
            "idle", List.of("idle", "stand", "wait"),
            "walk", List.of("walk", "run", "move"),
            "attack", List.of("attack", "slash", "punch", "claw", "bite", "strike"),
            "hurt", List.of("hurt", "damage", "hit"),
            "death", List.of("death", "die", "dead"));

    private final Map<String, String> map = new LinkedHashMap<>();

    public AnimationSet() {
        for (String s : STATES) map.put(s, "");
    }

    public String get(String state) {
        return map.getOrDefault(state, "");
    }

    public void set(String state, String value) {
        if (map.containsKey(state)) map.put(state, value == null ? "" : value);
    }

    public void read(ConfigurationSection s) {
        if (s == null) return;
        for (String st : STATES) map.put(st, s.getString(st, ""));
    }

    public void write(ConfigurationSection s) {
        for (Map.Entry<String, String> e : map.entrySet()) s.set(e.getKey(), e.getValue());
    }

    /** Asigna por coincidencia de nombre (exacta primero, luego "contiene"). Sobrescribe todo. */
    public void autoFill(List<String> names) {
        for (String st : STATES) map.put(st, find(st, names));
    }

    private static String find(String state, List<String> names) {
        List<String> kws = KEYWORDS.get(state);
        for (String kw : kws) {
            for (String n : names) if (n.equalsIgnoreCase(kw)) return n;
        }
        for (String kw : kws) {
            for (String n : names) if (n.toLowerCase(Locale.ROOT).contains(kw)) return n;
        }
        return "";
    }
}
