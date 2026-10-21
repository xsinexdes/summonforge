package com.customsmithing.data;

public class ItemStats {

    private double critChance;      // 0-100
    private double lifesteal;       // 0-100 (% de daño devuelto como vida)
    private double attackSpeedBonus;// 0-100 (% conceptual, ver CombatListener)
    private String elementalType;   // FUEGO, HIELO, VENENO, RAYO, NINGUNO
    private double elementalDamage; // daño plano extra
    private double resistance;      // 0-100 resistencia especial
    private double bonusDamage;     // daño plano extra que aporta el material

    public ItemStats() {
        this.elementalType = "NINGUNO";
    }

    public double getCritChance() { return critChance; }
    public void setCritChance(double critChance) { this.critChance = critChance; }

    public double getLifesteal() { return lifesteal; }
    public void setLifesteal(double lifesteal) { this.lifesteal = lifesteal; }

    public double getAttackSpeedBonus() { return attackSpeedBonus; }
    public void setAttackSpeedBonus(double attackSpeedBonus) { this.attackSpeedBonus = attackSpeedBonus; }

    public String getElementalType() { return elementalType; }
    public void setElementalType(String elementalType) { this.elementalType = elementalType; }

    public double getElementalDamage() { return elementalDamage; }
    public void setElementalDamage(double elementalDamage) { this.elementalDamage = elementalDamage; }

    public double getBonusDamage() { return bonusDamage; }
    public void setBonusDamage(double bonusDamage) { this.bonusDamage = bonusDamage; }

    public double getResistance() { return resistance; }
    public void setResistance(double resistance) { this.resistance = resistance; }
}
