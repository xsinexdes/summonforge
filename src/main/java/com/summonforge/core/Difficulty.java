package com.summonforge.core;

/** Perfiles de dificultad globales para bosses: multiplicadores de vida, daño, drops y tiempo de respawn. */
public enum Difficulty {
    FACIL("Fácil", 0.7, 0.7, 1.3, 0.7),
    NORMAL("Normal", 1.0, 1.0, 1.0, 1.0),
    DIFICIL("Difícil", 1.5, 1.4, 0.9, 1.2),
    HARDCORE("Hardcore", 2.0, 2.0, 0.75, 1.5);

    public final String label;
    public final double hp;
    public final double dmg;
    public final double drop;
    public final double respawn;

    Difficulty(String label, double hp, double dmg, double drop, double respawn) {
        this.label = label;
        this.hp = hp;
        this.dmg = dmg;
        this.drop = drop;
        this.respawn = respawn;
    }

    public Difficulty next() {
        Difficulty[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public static Difficulty of(String s) {
        try {
            return valueOf(s.trim().toUpperCase());
        } catch (RuntimeException e) {
            return NORMAL;
        }
    }
}
