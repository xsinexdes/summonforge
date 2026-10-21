package com.customsmithing.data;

import org.bukkit.Material;

public enum MoldType {
    PICO("Pico", Material.WOODEN_PICKAXE),
    HACHA("Hacha", Material.WOODEN_AXE),
    ESPADA("Espada", Material.WOODEN_SWORD),
    PALA("Pala", Material.WOODEN_SHOVEL),
    AZADA("Azada", Material.WOODEN_HOE),
    CASCO("Casco", Material.LEATHER_HELMET),
    PETO("Peto", Material.LEATHER_CHESTPLATE),
    PANTALON("Pantalón", Material.LEATHER_LEGGINGS),
    BOTAS("Botas", Material.LEATHER_BOOTS);

    private final String label;
    private final Material moldIcon;

    MoldType(String label, Material moldIcon) {
        this.label = label;
        this.moldIcon = moldIcon;
    }

    public String getLabel() { return label; }
    public Material getMoldIcon() { return moldIcon; }
}
