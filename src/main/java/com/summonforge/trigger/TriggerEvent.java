package com.summonforge.trigger;

public enum TriggerEvent {
    OWNER_HURT("Recibo daño"),
    OWNER_LOW_HEALTH("Mi vida baja de un %"),
    SUMMON_KILL("Mi invocación mata"),
    BOSS_KILL("Derroto a un boss"),
    SUMMON_SPAWN("Invoco");

    public final String label;

    TriggerEvent(String label) {
        this.label = label;
    }
}
