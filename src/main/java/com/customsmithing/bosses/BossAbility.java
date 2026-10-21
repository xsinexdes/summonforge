package com.customsmithing.bosses;

public enum BossAbility {
    FIRE_PULSE("Pulso de Fuego", "Prende fuego a los jugadores cercanos cada tanto"),
    SPEED_DASH("Embestida Veloz", "Gana un impulso de velocidad para embestir"),
    EXPLOSION_SLAM("Golpe Explosivo", "Genera una explosión de daño (sin romper bloques)"),
    SUMMON_MINIONS("Invocar Secuaces", "Aparecen refuerzos cerca del jefe");

    private final String label;
    private final String description;

    BossAbility(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
}
