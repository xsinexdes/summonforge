package com.customsmithing.unlock;

/** Cálculo (puro, sin Bukkit) de la maestría: más piezas hechas con un material = pequeño bonus de calidad con tope. */
public final class Mastery {

    private Mastery() {
    }

    /**
     * @param pieces         piezas ya procesadas con ese material
     * @param piecesPerPoint cuántas piezas dan un "punto" de maestría (<= 0 desactiva)
     * @param percentPerPoint bonus (%) que da cada punto
     * @param capPercent     bonus máximo (%)
     */
    public static double bonusPercent(int pieces, int piecesPerPoint, double percentPerPoint, double capPercent) {
        if (pieces <= 0 || piecesPerPoint <= 0 || percentPerPoint <= 0 || capPercent <= 0) return 0;
        return Math.min(capPercent, (pieces / piecesPerPoint) * percentPerPoint);
    }

    /** Multiplicador final: 1 + suma de bonos en %. */
    public static double multiplier(double... percents) {
        double total = 0;
        for (double p : percents) total += Math.max(0, p);
        return 1.0 + total / 100.0;
    }
}
