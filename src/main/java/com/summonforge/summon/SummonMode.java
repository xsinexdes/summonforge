package com.summonforge.summon;

/** Modo de comportamiento de la invocación (se cambia en "Mi Invocación" o con la tecla Modo). */
public enum SummonMode {
    AGGRESSIVE("Agresivo", "Ataca todo enemigo a 14 bloques."),
    DEFENSIVE("Defensivo", "Solo ataca lo cercano o lo que tú golpeas."),
    FOLLOW("Seguir", "Solo te sigue; no ataca."),
    STAY("Quieto", "Se queda donde está y ataca lo cercano."),
    MARKED("Objetivo marcado", "Solo ataca al objetivo que marcas (/inv marcar o golpeándolo).");

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
