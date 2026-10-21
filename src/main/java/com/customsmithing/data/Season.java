package com.customsmithing.data;

public enum Season {
    PRIMAVERA("Primavera", "🌸", "primavera"),
    VERANO("Verano", "☀", "verano"),
    OTONO("Otoño", "🍂", "otono"),
    INVIERNO("Invierno", "❄", "invierno");

    private final String label;
    private final String icon;
    private final String configKey;

    Season(String label, String icon, String configKey) {
        this.label = label;
        this.icon = icon;
        this.configKey = configKey;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }
    public String getConfigKey() { return configKey; }

    public Season next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
