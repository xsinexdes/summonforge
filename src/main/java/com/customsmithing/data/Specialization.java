package com.customsmithing.data;

public enum Specialization {
    ESPADAS("Espadas", "Daño y velocidad de ataque"),
    ARMADURAS("Armaduras", "Defensa y durabilidad"),
    HERRAMIENTAS("Herramientas", "Eficiencia y velocidad"),
    FORJA_MAGICA("Forja Mágica", "Efectos especiales y stats ocultos");

    private final String label;
    private final String description;

    Specialization(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
}
