package com.summonforge.summon;

import org.bukkit.Material;

/** Talento de la invocación: 1 punto por nivel (desde el nivel 2), máx. 5 puntos por talento. */
public enum Talent {
    VITALIDAD("Vitalidad", Material.GOLDEN_APPLE, 0.08, "+8% de vida por punto"),
    FUERZA("Fuerza", Material.IRON_SWORD, 0.06, "+6% de daño por punto"),
    CELERIDAD("Celeridad", Material.SUGAR, 0.04, "+4% de velocidad por punto");

    public static final int MAX_POINTS = 5;

    public final String label;
    public final Material icon;
    public final double perPoint;
    public final String desc;

    Talent(String label, Material icon, double perPoint, String desc) {
        this.label = label;
        this.icon = icon;
        this.perPoint = perPoint;
        this.desc = desc;
    }
}
