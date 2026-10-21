package com.customsmithing.data;

/**
 * Niveles de calidad. Es un enum INMUTABLE: los multiplicadores y umbrales
 * viven en {@link QualityConfig} (cargada desde config.yml), no aquí.
 * Lógica pura (sin Bukkit).
 */
public enum Quality {
    COMUN("Común", 'f', "comun"),
    RARO("Raro", 'b', "raro"),
    EPICO("Épico", 'd', "epico"),
    LEGENDARIO("Legendario", '6', "legendario");

    private final String displayName;
    private final char colorCode;
    private final String configKey;

    Quality(String displayName, char colorCode, String configKey) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.configKey = configKey;
    }

    public String getDisplayName() { return displayName; }

    /** Código de color legacy (sin el símbolo de sección). */
    public char getColorCode() { return colorCode; }

    /** Sufijo usado en config.yml (multiplicador_raro, probabilidad_negar_dano_raro, ...). */
    public String getConfigKey() { return configKey; }

    public String formatted() {
        return "\u00a7" + colorCode + displayName;
    }

    /** Sube (+) o baja (-) niveles sin salirse de los extremos. */
    public Quality shift(int delta) {
        Quality[] all = values();
        int index = Math.max(0, Math.min(all.length - 1, ordinal() + delta));
        return all[index];
    }

    /** Lee el nombre guardado en un item; null si es desconocido o nulo. */
    public static Quality parse(String name) {
        if (name == null) return null;
        for (Quality q : values()) {
            if (q.name().equals(name)) return q;
        }
        return null;
    }
}
