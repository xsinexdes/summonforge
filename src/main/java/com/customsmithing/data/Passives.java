package com.customsmithing.data;

import java.util.List;

/** Cálculo (puro, sin Bukkit) de los nodos pasivos del árbol de habilidades según el nivel. */
public final class Passives {

    private Passives() {
    }

    /** Cuántos nodos ha alcanzado un jugador de ese nivel; los umbrales pueden venir desordenados. */
    public static int tiersReached(int level, List<Integer> thresholds) {
        int count = 0;
        for (int threshold : thresholds) {
            if (level >= threshold) count++;
        }
        return count;
    }

    /** Casillas extra de zona perfecta: una cada {@code every} nodos alcanzados (every <= 0 desactiva). */
    public static int extraPerfectSlots(int tiers, int every) {
        return every <= 0 || tiers <= 0 ? 0 : tiers / every;
    }

    /** Multiplicador de stats: 1 + nodos * bonus% (bonus negativo o nulo = sin efecto). */
    public static double multiplier(int tiers, double bonusPercentPerTier) {
        if (tiers <= 0 || bonusPercentPerTier <= 0) return 1.0;
        return 1.0 + tiers * bonusPercentPerTier / 100.0;
    }
}
