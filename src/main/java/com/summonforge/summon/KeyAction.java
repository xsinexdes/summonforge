package com.summonforge.summon;

/** Acciones asignables a una tecla. slot = índice de habilidad equipada (-1 si no aplica). */
public enum KeyAction {
    TOGGLE("Invocar / despedir", -1),
    ABILITY_1("Habilidad 1", 0),
    ABILITY_2("Habilidad 2", 1),
    ABILITY_3("Habilidad 3", 2);

    public final String label;
    public final int slot;

    KeyAction(String label, int slot) {
        this.label = label;
        this.slot = slot;
    }
}
