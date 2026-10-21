package com.customsmithing.bosses;

import org.bukkit.entity.EntityType;

import java.util.EnumSet;

public class BossDefinition {

    private final String id;
    private String displayName;
    private EntityType entityType = EntityType.WITHER_SKELETON;
    private double healthMultiplier = 3.0;
    private double damageMultiplier = 2.0;
    private double speedMultiplier = 1.2;
    private EnumSet<BossAbility> abilities = EnumSet.noneOf(BossAbility.class);

    private String dropMineralId = null;
    private int dropMineralAmount = 1;
    private double dropMineralChance = 100.0;

    private String dropBlueprintId = null;
    private double dropBlueprintChance = 25.0;

    private String structureId = null; // estructura donde debe aparecer

    public BossDefinition(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }

    public double getHealthMultiplier() { return healthMultiplier; }
    public void setHealthMultiplier(double healthMultiplier) { this.healthMultiplier = Math.max(0.1, healthMultiplier); }

    public double getDamageMultiplier() { return damageMultiplier; }
    public void setDamageMultiplier(double damageMultiplier) { this.damageMultiplier = Math.max(0.1, damageMultiplier); }

    public double getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(double speedMultiplier) { this.speedMultiplier = Math.max(0.1, speedMultiplier); }

    public EnumSet<BossAbility> getAbilities() { return abilities; }
    public void setAbilities(EnumSet<BossAbility> abilities) { this.abilities = abilities; }

    public String getDropMineralId() { return dropMineralId; }
    public void setDropMineralId(String dropMineralId) { this.dropMineralId = dropMineralId; }

    public int getDropMineralAmount() { return dropMineralAmount; }
    public void setDropMineralAmount(int dropMineralAmount) { this.dropMineralAmount = Math.max(1, dropMineralAmount); }

    public double getDropMineralChance() { return dropMineralChance; }
    public void setDropMineralChance(double dropMineralChance) { this.dropMineralChance = Math.max(0, Math.min(100, dropMineralChance)); }

    public String getDropBlueprintId() { return dropBlueprintId; }
    public void setDropBlueprintId(String dropBlueprintId) { this.dropBlueprintId = dropBlueprintId; }

    public double getDropBlueprintChance() { return dropBlueprintChance; }
    public void setDropBlueprintChance(double dropBlueprintChance) { this.dropBlueprintChance = Math.max(0, Math.min(100, dropBlueprintChance)); }

    public String getStructureId() { return structureId; }
    public void setStructureId(String structureId) { this.structureId = structureId; }
}
