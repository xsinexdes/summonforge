package com.customsmithing.minerals;

import org.bukkit.Material;

public class CustomMineral {

    private final String id;
    private String displayName;
    private Material iconMaterial = Material.NETHERITE_INGOT;
    // Bonus plano que se suma a CUALQUIER objeto fabricado con este mineral,
    // ademas de las estadisticas normales por calidad/especializacion.
    private double bonusResistance = 5.0;
    private double bonusDamageFlat = 1.0;

    public CustomMineral(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Material getIconMaterial() { return iconMaterial; }
    public void setIconMaterial(Material iconMaterial) { this.iconMaterial = iconMaterial; }

    public double getBonusResistance() { return bonusResistance; }
    public void setBonusResistance(double bonusResistance) { this.bonusResistance = bonusResistance; }

    public double getBonusDamageFlat() { return bonusDamageFlat; }
    public void setBonusDamageFlat(double bonusDamageFlat) { this.bonusDamageFlat = bonusDamageFlat; }
}
