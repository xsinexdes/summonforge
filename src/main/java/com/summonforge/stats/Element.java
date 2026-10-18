package com.summonforge.stats;

public enum Element {
    NONE("Neutro", "white"),
    FIRE("Fuego", "gold"),
    WATER("Agua", "aqua"),
    EARTH("Tierra", "dark_green"),
    WIND("Viento", "green"),
    LIGHTNING("Rayo", "yellow"),
    ICE("Hielo", "aqua"),
    POISON("Veneno", "dark_green"),
    SHADOW("Sombra", "dark_purple"),
    LIGHT("Luz", "white"),
    VOID("Vacío", "light_purple");

    public final String label;
    /** Color MiniMessage usado en números de daño. */
    public final String tag;

    Element(String label, String tag) {
        this.label = label;
        this.tag = tag;
    }
}
