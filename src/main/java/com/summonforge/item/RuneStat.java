package com.summonforge.item;

/** Estadística que potencia una runa equipada. */
public enum RuneStat {
    HEALTH("Vida"),
    DAMAGE("Daño (y habilidades)"),
    SPEED("Velocidad"),
    MANA("Maná máximo");

    public final String label;

    RuneStat(String label) {
        this.label = label;
    }
}
