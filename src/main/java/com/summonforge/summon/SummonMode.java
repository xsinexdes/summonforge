package com.summonforge.summon;

/** Modo de comportamiento de la invocación (el jugador lo cambia desde "Mi Invocación"). */
public enum SummonMode {
    AGGRESSIVE("Agresivo", "Ataca todo enemigo a 14 bloques."),
    DEFENSIVE("Defensivo", "Solo ataca lo que está cerca o lo que tú golpeas."),
    PASSIVE("Pasivo", "No ataca; solo te sigue.");

    public final String label;
    public final String desc;

    SummonMode(String label, String desc) {
        this.label = label;
        this.desc = desc;
    }

    public SummonMode next() {
        SummonMode[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
