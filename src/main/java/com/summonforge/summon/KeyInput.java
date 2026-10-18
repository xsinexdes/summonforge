package com.summonforge.summon;

/** Entradas que el servidor puede detectar (Bukkit no ve teclas arbitrarias). */
public enum KeyInput {
    NONE("Sin asignar"),
    F("F (cambiar de mano)"),
    SHIFT_F("Shift + F"),
    Q("Q (soltar ítem)"),
    DOUBLE_SHIFT("Doble Shift"),
    HOTBAR_8("Tecla 8 (ranura 8)"),
    HOTBAR_9("Tecla 9 (ranura 9)"),
    SHIFT_LEFT_CLICK("Shift + clic izquierdo");

    public final String label;

    KeyInput(String label) {
        this.label = label;
    }

    public KeyInput next(int dir) {
        KeyInput[] v = values();
        return v[Math.floorMod(ordinal() + dir, v.length)];
    }
}
