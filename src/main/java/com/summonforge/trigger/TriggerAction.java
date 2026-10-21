package com.summonforge.trigger;

public enum TriggerAction {
    CAST_ABILITY("Lanzar habilidad (param = id)"),
    APPLY_STATUS("Aplicarme un estado (param = id)"),
    HEAL_OWNER("Curarme (param = vida)"),
    GIVE_MANA("Darme maná (param = cantidad)"),
    MESSAGE("Enviarme un mensaje (param = texto)");

    public final String label;

    TriggerAction(String label) {
        this.label = label;
    }
}
