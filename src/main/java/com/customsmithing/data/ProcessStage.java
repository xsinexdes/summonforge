package com.customsmithing.data;

/**
 * Etapas del proceso de herrería. También se usa como "estado" de una pieza
 * intermedia guardado en su PDC (CALENTADO, GOLPEADO, ENFRIADO, TALLADO, TEJIDO),
 * en lugar de strings sueltos. Los nombres coinciden con los que ya guardaban
 * las piezas antiguas, así que los items existentes siguen funcionando.
 */
public enum ProcessStage {
    FUNDICION("Fundición", "🔩"),
    CALENTADO("Calentado", "🔥"),
    GOLPEADO("Golpeado", "🔨"),
    ENFRIADO("Enfriado", "🧊"),
    ENSAMBLAJE("Ensamblaje", "⚙"),
    REFINADO("Refinado", "✨"),
    TALLADO("Tallado", "🪵"),
    TEJIDO("Tejido", "🧵");

    private final String label;
    private final String icon;

    ProcessStage(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }

    /** Lee un nombre guardado (PDC o YAML); null si es desconocido o nulo. */
    public static ProcessStage fromName(String name) {
        if (name == null) return null;
        for (ProcessStage stage : values()) {
            if (stage.name().equals(name)) return stage;
        }
        return null;
    }
}
