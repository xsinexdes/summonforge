package com.customsmithing.data;

import java.util.function.Consumer;
import java.util.function.ToDoubleBiFunction;

/**
 * Configuración INMUTABLE de calidad (umbrales y multiplicadores). Se reconstruye
 * entera en cada recarga: nada global se muta (antes {@code Quality.setMultiplier}).
 * Lógica pura (sin Bukkit): se carga con una función (ruta, valor por defecto).
 */
public final class QualityConfig {

    private static final double DEFAULT_RARO = 40;
    private static final double DEFAULT_EPICO = 65;
    private static final double DEFAULT_LEGENDARIO = 85;
    private static final double[] DEFAULT_MULTIPLIERS = {1.0, 1.15, 1.35, 1.6};
    private static final double MAX_MULTIPLIER = 10.0;

    private final double umbralRaro;
    private final double umbralEpico;
    private final double umbralLegendario;
    private final double[] multipliers;

    private QualityConfig(double raro, double epico, double legendario, double[] multipliers) {
        this.umbralRaro = raro;
        this.umbralEpico = epico;
        this.umbralLegendario = legendario;
        this.multipliers = multipliers.clone();
    }

    public static QualityConfig defaults() {
        return new QualityConfig(DEFAULT_RARO, DEFAULT_EPICO, DEFAULT_LEGENDARIO, DEFAULT_MULTIPLIERS);
    }

    /**
     * @param getter lee un número de config.yml: (ruta, valorPorDefecto) -> valor
     * @param warn   recibe un aviso legible por cada valor inválido (se usa el valor por defecto)
     */
    public static QualityConfig load(ToDoubleBiFunction<String, Double> getter, Consumer<String> warn) {
        double raro = getter.applyAsDouble("calidad.umbral_raro", DEFAULT_RARO);
        double epico = getter.applyAsDouble("calidad.umbral_epico", DEFAULT_EPICO);
        double legendario = getter.applyAsDouble("calidad.umbral_legendario", DEFAULT_LEGENDARIO);

        boolean inRange = isPercent(raro) && isPercent(epico) && isPercent(legendario);
        boolean ordered = raro <= epico && epico <= legendario;
        if (!inRange || !ordered) {
            warn.accept("calidad.umbral_*: deben estar entre 0 y 100 y cumplir raro <= epico <= legendario "
                    + "(valores leídos: " + raro + ", " + epico + ", " + legendario + "). Se usan los valores por defecto.");
            raro = DEFAULT_RARO;
            epico = DEFAULT_EPICO;
            legendario = DEFAULT_LEGENDARIO;
        }

        Quality[] all = Quality.values();
        double[] mult = new double[all.length];
        for (Quality q : all) {
            String path = "calidad.multiplicador_" + q.getConfigKey();
            double def = DEFAULT_MULTIPLIERS[q.ordinal()];
            double value = getter.applyAsDouble(path, def);
            if (Double.isNaN(value) || value <= 0 || value > MAX_MULTIPLIER) {
                warn.accept(path + ": debe ser mayor que 0 y como máximo " + MAX_MULTIPLIER
                        + " (leído: " + value + "). Se usa " + def + ".");
                value = def;
            }
            mult[q.ordinal()] = value;
        }
        return new QualityConfig(raro, epico, legendario, mult);
    }

    private static boolean isPercent(double v) {
        return !Double.isNaN(v) && v >= 0 && v <= 100;
    }

    public Quality tierFor(double score) {
        if (Double.isNaN(score)) return Quality.COMUN;
        if (score >= umbralLegendario) return Quality.LEGENDARIO;
        if (score >= umbralEpico) return Quality.EPICO;
        if (score >= umbralRaro) return Quality.RARO;
        return Quality.COMUN;
    }

    public double multiplier(Quality quality) {
        return multipliers[quality.ordinal()];
    }

    /** Factor para reescalar stats al cambiar de calidad (p. ej. al refinar). */
    public double ratio(Quality from, Quality to) {
        return multiplier(to) / multiplier(from);
    }

    public double getUmbralRaro() { return umbralRaro; }
    public double getUmbralEpico() { return umbralEpico; }
    public double getUmbralLegendario() { return umbralLegendario; }
}
